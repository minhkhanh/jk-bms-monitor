cmake_minimum_required(VERSION 3.22.1)
project("jkbms-android")

# --- Rust Project Integration ---

set(RUST_PROJECT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../rust)
set(WORKSPACE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../rust)
message(STATUS "Rust project source directory: ${RUST_PROJECT_DIR}")
message(STATUS "Workspace directory: ${WORKSPACE_DIR}")

set(RUST_BUILD_DIR ${CMAKE_CURRENT_BINARY_DIR}/rust-build)
message(STATUS "Rust target directory RUST_BUILD_DIR: ${RUST_BUILD_DIR}")

# Find Cargo
find_program(CARGO_EXECUTABLE cargo DOC "Path to the cargo executable")
if(NOT CARGO_EXECUTABLE)
    message(FATAL_ERROR "Cargo executable not found. Please ensure Rust and Cargo are installed and in your PATH.")
else()
    message(STATUS "Found Cargo: ${CARGO_EXECUTABLE}")
endif()

# Build type
set(RUST_BUILD_PROFILE "debug")
if(CMAKE_BUILD_TYPE MATCHES "^([Rr][Ee][Ll][Ee][Aa][Ss][Ee]|[Mm][Ii][Nn][Ss][Ii][Zz][Ee][Rr][Ee][Ll]|[Rr][Ee][Ll][Ww][Ii][Tt][Hh][Dd][Ee][Bb][Ii][Nn][Ff][Oo])$")
    set(RUST_BUILD_PROFILE "release")
    message(STATUS "Configuring Rust build for Release")
else()
    message(STATUS "Configuring Rust build for Debug")
endif()

# Android ABI mapping
if(ANDROID_ABI)
    message(STATUS "Android NDK build detected for ABI: ${ANDROID_ABI}")

    if(${ANDROID_ABI} STREQUAL "arm64-v8a")
        set(RUST_TARGET_TRIPLE "aarch64-linux-android")
        set(RUST_NDK_TOOLCHAIN "aarch64-linux-android")
    elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
        set(RUST_TARGET_TRIPLE "armv7-linux-androideabi")
        set(RUST_NDK_TOOLCHAIN "armv7a-linux-androideabi")
    elseif(${ANDROID_ABI} STREQUAL "x86_64")
        set(RUST_TARGET_TRIPLE "x86_64-linux-android")
    elseif(${ANDROID_ABI} STREQUAL "x86")
        set(RUST_TARGET_TRIPLE "i686-linux-android")
    else()
        message(FATAL_ERROR "Unsupported Android ABI: ${ANDROID_ABI}")
    endif()

    set(RUST_API 26)
    if(DEFINED CMAKE_ANDROID_NDK)
        set(NDK_HOME ${CMAKE_ANDROID_NDK})
    elseif(DEFINED ENV{ANDROID_NDK_HOME})
        set(NDK_HOME $ENV{ANDROID_NDK_HOME})
    else()
        message(FATAL_ERROR "ANDROID_NDK_HOME or CMAKE_ANDROID_NDK must be set!")
    endif()

    if(CMAKE_HOST_SYSTEM_NAME STREQUAL "Darwin")
        set(TOOLCHAIN "${NDK_HOME}/toolchains/llvm/prebuilt/darwin-x86_64")
    elseif(CMAKE_HOST_SYSTEM_NAME STREQUAL "Linux")
        set(TOOLCHAIN "${NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64")
    else()
        message(FATAL_ERROR "Unsupported host OS: ${CMAKE_HOST_SYSTEM_NAME}")
    endif()

    set(CC "${TOOLCHAIN}/bin/${RUST_NDK_TOOLCHAIN}${RUST_API}-clang")
    set(AR "${TOOLCHAIN}/bin/llvm-ar")
    set(RANLIB "${TOOLCHAIN}/bin/llvm-ranlib")
    set(CARGO_LINKER "${CC}")

    message(STATUS "Mapping Android ABI ${ANDROID_ABI} to Rust target: ${RUST_TARGET_TRIPLE}")
    set(RUST_BUILD_SUBDIR "${RUST_TARGET_TRIPLE}/${RUST_BUILD_PROFILE}")
else()
    message(STATUS "Non-Android build detected (or ANDROID_ABI not set).")
endif()

set(RUST_OUTPUT_LIB_FILENAME "libjkbms_protocol.so")
set(RUST_OUTPUT_LIB ${RUST_BUILD_DIR}/jkbms-protocol/${RUST_BUILD_SUBDIR}/${RUST_OUTPUT_LIB_FILENAME})
message(STATUS "RUST_OUTPUT_LIB: ${RUST_OUTPUT_LIB}")

