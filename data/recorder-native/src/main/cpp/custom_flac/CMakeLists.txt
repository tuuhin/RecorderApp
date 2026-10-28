add_library(flac_recorder SHARED
        src/flac_jni_interop.cpp
        src/flac_file_writer.cpp
)

configure_native_target(flac_recorder)

target_include_directories(
        flac_recorder
        PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include
        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/src
)

target_link_libraries(flac_recorder PRIVATE recorder_common FLAC::FLAC)