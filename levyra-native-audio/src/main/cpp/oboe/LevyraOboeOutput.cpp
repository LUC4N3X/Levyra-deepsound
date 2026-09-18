#include "LevyraOboeOutput.h"

#include <algorithm>
#include <cmath>
#include <cstring>
#include <ctime>

namespace levyra {
namespace {

constexpr int64_t kNanosPerSecond = 1000000000LL;
constexpr int64_t kControlTimeoutNanos = 200LL * 1000000LL;

oboe::Usage sanitizeUsage(int32_t usage) {
    switch (usage) {
        case 1: case 2: case 3: case 4: case 5: case 6: case 10: case 11: case 12: case 13: case 14: case 16:
            return static_cast<oboe::Usage>(usage);
        default:
            return oboe::Usage::Media;
    }
}

oboe::ContentType sanitizeContentType(int32_t contentType) {
    if (contentType >= 1 && contentType <= 4) {
        return static_cast<oboe::ContentType>(contentType);
    }
    return oboe::ContentType::Music;
}

int64_t monotonicNanos() {
    timespec now{};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return static_cast<int64_t>(now.tv_sec) * kNanosPerSecond + now.tv_nsec;
}

}  // namespace

LevyraOboeOutput::LevyraOboeOutput(int32_t channelCount, int32_t sampleRate, int32_t ringCapacityFrames)
    : channelCount_(channelCount),
      sampleRate_(sampleRate),
      ringCapacityFrames_(ringCapacityFrames),
      ring_(new int16_t[static_cast<size_t>(ringCapacityFrames) * static_cast<size_t>(channelCount)]()) {}

LevyraOboeOutput::~LevyraOboeOutput() {
    close();
}

std::shared_ptr<LevyraOboeOutput> LevyraOboeOutput::open(const OboeOutputParams &params, int32_t *errorOut) {
    *errorOut = 0;
    if (params.channelCount < 1 || params.channelCount > 2 || params.sampleRate < 8000 ||
        params.sampleRate > 384000 || params.ringCapacityFrames <= 0 ||
        params.ringCapacityFrames > params.sampleRate * 10) {
        *errorOut = static_cast<int32_t>(oboe::Result::ErrorIllegalArgument);
        return nullptr;
    }
    std::shared_ptr<LevyraOboeOutput> output(
        new LevyraOboeOutput(params.channelCount, params.sampleRate, params.ringCapacityFrames));

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setAudioApi(oboe::AudioApi::AAudio)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Shared)
        ->setFormat(oboe::AudioFormat::I16)
        ->setChannelCount(params.channelCount)
        ->setSampleRate(params.sampleRate)
        ->setFormatConversionAllowed(false)
        ->setChannelConversionAllowed(false)
        ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::None)
        ->setUsage(sanitizeUsage(params.usage))
        ->setContentType(sanitizeContentType(params.contentType))
        ->setSessionId(static_cast<oboe::SessionId>(params.sessionId))
        ->setDataCallback(output)
        ->setErrorCallback(output);
    if (params.deviceId > 0) {
        builder.setDeviceId(params.deviceId);
    }

    std::shared_ptr<oboe::AudioStream> stream;
    const oboe::Result result = builder.openStream(stream);
    if (result != oboe::Result::OK || !stream) {
        *errorOut = static_cast<int32_t>(result == oboe::Result::OK ? oboe::Result::ErrorInternal : result);
        return nullptr;
    }
    if (stream->getAudioApi() != oboe::AudioApi::AAudio ||
        stream->getFormat() != oboe::AudioFormat::I16 ||
        stream->getChannelCount() != params.channelCount ||
        stream->getSampleRate() != params.sampleRate) {
        stream->close();
        *errorOut = static_cast<int32_t>(oboe::Result::ErrorInvalidFormat);
        return nullptr;
    }

    output->sessionId_ = static_cast<int32_t>(stream->getSessionId());
    output->deviceId_ = stream->getDeviceId();
    output->streamBufferFrames_ = std::max(0, stream->getBufferSizeInFrames());
    output->stream_ = std::move(stream);
    return output;
}

