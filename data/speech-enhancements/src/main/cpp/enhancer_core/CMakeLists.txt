# Layer 2a: orchestration and DSP state. Deliberately free of ONNX Runtime so it can be
# unit tested on any host.
add_library(dfn_enhancer_core STATIC
        src/model_config.cpp
        src/enhancer_state.cpp
        src/deepfilter_engine.cpp
        src/enhancer_session.cpp
)

target_include_directories(dfn_enhancer_core PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)

configure_project_target(dfn_enhancer_core)
target_compile_features(dfn_enhancer_core PUBLIC cxx_std_20)
target_link_libraries(dfn_enhancer_core PUBLIC dsp)

set_target_properties(dfn_enhancer_core PROPERTIES POSITION_INDEPENDENT_CODE ON)

# Layer 2b: ONNX Runtime backend. The `onnxruntime` imported target is provided by
# ../cmake/Config.cmake for Android (AAR) and host builds
# (DFN_ONNXRUNTIME_ROOT).
if (TARGET onnxruntime)
    add_library(dfn_enhancer_onnx STATIC src/onnx_backend.cpp)
    target_include_directories(dfn_enhancer_onnx PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
    configure_project_target(dfn_enhancer_onnx)
    target_link_libraries(dfn_enhancer_onnx PUBLIC dfn_enhancer_core PRIVATE onnxruntime)
    set_target_properties(dfn_enhancer_onnx PROPERTIES POSITION_INDEPENDENT_CODE ON)
    message(STATUS "dfn: ONNX Runtime backend enabled")
else ()
    message(STATUS "dfn: ONNX Runtime backend disabled (set DFN_ONNXRUNTIME_ROOT to enable)")
endif ()

if (DFN_BUILD_TESTS)
    # Orchestration and streaming tests (no ONNX Runtime required).
    add_executable(dfn_enhancer_tests
            test/test_model_config.cpp
            test/test_enhancer_session.cpp
            test/test_streaming.cpp
    )

    configure_project_target(dfn_enhancer_tests)
    target_include_directories(dfn_enhancer_tests PRIVATE ${DFN_TEST_INCLUDE_DIR})
    target_link_libraries(dfn_enhancer_tests PRIVATE dfn_enhancer_core GTest::gtest_main)

    gtest_discover_tests(dfn_enhancer_tests)

    # ONNX Runtime backend and end-to-end tests. Skipped when the backend is unavailable.
    if (TARGET onnxruntime)
        add_executable(dfn_onnx_tests test/test_onnx_backend.cpp)

        configure_project_target(dfn_onnx_tests)
        target_include_directories(dfn_onnx_tests PRIVATE ${DFN_TEST_INCLUDE_DIR})
        target_compile_definitions(dfn_onnx_tests PRIVATE
                DFN_TEST_DATA_DIR="${CMAKE_CURRENT_SOURCE_DIR}/../dsp/test/data"
                DFN_TEST_MODEL_DIR="${DFN_TEST_MODEL_DIR}"
                DFN_TEST_ENHANCER_DATA="${DFN_TEST_ENHANCER_DATA}")
        target_link_libraries(dfn_onnx_tests PRIVATE dfn_enhancer_onnx GTest::gtest_main)

        gtest_discover_tests(dfn_onnx_tests)
    endif ()
endif ()
