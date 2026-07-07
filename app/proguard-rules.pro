# ---- NewPipe extractor + Rhino (reflection/JS engine) ----
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-dontwarn org.mozilla.javascript.**
-dontwarn org.schabi.newpipe.extractor.**
-dontwarn javax.annotation.**

# ---- Keep source line numbers for readable crash reports ----
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# ---- kotlinx.serialization ----
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.luc4n3x.levyra.**$$serializer { *; }
-keepclassmembers class com.luc4n3x.levyra.** {
    *** Companion;
}
-keepclasseswithmembers class com.luc4n3x.levyra.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---- Domain models & data payloads (used via reflection/serialization) ----
-keep class com.luc4n3x.levyra.domain.** { *; }
-keep class com.luc4n3x.levyra.data.**Entity { *; }

# ---- Media3 / ExoPlayer ----
-dontwarn androidx.media3.**
-keep class androidx.media3.**  { *; }

# ---- OkHttp / Okio ----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---- Coil ----
-dontwarn coil3.**

# ---- Coroutines ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ---- Kotlin metadata / enums ----
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class **$WhenMappings { <fields>; }

# ---- Compose (BOM handles most; keep guard for safety) ----
-dontwarn androidx.compose.**

# ---- SLF4J (transitive, optional binder not shipped) ----
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.**
-keep class org.slf4j.** { *; }

# ---- Other optional transitive dependencies referenced but not shipped ----
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlin.reflect.**
-dontwarn com.google.errorprone.annotations.**