int32_t LevyraOboeOutput::write(const uint8_t *data, int32_t sizeInBytes) {
    const int32_t error = errorCode_.load(std::memory_order_acquire);
    if (error != 0) {
        return error;
    }
    const int32_t frameBytes = channelCount_ * static_cast<int32_t>(sizeof(int16_t));
    const int64_t written = writtenFrames_.load(std::memory_order_relaxed);
    const int64_t consumed = consumedFrames_.load(std::memory_order_acquire);
    const int32_t freeFrames = ringCapacityFrames_ - static_cast<int32_t>(written - consumed);
    const int32_t frames = std::min(sizeInBytes / frameBytes, freeFrames);
    if (frames <= 0) {
        return 0;
    }
    const int32_t start = static_cast<int32_t>(written % ringCapacityFrames_);
    const int32_t firstFrames = std::min(frames, ringCapacityFrames_ - start);
    std::memcpy(ring_.get() + static_cast<size_t>(start) * channelCount_, data,
                static_cast<size_t>(firstFrames) * frameBytes);
    if (frames > firstFrames) {
        std::memcpy(ring_.get(), data + static_cast<size_t>(firstFrames) * frameBytes,
                    static_cast<size_t>(frames - firstFrames) * frameBytes);
    }
    writtenFrames_.store(written + frames, std::memory_order_release);
    return frames * frameBytes;
}

int32_t LevyraOboeOutput::play() {
    std::lock_guard<std::mutex> lock(controlLock_);
    if (!stream_) {
        return static_cast<int32_t>(oboe::Result::ErrorClosed);
    }
    playing_.store(true, std::memory_order_relaxed);
    return static_cast<int32_t>(stream_->requestStart());
}

int32_t LevyraOboeOutput::pause() {
    std::lock_guard<std::mutex> lock(controlLock_);
    playing_.store(false, std::memory_order_relaxed);
    if (!stream_) {
        return static_cast<int32_t>(oboe::Result::ErrorClosed);
    }
    return static_cast<int32_t>(stream_->requestPause());
}

int32_t LevyraOboeOutput::flush() {
    std::lock_guard<std::mutex> lock(controlLock_);
    playing_.store(false, std::memory_order_relaxed);
    if (!stream_) {
        return static_cast<int32_t>(oboe::Result::ErrorClosed);
    }
    oboe::Result result = oboe::Result::OK;
    const oboe::StreamState state = stream_->getState();
    if (state == oboe::StreamState::Started ||
        state == oboe::StreamState::Starting ||
        state == oboe::StreamState::Pausing) {
        result = stream_->pause(kControlTimeoutNanos);
    }
    if (result == oboe::Result::OK) {
        result = stream_->flush(kControlTimeoutNanos);
    }
    if (result != oboe::Result::OK) {
        return static_cast<int32_t>(result);
    }
    const int64_t written = writtenFrames_.load(std::memory_order_relaxed);
    contentEndStreamFrame_.store(deliveredFrames_.load(std::memory_order_relaxed), std::memory_order_relaxed);
    consumedFrames_.store(written, std::memory_order_release);
    lastPlayedFrames_ = written;
    flushBaseFrames_.store(written, std::memory_order_relaxed);
    endOfStream_.store(false, std::memory_order_relaxed);
    stopRequested_.store(false, std::memory_order_relaxed);
    return static_cast<int32_t>(result);
}

void LevyraOboeOutput::stop() {
    endOfStream_.store(true, std::memory_order_relaxed);
}

void LevyraOboeOutput::close() {
    std::shared_ptr<oboe::AudioStream> stream;
    {
        std::lock_guard<std::mutex> lock(controlLock_);
        playing_.store(false, std::memory_order_relaxed);
        stream = std::move(stream_);
    }
    if (stream) {
        stream->requestStop();
        stream->close();
    }
}

int64_t LevyraOboeOutput::presentedStreamFrames(int64_t deliveredFrames) {
    if (!stream_) {
        return deliveredFrames;
    }
    auto timestamp = stream_->getTimestamp(CLOCK_MONOTONIC);
    int64_t presented;
    if (timestamp) {
        presented = timestamp.value().position;
        if (playing_.load(std::memory_order_relaxed)) {
            const int64_t elapsedNanos = monotonicNanos() - timestamp.value().timestamp;
            if (elapsedNanos > 0) {
                presented += elapsedNanos * sampleRate_ / kNanosPerSecond;
            }
        }
    } else {
        presented = stream_->getFramesRead();
    }
    return std::clamp<int64_t>(presented, 0, deliveredFrames);
}

