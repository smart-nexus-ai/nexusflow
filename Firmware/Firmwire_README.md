# NexusFlow Firmware

ESP32-based smart switch firmware for the **NexusFlow Smart Home Ecosystem**.

NexusFlow is designed as a modular, scalable firmware architecture supporting:

- Touch-based wall switches
- Relay control
- BLE pairing
- Wi-Fi provisioning
- Firebase cloud synchronization
- OTA firmware updates
- Scheduling & automation
- Environmental sensing

---

# Project Status

Current Development Phase:

```text
Phase 1  ✅ Touch → Relay Control
Phase 2  ✅ Relay State Persistence
Phase 3  ✅ Device Manager (Hardware identity, MAC matching)
Phase 4  ✅ BLE Pairing & GATT Server Setup
Phase 5  ✅ WiFi Provisioning & NTP Time Sync
Phase 6  ✅ Firebase Integration (HTTPS/REST bi-directional sync)
Phase 7  ✅ Sensor Manager (Environmental telemetry & Humidex tracking)
Phase 8  ✅ Direct State Sync & Local Automation Rules Engine (Direct state pull, loop prevention, schedules, environmental automation rules)
Phase 9  ⏳ OTA Updates
```

---

# Hardware Specifications

## Controller

- ESP32 DevKit V1
- ESP-WROOM-32

---

## Touch Inputs

- 6 × TTP223 Capacitive Touch Modules

Configuration:

```text
Mode: Momentary
Output: Active HIGH
A Pad: Open
B Pad: Open
```

Touch Logic:

```text
Touched     -> HIGH
Released    -> LOW
```

---

## Relay Outputs

- 6 Channel Relay Module
- Active LOW Relays

Logic:

```text
GPIO LOW    -> Relay ON
GPIO HIGH   -> Relay OFF
```

---

# Pin Mapping

## Relay Pins

| Relay | GPIO |
|---------|---------|
| Relay 1 | GPIO19 |
| Relay 2 | GPIO18 |
| Relay 3 | GPIO5 |
| Relay 4 | GPIO17 |
| Relay 5 | GPIO16 |
| Relay 6 | GPIO25 |

---

## Touch Pins

| Touch | GPIO |
|---------|---------|
| Touch 1 | GPIO32 |
| Touch 2 | GPIO33 |
| Touch 3 | GPIO14 |
| Touch 4 | GPIO13 |
| Touch 5 | GPIO12 |
| Touch 6 | GPIO15 |

---

# Architecture

The firmware follows a modular manager-based architecture.

```text
NexusFlow_Firmware
│
├── Config
│
├── DeviceManager
│
├── PreferencesManager
│
├── RelayManager
│
├── TouchManager
│
├── BLEManager
│
├── WiFiManager
│
├── FirebaseManager
│
├── SensorManager
│
├── ScheduleManager
│
└── OTAUpdateManager
```

Every module contains:

```cpp
begin()
update()
```

This ensures:

- Non-blocking execution
- Easy scalability
- Clean code separation
- Future expansion without refactoring

---

# Current Folder Structure

```text
NexusFlow_Firmware/

Config.h

RelayManager.h
RelayManager.cpp

TouchManager.h
TouchManager.cpp

NexusFlow_Firmware.ino
```

Future:

```text
NexusFlow_Firmware/

Config.h

DeviceManager.h
DeviceManager.cpp

PreferencesManager.h
PreferencesManager.cpp

RelayManager.h
RelayManager.cpp

TouchManager.h
TouchManager.cpp

BLEManager.h
BLEManager.cpp

WiFiManager.h
WiFiManager.cpp

FirebaseManager.h
FirebaseManager.cpp

SensorManager.h
SensorManager.cpp

ScheduleManager.h
ScheduleManager.cpp

OTAUpdateManager.h
OTAUpdateManager.cpp

NexusFlow_Firmware.ino
```

---

# Phase 1 Features

## Touch → Relay Control

Features:

- 6 Touch Inputs
- 6 Relays
- 1:1 Mapping
- Debounced Touch Detection
- Active LOW Relay Support
- millis() Based Polling
- No delay()

Example:

```text
Touch 1
↓
Relay 1 Toggle

Touch 2
↓
Relay 2 Toggle
```

---

# Touch State Machine

```text
IDLE
 │
 ▼
CANDIDATE
 │
 ▼
TOUCHED
 │
 ▼
IDLE
```

Behavior:

```text
Finger Touch
↓
Debounce
↓
Relay Toggle
↓
Wait Release
↓
Ready Again
```

---

# Relay Logic

```cpp
relayManager.toggleRelay(index);
```

Relay State:

```cpp
true  = ON
false = OFF
```

Physical Output:

```cpp
true  -> GPIO LOW
false -> GPIO HIGH
```

---

# Coding Standards

## Rules

- No delay()
- millis() only
- Modular architecture
- No dynamic memory allocation
- Separate .h and .cpp files
- Production-oriented code
- Hardware abstraction layer

---

# Future Features

## Device Manager

Provides:

- Hardware ID
- Device Name
- Firmware Version
- Uptime

