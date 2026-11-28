# Native compression

This CMake project builds `liblz4_compression.so`, a JNI wrapper over a statically built LZ4
library. The wrapper is used for cache compression/decompression; compressed data remains in
the LZ4 frame format.

## Size and performance

For Release and RelWithDebInfo, `cmake/Config.cmake` compiles the `lz4_static` dependency with
`-Os`. This makes the native library smaller at the cost of some compression throughput. It is
appropriate for this cache path, but should not be copied to the real-time recorder/encoder
targets, where throughput is the priority.

The JNI wrapper itself retains the normal NDK optimization level. CMake also enables section
garbage collection, archive symbol hiding, and ThinLTO for the wrapper target. The project keeps
the default static libc++ runtime: a whole-app `c++_shared` experiment increased APK size after
including the shared runtime.

## Build

```bash
./gradlew :data:compression:assembleRelease
```
