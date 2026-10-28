add_library(wav_recorder SHARED
        src/wav_jni_interop.cpp
        src/wav_file_writer.cpp
)

configure_native_target(wav_recorder)

target_include_directories(
        wav_recorder
        PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include
        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/src
)

target_link_libraries(wav_recorder PRIVATE recorder_common)