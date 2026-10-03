# Ki-Client 🚀

**Ki-Client** is a modern, high-performance, and resilient Android proxy and VPN client built on top of [Xray-core](https://github.com/XTLS/Xray-core). Designed for maximum privacy, fluid user experience, and stability even under restrictive network environments.

[![Latest Release](https://img.shields.io/github/v/release/ffr3nz0/ki-client?color=10B981&label=Release&logo=github)](https://github.com/ffr3nz0/ki-client/releases)
[![Android Support](https://img.shields.io/badge/Android-7.0%2B%20(API%2024%2B)-blue?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-GPL--3.0-orange.svg)](LICENSE)

---

## 🌟 Key Features / ویژگی‌های کلیدی

### 🎨 Modern Liquid Glass & Material 3 UI
- Clean, intuitive interface redesigned with Jetpack Compose & Material 3.
- Optimized typography and layout with high readability across all screen sizes.
- Smooth transitions and edge-to-edge support.

### 🔘 1x1 Quick-Toggle Home Screen Widget
- Redesigned circular 1x1 toggle widget with transparent background that sits naturally on any wallpaper.
- Real-time connection feedback (emerald green when active, deep slate when inactive).
- Instant VPN toggle with a single tap.

### 🌐 Smart DNS Selector & Benchmark
- Built-in DNS benchmark utility to test international and clean DNS resolvers on your current connection.
- Auto-selection and preset modes to bypass ISP-level DNS poisoning and throttling.

### 📡 LAN & Hotspot Sharing
- Share your VPN connection with Smart TVs, game consoles, laptops, and PCs connected to your phone's Wi-Fi hotspot.
- Displays local proxy IP and port (`10809`) with quick copy actions.

### ⚡ Smart Auto-Select
- One-tap ping measurement to automatically connect to the fastest, lowest-latency server available.

### 🛡️ User-Friendly Core Error Diagnostics
- Replaces cryptic internal Go errors with clear, localized explanations in 9 languages (including Persian, English, Arabic, Russian, Chinese, Vietnamese, Bengali, and Lori).
- Clear guidance on protocol security rules (e.g., explaining why unencrypted VLESS without TLS is blocked on public networks).

### 🔄 Resilient Architecture & Self-Healing
- Automated storage cache migration and self-healing to prevent configuration corruption across updates.
- Robust socket lifecycle management to prevent memory and port leaks.

### 📦 Supported Protocols
- **VLESS** (Reality, TLS, WebSocket, gRPC, HTTPUpgrade, XHTTP)
- **VMess** (AEAD, WebSocket, TCP, gRPC)
- **Trojan** (TLS, gRPC, WebSocket)
- **Shadowsocks** (AEAD ciphers)
- **WireGuard**
- **Hysteria 2**
- **SOCKS5 & HTTP**

---

## 📥 Download / دانلود

Download the latest version directly from GitHub Releases:

👉 **[Download Ki-Client on GitHub Releases](https://github.com/ffr3nz0/ki-client/releases/latest)**

| Asset | Target Architecture | Description |
| :--- | :--- | :--- |
| **`K-Client.apk`** | Universal | Recommended for most users (supports all devices) |
| **`K-Client_v..._arm64-v8a.apk`** | 64-bit ARM | Optimized smaller file for modern phones |
| **`K-Client_v..._armeabi-v7a.apk`** | 32-bit ARM | For older Android devices |
| **`K-Client_v..._x86_64.apk`** | 64-bit x86 | For Android Emulators & Chromebooks |

---

## 🛠️ Building from Source / کامپایل پروژه

1. **Clone the repository:**
   ```bash
   git clone https://github.com/ffr3nz0/ki-client.git
   cd ki-client/V2rayNG
   ```

2. **Build with Gradle:**
   - **Windows (PowerShell):**
     ```powershell
     .\gradlew.bat :app:assemblePlaystoreRelease
     ```
   - **Linux / macOS:**
     ```bash
     ./gradlew :app:assemblePlaystoreRelease
     ```

3. Output APKs will be located in:
   `V2rayNG/app/build/outputs/apk/playstore/release/`

---

## 🔒 Security & Privacy

- **Package ID:** `com.kclient.ang`
- Uses official Android `VpnService` strictly for user-initiated proxy routing.
- Zero analytics trackers, zero telemetry, no third-party tracking SDKs.
- 100% open-source and verifiable.

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for details.
