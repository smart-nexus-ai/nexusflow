# NexusFlow ⚡ Smart Home Automation System

NexusFlow is an end-to-end, production-grade Smart Home Automation System. The repository is organized into three major components:

1. **📱 Android Application (`/app`)**: A modern Kotlin-based mobile application built using Jetpack Compose, MVVM + Clean Architecture, Dagger Hilt, Room Database, and Firebase Realtime Database.
2. **📟 ESP32 Firmware (`/Firmware`)**: Clean, modular C++ firmware for the ESP32 DevKit V1 microcontroller, featuring active-LOW relay drivers, capacitive touch sensors, BLE-based device provisioning, local credentials storage (NVS), and real-time synchronization.
3. **📄 Project Documentation & Report (`/Documentation`)**: The complete final year academic project report source (LaTeX) and the final compiled [NexusFlow_Report.pdf](/Documentation/NexusFlow_Report.pdf) (99 pages).

---

## 📁 Repository Structure

*   [**📱 Android App**](app/) — Kotlin source code, UI screens, ViewModel, Hilt DI modules, database schema.
*   [**📟 ESP32 Firmware**](Firmware/) — ESP32 firmware source code, WiFi and BLE managers, schedule manager.
*   [**📄 Project Report & PDF**](Documentation/) — LaTeX chapters, figures, references, and compiled academic report PDF.

---

## 🚀 Android App Features

*   **🏠 Home Dashboard**: Multi-room relay control, real-time environment sensor cards (Temperature, Humidity), and quick automation rules.
*   **📟 Device Management**: Supports 4 smart devices with a total of **22 relay channels**:
    *   **Living Room (8 Channels)** — Hardware ID: `SN-88CH-4F2A` (`NexusFlow 8CH Pro`)
    *   **Bedroom (6 Channels)** — Hardware ID: `SN-86CH-1B9C` (`NexusFlow 6CH Pro`)
    *   **Kitchen (4 Channels)** — Hardware ID: `SN-84CH-7D3E` (`NexusFlow 4CH Standard`)
    *   **Guestroom (4 Channels)** — Hardware ID: `SN-84CH-9E1F` (`NexusFlow 4CH Standard`)
*   **⏰ Smart Schedules**: Create time-based & duration automation schedules with conflict prevention.
*   **🎭 Preset Scenes**: Custom scene presets (Morning Routine, Movie Night, All Off, etc.) with clickable badge status toggles.
*   **🛠️ Developer Debug Controls**: Floating drawer to simulate active device count and toggle network connectivity states.

---

## 📟 ESP32 Firmware Features

*   **⚡ Local Control**: Local touch-to-relay control with debounce filtering.
*   **🔒 State Persistence**: Relay state persistence across power cycles (EEPROM/NVS).
*   **📡 Hybrid Connectivity**: BLE-based Wi-Fi credential provisioning and real-time Firebase RTDB sync.
*   **⚙️ Unified Codebase**: Supports multiple hardware variants (4/6/8-channel) via configuration flags.

---

## 🛠️ Build & Installation

### Requirements
*   Android Studio Ladybug (2024.2.1+) or newer
*   JDK 17
*   Android SDK 36 (Min SDK 26)
*   MiKTeX / TeX Live (optional, to compile report source)

### Building the Android App
```bash
# Compile Kotlin debug build
./gradlew compileDebugKotlin

# Build debug APK
./gradlew assembleDebug
```
