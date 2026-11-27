# Preserve all app code, annotations, and attributes for safe shrinking
-dontobfuscate
-dontoptimize
-keep class com.example.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn **
