package com.slowthemdown.android.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
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
            @Suppress("DEPRECATION")
            val mainStreet = geocoder
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()?.thoroughfare ?: return@withContext ""

            // Try offset points (~40m in each cardinal direction) to find a cross street
            val offsetDeg = 0.00036
            val offsets = listOf(
                offsetDeg to 0.0, -offsetDeg to 0.0,
                0.0 to offsetDeg, 0.0 to -offsetDeg,
            )
            for ((latOff, lonOff) in offsets) {
                @Suppress("DEPRECATION")
                val crossStreet = geocoder
                    .getFromLocation(location.latitude + latOff, location.longitude + lonOff, 1)
                    ?.firstOrNull()?.thoroughfare
                if (crossStreet != null && crossStreet != mainStreet) {
                    return@withContext "$mainStreet & $crossStreet"
                }
            }

            mainStreet
        } catch (_: Exception) {
            ""
        }
    }
}
