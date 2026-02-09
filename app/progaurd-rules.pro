# Keep Room database entities
-keep class com.soundscheduler.app.data.** { *; }
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# Keep Gson models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepattributes Signature

# General Android rules
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.appwidget.AppWidgetProvider
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Retain annotations
-keepattributes *Annotation*

# Optimize for size
-optimizations !code/simplification/arithmetic

# Keep Location Services
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.location.**

# Keep Biometric classes
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# Keep data classes
-keepclassmembers class com.soundscheduler.app.data.** { *; }
