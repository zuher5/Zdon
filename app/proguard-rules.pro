# Zdon release ProGuard/R8 rules.

# --- Kotlin / Coroutines ---------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# --- Room -----------------------------------------------------------------
# Room generates implementations referenced only by name at runtime.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger --------------------------------------------------------
-dontwarn dagger.hilt.**

# --- WorkManager ----------------------------------------------------------
# Workers are instantiated reflectively by class name.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# --- youtubedl-android ----------------------------------------------------
# The library maps yt-dlp JSON onto these classes with Jackson, which resolves
# fields reflectively, so their names must survive shrinking.
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.youtubedl_common.** { *; }
-keep class com.yausername.ffmpeg.** { *; }

# --- Jackson (pulled in by youtubedl-android) -----------------------------
-keep class com.fasterxml.jackson.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn com.fasterxml.jackson.databind.**
-dontwarn java.beans.**
-dontwarn org.w3c.dom.bootstrap.DOMImplementationRegistry

# --- Apache commons-io (pulled in by youtubedl-android) -------------------
-dontwarn org.apache.commons.io.**
-keep class org.apache.commons.io.** { *; }

# --- Apache commons-compress (pulled in by youtubedl-android) -------------
# ZipUtils uses commons-compress reflectively for Python extraction
-keep class org.apache.commons.compress.** { *; }
-keepclassmembers class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**

# --- Timber ---------------------------------------------------------------
-dontwarn org.jetbrains.annotations.**

# --- Zdon domain models ---------------------------------------------------
# Enum names are persisted in Room and DataStore; keep valueOf/values intact.
-keepclassmembers enum com.zdon.core.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
