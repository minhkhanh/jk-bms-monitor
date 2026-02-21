# JK BMS Monitor

![JK BMS Monitor](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![Rust](https://img.shields.io/badge/Core-Rust-orange.svg)
![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)

**JK BMS Monitor** is an open-source Android application designed to connect and monitor JK Battery Management Systems (BMS) over Bluetooth Low Energy (BLE). It leverages a blazing-fast Rust core for protocol parsing combined with a modern Jetpack Compose native Android UI, allowing you to track battery statuses efficiently and reliably.

[*Đọc bằng tiếng Việt (Read in Vietnamese)*](README-vi.md)

---

## 🌟 Features

- **Real-time Monitoring:** View battery percentage, voltage, current, power, and mosfet temperature in real-time.
- **Continuous Stream Parsing:** Built with a continuous BLE frame assembler that avoids missing burst data packets from the BMS.
- **Home Screen Widget:** Convenient 2x2 Android Widget for monitoring battery stats directly from your home screen.
- **Background Sync:** The app uses Android's `WorkManager` and Foreground Services to keep your widget updated even when the app is closed.
- **Rust Core:** Uses [Mozilla's UniFFI](https://github.com/mozilla/uniffi-rs) to bind high-performance Rust parsing logic natively to Kotlin.
- **Modern UI:** Clean, dark-mode native interface using Jetpack Compose.
- **Privacy First:** On modern Android versions (12+), it safely connects via BLE without requesting location tracking permissions.

## 🛠️ Tech Stack

- **Android App:** Kotlin, Jetpack Compose, Coroutines, StateFlow, Glance (for Widgets), WorkManager.
- **Protocol Parser:** Rust, UniFFI.

## 🚀 Getting Started

### Prerequisites

To build this project from source, you need to install:
- [Android Studio](https://developer.android.com/studio) (Latest version recommended)
- [Rust Toolchain](https://rustup.rs/) (`rustup`, `cargo`)
- `cargo-ndk` (for cross-compiling Rust to Android architectures)

```bash
cargo install cargo-ndk
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
```

### Build Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/jk-bms-monitor.git
   cd jk-bms-monitor
   ```

2. **Build the Android App:**
   Open the `android` folder in Android Studio and hit **Run**, or build via CLI:
   
   ```bash
   cd android
   ./gradlew :app:assembleRelease
   ```
   The universal APK will be generated at `android/app/build/outputs/apk/release/app-release.apk`.

> Note: The Gradle build is configured to automatically call Cargo to compile the Rust core (`libjkbms_protocol.so`) and generate the Kotlin UniFFI bindings during the build process.

## 📱 Screenshots
*(Add screenshots of your application here)*

## 🤝 Contributing
Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/your-username/jk-bms-monitor/issues).

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
