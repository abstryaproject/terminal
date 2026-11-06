cmake_minimum_required(VERSION 3.22.1)
project(terminal_pty LANGUAGES CXX)

add_library(terminal-pty SHARED terminal_pty.cpp)
find_library(android-log log)
target_link_libraries(terminal-pty ${android-log})
