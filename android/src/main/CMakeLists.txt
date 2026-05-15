cmake_minimum_required(VERSION 3.10)

# UNIFIED CMAKE BUILD
# This CMakeLists.txt is a unified version combining:
# - Original: android/src/main/CMakeLists.txt (JNI wrapper)
# - Original: android/src/main/capllama/CMakeLists.txt (core capllama)
#
# Result: A single .so file per architecture containing both capllama core
# and JNI wrapper bindings.
#
# See CMAKE_MERGE_MIGRATION.md for migration details.

project(capllama)

option(HEXAGON_SDK_ROOT "Hexagon SDK root directory" OFF)
option(HEXAGON_TOOLS_ROOT "Hexagon SDK toolchain directory" OFF)

find_package(Python3 REQUIRED)
set(CMAKE_CXX_STANDARD 17)
set(CAPLLAMA_LIB_DIR ${CMAKE_SOURCE_DIR}/../../../cpp)

include_directories(
    ${CAPLLAMA_LIB_DIR}
    ${CAPLLAMA_LIB_DIR}/common
    ${CAPLLAMA_LIB_DIR}/common/jinja
    ${CAPLLAMA_LIB_DIR}/ggml-cpu
    ${CAPLLAMA_LIB_DIR}/tools/mtmd
)

find_library(LOG_LIB log)

