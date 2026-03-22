package com.slowthemdown.android.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class LocationInfo(
    val street: String,
    val city: String,
    val county: String,
    val state: String,
)

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        val cts = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location -> cont.resume(location) }
            .addOnFailureListener { cont.resume(null) }
        cont.invokeOnCancellation { cts.cancel() }
    }

    suspend fun getStreetName(location: Location): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val mainStreet = geocodeStreet(geocoder, location.latitude, location.longitude)
                ?: return@withContext ""

            // Try offset points (~40m in each cardinal direction) to find a cross street
            val offsetDeg = 0.00036
            val offsets = listOf(
                offsetDeg to 0.0, -offsetDeg to 0.0,
                0.0 to offsetDeg, 0.0 to -offsetDeg,
            )
            for ((latOff, lonOff) in offsets) {
                val crossStreet = geocodeStreet(
                    geocoder,
                    location.latitude + latOff,
                    location.longitude + lonOff,
                )
                if (crossStreet != null && crossStreet != mainStreet) {
                    return@withContext "$mainStreet & $crossStreet"
                }
            }

            mainStreet
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            ""
        }
    }

    suspend fun getLocationInfo(location: Location): LocationInfo? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val address = geocodeAddress(geocoder, location.latitude, location.longitude)
                ?: return@withContext null
            LocationInfo(
                street = address.thoroughfare ?: "",
                city = address.locality ?: "",
                county = address.subAdminArea ?: "",
                state = address.adminArea ?: "",
            )
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            null
        }
    }

    private suspend fun geocodeStreet(geocoder: Geocoder, lat: Double, lon: Double): String? {
        return geocodeAddress(geocoder, lat, lon)?.thoroughfare
    }

    private suspend fun geocodeAddress(geocoder: Geocoder, lat: Double, lon: Double): android.location.Address? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    cont.resume(addresses.firstOrNull())
                }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
        }
    }
}
