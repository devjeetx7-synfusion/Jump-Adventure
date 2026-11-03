# Jump Adventure — R8 / ProGuard rules.
#
# Release builds currently run with `isMinifyEnabled = false`, but AGP requires
# this file to exist because `proguardFiles(...)` references it. The rules below
# keep the build correct if minification is enabled later.

# Keep the custom View subclasses reachable from XML inflation. These are
# referenced by fully-qualified class name in layout resources, so R8 must not
# rename or strip their (Context, AttributeSet) constructors.
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep the Application subclass referenced from AndroidManifest.xml.
-keep class com.jumpadventure.game.JumpAdventureApplication { *; }

# Keep Activities and the crash-report extras, referenced from the manifest
# and from Intent extras that are read reflectively by the framework.
-keep class com.jumpadventure.game.MainActivity { *; }
-keep class com.jumpadventure.game.CrashActivity { *; }
-keepclassmembers class com.jumpadventure.game.CrashActivity {
    public static final java.lang.String EXTRA_*;
}

# Enum valueOf / values() are resolved by name in a few places.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kotlin metadata is not needed at runtime and only bloats the APK.
-dontwarn kotlin.Metadata
