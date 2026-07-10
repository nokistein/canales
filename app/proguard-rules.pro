# ML Kit models are loaded via reflection/dynamic feature delivery; keep their entry points.
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
