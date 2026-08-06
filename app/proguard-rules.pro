# Keep Gson data models
-keep class com.videodownloader.app.data.model.** { *; }
-keep class com.videodownloader.app.data.config.** { *; }

# Keep OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep Coil Image Loader
-dontwarn io.coilkt.**

# Keep Compose
-keep class androidx.compose.** { *; }
