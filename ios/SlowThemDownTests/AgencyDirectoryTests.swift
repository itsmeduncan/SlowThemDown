import Testing
@testable import SlowThemDown

@Suite("AgencyDirectory")
struct AgencyDirectoryTests {

    // MARK: - Matching

    @Test func matching_cityJurisdiction_matchesCityAndState() {
        let agencies = [
            makeAgency(jurisdiction: .city, city: "Portland", state: "OR"),
        ]
        let result = filter(agencies, city: "Portland", county: nil, state: "OR")
        #expect(result.count == 1)
    }

    @Test func matching_cityJurisdiction_doesNotMatchDifferentCity() {
        let agencies = [
            makeAgency(jurisdiction: .city, city: "Portland", state: "OR"),
        ]
        let result = filter(agencies, city: "Salem", county: nil, state: "OR")
        #expect(result.isEmpty)
    }

    @Test func matching_cityJurisdiction_doesNotMatchDifferentState() {
        let agencies = [
            makeAgency(jurisdiction: .city, city: "Portland", state: "OR"),
        ]
        let result = filter(agencies, city: "Portland", county: nil, state: "ME")
        #expect(result.isEmpty)
    }

    @Test func matching_countyJurisdiction_matchesCountyAndState() {
        let agencies = [
            makeAgency(jurisdiction: .county, county: "Multnomah", state: "OR"),
        ]
        let result = filter(agencies, city: nil, county: "Multnomah", state: "OR")
        #expect(result.count == 1)
    }

    @Test func matching_countyJurisdiction_doesNotMatchDifferentCounty() {
        let agencies = [
            makeAgency(jurisdiction: .county, county: "Multnomah", state: "OR"),
        ]
        let result = filter(agencies, city: nil, county: "Washington", state: "OR")
        #expect(result.isEmpty)
    }

    @Test func matching_stateJurisdiction_matchesAnyLocationInState() {
        let agencies = [
            makeAgency(jurisdiction: .state, state: "OR"),
        ]
        let result = filter(agencies, city: "Salem", county: "Marion", state: "OR")
        #expect(result.count == 1)
    }

    @Test func matching_regionalJurisdiction_matchesAnyLocationInState() {
        let agencies = [
            makeAgency(jurisdiction: .regional, state: "OR"),
        ]
        let result = filter(agencies, city: nil, county: nil, state: "OR")
        #expect(result.count == 1)
    }

    @Test func matching_nilState_returnsEmpty() {
        let agencies = [
            makeAgency(jurisdiction: .state, state: "OR"),
        ]
        let result = filter(agencies, city: nil, county: nil, state: nil)
        #expect(result.isEmpty)
    }

    @Test func matching_cityWithNilCity_doesNotMatch() {
        let agencies = [
            makeAgency(jurisdiction: .city, city: "Portland", state: "OR"),
        ]
        let result = filter(agencies, city: nil, county: nil, state: "OR")
        #expect(result.isEmpty)
    }

    @Test func matching_caseInsensitive() {
        let agencies = [
            makeAgency(jurisdiction: .city, city: "Portland", state: "OR"),
        ]
        let result = filter(agencies, city: "portland", county: nil, state: "OR")
        #expect(result.count == 1)
    }

    @Test func matching_multipleAgencies_returnsAllMatches() {
        let agencies = [
            makeAgency(name: "City DOT", jurisdiction: .city, city: "Portland", state: "OR"),
            makeAgency(name: "County DOT", jurisdiction: .county, county: "Multnomah", state: "OR"),
            makeAgency(name: "State DOT", jurisdiction: .state, state: "OR"),
            makeAgency(name: "Other State", jurisdiction: .state, state: "WA"),
        ]
        let result = filter(agencies, city: "Portland", county: "Multnomah", state: "OR")
        #expect(result.count == 3)
        #expect(result.contains { $0.name == "City DOT" })
        #expect(result.contains { $0.name == "County DOT" })
        #expect(result.contains { $0.name == "State DOT" })
    }

    @Test func matching_fullStateName_matchesAbbreviation() {
        let agencies = [
            makeAgency(jurisdiction: .city, city: "San Clemente", state: "CA"),
            makeAgency(jurisdiction: .state, state: "CA"),
        ]
        let result = filter(agencies, city: "San Clemente", county: "Orange", state: "California")
        #expect(result.count == 2)
    }

    @Test func matching_fullStateName_stateAgency() {
        let agencies = [
            makeAgency(jurisdiction: .state, state: "OR"),
        ]
        let result = filter(agencies, city: nil, county: nil, state: "Oregon")
        #expect(result.count == 1)
    }

    // MARK: - Helpers

    private func makeAgency(
        name: String = "Test Agency",
        jurisdiction: Agency.Jurisdiction = .city,
        city: String? = nil,
        county: String? = nil,
        state: String = "OR"
    ) -> Agency {
        Agency(
            name: name,
            email: "test@example.com",
            jurisdiction: jurisdiction,
            state: state,
            city: city,
            county: county,
            website: nil,
            notes: nil
        )
    }

    private func filter(
        _ agencies: [Agency],
        city: String?,
        county: String?,
        state: String?
    ) -> [Agency] {
        // Mirror AgencyDirectory.matching logic
        AgencyDirectory.matching(city: city, county: county, state: state, from: agencies)
    }
}
