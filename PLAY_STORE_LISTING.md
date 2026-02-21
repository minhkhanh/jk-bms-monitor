# Google Play Store Listing Info
Dưới đây là toàn bộ nội dung văn bản (Text Assets) để copy/paste vào mục **Store Presence -> Main Store Listing** trên Google Play Console.

---

## 1. Bản Tiếng Việt (Vietnamese - Default)

**Tên ứng dụng (App Name) [Max 30 chars]:**
JK BMS Monitor & Widget

**Mô tả ngắn (Short description) [Max 80 chars]:**
Trình theo dõi pin Lithium JK BMS qua Bluetooth siêu tốc với Widget màn hình.

**Mô tả đầy đủ (Full description) [Max 4000 chars]:**
JK BMS Monitor là công cụ giám sát pin mạnh mẽ nhất dành cho các hệ thống pin Lithium sử dụng mạch JK (Jikong) BMS. Quên đi việc phải mở app thủ công, ứng dụng mang đến trải nghiệm liền mạch ngay trên Màn hình chính của bạn với Widget theo dõi thời gian thực.

Tại sao chọn JK BMS Monitor?
⚡ HIỆU NĂNG VƯỢT TRỘI: Ứng dụng duy nhất trên thị trường được xây dựng bằng lõi RUST tốc độ cao, giúp đọc luồng dữ liệu BLE của JK BMS chính xác tới từng mili-giây, không bao giờ trễ gói tin.
🔋 WIDGET MÀN HÌNH CHÍNH TỰ ĐỘNG: Không cần mở app. Theo dõi % Pin, Điện áp, Dòng điện xả, Công suất và Nhiệt độ ngay từ màn hình chính (hỗ trợ nền tối/sáng tự động). Cập nhật ngầm liên tục mà không lo tốn pin điện thoại.
🔒 QUYỀN RIÊNG TƯ LÀ SỐ 1: Hoàn toàn không yêu cầu cấp quyền Vị Trí (Location) trên Android 12+. Chúng tôi kết nối qua Bluetooth 100% minh bạch, không thu thập dữ liệu người dùng, không quảng cáo, mã nguồn mở mảng Core.
✨ THIẾT KẾ HIỆN ĐẠI: Giao diện Native Material Design sắc nét, các thao tác vuốt - chạm mượt mà chuẩn Android đời mới.

Các tính năng chính:
- Hiển thị theo thời gian thực: % dung lượng, Tổng điện áp, Dòng sạc/xả, Công suất tiêu thụ, Nhiệt độ Mosfet.
- Thuật toán gộp gói tin BMS thông minh.
- Thiết kế Widget 2x2 tối ưu không gian màn hình.
- Tiết kiệm pin tối đa bằng Background Sync thế hệ mới.

Tải JK BMS Monitor ngay hôm nay để quản lý hệ thống năng lượng mặt trời, xe điện, hay pin dự phòng của bạn một cách chuyên nghiệp nhất!

---

## 2. Bản Tiếng Anh (English - US)

**App Name [Max 30 chars]:**
JK BMS Monitor & Widget

**Short description [Max 80 chars]:**
Ultra-fast Bluetooth monitor for JK BMS Lithium batteries with Home Screen Widget.

**Full description [Max 4000 chars]:**
JK BMS Monitor is the ultimate tracking tool for Lithium battery systems powered by JK (Jikong) Battery Management Systems. Forget about opening an app manually—enjoy a seamless experience right on your Home Screen with our real-time tracking Widget.

Why choose JK BMS Monitor?
⚡ BLAZING FAST PERFORMANCE: The only app on the market utilizing a high-speed RUST core, ensuring precise millisecond-level BLE data streaming from your JK BMS with zero packet loss.
🔋 AUTOMATIC HOME SCREEN WIDGET: No need to open the app. Monitor Battery %, Total Voltage, Amp Current, Power output, and Mosfet Temperature directly from your home screen. Relies on smart background sync with negligible battery drain.
🔒 PRIVACY ABOVE ALL: Zero Location permissions required on Android 12+. We connect via Bluetooth transparently. No user data tracking, zero ads, and open-source core logic.
✨ MODERN DESIGN: Built entirely with Native Material Design for smooth swiping and a gorgeous dark layout.

Key Features:
- Real-time display: Capacity %, Voltage, Charge/Discharge Current, Power, Mosfet Temp.
- Intelligent BMS packet assembly algorithm.
- Space-saving 2x2 Widget design.
- Maximize phone battery life with Next-Gen Background Sync.

Download JK BMS Monitor today and manage your solar setup, EV battery, or backup power professionally!

---

## 3. Khai báo Data Safety (An toàn dữ liệu)

Khi Google hỏi trong mục App Content -> Data Safety, bạn hãy trả lời theo hướng dẫn sau:
- **Câu hỏi:** Does your app collect or share any of the required user data types? (App của bạn có thu thập dữ liệu người dùng không?)
  👉 **Trả lời:** **NO**.
- Mặc dù App có quét tín hiệu Bluetooth (như thiết bị xung quanh), nhưng app không thu thập, không gửi bất cứ gì lên Server. Mọi thứ được lưu nội bộ (Caching) trên điện thoại!

## 4. Chính sách bảo mật (Privacy Policy)
Google CH Play bắt buộc mọi App phải có 1 đường link dẫn đến Privacy Policy. 
Vì đây là App mã nguồn mở không thu thập dữ liệu tĩnh, bạn có thể copy nội dung sau dán vào 1 file trên Github Gist hoặc Google Docs (bật chế độ Công khai link), rồi copy link đó điền vào Play Console:

**Draft Privacy Policy (English):**
```text
JK BMS Monitor ("the App") respects your privacy and is committed to protecting it. 
This Privacy Policy clarifies that the App DOES NOT collect, store, or transmit any personal information, location data, or telemetry data to any external servers. 

- Bluetooth Permissions: The app requires Bluetooth permissions exclusively to scan for and connect to your local JK Battery Management System hardware.
- Local Storage: Any data saved (like MAC addresses or recent battery statistics) is stored purely locally on your personal device to enable App Widget functionality.

We do not use trackers, analytics, or ads.
For inquiries, please refer to the project's source code on GitHub.
```
