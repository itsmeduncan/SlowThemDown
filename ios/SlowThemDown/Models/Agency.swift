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

    static func matching(city: String?, county: String?, state: String?, from agencies: [Agency]? = nil) -> [Agency] {
        guard let state else { return [] }
        let normalizedState = stateAbbreviation(state) ?? state
        let all = agencies ?? load()
        return all.filter { agency in
            guard agency.state.caseInsensitiveCompare(normalizedState) == .orderedSame
                    || agency.state.caseInsensitiveCompare(state) == .orderedSame else {
                return false
            }
            switch agency.jurisdiction {
            case .city:
                guard let city else { return false }
                let normalizedCity = normalizeCity(city)
                return agency.city.map { normalizeCity($0) == normalizedCity } ?? false
            case .county:
                guard let county else { return false }
                let normalizedCounty = normalizeCounty(county)
                return agency.county.map { normalizeCounty($0) == normalizedCounty } ?? false
            case .state, .regional:
                return true
            }
        }
    }

    /// Strip "City of" prefix geocoders sometimes add
    private static func normalizeCity(_ input: String) -> String {
        var result = input
        if result.lowercased().hasPrefix("city of ") {
            result = String(result.dropFirst(8))
        }
        return result.trimmingCharacters(in: .whitespaces).lowercased()
    }

    /// Strip common suffixes geocoders add (e.g., "Orange County" → "Orange")
    private static func normalizeCounty(_ input: String) -> String {
        input
            .replacingOccurrences(of: " County", with: "", options: .caseInsensitive)
            .replacingOccurrences(of: " Parish", with: "", options: .caseInsensitive)
            .trimmingCharacters(in: .whitespaces)
            .lowercased()
    }

    private static func stateAbbreviation(_ input: String) -> String? {
        // If already a 2-letter code, return as-is
        if input.count == 2 { return input.uppercased() }
        return stateNameToAbbr[input.lowercased()]
    }

    private static let stateNameToAbbr: [String: String] = [
        "alabama": "AL", "alaska": "AK", "arizona": "AZ", "arkansas": "AR",
        "california": "CA", "colorado": "CO", "connecticut": "CT", "delaware": "DE",
        "florida": "FL", "georgia": "GA", "hawaii": "HI", "idaho": "ID",
        "illinois": "IL", "indiana": "IN", "iowa": "IA", "kansas": "KS",
        "kentucky": "KY", "louisiana": "LA", "maine": "ME", "maryland": "MD",
        "massachusetts": "MA", "michigan": "MI", "minnesota": "MN", "mississippi": "MS",
        "missouri": "MO", "montana": "MT", "nebraska": "NE", "nevada": "NV",
        "new hampshire": "NH", "new jersey": "NJ", "new mexico": "NM", "new york": "NY",
        "north carolina": "NC", "north dakota": "ND", "ohio": "OH", "oklahoma": "OK",
        "oregon": "OR", "pennsylvania": "PA", "rhode island": "RI", "south carolina": "SC",
        "south dakota": "SD", "tennessee": "TN", "texas": "TX", "utah": "UT",
        "vermont": "VT", "virginia": "VA", "washington": "WA", "west virginia": "WV",
        "wisconsin": "WI", "wyoming": "WY", "district of columbia": "DC",
    ]
}