# --- Build Rust library directly with Cargo ---
file(GLOB_RECURSE RUST_SOURCES
    "${WORKSPACE_DIR}/Cargo.toml"
    "${WORKSPACE_DIR}/*/**.rs"
    "${WORKSPACE_DIR}/*/Cargo.toml"
)

set(RUST_BUILD_PROFILE_FLAGS "")
if(RUST_BUILD_PROFILE STREQUAL "release")
    set(RUST_BUILD_PROFILE_FLAGS "--release")
endif()

set(RUST_BUILD_FLAGS "-C link-arg=-lc++abi -C link-arg=-Wl,-z,max-page-size=16384")
set(RUST_FEATURES "android")

add_custom_command(
    OUTPUT ${RUST_OUTPUT_LIB}
    COMMAND ${CMAKE_COMMAND} -E env
        CARGO_TARGET_DIR=${RUST_BUILD_DIR}
        ANDROID_ABI=${ANDROID_ABI}
        NDK_HOME=${NDK_HOME}
        TOOLCHAIN=${TOOLCHAIN}
        CC=${CC}
        AR=${AR}
        CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER=${CARGO_LINKER}
        CARGO_TARGET_ARMV7_LINUX_ANDROIDEABI_LINKER=${CARGO_LINKER}
        RANLIB=${RANLIB}
        RUSTUP_TOOLCHAIN=stable
        CARGO_BUILD_TARGET=${RUST_TARGET_TRIPLE}
        RUSTFLAGS=${RUST_BUILD_FLAGS}
        ${CARGO_EXECUTABLE} build --lib -p jkbms-protocol --manifest-path ${RUST_PROJECT_DIR}/Cargo.toml
            --target-dir ${RUST_BUILD_DIR}/jkbms-protocol --features ${RUST_FEATURES} ${RUST_BUILD_PROFILE_FLAGS}
    WORKING_DIRECTORY ${RUST_PROJECT_DIR}
    DEPENDS ${RUST_SOURCES}
    COMMENT "Building Rust library for Android (${RUST_TARGET_TRIPLE})"
    VERBATIM
)
add_custom_target(rust_build ALL
    DEPENDS ${RUST_OUTPUT_LIB}
    COMMENT "Ensuring rust buildings are successful")

# --- IMPORTED library target for Rust library ---
add_library(jkbms_rust SHARED IMPORTED)
set_target_properties(jkbms_rust PROPERTIES
    IMPORTED_LOCATION ${RUST_OUTPUT_LIB}
)
add_dependencies(jkbms_rust rust_build)

# --- UniFFI Binding Generation ---
set(UNIFFI_OUTPUT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../java)
file(MAKE_DIRECTORY ${UNIFFI_OUTPUT_DIR})
message(STATUS "UNIFFI_OUTPUT_DIR: ${UNIFFI_OUTPUT_DIR}")

add_custom_command(
    OUTPUT ${UNIFFI_OUTPUT_DIR}/jkbms_protocol.kt
    COMMAND ${CARGO_EXECUTABLE} run --bin uniffi-bindgen --manifest-path ${RUST_PROJECT_DIR}/Cargo.toml
        --target-dir ${RUST_BUILD_DIR}/uniffi generate --library ${RUST_OUTPUT_LIB}
        --language kotlin --out-dir ${UNIFFI_OUTPUT_DIR}
    DEPENDS ${RUST_OUTPUT_LIB}
    WORKING_DIRECTORY ${RUST_PROJECT_DIR}
    COMMENT "Generating UniFFI Kotlin bindings for jkbms-protocol"
    USES_TERMINAL
    VERBATIM
)
add_custom_target(uniffi_bindings
    DEPENDS ${UNIFFI_OUTPUT_DIR}/jkbms_protocol.kt
    COMMENT "Ensuring UniFFI Kotlin bindings are generated")

# --- Build main library ---
add_library(${CMAKE_PROJECT_NAME} SHARED dummy.cpp)
add_dependencies(${CMAKE_PROJECT_NAME} jkbms_rust uniffi_bindings)
target_link_libraries(${CMAKE_PROJECT_NAME}
    android
    log
    jkbms_rust)

# --- Clean ---
set_property(DIRECTORY APPEND PROPERTY ADDITIONAL_MAKE_CLEAN_FILES
    ${RUST_BUILD_DIR}
    ${UNIFFI_OUTPUT_DIR}/jkbms_protocol.kt
)
