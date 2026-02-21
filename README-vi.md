# JK BMS Monitor

![JK BMS Monitor](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![Rust](https://img.shields.io/badge/Core-Rust-orange.svg)
![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)

**JK BMS Monitor** là ứng dụng Android mã nguồn mở được thiết kế để kết nối và giám sát các mạch quản lý pin (BMS) của hãng JK thông qua Bluetooth Low Energy (BLE). Ứng dụng là sự kết hợp giữa lõi xử lý tốc độ cao viết bằng **Rust** (dùng để phân tích giao thức) và giao diện native hiện đại viết bằng **Jetpack Compose**, mang lại trải nghiệm theo dõi pin mượt mà và tin cậy.

[*Read in English*](README.md)

---

## 🌟 Tính Năng Nổi Bật

- **Giám sát thời gian thực:** Xem % pin, điện áp, dòng điện, công suất và nhiệt độ Mosfet theo thời gian thực.
- **Giải mã luồng dữ liệu (Stream Parsing):** Kiến trúc xử lý ghép nối Frame BLE liên tục, đảm bảo không bao giờ bị rớt gói tin khi BMS trả dữ liệu tốc độ cao.
- **Widget Màn Hình Chính:** Widget nhỏ gọn (2x2) giúp theo dõi nhanh các thông số pin ngay từ màn hình chính của điện thoại.
- **Đồng bộ ngầm (Background Sync):** Tự động cập nhật dữ liệu cho Widget ngay cả khi không mở app nhờ `WorkManager` và Foreground Services.
- **Lõi Rust siêu tốc:** Sử dụng [UniFFI](https://github.com/mozilla/uniffi-rs) để liên kết chéo mã nguồn Rust biên dịch trực tiếp sang Kotlin.
- **Giao diện hiện đại:** Giao diện Dark Mode sạch sẽ, mượt mà được xây dựng 100% bằng Jetpack Compose.
- **Tôn trọng Quyền riêng tư:** Đối với các máy chạy Android 12 trở lên, app tự động quét và kết nối BLE mà không yêu cầu cấp quyền Định vị (Location).

## 🛠️ Công Nghệ Sử Dụng

- **Android App:** Kotlin, Jetpack Compose, Coroutines, StateFlow, Glance (Widget), WorkManager.
- **Khối xử lý Protocol:** Rust, UniFFI.

## 🚀 Hướng Dẫn Cài Đặt & Build Mã Nguồn

### Yêu cầu hệ thống

Để build dự án này, máy tính của bạn cần cài đặt:
- [Android Studio](https://developer.android.com/studio) (Bản mới nhất)
- [Rust Toolchain](https://rustup.rs/) (`rustup`, `cargo`)
- `cargo-ndk` (thư viện hỗ trợ build chéo mã Rust sang các kiến trúc chip của Android)

```bash
cargo install cargo-ndk
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
```

### Các bước Build

1. **Clone mã nguồn:**
   ```bash
   git clone https://github.com/your-username/jk-bms-monitor.git
   cd jk-bms-monitor
   ```

2. **Build trên Android Studio:**
   Mở thư mục `android` bằng Android Studio và bấm **Run**, hoặc build qua dòng lệnh Terminal:
   
   ```bash
   cd android
   ./gradlew :app:assembleRelease
   ```
   File APK cài đặt sẽ được tạo ra tại: `android/app/build/outputs/apk/release/app-release.apk`.

> Lưu ý: Hệ thống Gradle đã được cấu hình tự động gọi lệnh Cargo để biên dịch lõi Rust (`libjkbms_protocol.so`) và sinh ra các file liên kết Kotlin (bindings) trong quá trình build Android.

## 📱 Ảnh Chụp Màn Hình
*(Thêm hình ảnh thực tế của ứng dụng vào đây)*

## 🤝 Đóng Góp
Mọi ý tưởng đóng góp cấu hình, báo lỗi (issue) hay yêu cầu tính năng mới đều được chào đón! Truy cập [trang Issues](https://github.com/your-username/jk-bms-monitor/issues) để thảo luận.

## 📄 Giấy Phép
Dự án được phân phối dưới giấy phép MIT - xem chi tiết tại file [LICENSE](LICENSE).
