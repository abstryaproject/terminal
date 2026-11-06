#include <jni.h>
#include <cerrno>
#include <cstdlib>
#include <cstring>
#include <fcntl.h>
#include <signal.h>
#include <string>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

static int g_master_fd = -1;
static pid_t g_child_pid = -1;

extern "C" JNIEXPORT jint JNICALL
Java_com_lailaba_linux_NativePty_start(JNIEnv*, jobject) {
    if (g_master_fd >= 0) return 0;

    int master = posix_openpt(O_RDWR | O_NOCTTY);
    if (master < 0) return errno;
    if (grantpt(master) != 0 || unlockpt(master) != 0) {
        int error = errno;
        close(master);
        return error;
    }

    char* slave_name = ptsname(master);
    if (slave_name == nullptr) {
        int error = errno;
        close(master);
        return error;
    }

    struct winsize window_size {};
    window_size.ws_row = 24;
    window_size.ws_col = 80;

    pid_t pid = fork();
    if (pid < 0) {
        int error = errno;
        close(master);
        return error;
    }

    if (pid == 0) {
        close(master);
        if (setsid() < 0) _exit(126);

        int slave = open(slave_name, O_RDWR);
        if (slave < 0) _exit(126);
        ioctl(slave, TIOCSCTTY, 0);
        ioctl(slave, TIOCSWINSZ, &window_size);

        dup2(slave, STDIN_FILENO);
        dup2(slave, STDOUT_FILENO);
        dup2(slave, STDERR_FILENO);
        if (slave > STDERR_FILENO) close(slave);

        setenv("TERM", "xterm-256color", 1);
        setenv("HOME", "/data/data/com.lailaba.terminal/files/home", 1);
        setenv("PATH", "/system/bin:/system/xbin", 1);
        execl("/system/bin/sh", "sh", "-i", static_cast<char*>(nullptr));
        _exit(127);
    }

    g_child_pid = pid;
    g_master_fd = master;
    int flags = fcntl(g_master_fd, F_GETFL, 0);
    if (flags >= 0) fcntl(g_master_fd, F_SETFL, flags | O_NONBLOCK);
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_lailaba_linux_NativePty_write(JNIEnv* env, jobject, jstring value) {
    if (g_master_fd < 0 || value == nullptr) return;
    const char* data = env->GetStringUTFChars(value, nullptr);
    if (data != nullptr) {
        size_t remaining = std::strlen(data);
        const char* cursor = data;
        while (remaining > 0) {
            ssize_t written = ::write(g_master_fd, cursor, remaining);
            if (written > 0) {
                cursor += written;
                remaining -= static_cast<size_t>(written);
            } else if (written < 0 && errno == EINTR) {
                continue;
            } else {
                break;
            }
        }
        env->ReleaseStringUTFChars(value, data);
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_lailaba_linux_NativePty_read(JNIEnv* env, jobject) {
    if (g_master_fd < 0) return env->NewStringUTF("");
    char buffer[4096];
    ssize_t count = ::read(g_master_fd, buffer, sizeof(buffer));
    if (count <= 0) return env->NewStringUTF("");
    return env->NewStringUTF(std::string(buffer, static_cast<size_t>(count)).c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_lailaba_linux_NativePty_resize(JNIEnv*, jobject, jint rows, jint cols) {
    if (g_master_fd < 0) return;
    struct winsize window_size {};
    window_size.ws_row = static_cast<unsigned short>(rows);
    window_size.ws_col = static_cast<unsigned short>(cols);
    ioctl(g_master_fd, TIOCSWINSZ, &window_size);
    if (g_child_pid > 0) kill(g_child_pid, SIGWINCH);
}

extern "C" JNIEXPORT void JNICALL
Java_com_lailaba_linux_NativePty_close(JNIEnv*, jobject) {
    if (g_child_pid > 0) {
        kill(g_child_pid, SIGHUP);
        waitpid(g_child_pid, nullptr, WNOHANG);
    }
    if (g_master_fd >= 0) close(g_master_fd);
    g_master_fd = -1;
    g_child_pid = -1;
}
