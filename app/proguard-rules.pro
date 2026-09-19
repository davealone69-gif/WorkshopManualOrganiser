# Workshop Manual Organiser — R8 / ProGuard rules

# Keep source file/line information OUT of release stack traces.
-renamesourcefileattribute SourceFile
-keepattributes !SourceFile,!LineNumberTable

# OkHttp (consumer rules are bundled, but keep these for safety).
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# Keep enum valueOf/values used reflectively by Compose runtime state.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
