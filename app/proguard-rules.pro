# Keep model classes used by Gson.
-keep class ru.konspekt.app.data.** { *; }
-keep class com.hrm.latex.** { *; }
-dontwarn org.jetbrains.skia.**

-keepclasseswithmembernames class * { native <methods>; }

# sherpa-onnx JNI
-keep class com.k2fsa.sherpa.onnx.** { *; }
-dontwarn com.k2fsa.sherpa.onnx.**
