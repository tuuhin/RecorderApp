add_library(custom_flac SHARED src/custom_flac.cpp)

configure_native_target(custom_flac)

target_include_directories(
        custom_flac
        PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/src
)