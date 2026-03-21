import Foundation

struct Agency: Codable, Identifiable {
    var id: String { email }
    let name: String
    let email: String
    let jurisdiction: Jurisdiction
    let state: String
    let city: String?
    let county: String?
    let website: String?
    let notes: String?

    enum Jurisdiction: String, Codable {
        case city
        case county
        case state
        case regional
    }
}

enum AgencyDirectory {
    static func load() -> [Agency] {
        guard let url = Bundle.main.url(forResource: "agencies", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let agencies = try? JSONDecoder().decode([Agency].self, from: data) else {
            return []
        }
        return agencies
    }

    static func matching(city: String?, county: String?, state: String?) -> [Agency] {
        guard let state else { return [] }
        let all = load()
        return all.filter { agency in
            guard agency.state == state else { return false }
            switch agency.jurisdiction {
            case .city:
                guard let city else { return false }
                return agency.city?.localizedCaseInsensitiveCompare(city) == .orderedSame
            case .county:
                guard let county else { return false }
                return agency.county?.localizedCaseInsensitiveCompare(county) == .orderedSame
            case .state, .regional:
                return true
            }
        }
    }
}
