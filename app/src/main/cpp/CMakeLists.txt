# HK-OPERATION : ELITE NATIVE BUILD MATRIX
cmake_minimum_minimum_required(VERSION 3.10.2)

project("hk-pty")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
add_library(
        hk-pty         # Library name (This matches System.loadLibrary("hk-pty") in Java)
        SHARED         # Sets the library as a shared library (.so)
        native-pty.cpp # The source file we just created
)

# Searches for a specified prebuilt library and stores the path as a variable.
find_library(
        log-lib
        log
)

# Specifies libraries CMake should link to your target library.
target_link_libraries(
        hk-pty
        ${log-lib}
)
