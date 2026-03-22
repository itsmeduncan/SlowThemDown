package com.slowthemdown.android.data

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

data class Agency(
    val name: String,
    val email: String,
    val jurisdiction: String,
    val state: String,
    val city: String? = null,
    val county: String? = null,
    val website: String? = null,
    val notes: String? = null,
)

@Singleton
class AgencyDirectory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val agencies: List<Agency> by lazy { loadAgencies() }

    private fun loadAgencies(): List<Agency> {
        return try {
            val json = context.assets.open("agencies.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Agency(
                    name = obj.getString("name"),
                    email = obj.getString("email"),
                    jurisdiction = obj.getString("jurisdiction"),
                    state = obj.getString("state"),
                    city = obj.optString("city").ifEmpty { null },
                    county = obj.optString("county").ifEmpty { null },
                    website = obj.optString("website").ifEmpty { null },
                    notes = obj.optString("notes").ifEmpty { null },
                )
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            emptyList()
        }
    }

    fun matching(city: String?, county: String?, state: String?): List<Agency> =
        matchAgencies(agencies, city, county, state)

    companion object {
        /** Testable matching logic that works on any agency list */
        fun matchAgencies(
            agencies: List<Agency>,
            city: String?,
            county: String?,
            state: String?,
        ): List<Agency> {
            if (state == null) return emptyList()
            val normalizedState = stateAbbreviation(state) ?: state
            return agencies.filter { agency ->
                val stateMatch = agency.state.equals(normalizedState, ignoreCase = true)
                        || agency.state.equals(state, ignoreCase = true)
                if (!stateMatch) return@filter false
                when (agency.jurisdiction) {
                    "city" -> city != null && normalizeCity(agency.city ?: "") == normalizeCity(city)
                    "county" -> county != null && normalizeCounty(agency.county ?: "") == normalizeCounty(county)
                    "state", "regional" -> true
                    else -> false
                }
            }
        }

        /** Strip "City of" prefix geocoders sometimes add */
        private fun normalizeCity(input: String): String {
            var result = input
            if (result.startsWith("City of ", ignoreCase = true)) {
                result = result.drop(8)
            }
            return result.trim().lowercase()
        }

        /** Strip "County"/"Parish" suffix geocoders add (e.g., "Orange County" → "Orange") */
        private fun normalizeCounty(input: String): String {
            return input
                .replace(Regex("\\s+County$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s+Parish$", RegexOption.IGNORE_CASE), "")
                .trim()
                .lowercase()
        }

        private fun stateAbbreviation(input: String): String? {
            if (input.length == 2) return input.uppercase()
            return STATE_NAME_TO_ABBR[input.lowercase()]
        }

        private val STATE_NAME_TO_ABBR = mapOf(
            "alabama" to "AL", "alaska" to "AK", "arizona" to "AZ", "arkansas" to "AR",
            "california" to "CA", "colorado" to "CO", "connecticut" to "CT", "delaware" to "DE",
            "florida" to "FL", "georgia" to "GA", "hawaii" to "HI", "idaho" to "ID",
            "illinois" to "IL", "indiana" to "IN", "iowa" to "IA", "kansas" to "KS",
            "kentucky" to "KY", "louisiana" to "LA", "maine" to "ME", "maryland" to "MD",
            "massachusetts" to "MA", "michigan" to "MI", "minnesota" to "MN", "mississippi" to "MS",
            "missouri" to "MO", "montana" to "MT", "nebraska" to "NE", "nevada" to "NV",
            "new hampshire" to "NH", "new jersey" to "NJ", "new mexico" to "NM", "new york" to "NY",
            "north carolina" to "NC", "north dakota" to "ND", "ohio" to "OH", "oklahoma" to "OK",
            "oregon" to "OR", "pennsylvania" to "PA", "rhode island" to "RI", "south carolina" to "SC",
            "south dakota" to "SD", "tennessee" to "TN", "texas" to "TX", "utah" to "UT",
            "vermont" to "VT", "virginia" to "VA", "washington" to "WA", "west virginia" to "WV",
            "wisconsin" to "WI", "wyoming" to "WY", "district of columbia" to "DC",
        )
    }
}
