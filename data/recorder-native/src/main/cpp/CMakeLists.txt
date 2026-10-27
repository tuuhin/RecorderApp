cmake_minimum_required(VERSION 3.22.1)

project(CustomAudioRecorder LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# strips everything which is not needed
set(CMAKE_C_VISIBILITY_PRESET hidden)
set(CMAKE_CXX_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)

# using thin-link-time-optimization
set(CMAKE_INTERPROCEDURAL_OPTIMIZATION_RELEASE ON)
set(CMAKE_INTERPROCEDURAL_OPTIMIZATION_RELWITHDEBINFO ON)

# while using thinlto we need to set ththe lto as thin
set(CMAKE_C_LINK_OPTIONS_IPO "-flto=thin")
set(CMAKE_CXX_LINK_OPTIONS_IPO "-flto=thin")

include(cmake/Config.cmake)

add_subdirectory(custom_wav)
add_subdirectory(custom_flac)