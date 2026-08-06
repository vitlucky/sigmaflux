# SigmaFlux ProGuard rules (release)
# kotlinx.serialization keeps
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class com.sigmaflux.market.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.sigmaflux.market.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions

# Glance
-dontwarn androidx.glance.**
