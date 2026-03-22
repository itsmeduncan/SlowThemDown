package com.slowthemdown.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgencyDirectoryTest {

    @Test
    fun matching_cityJurisdiction_matchesCityAndState() {
        val agencies = listOf(makeAgency(jurisdiction = "city", city = "Portland", state = "OR"))
        val result = filterAgencies(agencies, city = "Portland", county = null, state = "OR")
        assertEquals(1, result.size)
    }

    @Test
    fun matching_cityJurisdiction_doesNotMatchDifferentCity() {
        val agencies = listOf(makeAgency(jurisdiction = "city", city = "Portland", state = "OR"))
        val result = filterAgencies(agencies, city = "Salem", county = null, state = "OR")
        assertTrue(result.isEmpty())
    }

    @Test
    fun matching_cityJurisdiction_doesNotMatchDifferentState() {
        val agencies = listOf(makeAgency(jurisdiction = "city", city = "Portland", state = "OR"))
        val result = filterAgencies(agencies, city = "Portland", county = null, state = "ME")
        assertTrue(result.isEmpty())
    }

    @Test
    fun matching_countyJurisdiction_matchesCountyAndState() {
        val agencies = listOf(makeAgency(jurisdiction = "county", county = "Multnomah", state = "OR"))
        val result = filterAgencies(agencies, city = null, county = "Multnomah", state = "OR")
        assertEquals(1, result.size)
    }

    @Test
    fun matching_countyJurisdiction_doesNotMatchDifferentCounty() {
        val agencies = listOf(makeAgency(jurisdiction = "county", county = "Multnomah", state = "OR"))
        val result = filterAgencies(agencies, city = null, county = "Washington", state = "OR")
        assertTrue(result.isEmpty())
    }

    @Test
    fun matching_stateJurisdiction_matchesAnyLocationInState() {
        val agencies = listOf(makeAgency(jurisdiction = "state", state = "OR"))
        val result = filterAgencies(agencies, city = "Salem", county = "Marion", state = "OR")
        assertEquals(1, result.size)
    }

    @Test
    fun matching_regionalJurisdiction_matchesAnyLocationInState() {
        val agencies = listOf(makeAgency(jurisdiction = "regional", state = "OR"))
        val result = filterAgencies(agencies, city = null, county = null, state = "OR")
        assertEquals(1, result.size)
    }

    @Test
    fun matching_nullState_returnsEmpty() {
        val agencies = listOf(makeAgency(jurisdiction = "state", state = "OR"))
        val result = filterAgencies(agencies, city = null, county = null, state = null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun matching_cityWithNullCity_doesNotMatch() {
        val agencies = listOf(makeAgency(jurisdiction = "city", city = "Portland", state = "OR"))
        val result = filterAgencies(agencies, city = null, county = null, state = "OR")
        assertTrue(result.isEmpty())
    }

    @Test
    fun matching_caseInsensitive() {
        val agencies = listOf(makeAgency(jurisdiction = "city", city = "Portland", state = "OR"))
        val result = filterAgencies(agencies, city = "portland", county = null, state = "OR")
        assertEquals(1, result.size)
    }

    @Test
    fun matching_multipleAgencies_returnsAllMatches() {
        val agencies = listOf(
            makeAgency(name = "City DOT", jurisdiction = "city", city = "Portland", state = "OR"),
            makeAgency(name = "County DOT", jurisdiction = "county", county = "Multnomah", state = "OR"),
            makeAgency(name = "State DOT", jurisdiction = "state", state = "OR"),
            makeAgency(name = "Other State", jurisdiction = "state", state = "WA"),
        )
        val result = filterAgencies(agencies, city = "Portland", county = "Multnomah", state = "OR")
        assertEquals(3, result.size)
        assertTrue(result.any { it.name == "City DOT" })
        assertTrue(result.any { it.name == "County DOT" })
        assertTrue(result.any { it.name == "State DOT" })
    }

    @Test
    fun matching_fullStateName_matchesAbbreviation() {
        val agencies = listOf(
            makeAgency(jurisdiction = "city", city = "San Clemente", state = "CA"),
            makeAgency(jurisdiction = "state", state = "CA"),
        )
        val result = filterAgencies(agencies, city = "San Clemente", county = "Orange", state = "California")
        assertEquals(2, result.size)
    }

    @Test
    fun matching_fullStateName_stateAgency() {
        val agencies = listOf(makeAgency(jurisdiction = "state", state = "OR"))
        val result = filterAgencies(agencies, city = null, county = null, state = "Oregon")
        assertEquals(1, result.size)
    }

    // County normalization

    @Test
    fun matching_countyWithSuffix_matchesWithoutSuffix() {
        val agencies = listOf(makeAgency(jurisdiction = "county", county = "Orange", state = "CA"))
        val result = filterAgencies(agencies, city = null, county = "Orange County", state = "CA")
        assertEquals(1, result.size)
    }

    @Test
    fun matching_countyWithoutSuffix_matchesWithSuffix() {
        val agencies = listOf(makeAgency(jurisdiction = "county", county = "Orange County", state = "CA"))
        val result = filterAgencies(agencies, city = null, county = "Orange", state = "CA")
        assertEquals(1, result.size)
    }

    @Test
    fun matching_countyParish_matchesWithoutSuffix() {
        val agencies = listOf(makeAgency(jurisdiction = "county", county = "Orleans", state = "LA"))
        val result = filterAgencies(agencies, city = null, county = "Orleans Parish", state = "LA")
        assertEquals(1, result.size)
    }

    // City normalization

    @Test
    fun matching_cityWithPrefix_matchesWithoutPrefix() {
        val agencies = listOf(makeAgency(jurisdiction = "city", city = "San Clemente", state = "CA"))
        val result = filterAgencies(agencies, city = "City of San Clemente", county = null, state = "CA")
        assertEquals(1, result.size)
    }

    @Test
    fun matching_cityWithoutPrefix_matchesWithPrefix() {
        val agencies = listOf(makeAgency(jurisdiction = "city", city = "City of San Clemente", state = "CA"))
        val result = filterAgencies(agencies, city = "San Clemente", county = null, state = "CA")
        assertEquals(1, result.size)
    }

    // Realistic geocoder scenarios

    @Test
    fun matching_sanClemente_returnsAllThreeTiers() {
        val agencies = listOf(
            makeAgency(name = "City PW", jurisdiction = "city", city = "San Clemente", county = "Orange", state = "CA"),
            makeAgency(name = "County PW", jurisdiction = "county", county = "Orange", state = "CA"),
            makeAgency(name = "State DOT", jurisdiction = "state", state = "CA"),
            makeAgency(name = "Other City", jurisdiction = "city", city = "Dana Point", state = "CA"),
        )
        val result = filterAgencies(agencies, city = "San Clemente", county = "Orange County", state = "California")
        assertEquals(3, result.size)
        assertTrue(result.any { it.name == "City PW" })
        assertTrue(result.any { it.name == "County PW" })
        assertTrue(result.any { it.name == "State DOT" })
    }

    @Test
    fun matching_stateAbbreviationInput_matchesAbbreviation() {
        val agencies = listOf(makeAgency(jurisdiction = "state", state = "CA"))
        val result = filterAgencies(agencies, city = null, county = null, state = "CA")
        assertEquals(1, result.size)
    }

    @Test
    fun matching_cupertino_returnsOnlyStateLevel() {
        val agencies = listOf(
            makeAgency(name = "San Clemente PW", jurisdiction = "city", city = "San Clemente", state = "CA"),
            makeAgency(name = "OC PW", jurisdiction = "county", county = "Orange", state = "CA"),
            makeAgency(name = "Caltrans", jurisdiction = "state", state = "CA"),
        )
        val result = filterAgencies(agencies, city = "Cupertino", county = "Santa Clara County", state = "California")
        assertEquals(1, result.size)
        assertEquals("Caltrans", result.first().name)
    }

    // Empty and malformed inputs

    @Test
    fun matching_emptyStringCity_doesNotMatchCityAgency() {
        val agencies = listOf(makeAgency(jurisdiction = "city", city = "Portland", state = "OR"))
        val result = filterAgencies(agencies, city = "", county = null, state = "OR")
        assertTrue(result.isEmpty())
    }

    @Test
    fun matching_emptyStringCounty_doesNotMatchCountyAgency() {
        val agencies = listOf(makeAgency(jurisdiction = "county", county = "Multnomah", state = "OR"))
        val result = filterAgencies(agencies, city = null, county = "", state = "OR")
        assertTrue(result.isEmpty())
    }

    @Test
    fun matching_emptyAgencyList_returnsEmpty() {
        val result = filterAgencies(emptyList(), city = "Portland", county = "Multnomah", state = "OR")
        assertTrue(result.isEmpty())
    }

    @Test
    fun matching_cityAgencyWithNullCity_doesNotMatch() {
        val agencies = listOf(makeAgency(jurisdiction = "city", city = null, state = "OR"))
        val result = filterAgencies(agencies, city = "Portland", county = null, state = "OR")
        assertTrue(result.isEmpty())
    }

    @Test
    fun matching_countyAgencyWithNullCounty_doesNotMatch() {
        val agencies = listOf(makeAgency(jurisdiction = "county", county = null, state = "OR"))
        val result = filterAgencies(agencies, city = null, county = "Multnomah", state = "OR")
        assertTrue(result.isEmpty())
    }

    @Test
    fun matching_whitespaceInCity_stillMatches() {
        val agencies = listOf(makeAgency(jurisdiction = "city", city = "San Clemente", state = "CA"))
        val result = filterAgencies(agencies, city = " San Clemente ", county = null, state = "CA")
        assertEquals(1, result.size)
    }

    @Test
    fun matching_whitespaceInCounty_stillMatches() {
        val agencies = listOf(makeAgency(jurisdiction = "county", county = "Orange", state = "CA"))
        val result = filterAgencies(agencies, city = null, county = " Orange County ", state = "CA")
        assertEquals(1, result.size)
    }

    private fun filterAgencies(
        agencies: List<Agency>,
        city: String?,
        county: String?,
        state: String?,
    ): List<Agency> = AgencyDirectory.matchAgencies(agencies, city, county, state)

    private fun makeAgency(
        name: String = "Test Agency",
        jurisdiction: String = "city",
        city: String? = null,
        county: String? = null,
        state: String = "OR",
    ) = Agency(
        name = name,
        email = "test@example.com",
        jurisdiction = jurisdiction,
        state = state,
        city = city,
        county = county,
    )
}
