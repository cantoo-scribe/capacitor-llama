cmake_minimum_required(VERSION 3.16)
project(capllama VERSION 1.0.0 LANGUAGES CXX C)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# iOS specific settings
set(CMAKE_OSX_DEPLOYMENT_TARGET 13.0)
set(CMAKE_XCODE_ATTRIBUTE_ENABLE_BITCODE NO)

# Dependencies and compile options
add_definitions(
    -DNDEBUG
    -DO3
    -DLM_GGML_USE_CPU
    -DLM_GGML_USE_ACCELERATE
    -DLM_GGML_USE_METAL
    -DLM_GGML_METAL_USE_BF16
)

set(SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../cpp)

# Define public headers
set(PUBLIC_HEADERS
    ${SOURCE_DIR}/rn-llama.h
    ${SOURCE_DIR}/llama.h
    ${SOURCE_DIR}/llama-impl.h
    ${SOURCE_DIR}/ggml.h
)

# Create library target
add_library(capllama SHARED
    ${SOURCE_DIR}/ggml.c
    ${SOURCE_DIR}/ggml-alloc.c
    ${SOURCE_DIR}/ggml-backend.cpp
    ${SOURCE_DIR}/ggml-backend-reg.cpp
    ${SOURCE_DIR}/ggml-cpu.c
    ${SOURCE_DIR}/ggml-cpu.cpp
    ${SOURCE_DIR}/ggml-cpu-aarch64.cpp
    ${SOURCE_DIR}/ggml-cpu-quants.c
    ${SOURCE_DIR}/ggml-cpu-traits.cpp
    ${SOURCE_DIR}/ggml-metal.m
    ${SOURCE_DIR}/ggml-opt.cpp
    ${SOURCE_DIR}/ggml-threading.cpp
    ${SOURCE_DIR}/ggml-quants.c
    ${SOURCE_DIR}/gguf.cpp
    ${SOURCE_DIR}/log.cpp
    ${SOURCE_DIR}/llama-impl.cpp
    ${SOURCE_DIR}/llama-grammar.cpp
    ${SOURCE_DIR}/llama-sampling.cpp
    ${SOURCE_DIR}/llama-vocab.cpp
    ${SOURCE_DIR}/llama-adapter.cpp
    ${SOURCE_DIR}/llama-chat.cpp
    ${SOURCE_DIR}/llama-context.cpp
    ${SOURCE_DIR}/llama-kv-cache.cpp
    ${SOURCE_DIR}/llama-arch.cpp
    ${SOURCE_DIR}/llama-batch.cpp
    ${SOURCE_DIR}/llama-cparams.cpp
    ${SOURCE_DIR}/llama-hparams.cpp
    ${SOURCE_DIR}/llama.cpp
    ${SOURCE_DIR}/llama-model.cpp
    ${SOURCE_DIR}/llama-model-loader.cpp
    ${SOURCE_DIR}/llama-mmap.cpp
    ${SOURCE_DIR}/llama-vocab.cpp
    ${SOURCE_DIR}/llama-memory.cpp
    ${SOURCE_DIR}/llama-io.cpp
    ${SOURCE_DIR}/llama-graph.cpp
    ${SOURCE_DIR}/sampling.cpp
    ${SOURCE_DIR}/unicode-data.cpp
    ${SOURCE_DIR}/unicode.cpp
    ${SOURCE_DIR}/sgemm.cpp
    ${SOURCE_DIR}/common.cpp
    ${SOURCE_DIR}/chat.cpp
    ${SOURCE_DIR}/json-schema-to-grammar.cpp
    ${SOURCE_DIR}/minja/minja.hpp
    ${SOURCE_DIR}/minja/chat-template.hpp
    ${SOURCE_DIR}/json.hpp
    ${SOURCE_DIR}/amx/amx.cpp
    ${SOURCE_DIR}/amx/mmq.cpp
    ${SOURCE_DIR}/rn-llama.cpp
)

# Setup include directories
target_include_directories(capllama
    PUBLIC
        $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/../../../cpp>
        $<INSTALL_INTERFACE:include>
)

# Link required frameworks
target_link_libraries(capllama PRIVATE
    "-framework Accelerate"
    "-framework Foundation"
    "-framework Metal"
    "-framework MetalKit"
)

# Set properties for framework
set_target_properties(capllama PROPERTIES
    MACOSX_FRAMEWORK_IDENTIFIER "com.capllama"
    MACOSX_FRAMEWORK_BUNDLE_VERSION 1.0.0
    MACOSX_FRAMEWORK_SHORT_VERSION_STRING 1.0.0
    FRAMEWORK TRUE
    FRAMEWORK_VERSION 1.0.0
    VERSION 1.0.0
    PUBLIC_HEADER "${PUBLIC_HEADERS}"
    XCODE_ATTRIBUTE_CLANG_ENABLE_OBJC_ARC NO
)