# Collect source files using glob patterns
file(GLOB MODEL_FILES ${CAPLLAMA_LIB_DIR}/models/*.cpp)
file(GLOB GGML_CPU_C_FILES ${CAPLLAMA_LIB_DIR}/ggml-cpu/*.c)
file(GLOB GGML_CPU_CPP_FILES ${CAPLLAMA_LIB_DIR}/ggml-cpu/*.cpp)
file(GLOB GGML_CPU_AMX_FILES ${CAPLLAMA_LIB_DIR}/ggml-cpu/amx/*.cpp)
file(GLOB LLAMA_FILES ${CAPLLAMA_LIB_DIR}/llama*.cpp)
file(GLOB MTMD_FILES ${CAPLLAMA_LIB_DIR}/tools/mtmd/*.cpp)
file(GLOB MTMD_MODEL_FILES ${CAPLLAMA_LIB_DIR}/tools/mtmd/models/*.cpp)
file(GLOB COMMON_FILES ${CAPLLAMA_LIB_DIR}/common/*.cpp)
file(GLOB JINJA_FILES ${CAPLLAMA_LIB_DIR}/common/jinja/*.cpp)

# Core capllama library source files
set(
    CAPLLAMA_SOURCE_FILES
    # Core GGML files
    ${CAPLLAMA_LIB_DIR}/ggml.c
    ${CAPLLAMA_LIB_DIR}/ggml-alloc.c
    ${CAPLLAMA_LIB_DIR}/ggml-backend.cpp
    ${CAPLLAMA_LIB_DIR}/ggml-backend-dl.cpp
    ${CAPLLAMA_LIB_DIR}/ggml-backend-meta.cpp
    ${CAPLLAMA_LIB_DIR}/ggml-backend-reg.cpp
    ${CAPLLAMA_LIB_DIR}/ggml-opt.cpp
    ${CAPLLAMA_LIB_DIR}/ggml-threading.cpp
    ${CAPLLAMA_LIB_DIR}/ggml-quants.c
    ${CAPLLAMA_LIB_DIR}/gguf.cpp

    # GGML CPU files (globbed)
    ${GGML_CPU_C_FILES}
    ${GGML_CPU_CPP_FILES}
    ${GGML_CPU_AMX_FILES}

    # Llama files (globbed)
    ${LLAMA_FILES}
    ${CAPLLAMA_LIB_DIR}/unicode-data.cpp
    ${CAPLLAMA_LIB_DIR}/unicode.cpp

    # Common utilities
    ${COMMON_FILES}

    ${CAPLLAMA_LIB_DIR}/anyascii.c

    # Jinja template engine
    ${JINJA_FILES}

    # Multimodal support (globbed)
    ${MTMD_MODEL_FILES}
    ${MTMD_FILES}

    # React Native llama APIs
    ${CAPLLAMA_LIB_DIR}/rn-llama.cpp
    ${CAPLLAMA_LIB_DIR}/rn-completion.cpp
    ${CAPLLAMA_LIB_DIR}/rn-tts.cpp
    ${CAPLLAMA_LIB_DIR}/rn-slot.cpp
    ${CAPLLAMA_LIB_DIR}/rn-slot-manager.cpp

    # Model implementations (globbed)
    ${MODEL_FILES}
)

# JNI wrapper source files
set(JNI_SOURCE_FILES
    ${CMAKE_SOURCE_DIR}/jni-utils.h
    ${CMAKE_SOURCE_DIR}/jni-utils.cpp
    ${CMAKE_SOURCE_DIR}/jni.cpp
)

function(build_capllama_unified jni_name arch cpu_flags)
    message(STATUS "Configuring unified target ${jni_name} with arch=${arch} and cpu_flags=${cpu_flags}")
    
    set(ENABLE_OPENCL OFF)
    if (${jni_name} MATCHES ".*_opencl$")
        set(ENABLE_OPENCL ON)
    endif ()

    set(ENABLE_HEXAGON OFF)
    if (${jni_name} MATCHES ".*_hexagon.*")
        set(ENABLE_HEXAGON ON)
    endif ()

    if (NOT ${arch} STREQUAL "generic")
        set(SOURCE_FILES_ARCH
            ${CAPLLAMA_LIB_DIR}/ggml-cpu/arch/${arch}/quants.c
            ${CAPLLAMA_LIB_DIR}/ggml-cpu/arch/${arch}/repack.cpp
        )
    endif ()

    # Build unified shared library with both capllama and JNI wrapper
    add_library(
        ${jni_name}
        SHARED
        ${CAPLLAMA_SOURCE_FILES}
        ${SOURCE_FILES_ARCH}
        ${JNI_SOURCE_FILES}
    )

    target_link_libraries(${jni_name} PRIVATE ${LOG_LIB} android)

    # Capllama library compile options
    if (${arch} STREQUAL "generic")
        target_compile_options(${jni_name} PRIVATE -DLM_GGML_CPU_GENERIC)
    endif ()

    target_compile_options(${jni_name} PRIVATE -DLM_GGML_USE_CPU -DLM_GGML_USE_CPU_REPACK -pthread ${cpu_flags} -fvectorize -ffp-model=fast -fno-finite-math-only -flto -D_GNU_SOURCE)
    
    # Debug logging option
    if (CMAKE_BUILD_TYPE AND CMAKE_BUILD_TYPE STREQUAL "Debug")
        target_compile_options(${jni_name} PRIVATE -DRNLLAMA_ANDROID_ENABLE_LOGGING)
    endif ()

    # Hexagon backend support
    if (ENABLE_HEXAGON)
        set(HEXAGON_SDK_AVAILABLE OFF)
        if (HEXAGON_SDK_ROOT AND HEXAGON_TOOLS_ROOT)
            set(CDSPRPC_LIB "${HEXAGON_SDK_ROOT}/ipc/fastrpc/remote/ship/android_aarch64/libcdsprpc.so")
            if (EXISTS ${CDSPRPC_LIB})
                set(HEXAGON_SDK_AVAILABLE ON)
                message(STATUS "Hexagon SDK verified and available")
            else()
                message(WARNING
                    "Hexagon SDK found but libcdsprpc.so is missing at ${CDSPRPC_LIB}. "
                    "Building ${jni_name} without Hexagon support.")
            endif()
        else()
            message(STATUS "Hexagon SDK not found — building ${jni_name} without Hexagon support.")
        endif()

        if (HEXAGON_SDK_AVAILABLE)
            # Add host-side hexagon sources
            target_sources(${jni_name} PRIVATE
                ${CAPLLAMA_LIB_DIR}/ggml-hexagon/ggml-hexagon.cpp
                ${CAPLLAMA_LIB_DIR}/ggml-hexagon/htp-drv.cpp
            )

            set(HEXAGON_PREBUILT_HTP_DIR "${CAPLLAMA_LIB_DIR}/ggml-hexagon/htp/v73/")
            set(HEXAGON_HOST_STUB "${HEXAGON_PREBUILT_HTP_DIR}/htp_iface_stub.c")
            set(HEXAGON_HOST_HEADER "${HEXAGON_PREBUILT_HTP_DIR}/htp_iface.h")

            if (NOT EXISTS ${HEXAGON_HOST_STUB} OR NOT EXISTS ${HEXAGON_HOST_HEADER})
                message(FATAL_ERROR
                    "Hexagon host QAIC artifacts are missing. "
                    "Run scripts/build-hexagon-htp.sh to generate ${HEXAGON_PREBUILT_HTP_DIR}.")
            endif()

            target_sources(${jni_name} PRIVATE
                ${HEXAGON_HOST_STUB}
            )

            target_include_directories(${jni_name} PRIVATE
                ${HEXAGON_SDK_ROOT}/incs
                ${HEXAGON_SDK_ROOT}/incs/stddef
                ${HEXAGON_SDK_ROOT}/ipc/fastrpc/rpcmem/inc
                ${HEXAGON_SDK_ROOT}/utils/examples
                ${CAPLLAMA_LIB_DIR}/ggml-hexagon
                ${CAPLLAMA_LIB_DIR}/ggml-hexagon/htp
                ${HEXAGON_PREBUILT_HTP_DIR}
                ${CMAKE_CURRENT_BINARY_DIR}
            )

            target_link_libraries(${jni_name} PRIVATE ${CDSPRPC_LIB})
            message(STATUS "Linking Hexagon cdsprpc library: ${CDSPRPC_LIB}")

            target_compile_options(${jni_name} PRIVATE
                -DLM_GGML_USE_HEXAGON
            )

            message(STATUS "Hexagon backend enabled for ${jni_name}")
        endif()
    endif ()

    # OpenCL backend support
    if (ENABLE_OPENCL)
        include_directories(${CMAKE_SOURCE_DIR}/../../../third_party/OpenCL-Headers)

        set(GGML_OPENCL_KERNELS
            add add_id argsort tri fill clamp cpy cvt diag_mask_inf diag div gelu
            gemv_noshuffle_general gemv_noshuffle get_rows glu group_norm solve_tri
            im2col_f32 im2col_f16 mean mul_mat_Ab_Bi_8x4 mul_mv_f16_f16
            mul_mv_f16_f32_1row mul_mv_f16_f32_l4 mul_mv_f16_f32 mul_mv_f32_f32
            mul_mv_q4_0_f32 mul_mv_q4_0_f32_v mul_mv_q4_0_f32_8x_flat mul_mv_q4_0_f32_1d_8x_flat
            mul_mv_q4_0_f32_1d_16x_flat mul_mv_q4_1_f32 mul_mv_q4_1_f32_flat
            mul_mv_q4_k_f32 mul_mv_q4_k_f32_flat mul_mv_q5_k_f32 mul_mv_q5_k_f32_flat
            mul_mv_q6_k_f32 mul_mv_q6_k_f32_flat mul_mv_q8_0_f32 mul_mv_q8_0_f32_flat
            mul_mv_mxfp4_f32 mul_mv_mxfp4_f32_flat mul_mv_id_q4_0_f32_8x_flat
            mul_mv_id_q8_0_f32 mul_mv_id_q8_0_f32_flat mul_mv_id_mxfp4_f32
            mul_mv_id_mxfp4_f32_flat gemm_moe_mxfp4_f32 gemv_moe_mxfp4_f32
            mul_mm_f32_f32_l4_lm mul_mm_f16_f32_l4_lm mul_mm_q4_0_f32_l4_lm
            mul_mm_q4_1_f32_l4_lm mul_mm_q8_0_f32_l4_lm mul_mm_q4_k_f32_l4_lm
            mul_mm_q5_k_f32_l4_lm mul_mm_q6_k_f32_l4_lm mul_mm_q8_0_f32_8x4
            gemv_noshuffle_q4_1_f32 gemm_noshuffle_q4_1_f32 gemv_noshuffle_general_q8_0_f32
            gemv_noshuffle_q4_k_f32 gemm_noshuffle_q4_k_f32 gemv_noshuffle_q5_k_f32
            gemm_noshuffle_q5_k_f32 gemv_noshuffle_q6_k_f32 gemm_noshuffle_q6_k_f32
            mul neg norm relu l2_norm rms_norm rope scale set_rows sigmoid silu
            softmax_4_f32 softmax_4_f16 softmax_f32 softmax_f16 sqr sqrt ssm_conv
            sub sum_rows cumsum transpose concat tsembd upscale tanh exp expm1
            softplus pad repeat mul_mat_f16_f32 mul_mm_f16_f32_kq_kqv conv2d
            conv2d_f16_f32 flash_attn_f32_f16 flash_attn_f16 flash_attn_f32
        )
        
        set(GGML_OPENCL_KERNEL_DIR ${CAPLLAMA_LIB_DIR}/ggml-opencl/kernels)
        set(GGML_OPENCL_KERNEL_HEADERS "")

        foreach(kernel ${GGML_OPENCL_KERNELS})
            set(input_file ${GGML_OPENCL_KERNEL_DIR}/${kernel}.cl)
            set(output_file ${CMAKE_CURRENT_BINARY_DIR}/${kernel}.cl.h)
            message(STATUS "opencl: embedding kernel ${kernel}")
            add_custom_command(
                OUTPUT ${output_file}
                COMMAND ${CMAKE_COMMAND} -E env PYTHONPATH=${CMAKE_CURRENT_SOURCE_DIR}
                        ${Python3_EXECUTABLE} ${CAPLLAMA_LIB_DIR}/ggml-opencl/kernels/embed_kernel.py
                        ${input_file} ${output_file}
                DEPENDS ${input_file} ${CAPLLAMA_LIB_DIR}/ggml-opencl/kernels/embed_kernel.py
                COMMENT "Embedding OpenCL kernel: ${kernel}.cl"
            )

            list(APPEND GGML_OPENCL_KERNEL_HEADERS ${output_file})
        endforeach()

        set(OPENCL_STUB_DIR ${CMAKE_SOURCE_DIR}/../../../bin/${ANDROID_ABI})
        set(OPENCL_STUB     ${OPENCL_STUB_DIR}/libOpenCL.so)

        if (EXISTS ${OPENCL_STUB})
            target_link_directories(${jni_name} PRIVATE ${OPENCL_STUB_DIR})
            target_link_libraries(${jni_name} PRIVATE OpenCL)
        else()
            message(WARNING
                "OpenCL library not found. Please build with ./scripts/build-opencl.sh")
        endif()

        target_include_directories(${jni_name} PRIVATE ${CMAKE_CURRENT_BINARY_DIR})
        target_sources(${jni_name} PRIVATE
            ${CAPLLAMA_LIB_DIR}/ggml-opencl/ggml-opencl.cpp
            ${GGML_OPENCL_KERNEL_HEADERS}
        )
        target_compile_options(${jni_name} PRIVATE
            -DLM_GGML_USE_OPENCL
            -DLM_GGML_OPENCL_USE_ADRENO_KERNELS
            -DLM_GGML_OPENCL_EMBED_KERNELS
            -DLM_GGML_OPENCL_SOA_Q
        )
    endif ()

    message(STATUS "Finished configuring unified target ${jni_name}")

    # Optimize for size and performance
    target_compile_options(${jni_name} PRIVATE -O3 -DNDEBUG)
    target_compile_options(${jni_name} PRIVATE -fvisibility=hidden -fvisibility-inlines-hidden)
    target_compile_options(${jni_name} PRIVATE -ffunction-sections -fdata-sections)

    target_link_options(${jni_name} PRIVATE -Wl,--gc-sections)
    target_link_options(${jni_name} PRIVATE -Wl,--exclude-libs,ALL)
    target_link_options(${jni_name} PRIVATE -flto)
endfunction()

# Build unified targets for supported architectures
if (ANDROID_ABI AND ANDROID_ABI STREQUAL "arm64-v8a")
    # ARM64 targets - unified .so files with both capllama and JNI
    build_capllama_unified("capllama_jni_v8" "arm" "-march=armv8-a")
    # build_capllama_unified("capllama_jni_v8_2" "arm" "-march=armv8.2-a")
    # build_capllama_unified("capllama_jni_v8_2_dotprod" "arm" "-march=armv8.2-a+dotprod")
    # build_capllama_unified("capllama_jni_v8_2_i8mm" "arm" "-march=armv8.2-a+i8mm")
    build_capllama_unified("capllama_jni_v8_2_dotprod_i8mm" "arm" "-march=armv8.2-a+dotprod+i8mm")
    # build_capllama_unified("capllama_jni_v8_2_dotprod_i8mm_hexagon_opencl" "arm" "-march=armv8.2-a+dotprod+i8mm")

elseif (ANDROID_ABI AND ANDROID_ABI STREQUAL "x86_64")
    # x86_64 target - unified .so file
    build_capllama_unified("capllama_jni_x86_64" "x86" "-march=x86-64;-mtune=generic;-msse4.2;-mpopcnt")

endif ()
