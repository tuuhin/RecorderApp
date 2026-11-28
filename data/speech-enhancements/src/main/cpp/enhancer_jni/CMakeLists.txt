# Android JNI bridge. This is the only native target that knows about JNI.
add_library(speech_enhancement SHARED
        src/speech_enhancer_manager.cpp
        src/enhancer_jni_interop.cpp
)

target_include_directories(speech_enhancement PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)

configure_project_target(speech_enhancement)

target_link_libraries(speech_enhancement
        PRIVATE
        dfn_enhancer_core
        dfn_enhancer_onnx
        dsp
        onnxruntime
)

# JNI is provided by the NDK; the log library is used for diagnostic messages.
find_library(log-lib log)
if (log-lib)
    target_link_libraries(speech_enhancement PRIVATE ${log-lib})
endif ()
