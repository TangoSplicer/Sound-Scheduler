# Room generates direct implementations at compile time; retain database metadata for safe runtime access.
-keep class com.soundscheduler.app.data.AppDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault,Signature
