-keepclasseswithmembernames,includedescriptorclasses class androidx.media3.decoder.ffmpeg.** {
    native <methods>;
}

-keep,includedescriptorclasses class androidx.media3.decoder.ffmpeg.FfmpegAudioDecoder {
    private java.nio.ByteBuffer growOutputBuffer(androidx.media3.decoder.SimpleDecoderOutputBuffer, int);
}

-keep class androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer {
    <init>(android.os.Handler, androidx.media3.exoplayer.audio.AudioRendererEventListener, androidx.media3.exoplayer.audio.AudioSink);
}

-keepclasseswithmembernames,includedescriptorclasses class com.luc4n3x.levyra.nativeaudio.OboeNative {
    native <methods>;
}
