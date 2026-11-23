add_library(dsp STATIC
        src/fft.cpp
        src/audio_frame.cpp
        src/audio_buffer.cpp
        src/window.cpp
        src/stft.cpp
        src/istft.cpp
        src/erb.cpp
        src/features.cpp
        src/deep_filter.cpp
        src/resampler.cpp
)

target_include_directories(dsp PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
configure_project_target(dsp)
target_compile_features(dsp PUBLIC cxx_std_20)
target_link_libraries(dsp PUBLIC PFFFT::PFFFT)
set_target_properties(dsp PROPERTIES POSITION_INDEPENDENT_CODE ON)

# Unit tests for the DSP layer. Run through the top-level project:
#
#   cmake -S src/main/cpp -B build -DDFN_BUILD_TESTS=ON
#   cmake --build build -j
#   ctest --test-dir build --output-on-failure
if (DFN_BUILD_TESTS)
    add_executable(dfn_dsp_tests
            test/test_audio_frame.cpp
            test/test_window.cpp
            test/test_stft.cpp
            test/test_istft.cpp
            test/test_erb.cpp
            test/test_features.cpp
            test/test_deep_filter.cpp
            test/test_resampler.cpp
    )

    configure_project_target(dfn_dsp_tests)
    target_include_directories(dfn_dsp_tests PRIVATE ${DFN_TEST_INCLUDE_DIR})
    target_compile_definitions(dfn_dsp_tests PRIVATE
            DFN_TEST_DATA_DIR="${CMAKE_CURRENT_SOURCE_DIR}/test/data")
    target_link_libraries(dfn_dsp_tests PRIVATE dsp GTest::gtest_main)

    gtest_discover_tests(dfn_dsp_tests)
endif ()
