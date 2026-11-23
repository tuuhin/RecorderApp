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

# JNI smoke tests. These run on-device: `gtest_discover_tests` executes the test binary at
# build time, which is not possible for a cross-compiled Android executable, so the test is
# registered explicitly instead. The bridge sources are compiled straight into the test so
# it can exercise them without a JVM.
if (DFN_BUILD_TESTS)
    add_executable(dfn_jni_tests
            test/test_enhancer_jni.cpp
            src/speech_enhancer_manager.cpp
    )

    configure_project_target(dfn_jni_tests)
    target_link_libraries(dfn_jni_tests PRIVATE
            dfn_enhancer_core
            dfn_enhancer_onnx
            dsp
            onnxruntime
            GTest::gtest_main)

    add_test(NAME dfn_jni_tests COMMAND dfn_jni_tests)
endif ()
