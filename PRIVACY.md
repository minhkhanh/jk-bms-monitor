# Privacy Policy

**Effective Date: February 21, 2026**

This Privacy Policy applies to the **JK BMS Monitor** application ("the App") developed as an open-source project. This policy explains what information we collect, how we use it, and what rights you have.

## 1. Information Collection and Use

**We do not collect, store, transmit, or share any personal data.**

The JK BMS Monitor App is designed purely to communicate locally with your Bluetooth Battery Management System (BMS) hardware.

### 1.1 Bluetooth Data
The App requests Bluetooth permissions (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`) strictly to discover and establish a local connection with your JK BMS device. The data read from the BMS (such as voltage, current, and temperature) is displayed on your screen and is never transmitted over the internet.

### 1.2 Location Data (Android 11 and below)
If your device runs Android 11 or an older version, the Android operating system requires apps to hold Location permissions (`ACCESS_FINE_LOCATION`) in order to perform Bluetooth Low Energy (BLE) scanning.
- **We do not track your location.**
- **We do not collect GPS coordinates.**
This permission is requested solely to satisfy Android system requirements for detecting nearby Bluetooth devices. On Android 12 and above, the App uses the modern `BLUETOOTH_SCAN` permission and does not request location access at all.

### 1.3 Local Caching
The App may cache certain device information (such as the BMS MAC address or recent battery percentage) locally on your smartphone. This local caching is required to provide features like the Home Screen Widget and background data synchronization. This data never leaves your device and is kept locally within the App's isolated sandbox environment.

## 2. Third-Party Services
We do not use any third-party analytics, tracking scripts, or advertising networks. The App operates completely offline (except for local Bluetooth connections) and does not require an active internet connection to function.

## 3. Open Source Transparency
JK BMS Monitor is an open-source project. You can inspect the entire source code to verify that no tracking or data collection mechanisms are implemented. The source code is publicly available on our GitHub repository.

## 4. Changes to This Privacy Policy
We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page. These changes are effective immediately after they are posted.

## 5. Contact Us
If you have any questions or suggestions about our Privacy Policy, do not hesitate to open an issue on our GitHub repository.