Example:

```text
Hardware ID:

SN-6CH-ABCD
```

## BLE Pairing & Name

BLE Name advertised is **`NexusFlow-XXXX`** (where `XXXX` is the last 4 hex characters of the MAC address).

Dynamic 6-Digit PIN calculation:
- Suffix is the last 4 hex digits of the MAC address.
- Prefix:
  - `6` Relays: `"9"`
  - `8` Relays: `"5"`
  - `4` Relays: `"8"`
  - Default: `"3"`
- PIN = Hex-to-Decimal conversion of `(prefix + suffixHex)`. E.g., `0x9ABCD` = `633805`.

---

## BLE GATT Characteristics

Pairing and provisioning utilizes a 3-characteristic service model:
1. **Device Info Characteristic (`12345678-1234-5678-1234-56789abcdef1` - Read)**:
   Returns JSON metadata about the hardware:
   ```json
   {
     "hardwareId": "SN-6CH-ABCD",
     "deviceName": "NexusFlow-ABCD",
     "firmwareVersion": "v1.2.5",
     "relayCount": 6
   }
   ```
2. **WiFi Provisioning Characteristic (`12345678-1234-5678-1234-56789abcdef2` - Write)**:
   Accepts JSON credentials payload from the App:
   ```json
   {
     "ssid": "<SSID>",
     "password": "<PASSWORD>",
     "pin": "<PIN>",
     "deviceId": "<DEVICE_ID>"
   }
   ```
3. **Status Characteristic (`12345678-1234-5678-1234-56789abcdef3` - Read/Notify)**:
   Emits JSON pairing and connection progress notifications to the App:
   - `{"status":"pin_verified"}` (On-device PIN verification passed)
   - `{"status":"pin_failed"}` (PIN verification failed)
   - `{"status":"wifi_connecting"}` (Attempting WiFi connection)
   - `{"status":"wifi_failed","error":"WL_CONNECT_FAILED"}` (WiFi connection failed)
   - `{"status":"registered"}` (WiFi connected and registered in Firebase)

---

## Firebase RTDB Flat Schema Sync

Bi-directional real-time sync mapping:
- ESP32 writes:
  - `/devices/{deviceId}` (Device metadata)
  - `/device_states/{deviceId}` (Relays state, sensors, online flag)
- ESP32 listens to:
  - `/commands/{deviceId}` (Toggles execution)
  - `/schedules/{deviceId}` (Schedules configuration)

---

## Sensors

Planned Sensors:

- AHT10
- BME280
- BME680
- LD2410
- PIR
- Door Sensors

---

## OTA Updates

Future:

```text
App
↓
Firebase
↓
ESP32
↓
OTA Firmware Update
```

---

# Build Environment

## Arduino IDE

Recommended:

```text
ESP32 Board Package:
3.x or newer
```

Board:

```text
ESP32 Dev Module
```

---

# Required Libraries

Current Phase:

```text
No external libraries required
```

Future Phases:

```text
Preferences
WiFi
ESP32 BLE
Firebase Client
ArduinoJson
AHT10 Library
```

---

# Serial Output Example

```text
================================
NexusFlow Firmware Phase 1
6 Touch / 6 Relay Test
================================

Relay Manager Initialized

Touch Manager Initialized

Touch detected on Touch 1

Relay 1 ON

Touch detected on Touch 3

Relay 3 ON

Touch detected on Touch 3

Relay 3 OFF
```

---

# NexusFlow Ecosystem

Firmware is part of the complete NexusFlow platform:

```text
ESP32 Firmware
        │
        ▼
Firebase RTDB
        │
        ▼
NexusFlow Android App
        │
        ▼
Smart Home Automation
```

---

# Advanced System Features

### 1. NTP Time Synchronization
- On successful Wi-Fi connection, the firmware calls `configTime()` with GMT+5:30 offset (`19800` seconds) pointing to `pool.ntp.org` and `time.nist.gov`.
- Automatically schedules background network time synchronization for timestamp-based scheduling and logging consistency.

### 2. Millis Rollover & Overflow Protection
- Handles the standard ESP32 `millis()` overflow (occurs roughly every 49.7 days) safely using unsigned subtraction checks:
  ```cpp
  if (current_time < start_time) {
      elapsed = (UINT32_MAX - start_time) + current_time;
  } else {
      elapsed = current_time - start_time;
  }
  ```
- Prevents runtime counter stalls or jumps, maintaining solid diagnostic uptime.

### 3. NVS Sub-Minute Remainder Tracking
- To protect ESP32 flash memory from write wear-out, total relay active minutes are updated periodically (every 60 seconds) or immediately on state transition (turning OFF).
- The sub-minute remainder seconds (0–59) are tracked in memory and saved to NVS dynamically upon toggle transitions or at 60-second intervals to minimize write operations while preserving high accuracy across unexpected reboots or power loss.

---

# Author

Firdous Rahaman

Project:

NexusFlow Smart Home Automation System

Platform:

ESP32 + Firebase + Android (Jetpack Compose)

License:

MIT License