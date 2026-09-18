#include <jni.h>

#include <memory>

#include "LevyraOboeOutput.h"

namespace {

using levyra::LevyraOboeOutput;
using levyra::OboeOutputParams;

std::shared_ptr<LevyraOboeOutput> *fromHandle(jlong handle) {
    return reinterpret_cast<std::shared_ptr<LevyraOboeOutput> *>(handle);
}

LevyraOboeOutput *outputOf(jlong handle) {
    auto *holder = fromHandle(handle);
    return holder == nullptr ? nullptr : holder->get();
}

constexpr jint kErrorClosed = static_cast<jint>(oboe::Result::ErrorClosed);

}  // namespace

#define OBOE_NATIVE_FUNC(RETURN_TYPE, NAME, ...) \
    extern "C" JNIEXPORT RETURN_TYPE JNICALL     \
    Java_com_luc4n3x_levyra_nativeaudio_OboeNative_##NAME([[maybe_unused]] JNIEnv *env, [[maybe_unused]] jclass clazz, ##__VA_ARGS__)

OBOE_NATIVE_FUNC(jlong, nativeOpen, jint sampleRate, jint channelCount, jint sessionId, jint deviceId,
                 jint ringCapacityFrames, jint usage, jint contentType, jintArray errorOut) {
    const OboeOutputParams params{sampleRate, channelCount, sessionId, deviceId, ringCapacityFrames, usage, contentType};
    int32_t error = 0;
    std::shared_ptr<LevyraOboeOutput> output = LevyraOboeOutput::open(params, &error);
    if (errorOut != nullptr && env->GetArrayLength(errorOut) > 0) {
        const jint value = static_cast<jint>(error);
        env->SetIntArrayRegion(errorOut, 0, 1, &value);
    }
    if (!output) {
        return 0;
    }
    return reinterpret_cast<jlong>(new std::shared_ptr<LevyraOboeOutput>(std::move(output)));
}

OBOE_NATIVE_FUNC(jint, nativeWrite, jlong handle, jobject buffer, jint position, jint size) {
    LevyraOboeOutput *output = outputOf(handle);
    if (output == nullptr) {
        return kErrorClosed;
    }
    auto *address = static_cast<uint8_t *>(env->GetDirectBufferAddress(buffer));
    const jlong capacity = env->GetDirectBufferCapacity(buffer);
    if (address == nullptr || position < 0 || size < 0 || static_cast<jlong>(position) + size > capacity) {
        return static_cast<jint>(oboe::Result::ErrorIllegalArgument);
    }
    return output->write(address + position, size);
}

OBOE_NATIVE_FUNC(jint, nativePlay, jlong handle) {
    LevyraOboeOutput *output = outputOf(handle);
    return output == nullptr ? kErrorClosed : output->play();
}

OBOE_NATIVE_FUNC(jint, nativePause, jlong handle) {
    LevyraOboeOutput *output = outputOf(handle);
    return output == nullptr ? kErrorClosed : output->pause();
}

OBOE_NATIVE_FUNC(jint, nativeFlush, jlong handle) {
    LevyraOboeOutput *output = outputOf(handle);
    return output == nullptr ? kErrorClosed : output->flush();
}

OBOE_NATIVE_FUNC(void, nativeStop, jlong handle) {
    LevyraOboeOutput *output = outputOf(handle);
    if (output != nullptr) {
        output->stop();
    }
}

OBOE_NATIVE_FUNC(void, nativeRelease, jlong handle) {
    auto *holder = fromHandle(handle);
    if (holder == nullptr) {
        return;
    }
    (*holder)->close();
    delete holder;
}

OBOE_NATIVE_FUNC(void, nativeSetVolume, jlong handle, jfloat volume) {
    LevyraOboeOutput *output = outputOf(handle);
    if (output != nullptr) {
        output->setVolume(volume);
    }
}

OBOE_NATIVE_FUNC(jlong, nativeGetPlayedFrames, jlong handle) {
    LevyraOboeOutput *output = outputOf(handle);
    return output == nullptr ? 0 : static_cast<jlong>(output->playedFrames());
}

OBOE_NATIVE_FUNC(jint, nativeGetUnderrunCount, jlong handle) {
    LevyraOboeOutput *output = outputOf(handle);
    return output == nullptr ? 0 : output->underrunCount();
}

OBOE_NATIVE_FUNC(jint, nativeGetErrorCode, jlong handle) {
    LevyraOboeOutput *output = outputOf(handle);
    return output == nullptr ? kErrorClosed : output->errorCode();
}

OBOE_NATIVE_FUNC(jint, nativeGetSessionId, jlong handle) {
    LevyraOboeOutput *output = outputOf(handle);
    return output == nullptr ? 0 : output->sessionId();
}

OBOE_NATIVE_FUNC(jint, nativeGetDeviceId, jlong handle) {
    LevyraOboeOutput *output = outputOf(handle);
    return output == nullptr ? 0 : output->deviceId();
}

OBOE_NATIVE_FUNC(jint, nativeGetBufferSizeInFrames, jlong handle) {
    LevyraOboeOutput *output = outputOf(handle);
    return output == nullptr ? 0 : output->bufferSizeInFrames();
}
