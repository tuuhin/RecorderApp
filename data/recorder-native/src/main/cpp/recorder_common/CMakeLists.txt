add_library(recorder_common INTERFACE)

target_include_directories(recorder_common INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/include)

target_link_libraries(recorder_common INTERFACE oboe)
