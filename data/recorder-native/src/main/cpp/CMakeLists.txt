cmake_minimum_required(VERSION 3.22.1)

project(CustomAudioRecorder LANGUAGES C CXX)

include(cmake/Config.cmake)

add_subdirectory(custom_wav)
add_subdirectory(custom_flac)