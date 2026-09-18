#pragma once

#include <oboe/Oboe.h>

#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>

namespace levyra {

struct OboeOutputParams {
    int32_t sampleRate;
    int32_t channelCount;
    int32_t sessionId;
    int32_t deviceId;
    int32_t ringCapacityFrames;
    int32_t usage;
    int32_t contentType;
};

class LevyraOboeOutput final : public oboe::AudioStreamDataCallback,
                               public oboe::AudioStreamErrorCallback {
public:
    static std::shared_ptr<LevyraOboeOutput> open(const OboeOutputParams &params, int32_t *errorOut);

    ~LevyraOboeOutput() override;

    int32_t write(const uint8_t *data, int32_t sizeInBytes);
    int32_t play();
    int32_t pause();
    int32_t flush();
    void stop();
    void close();

    int64_t playedFrames();
    int32_t underrunCount() const { return underrunCount_.load(std::memory_order_relaxed); }
    int32_t errorCode() const { return errorCode_.load(std::memory_order_acquire); }
    int32_t sessionId() const { return sessionId_; }
    int32_t deviceId() const { return deviceId_; }
    int32_t bufferSizeInFrames() const { return ringCapacityFrames_ + streamBufferFrames_; }
    void setVolume(float volume) { targetVolume_.store(volume, std::memory_order_relaxed); }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *stream, void *audioData, int32_t numFrames) override;
    bool onError(oboe::AudioStream *stream, oboe::Result error) override;

private:
    LevyraOboeOutput(int32_t channelCount, int32_t sampleRate, int32_t ringCapacityFrames);

    void copyFromRing(int16_t *out, int64_t readFrame, int32_t frames) const;
    void applyGain(int16_t *samples, int32_t frames);
    int64_t presentedStreamFrames(int64_t deliveredFrames);

    const int32_t channelCount_;
    const int32_t sampleRate_;
    const int32_t ringCapacityFrames_;
    std::unique_ptr<int16_t[]> ring_;

    std::shared_ptr<oboe::AudioStream> stream_;
    std::mutex controlLock_;
    int32_t sessionId_ = 0;
    int32_t deviceId_ = 0;
    int32_t streamBufferFrames_ = 0;

    std::atomic<int64_t> writtenFrames_{0};
    std::atomic<int64_t> consumedFrames_{0};
    std::atomic<int64_t> deliveredFrames_{0};
    std::atomic<int64_t> contentEndStreamFrame_{0};
    std::atomic<int32_t> underrunCount_{0};
    std::atomic<int32_t> errorCode_{0};
    std::atomic<bool> endOfStream_{false};
    std::atomic<bool> playing_{false};
    std::atomic<float> targetVolume_{1.0f};

    float appliedVolume_ = 1.0f;
    bool inUnderrun_ = false;

    int64_t lastPlayedFrames_ = 0;
    std::atomic<int64_t> flushBaseFrames_{0};
};

}  // namespace levyra
