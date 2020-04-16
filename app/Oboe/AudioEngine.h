//
// Created by macropreprocessor on 17/06/19.
//

#ifndef MEDIA_PLAYER_PRO_AUDIOENGINE_H
#define MEDIA_PLAYER_PRO_AUDIOENGINE_H
#include <AudioTime.h>
#include <aaudio/AAudio.h>
#include "Generator.h"

extern AudioTime GlobalTime;

class AudioEngine {
public:
    AAudioStreamBuilder *builder;
    AAudioStream *stream;
    int sampleRate = 48000;
    int BufferCapacityInFrames = 192;
    int channelCount = 2;

    int32_t underrunCount = 0;
    int32_t previousUnderrunCount = 0;
    int32_t framesPerBurst = 0;
    int32_t bufferSize = 0;
    int32_t bufferCapacity = 0;

    Generator generator;

    static aaudio_data_callback_result_t onAudioReady(
            AAudioStream *stream, void *userData, void *audioData, int32_t numFrames
    );

    static void onError(
            AAudioStream *stream, void *userData, aaudio_result_t error
    );

    void RestartStream();
