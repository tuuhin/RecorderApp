cmake_minimum_required(VERSION 3.22.1)

project(compression LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_C_VISIBILITY_PRESET hidden)
set(CMAKE_CXX_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)

# using thin-link-time-optimization, enabled per-target in configure_native_target
# while using thinlto we need to set the lto as thin
set(CMAKE_C_LINK_OPTIONS_IPO "-flto=thin")
set(CMAKE_CXX_LINK_OPTIONS_IPO "-flto=thin")

include(cmake/Config.cmake)

add_subdirectory(lz4_compression)
