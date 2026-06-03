# Consumer ProGuard rules for shared module
# These rules are automatically applied to apps that depend on this library

# Keep all public API
-keep public class com.youtube.rating.shared.** { public *; }

# Keep all data models (they're serialized)
-keep class com.youtube.rating.shared.models.** { *; }
-keepclassmembers class com.youtube.rating.shared.models.** { *; }

# Keep API client
-keep class com.youtube.rating.shared.api.** { *; }

# Keep all @Serializable classes from shared module
-keep @kotlinx.serialization.Serializable class com.youtube.rating.shared.** { *; }

# Keep serializers
-keep class com.youtube.rating.shared.**$$serializer { *; }
-keepclassmembers class com.youtube.rating.shared.** {
    *** Companion;
}
