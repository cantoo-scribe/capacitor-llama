cmake_minimum_required(VERSION 3.10)

# project(llama.rn)
project(capllama)

set(CMAKE_CXX_STANDARD 17)
set(CAPLLAMA_LIB_DIR ${CMAKE_SOURCE_DIR}/../../../cpp)

include_directories(${CAPLLAMA_LIB_DIR})

set(
    SOURCE_FILES
    ${CAPLLAMA_LIB_DIR}/ggml.c
    ${CAPLLAMA_LIB_DIR}/ggml-alloc.c
    ${CAPLLAMA_LIB_DIR}/ggml-backend.cpp
    ${CAPLLAMA_LIB_DIR}/ggml-backend-reg.cpp
    ${CAPLLAMA_LIB_DIR}/ggml-cpu.c
    ${CAPLLAMA_LIB_DIR}/ggml-cpu.cpp
    ${CAPLLAMA_LIB_DIR}/ggml-cpu-aarch64.cpp
    ${CAPLLAMA_LIB_DIR}/ggml-cpu-quants.c
    ${CAPLLAMA_LIB_DIR}/ggml-cpu-traits.cpp
    ${CAPLLAMA_LIB_DIR}/ggml-opt.cpp
    ${CAPLLAMA_LIB_DIR}/ggml-threading.cpp
    ${CAPLLAMA_LIB_DIR}/ggml-quants.c
    ${CAPLLAMA_LIB_DIR}/gguf.cpp
    ${CAPLLAMA_LIB_DIR}/log.cpp
    ${CAPLLAMA_LIB_DIR}/llama-impl.cpp
    ${CAPLLAMA_LIB_DIR}/llama-grammar.cpp
    ${CAPLLAMA_LIB_DIR}/llama-sampling.cpp
    ${CAPLLAMA_LIB_DIR}/llama-vocab.cpp
    ${CAPLLAMA_LIB_DIR}/llama-adapter.cpp
    ${CAPLLAMA_LIB_DIR}/llama-chat.cpp
    ${CAPLLAMA_LIB_DIR}/llama-context.cpp
    ${CAPLLAMA_LIB_DIR}/llama-kv-cache.cpp
    ${CAPLLAMA_LIB_DIR}/llama-arch.cpp
    ${CAPLLAMA_LIB_DIR}/llama-batch.cpp
    ${CAPLLAMA_LIB_DIR}/llama-cparams.cpp
    ${CAPLLAMA_LIB_DIR}/llama-hparams.cpp
    ${CAPLLAMA_LIB_DIR}/llama.cpp
    ${CAPLLAMA_LIB_DIR}/llama-model.cpp
    ${CAPLLAMA_LIB_DIR}/llama-model-loader.cpp
    ${CAPLLAMA_LIB_DIR}/llama-mmap.cpp
    ${CAPLLAMA_LIB_DIR}/llama-vocab.cpp
    ${CAPLLAMA_LIB_DIR}/llama-memory.cpp
    ${CAPLLAMA_LIB_DIR}/llama-io.cpp
    ${CAPLLAMA_LIB_DIR}/llama-graph.cpp
    ${CAPLLAMA_LIB_DIR}/sampling.cpp
    ${CAPLLAMA_LIB_DIR}/unicode-data.cpp
    ${CAPLLAMA_LIB_DIR}/unicode.cpp
    ${CAPLLAMA_LIB_DIR}/sgemm.cpp
    ${CAPLLAMA_LIB_DIR}/common.cpp
    ${CAPLLAMA_LIB_DIR}/chat.cpp
    ${CAPLLAMA_LIB_DIR}/json-schema-to-grammar.cpp
    ${CAPLLAMA_LIB_DIR}/json.hpp
    ${CAPLLAMA_LIB_DIR}/amx/amx.cpp
    ${CAPLLAMA_LIB_DIR}/amx/mmq.cpp
    ${CAPLLAMA_LIB_DIR}/minja/minja.hpp
    ${CAPLLAMA_LIB_DIR}/minja/chat-template.hpp
    ${CAPLLAMA_LIB_DIR}/rn-llama.cpp
    ${CMAKE_SOURCE_DIR}/jni-utils.h
    ${CMAKE_SOURCE_DIR}/jni.cpp
)

find_library(LOG_LIB log)

function(build_library target_name cpu_flags)
    add_library(
        ${target_name}
        SHARED
        ${SOURCE_FILES}
    )

    target_link_libraries(${target_name} ${LOG_LIB} android)

    target_compile_options(${target_name} PRIVATE -DLM_GGML_USE_CPU -DLM_GGML_USE_CPU_AARCH64 -pthread ${cpu_flags})

    if (${CMAKE_BUILD_TYPE} STREQUAL "Debug")
        target_compile_options(${target_name} PRIVATE -DRNLLAMA_ANDROID_ENABLE_LOGGING)
    endif ()

    # NOTE: If you want to debug the native code, you can uncomment if and endif
    # Note that it will be extremely slow
    # if (NOT ${CMAKE_BUILD_TYPE} STREQUAL "Debug")
        target_compile_options(${target_name} PRIVATE -O3 -DNDEBUG)
        target_compile_options(${target_name} PRIVATE -fvisibility=hidden -fvisibility-inlines-hidden)
        target_compile_options(${target_name} PRIVATE -ffunction-sections -fdata-sections)

        target_link_options(${target_name} PRIVATE -Wl,--gc-sections)
        target_link_options(${target_name} PRIVATE -Wl,--exclude-libs,ALL)
        target_link_options(${target_name} PRIVATE -flto)
    # endif ()
endfunction()

# Default target (no specific CPU features)
build_library("capllama" "")

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    # ARM64 targets
    # Removing fp16 for now as it leads to issues with some models like deepseek r1 distills
    # https://github.com/mybigday/llama.rn/pull/110#issuecomment-2609918310
    build_library("capllama_v8" "-march=armv8-a")
    build_library("capllama_v8_2" "-march=armv8.2-a")
    build_library("capllama_v8_2_dotprod" "-march=armv8.2-a+dotprod")
    build_library("capllama_v8_2_i8mm" "-march=armv8.2-a+i8mm")
    build_library("capllama_v8_2_dotprod_i8mm" "-march=armv8.2-a+dotprod+i8mm")

    # https://github.com/ggerganov/llama.cpp/blob/master/docs/android.md#cross-compile-using-android-ndk
    # llama.cpp will deal with the cpu features
    # build_library("capllama_v8_7" "-march=armv8.7-a")
    # TODO: Add support runtime check for cpu features
    # At the moment runtime check is failing.

elseif (${ANDROID_ABI} STREQUAL "x86_64")
    # x86_64 target
    build_library("capllama_x86_64" "-march=x86-64" "-mtune=intel" "-msse4.2" "-mpopcnt")

endif ()
