import Foundation
import Testing
@testable import SlowThemDown

private class BundleToken {}

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

    // MARK: - County normalization

    @Test func matching_countyWithSuffix_matchesWithoutSuffix() {
        let agencies = [
            makeAgency(jurisdiction: .county, county: "Orange", state: "CA"),
        ]
        let result = filter(agencies, city: nil, county: "Orange County", state: "CA")
        #expect(result.count == 1)
    }

    @Test func matching_countyWithoutSuffix_matchesWithSuffix() {
        let agencies = [
            makeAgency(jurisdiction: .county, county: "Orange County", state: "CA"),
        ]
        let result = filter(agencies, city: nil, county: "Orange", state: "CA")
        #expect(result.count == 1)
    }

    @Test func matching_countyParish_matchesWithoutSuffix() {
        let agencies = [
            makeAgency(jurisdiction: .county, county: "Orleans", state: "LA"),
        ]
        let result = filter(agencies, city: nil, county: "Orleans Parish", state: "LA")
        #expect(result.count == 1)
    }

    // MARK: - City normalization

    @Test func matching_cityWithPrefix_matchesWithoutPrefix() {
        let agencies = [
            makeAgency(jurisdiction: .city, city: "San Clemente", state: "CA"),
        ]
        let result = filter(agencies, city: "City of San Clemente", county: nil, state: "CA")
        #expect(result.count == 1)
    }

    @Test func matching_cityWithoutPrefix_matchesWithPrefix() {
        let agencies = [
            makeAgency(jurisdiction: .city, city: "City of San Clemente", state: "CA"),
        ]
        let result = filter(agencies, city: "San Clemente", county: nil, state: "CA")
        #expect(result.count == 1)
    }

    // MARK: - Realistic geocoder scenarios

    @Test func matching_sanClemente_returnsAllThreeTiers() {
        // Simulates CLGeocoder output for San Clemente, CA
        let agencies = [
            makeAgency(name: "City PW", jurisdiction: .city, city: "San Clemente", county: "Orange", state: "CA"),
            makeAgency(name: "County PW", jurisdiction: .county, county: "Orange", state: "CA"),
            makeAgency(name: "State DOT", jurisdiction: .state, state: "CA"),
            makeAgency(name: "Other City", jurisdiction: .city, city: "Dana Point", state: "CA"),
        ]
        let result = filter(agencies, city: "San Clemente", county: "Orange County", state: "California")
        #expect(result.count == 3)
        #expect(result.contains { $0.name == "City PW" })
        #expect(result.contains { $0.name == "County PW" })
        #expect(result.contains { $0.name == "State DOT" })
    }

    @Test func matching_stateAbbreviationInput_matchesAbbreviation() {
        // Geocoder may return "CA" instead of "California"
        let agencies = [
            makeAgency(jurisdiction: .state, state: "CA"),
        ]
        let result = filter(agencies, city: nil, county: nil, state: "CA")
        #expect(result.count == 1)
    }

    @Test func matching_cupertino_returnsOnlyStateLevel() {
        // Simulates the simulator default location — no city/county agency for Cupertino
        let agencies = [
            makeAgency(name: "San Clemente PW", jurisdiction: .city, city: "San Clemente", state: "CA"),
            makeAgency(name: "OC PW", jurisdiction: .county, county: "Orange", state: "CA"),
            makeAgency(name: "Caltrans", jurisdiction: .state, state: "CA"),
        ]
        let result = filter(agencies, city: "Cupertino", county: "Santa Clara County", state: "California")
        #expect(result.count == 1)
        #expect(result.first?.name == "Caltrans")
    }

    // MARK: - Empty and malformed inputs

    @Test func matching_emptyStringCity_doesNotMatchCityAgency() {
        let agencies = [
            makeAgency(jurisdiction: .city, city: "Portland", state: "OR"),
        ]
        let result = filter(agencies, city: "", county: nil, state: "OR")
        #expect(result.isEmpty)
    }

    @Test func matching_emptyStringCounty_doesNotMatchCountyAgency() {
        let agencies = [
            makeAgency(jurisdiction: .county, county: "Multnomah", state: "OR"),
        ]
        let result = filter(agencies, city: nil, county: "", state: "OR")
        #expect(result.isEmpty)
    }

    @Test func matching_emptyAgencyList_returnsEmpty() {
        let result = filter([], city: "Portland", county: "Multnomah", state: "OR")
        #expect(result.isEmpty)
    }

    @Test func matching_cityAgencyWithNilCity_doesNotMatch() {
        let agencies = [
            makeAgency(jurisdiction: .city, city: nil, state: "OR"),
        ]
        let result = filter(agencies, city: "Portland", county: nil, state: "OR")
        #expect(result.isEmpty)
    }

    @Test func matching_countyAgencyWithNilCounty_doesNotMatch() {
        let agencies = [
            makeAgency(jurisdiction: .county, county: nil, state: "OR"),
        ]
        let result = filter(agencies, city: nil, county: "Multnomah", state: "OR")
        #expect(result.isEmpty)
    }

    @Test func matching_whitespaceInCity_stillMatches() {
        let agencies = [
            makeAgency(jurisdiction: .city, city: "San Clemente", state: "CA"),
        ]
        let result = filter(agencies, city: " San Clemente ", county: nil, state: "CA")
        #expect(result.count == 1)
    }

    @Test func matching_whitespaceInCounty_stillMatches() {
        let agencies = [
            makeAgency(jurisdiction: .county, county: "Orange", state: "CA"),
        ]
        let result = filter(agencies, city: nil, county: " Orange County ", state: "CA")
        #expect(result.count == 1)
    }

    // MARK: - JSON parsing

    @Test func load_parsesAgenciesFromJSON() throws {
        let url = try #require(Bundle(for: BundleToken.self).url(forResource: "agencies", withExtension: "json")
            ?? Bundle.main.url(forResource: "agencies", withExtension: "json"))
        let data = try Data(contentsOf: url)
        let agencies = try JSONDecoder().decode([Agency].self, from: data)
        #expect(!agencies.isEmpty)
        #expect(agencies.contains { $0.name == "California Department of Transportation" })
    }

    @Test func load_sanClementeAgencyExists() throws {
        let url = try #require(Bundle(for: BundleToken.self).url(forResource: "agencies", withExtension: "json")
            ?? Bundle.main.url(forResource: "agencies", withExtension: "json"))
        let data = try Data(contentsOf: url)
        let agencies = try JSONDecoder().decode([Agency].self, from: data)
        #expect(agencies.contains { $0.city == "San Clemente" && $0.state == "CA" })
    }

    @Test func load_allAgenciesHaveRequiredFields() throws {
        let url = try #require(Bundle(for: BundleToken.self).url(forResource: "agencies", withExtension: "json")
            ?? Bundle.main.url(forResource: "agencies", withExtension: "json"))
        let data = try Data(contentsOf: url)
        let agencies = try JSONDecoder().decode([Agency].self, from: data)
        for agency in agencies {
            #expect(!agency.name.isEmpty)
            #expect(!agency.email.isEmpty)
            #expect(!agency.state.isEmpty)
        }
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