int64_t LevyraOboeOutput::playedFrames() {
    std::lock_guard<std::mutex> lock(controlLock_);
    const int64_t consumed = consumedFrames_.load(std::memory_order_acquire);
    const int64_t contentEnd = contentEndStreamFrame_.load(std::memory_order_acquire);
    const int64_t delivered = deliveredFrames_.load(std::memory_order_acquire);
    const int64_t inFlight = std::max<int64_t>(0, contentEnd - presentedStreamFrames(delivered));
    const int64_t played = std::clamp<int64_t>(consumed - inFlight, lastPlayedFrames_, consumed);
    lastPlayedFrames_ = played;
    if (stream_ &&
        errorCode_.load(std::memory_order_acquire) == 0 &&
        endOfStream_.load(std::memory_order_relaxed) &&
        inFlight == 0 &&
        consumed >= writtenFrames_.load(std::memory_order_acquire) &&
        !stopRequested_.exchange(true, std::memory_order_acq_rel)) {
        const oboe::Result stopResult = stream_->requestStop();
        if (stopResult != oboe::Result::OK) {
            stopRequested_.store(false, std::memory_order_release);
            errorCode_.store(static_cast<int32_t>(stopResult), std::memory_order_release);
        } else {
            playing_.store(false, std::memory_order_relaxed);
        }
    }
    return played - flushBaseFrames_.load(std::memory_order_relaxed);
}

void LevyraOboeOutput::copyFromRing(int16_t *out, int64_t readFrame, int32_t frames) const {
    const int32_t start = static_cast<int32_t>(readFrame % ringCapacityFrames_);
    const int32_t firstFrames = std::min(frames, ringCapacityFrames_ - start);
    const size_t frameSamples = static_cast<size_t>(channelCount_);
    std::memcpy(out, ring_.get() + start * frameSamples, firstFrames * frameSamples * sizeof(int16_t));
    if (frames > firstFrames) {
        std::memcpy(out + firstFrames * frameSamples, ring_.get(),
                    static_cast<size_t>(frames - firstFrames) * frameSamples * sizeof(int16_t));
    }
}

void LevyraOboeOutput::applyGain(int16_t *samples, int32_t frames) {
    const float target = targetVolume_.load(std::memory_order_relaxed);
    const float start = appliedVolume_;
    if (start == 1.0f && target == 1.0f) {
        return;
    }
    const float step = (target - start) / static_cast<float>(frames);
    float gain = start;
    for (int32_t frame = 0; frame < frames; ++frame) {
        gain += step;
        int16_t *frameSamples = samples + static_cast<size_t>(frame) * channelCount_;
        for (int32_t channel = 0; channel < channelCount_; ++channel) {
            const float scaled = static_cast<float>(frameSamples[channel]) * gain;
            frameSamples[channel] = static_cast<int16_t>(std::lrintf(std::clamp(scaled, -32768.0f, 32767.0f)));
        }
    }
    appliedVolume_ = target;
}

oboe::DataCallbackResult LevyraOboeOutput::onAudioReady(oboe::AudioStream *, void *audioData, int32_t numFrames) {
    auto *out = static_cast<int16_t *>(audioData);
    const int64_t streamStart = deliveredFrames_.load(std::memory_order_relaxed);
    const int64_t consumed = consumedFrames_.load(std::memory_order_relaxed);
    const int64_t written = writtenFrames_.load(std::memory_order_acquire);
    const int32_t available = static_cast<int32_t>(std::clamp<int64_t>(written - consumed, 0, numFrames));
    if (available > 0) {
        copyFromRing(out, consumed, available);
        applyGain(out, available);
        contentEndStreamFrame_.store(streamStart + available, std::memory_order_release);
        consumedFrames_.store(consumed + available, std::memory_order_release);
    }
    if (available < numFrames) {
        std::memset(out + static_cast<size_t>(available) * channelCount_, 0,
                    static_cast<size_t>(numFrames - available) * channelCount_ * sizeof(int16_t));
        const bool starved = consumed + available > flushBaseFrames_.load(std::memory_order_relaxed) &&
                             !endOfStream_.load(std::memory_order_relaxed) &&
                             playing_.load(std::memory_order_relaxed);
        if (starved && !inUnderrun_) {
            underrunCount_.fetch_add(1, std::memory_order_relaxed);
        }
        inUnderrun_ = starved;
    } else {
        inUnderrun_ = false;
    }
    deliveredFrames_.fetch_add(numFrames, std::memory_order_release);
    return oboe::DataCallbackResult::Continue;
}

bool LevyraOboeOutput::onError(oboe::AudioStream *, oboe::Result error) {
    const int32_t code = static_cast<int32_t>(error);
    errorCode_.store(code == 0 ? static_cast<int32_t>(oboe::Result::ErrorInternal) : code,
                     std::memory_order_release);
    playing_.store(false, std::memory_order_relaxed);
    return false;
}

}  // namespace levyra
