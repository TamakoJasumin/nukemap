# osmdroid
-dontwarn org.osmdroid.**
-keep class org.osmdroid.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mirvsim.app.model.**$$serializer { *; }
-keepclassmembers class com.mirvsim.app.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.mirvsim.app.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
