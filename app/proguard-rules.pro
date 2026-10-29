# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# Keep Lifecycle annotations
-keepclassmembers class * {
    @androidx.lifecycle.* <methods>;
}

# Keep model classes
-keep class io.github.petsaudedigitalgat1.isttimer.model.** { *; }
-keep class io.github.petsaudedigitalgat1.isttimer.db.** { *; }
-keep class io.github.petsaudedigitalgat1.isttimer.viewmodel.** { *; }
-keep class io.github.petsaudedigitalgat1.isttimer.adapter.** { *; }

# Keep all public constructors
-keepclassmembers class * {
    public <init>(...);
}

# Preserve annotations
-keepattributes *Annotation*
