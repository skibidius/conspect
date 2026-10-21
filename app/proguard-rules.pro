# Keep model classes used by Gson.
-keep class ru.konspekt.app.data.** { *; }
-keep class com.hrm.latex.** { *; }
-dontwarn org.jetbrains.skia.**

# JNA is required by Vosk at runtime and breaks under R8 without these rules.
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn com.sun.jna.**
-keep class com.sun.jna.** { *; }
-keep class * extends com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public <methods>; }
-keepclasseswithmembernames class * { native <methods>; }
-keep class org.vosk.** { *; }
-dontwarn org.vosk.**
