package com.slowthemdown.android.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun impact(style: ImpactStyle = ImpactStyle.MEDIUM) {
        val duration = when (style) {
            ImpactStyle.LIGHT -> 20L
            ImpactStyle.MEDIUM -> 40L
            ImpactStyle.HEAVY -> 60L
        }
        vibrate(duration)
    }

    fun notification(type: NotificationType = NotificationType.SUCCESS) {
        val duration = when (type) {
            NotificationType.SUCCESS -> 30L
            NotificationType.WARNING -> 50L
            NotificationType.ERROR -> 80L
        }
        vibrate(duration)
    }

    fun selection() {
        vibrate(10L)
    }

    private fun vibrate(durationMs: Long) {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        }
    }

    enum class ImpactStyle { LIGHT, MEDIUM, HEAVY }
    enum class NotificationType { SUCCESS, WARNING, ERROR }
}
