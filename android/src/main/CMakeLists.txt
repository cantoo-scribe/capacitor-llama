cmake_minimum_required(VERSION 3.10)

project(capllama)

set(CMAKE_CXX_STANDARD 17)
set(CAPLLAMA_LIB_DIR ${CMAKE_SOURCE_DIR}/../../../cpp)

# Include directories for rnllama headers (needed by JNI wrapper)
include_directories(
    ${CAPLLAMA_LIB_DIR}
    ${CAPLLAMA_LIB_DIR}/common
    ${RNLLAMA_LIB_DIR}/common/jinja
)

find_library(LOG_LIB log)

# TODO: check if build from source is required
# If building from source, include the capllama library build
option(RNLLAMA_BUILD_FROM_SOURCE "Build capllama libraries from source" ON)

if (RNLLAMA_BUILD_FROM_SOURCE)
    message(STATUS "Building capllama libraries from source")
    add_subdirectory(capllama)
endif()

# JNI wrapper source files (always build from source)
set(JNI_SOURCE_FILES
    ${CMAKE_SOURCE_DIR}/jni-utils.h
    ${CMAKE_SOURCE_DIR}/jni-utils.cpp
    ${CMAKE_SOURCE_DIR}/jni.cpp
)

function(build_capllama_jni jni_name capllama_name arch cpu_flags)
   if (NOT RNLLAMA_BUILD_FROM_SOURCE)
        set(PREBUILT_CHECK_PATH ${CMAKE_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/lib${rnllama_name}.so)
        if (NOT EXISTS ${PREBUILT_CHECK_PATH})
            message(STATUS "Skipping ${jni_name} - no prebuilt for ${ANDROID_ABI}")
            return()
        endif()
    endif()

    set(ENABLE_OPENCL OFF)
    if (${capllama_name} MATCHES ".*_opencl$")
        set(ENABLE_OPENCL ON)
    endif ()

    set(ENABLE_HEXAGON OFF)
    if (${capllama_name} MATCHES ".*_hexagon.*")
        set(ENABLE_HEXAGON ON)
    endif ()

    # Create the shared library with JNI wrapper
    add_library(
        ${jni_name}
        SHARED
        ${JNI_SOURCE_FILES}
    )

    target_link_libraries(${jni_name} PRIVATE ${LOG_LIB} android)

    # TODO: rename DRNLLAMA_ANDROID_ENABLE_LOGGING. Check where it's used
    if (CMAKE_BUILD_TYPE AND CMAKE_BUILD_TYPE STREQUAL "Debug")
        target_compile_options(${jni_name} PRIVATE -DRNLLAMA_ANDROID_ENABLE_LOGGING)
    endif ()

    # Link to rnllama library
    if (RNLLAMA_BUILD_FROM_SOURCE)
        # Link to the shared library we just built in rnllama subdirectory
        target_link_libraries(${jni_name} PRIVATE ${capllama_name})
    else()
        # Link to prebuilt shared library from jniLibs
        # The prebuilt .so files will be loaded at runtime via System.loadLibrary()
        # We need to tell CMake where to find them for linking
        set(PREBUILT_LIB_PATH ${CMAKE_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/lib${capllama_name}.so)
        target_link_libraries(${jni_name} PRIVATE ${PREBUILT_LIB_PATH})
    endif()

    target_compile_options(${jni_name} PRIVATE -pthread ${cpu_flags})

    # Special handling for OpenCL variant when building from source
    if (RNLLAMA_BUILD_FROM_SOURCE AND ENABLE_OPENCL)
        set(OPENCL_STUB_DIR ${CMAKE_SOURCE_DIR}/../../../bin/${ANDROID_ABI})
        set(OPENCL_STUB ${OPENCL_STUB_DIR}/libOpenCL.so)

        if (EXISTS ${OPENCL_STUB})
            target_link_directories(${jni_name} PRIVATE ${OPENCL_STUB_DIR})
            target_link_libraries(${jni_name} PRIVATE OpenCL)
        else()
            message(WARNING "OpenCL library not found for JNI wrapper. Please build with ./scripts/build-opencl.sh")
        endif()
    endif()
    if (ENABLE_OPENCL)
        target_compile_options(${jni_name} PRIVATE -DLM_GGML_USE_OPENCL)
    endif()

    if (ENABLE_HEXAGON AND HEXAGON_SDK_ROOT)
        target_compile_options(${jni_name} PRIVATE -DLM_GGML_USE_HEXAGON)
    endif()


    # Optimize JNI wrapper
    target_compile_options(${jni_name} PRIVATE -O3 -DNDEBUG)
    target_compile_options(${jni_name} PRIVATE -fvisibility=hidden -fvisibility-inlines-hidden)
    target_compile_options(${jni_name} PRIVATE -ffunction-sections -fdata-sections)

    target_link_options(${jni_name} PRIVATE -Wl,--gc-sections)
    target_link_options(${jni_name} PRIVATE -Wl,--exclude-libs,ALL)
    target_link_options(${jni_name} PRIVATE -flto)
endfunction()

# Default target (no specific CPU features)
# build_capllama_jni("capllama_jni" "capllama" "generic" "")

if (ANDROID_ABI AND ANDROID_ABI STREQUAL "arm64-v8a")
    # ARM64 targets
    # Removing fp16 for now as it leads to issues with some models like deepseek r1 distills
    # https://github.com/mybigday/llama.rn/pull/110#issuecomment-2609918310
    # build_capllama_jni("capllama_jni_v8" "capllama_v8" "arm" "-march=armv8-a")
    # build_capllama_jni("capllama_jni_v8_2" "capllama_v8_2" "arm" "-march=armv8.2-a")
    # build_capllama_jni("capllama_jni_v8_2_dotprod" "capllama_v8_2_dotprod" "arm" "-march=armv8.2-a+dotprod")
    # build_capllama_jni("capllama_jni_v8_2_i8mm" "capllama_v8_2_i8mm" "arm" "-march=armv8.2-a+i8mm")
    build_capllama_jni("capllama_jni_v8_2_dotprod_i8mm" "capllama_v8_2_dotprod_i8mm" "arm" "-march=armv8.2-a+dotprod+i8mm")
    # build_capllama_jni("capllama_jni_v8_2_dotprod_i8mm_hexagon_opencl" "capllama_v8_2_dotprod_i8mm_hexagon_opencl" "arm" "-march=armv8.2-a+dotprod+i8mm")

    # https://github.com/ggerganov/llama.cpp/blob/master/docs/android.md#cross-compile-using-android-ndk
    # llama.cpp will deal with the cpu features
    # build_capllama_jni("capllama_jni_v8_7" "capllama_v8_7" "arm" "-march=armv8.7-a")
    # TODO: Add support runtime check for cpu features
    # At the moment runtime check is failing.

elseif (ANDROID_ABI AND ANDROID_ABI STREQUAL "x86_64")
    # x86_64 target
    # build_capllama_jni("capllama_jni_x86_64" "capllama_x86_64" "x86" "-march=x86-64;-mtune=generic;-msse4.2;-mpopcnt")

endif ()
