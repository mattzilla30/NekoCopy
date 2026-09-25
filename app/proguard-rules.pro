# Keep class and member names readable in crash logs. R8 still shrinks and optimizes.
-dontobfuscate

-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

##---------------Injekt ----------
# Injekt keys every dependency by the generic type captured in an anonymous FullTypeReference
# subclass, so those subclasses must keep their generic signatures.
-keep,allowoptimization class uy.kohesive.injekt.** { public protected *; }
-keep,allowoptimization class * extends uy.kohesive.injekt.api.FullTypeReference
-keep,allowoptimization class * extends uy.kohesive.injekt.api.TypeReference

##---------------Retrofit ----------
# Retrofit reads a @QueryMap's key and value types from the class's generic superclass. R8 full
# mode strips generic signatures from classes no rule keeps, which leaves HashMap<K, V>.
-keep,allowobfuscation,allowshrinking class org.nekomanga.core.network.ProxyRetrofitQueryMap

# Keep generic signatures on all app classes, as R8's compatibility mode would, so other reflection
# on generic types keeps working. Unused classes are still removed and the rest still optimized.
-keep,allowobfuscation,allowshrinking,allowoptimization class org.nekomanga.**
-keep,allowobfuscation,allowshrinking,allowoptimization class eu.kanade.**
-keep,allowobfuscation,allowshrinking,allowoptimization class tachiyomi.**

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
