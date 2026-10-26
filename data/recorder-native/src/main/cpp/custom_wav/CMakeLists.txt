
add_library(custom_wav SHARED src/custom_wav.cpp)
configure_native_target(custom_wav)

target_include_directories(
        custom_wav
        PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include
        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/src
)

target_link_libraries(custom_wav PRIVATE oboe)