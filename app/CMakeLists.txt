cmake_minimum_required(VERSION 3.4.1)

include_directories (native)
include_directories (libMonitor)

add_library (
        PlayerOboe SHARED
        native/AudioTime.cpp
        libMonitor/MonitorPool.cpp
        Oboe/Generator.cpp
        Oboe/AudioEngine.cpp
        Oboe/audio/SoundRecording.cpp
        Oboe/audio/Mixer.cpp
        Oboe/Player.cpp
)

target_link_libraries (
        PlayerOboe
        aaudio
        log
        android
        m
)