import CoreLocation
import FirebaseCrashlytics
import Foundation

@Observable
final class LocationManager: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private let geocoder = CLGeocoder()

    var currentLocation: CLLocation?
    var streetName: String = ""
    var city: String = ""
    var county: String = ""
    var administrativeState: String = ""
    var authorizationStatus: CLAuthorizationStatus = .notDetermined

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
    }

    func requestPermission() {
        manager.requestWhenInUseAuthorization()
    }

    func startUpdating() {
        manager.startUpdatingLocation()
    }

    func stopUpdating() {
        manager.stopUpdatingLocation()
    }

    func reverseGeocode() async {
        guard let location = currentLocation else { return }
        do {
            let placemarks = try await geocoder.reverseGeocodeLocation(location)
            guard let placemark = placemarks.first,
                  let mainStreet = placemark.thoroughfare else {
                streetName = ""
                return
            }

            city = placemark.locality ?? ""
            county = placemark.subAdministrativeArea ?? ""
            administrativeState = placemark.administrativeArea ?? ""

            // Try offset points (~40m in each cardinal direction) to find a cross street
            let offsetDeg = 0.00036 // ~40m
            let offsets: [(Double, Double)] = [
                (offsetDeg, 0), (-offsetDeg, 0),
                (0, offsetDeg), (0, -offsetDeg),
            ]
            for (latOff, lonOff) in offsets {
                let offsetLocation = CLLocation(
                    latitude: location.coordinate.latitude + latOff,
                    longitude: location.coordinate.longitude + lonOff
                )
                if let crossStreet = try? await geocoder.reverseGeocodeLocation(offsetLocation)
                    .first?.thoroughfare,
                    crossStreet != mainStreet {
                    streetName = "\(mainStreet) & \(crossStreet)"
                    return
                }
            }

            streetName = mainStreet
        } catch {
            Crashlytics.crashlytics().record(error: error)
            streetName = ""
        }
    }

    // MARK: - CLLocationManagerDelegate

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        currentLocation = locations.last
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
        if authorizationStatus == .authorizedWhenInUse || authorizationStatus == .authorizedAlways {
            manager.startUpdatingLocation()
        }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Crashlytics.crashlytics().record(error: error)
    }
}
