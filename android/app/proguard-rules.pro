# Keep shared KMP classes
-keep class com.slowthemdown.shared.** { *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.**

# Room — keep entities and DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * implements androidx.room.RoomDatabase$Callback { *; }

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Firebase Crashlytics — preserve source file names and line numbers
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Coil
-dontwarn coil.**
-keep class coil.** { *; }

# Play Services Location
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# Keep Compose runtime
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin metadata for reflection
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
