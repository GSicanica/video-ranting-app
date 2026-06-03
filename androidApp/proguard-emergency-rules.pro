# Emergency ProGuard Rules
# Dodaj u androidApp/proguard-rules.pro ako se i dalje ruši

# === EMERGENCY: Keep Everything (za testiranje) ===
# UNCOMMENT ONLY IF DESPERATE - app će biti velik!
#-keep class com.youtube.rating.** { *; }
#-keepclassmembers class com.youtube.rating.** { *; }

# === Specifične Klase (ako vidiš da fale u crash report) ===

# Keep ViewModels
#-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Navigation destinations
#-keep class * extends androidx.navigation.NavDestination { *; }

# Keep Compose functions
#-keep class * extends androidx.compose.runtime.Composable { *; }

# Keep Coroutine Contexts
#-keep class * extends kotlin.coroutines.CoroutineContext { *; }

# Keep Sealed Classes
#-keep class * extends kotlin.sealed { *; }

# Keep all constructors
#-keepclassmembers class * {
#    public <init>(...);
#}

# Keep all getters/setters
#-keepclassmembers class * {
#    public *** get*();
#    public void set*(***);
#}

# === DEBUG: Disable Obfuscation (za testiranje) ===
# UNCOMMENT za vidjeti prave class names u crash reportu
#-dontobfuscate

# === DEBUG: Print Kept Classes ===
# UNCOMMENT da vidiš šta ProGuard zadržava
#-printseeds androidApp/build/outputs/mapping/release/seeds.txt
#-printusage androidApp/build/outputs/mapping/release/usage.txt
#-printmapping androidApp/build/outputs/mapping/release/mapping.txt
