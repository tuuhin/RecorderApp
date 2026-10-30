include_guard(GLOBAL)

# Sets up safety rules, modern C++20, and size optimizations for a native target
function(configure_native_target target_name)
    # Force C++20 standard
    target_compile_features(${target_name} PRIVATE cxx_std_20)
    set_target_properties(${target_name} PROPERTIES CXX_EXTENSIONS OFF)

    # -Wl,--gc-sections: Removes unused code to shrink file size
    # --exclude-libs,ALL: Hides internal library code so only JNI functions are exposed
    target_link_options(${target_name} PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)

    # Thin link-time-optimization for this target only; the global
    # CMAKE_INTERPROCEDURAL_OPTIMIZATION variables are left alone so
    # FetchContent'd third-party projects keep their own build behavior
    set_target_properties(${target_name} PROPERTIES
            INTERPROCEDURAL_OPTIMIZATION_RELEASE ON
            INTERPROCEDURAL_OPTIMIZATION_RELWITHDEBINFO ON
    )

    # Turn on standard compiler warnings to catch bugs early
    if (CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
        target_compile_options(${target_name} PRIVATE -Wall -Wextra -Wpedantic)
    endif ()
endfunction()


include(FetchContent)

if (NOT TARGET lz4_static)
    # Only the lz4 compression library is needed: no command line tools,
    # everything built from source and linked statically through the
    # unified `lz4` interface target.
    #
    # lz4's CMakeLists (cmake_minimum_required 3.5) reads its option() calls
    # with the OLD CMP0077 behavior, so plain set() overrides are silently
    # cleared; they have to be forced into the cache to stick.
    set(LZ4_BUILD_CLI OFF CACHE BOOL "" FORCE)
    set(LZ4_POSITION_INDEPENDENT_LIB ON CACHE BOOL "" FORCE)

    set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
    set(BUILD_STATIC_LIBS ON CACHE BOOL "" FORCE)

    FetchContent_Declare(
            lz4
            GIT_REPOSITORY https://github.com/lz4/lz4.git
            GIT_TAG v1.10.0
            GIT_SHALLOW TRUE
            SOURCE_SUBDIR build/cmake
    )
    FetchContent_MakeAvailable(lz4)
endif ()
