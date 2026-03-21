package com.slowthemdown.android.data

import android.content.Context
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
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun matching(city: String?, county: String?, state: String?): List<Agency> {
        if (state == null) return emptyList()
        return agencies.filter { agency ->
            if (!agency.state.equals(state, ignoreCase = true)) return@filter false
            when (agency.jurisdiction) {
                "city" -> city != null && agency.city.equals(city, ignoreCase = true)
                "county" -> county != null && agency.county.equals(county, ignoreCase = true)
                "state", "regional" -> true
                else -> false
            }
        }
    }
}
