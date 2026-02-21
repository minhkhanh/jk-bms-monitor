# SDK and basic keep rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep UniFFI bindings and auto-generated code
-keep class uniffi.** { *; }
-keep interface uniffi.** { *; }
-keepclassmembers class uniffi.** { *; }

# Keep AndroidX Glance (Widgets)
-keep class androidx.glance.** { *; }
-keep interface androidx.glance.** { *; }
-keepclassmembers class androidx.glance.** { *; }

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
