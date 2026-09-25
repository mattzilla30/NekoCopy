# Keep class and member names readable in crash logs. R8 still shrinks and optimizes.
-dontobfuscate

-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

##---------------Injekt ----------
# Injekt keys every dependency by the generic type captured in an anonymous FullTypeReference
# subclass, so those subclasses must keep their generic signatures.
-keep,allowoptimization class uy.kohesive.injekt.** { public protected *; }
-keep,allowoptimization class * extends uy.kohesive.injekt.api.FullTypeReference
-keep,allowoptimization class * extends uy.kohesive.injekt.api.TypeReference

##---------------androidx.window ----------
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**

##---------------kotlinx.serialization ----------
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class org.nekomanga.**$$serializer { *; }
-keepclassmembers class org.nekomanga.** {
    *** Companion;
}
-keepclasseswithmembers class org.nekomanga.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class eu.kanade.**$$serializer { *; }
-keepclassmembers class eu.kanade.** {
    *** Companion;
}
-keepclasseswithmembers class eu.kanade.** {
    kotlinx.serialization.KSerializer serializer(...);
}

##---------------Coil ----------
-keep class * extends coil3.util.DecoderServiceLoaderTarget { *; }
-keep class * extends coil3.util.FetcherServiceLoaderTarget { *; }

##---------------Warnings for optional dependencies ----------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.**
