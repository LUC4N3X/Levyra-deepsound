-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }

# Keep only the NewPipe pieces that rely on stable generated/runtime classes.
-keep class org.schabi.newpipe.extractor.services.youtube.protos.** { *; }
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }

# WebView calls these methods by name from JavaScript.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface public <methods>;
}

-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-dontwarn org.mozilla.javascript.**
-dontwarn org.schabi.newpipe.extractor.**
-dontwarn javax.annotation.**

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
