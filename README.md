# Linux Terminal Android — Codemagic build package

This repository is a **buildable first milestone**, not the finished Debian environment.

## What this milestone contains
- Kotlin + Jetpack Compose Android application.
- JNI bridge to native C++.
- PTY master/slave setup using Android NDK-compatible POSIX PTY calls.
- Interactive unprivileged Android `/system/bin/sh` host shell.
- Codemagic workflow that builds and exports a debug APK.

## Build on Codemagic
1. Create a new Codemagic app and connect this repository.
2. Commit/push the contents of this folder to the repository root.
3. In Codemagic, select the `android-debug-apk` workflow from `codemagic.yaml`.
4. Start the build.
5. Download `app-debug.apk` from the build's Artifacts section.

The workflow uses Java 17, Android Gradle Plugin 8.10.1, Gradle 8.11.1, Android SDK 35, NDK and CMake 3.22.1. If Gradle is absent on the selected worker, the workflow downloads Gradle 8.11.1.

## Build locally
Open the folder in Android Studio, install Android SDK 35, NDK and CMake 3.22.1, sync Gradle, then run:
```bash
gradle :app:assembleDebug
```

## Honest implementation status
**Included:** app foundation, Compose terminal screen, native PTY process transport, host-shell launch.

**Not included yet:** PRoot, Debian root filesystem, Debian Bash, APT, distribution installer, persistent Linux rootfs, terminal emulator/ANSI parser, multiple independent sessions, file manager, SAF import/export, release signing.

The current shell is Android's restricted host shell. It is not Debian and must not be advertised as a complete Linux environment. The next development milestone is to add a checksum-verified PRoot executable and Debian rootfs for supported ABIs, then launch Debian `/bin/bash` inside the rootfs.

## APK type
The workflow exports a **debug APK** for installation/testing. A distributable release APK needs a signing keystore and Codemagic signing configuration. Do not put keystore passwords or private signing files in source control.
