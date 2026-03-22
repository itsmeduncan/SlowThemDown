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
