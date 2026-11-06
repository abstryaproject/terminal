package com.lailaba.linux

/**
 * Native PTY bridge. This milestone starts Android's unprivileged host shell.
 * Debian/PRoot will replace the child command in a later milestone.
 */
class NativePty {
    companion object {
        init { System.loadLibrary("terminal-pty") }
    }

    external fun start(): Int
    external fun write(data: String)
    external fun read(): String
    external fun resize(rows: Int, cols: Int)
    external fun close()
}
