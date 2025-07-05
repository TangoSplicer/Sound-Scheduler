# Keep Google Play Billing
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

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