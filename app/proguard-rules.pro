# Room (entités/DAO)
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class com.etix.model.** { *; }
-keepattributes *Annotation*

# Navigation Safe Args (si minifyEnabled true)
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}