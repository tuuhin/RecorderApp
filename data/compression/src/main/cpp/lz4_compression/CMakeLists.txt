add_library(lz4_compression SHARED
        src/lz4_jni_interop.cpp
        src/lz4_compressor.cpp
)

configure_native_target(lz4_compression)

target_include_directories(
        lz4_compression
        PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include
        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/src
)

target_link_libraries(lz4_compression PRIVATE lz4 log)
