/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// THIS FILE IS MODIFIED

#include "SoundRecording.h"
#include <OboeDebug.h>
#include <cmath>
#include <native.h>
#include <iostream>
#include <fstream>
#include <unistd.h>
#include <asm/fcntl.h>
#include <fcntl.h>

extern AudioTime GlobalTime;

void SoundRecording::renderAudio(int16_t *targetData, int64_t totalFrames, SoundRecording *Audio){
    SoundRecordingAudioData * AudioData = Audio->AudioData;
    if (mIsPlaying) {

        // Check whether we're about to reach the end of the recording
        if (!mIsLooping && mReadFrameIndex + totalFrames >= mTotalFrames) {
            totalFrames = mTotalFrames - mReadFrameIndex;
            mIsPlaying = false;
        }

//        LOGW("SoundRecording::renderAudio: rendering %ld frames with data", totalFrames);

        if (mReadFrameIndex == 0) {
            GlobalTime.StartOfFile = true;
            GlobalTime.update(mReadFrameIndex, AudioData);
//            LOGW("SoundRecording::renderAudio: AudioTime in milliseconds = %lld", GlobalTime.milliseconds);
        }
        for (int i = 0; i < totalFrames; ++i) {
            for (int j = 0; j < AudioData->channelCount; ++j) {
                targetData[(i * AudioData->channelCount) + j] = Audio->Audio[(mReadFrameIndex * AudioData->channelCount) + j];
            }
            // Increment and handle wraparound
            if (++mReadFrameIndex >= mTotalFrames) {
                GlobalTime.EndOfFile = true;
                GlobalTime.update(mReadFrameIndex, AudioData);
                mReadFrameIndex = 0;
            } else {
                GlobalTime.update(mReadFrameIndex, AudioData);
            }
//            LOGW("SoundRecording::renderAudio: mReadFrameIndex = %ld", mReadFrameIndex);
//            LOGW("SoundRecording::renderAudio: AudioTime in milliseconds = %lld", GlobalTime.milliseconds);
        }
    } else {
//        LOGW("SoundRecording::renderAudio: rendering %d frames with zero", totalFrames);
        // fill with zeros to output silence
        for (int i = 0; i < totalFrames * AudioData->channelCount; ++i) {
            targetData[i] = 0;
        }
    }
}

SoundRecording * SoundRecording::loadFromAssets(AAssetManager *assetManager, const char *filename, int SampleRate, int mChannelCount) {

    // Load the backing track
    AAsset* asset = AAssetManager_open(assetManager, filename, AASSET_MODE_BUFFER);

    if (asset == nullptr){
        LOGE("Failed to open track, filename %s", filename);
        return nullptr;
    }

    // Get the length of the track (we assume it is stereo 48kHz)
    uint64_t trackSize = static_cast<uint64_t>(AAsset_getLength(asset));

    // Load it into memory
    const int16_t *audioBuffer = static_cast<const int16_t*>(AAsset_getBuffer(asset));
    if (audioBuffer == nullptr){
        LOGE("Could not get buffer for track");
        return nullptr;
    }
    const int actualSampleRate = 48000;
    const int actualChannelCount = 2;

    const uint64_t totalFrames = trackSize / (2 * actualChannelCount);

    SoundRecordingAudioData * AudioData = new SoundRecordingAudioData(totalFrames, mChannelCount, SampleRate);
    AudioTime * allFrames = new AudioTime();
    allFrames->update(totalFrames, AudioData);
    LOGD("Opened backing track");
    LOGD("length in human time:                              %s", allFrames->format(true).c_str());
    LOGD("length in nanoseconds:                             %G", allFrames->nanosecondsTotal);
    LOGD("length in microseconds:                            %G", allFrames->microsecondsTotal);
    LOGD("length in milliseconds:                            %G", allFrames->millisecondsTotal);
    LOGD("length in seconds:                                 %G", allFrames->secondsTotal);
    LOGD("length in minutes:                                 %G", allFrames->minutesTotal);
    LOGD("length in hours:                                   %G", allFrames->hoursTotal);
    LOGD("bytes:                                             %ld", trackSize);
    LOGD("frames:                                            %ld", totalFrames);
    LOGD("sample rate:                                       %d", SampleRate);
    LOGD("length of 1 frame at %d sample rate:", SampleRate);
    LOGD("Human Time:                                        %s", AudioData->TimeTruncated);
    LOGD("Nanoseconds:                                       %G", AudioData->nanosecondsPerFrame);
    LOGD("Microseconds:                                      %G", AudioData->microsecondsPerFrame);
    LOGD("Milliseconds:                                      %G", AudioData->millisecondsPerFrame);
    LOGD("Seconds:                                           %G", AudioData->secondsPerFrame);
    LOGD("Minutes:                                           %G", AudioData->minutesPerFrame);
    LOGD("Hours:                                             %G", AudioData->hoursPerFrame);
    return new SoundRecording(const_cast<int16_t *>(audioBuffer), AudioData);
}
