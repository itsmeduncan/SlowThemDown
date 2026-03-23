plugins {
    kotlin("multiplatform") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("com.android.application") version "9.1.0" apply false
    id("com.android.kotlin.multiplatform.library") version "9.1.0" apply false
    id("com.google.dagger.hilt.android") version "2.59" apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.5" apply false
}
