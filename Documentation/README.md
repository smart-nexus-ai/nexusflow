# README.md
## NexusFlow: Smart Home Automation System
### Final Year B.Tech Project Report — Development Guide

**Project Members:** Firdous Rahaman (34900322033) · Rishika Sarkar (34900322008) · Rajdeep Roy (34900322029) · Debarpita Sarkar (34900322035) · Tonima Bagchi (34900322037) · Priyanka Halder (34900323061)
**Supervisor:** Prof. Rajib Das · **Technical Facilitators:** Mr. Avisek Nandi, Mrs. Susmita Banik Barik
**Department:** Electronics and Communication Engineering
**College:** Cooch Behar Government Engineering College | **University:** MAKAUT

---

## Purpose of This Document

This is **not** the report itself — it is the master blueprint used to write it. It tells you exactly what goes in every chapter, which diagrams/screenshots are needed, where files live, and how figures/tables are numbered. Everything below has been updated to match the **actual captured app screenshots**, so Chapter 5 and Appendix D are no longer generic placeholders.

---

## Recommended Report Size

| Section | Pages |
|---|---|
| Front Matter | 8–10 |
| Ch.1 Introduction | 5–7 |
| Ch.2 Literature Review | 10–12 |
| Ch.3 System Design | 10–15 |
| Ch.4 Hardware Design | 10–12 |
| Ch.5 Software Design | 15–20 |
| Ch.6 Testing | 8–10 |
| Ch.7 Results | 5–8 |
| Ch.8 Future Scope | 3–5 |
| References + Appendix | 5–8 |
| **Total** | **~80–105 pages** |

---

## Folder Structure

```
NexusFlow_Report/
├── main.tex
├── references.bib
├── chapters/
│   ├── chapter1_introduction.tex
│   ├── chapter2_literature_review.tex
│   ├── chapter3_system_design.tex
│   ├── chapter4_hardware_design.tex
│   ├── chapter5_software_design.tex
│   ├── chapter6_testing.tex
│   ├── chapter7_results.tex
│   ├── chapter8_future_scope.tex
│   └── chapter9_conclusion.tex
├── images/
│   ├── architecture/
│   ├── hardware/
│   ├── app/
│   ├── database/
│   ├── firmware/
│   ├── testing/
│   ├── flowcharts/
│   └── screenshots/
│       ├── 01_onboarding/
│       ├── 02_auth/
│       ├── 03_home/
│       ├── 04_adddevice/
│       ├── 05_devices/
│       ├── 06_schedules/
│       ├── 07_scenes/
│       └── 08_settings/
├── tables/
├── diagrams/
└── appendix/
```

> **Naming convention for screenshots:** `screens/<folder>/<2-digit-order>_<short-name>.png` — e.g. `03_home/01_living_room_dashboard.png`. Keeping this consistent now saves re-numbering figures later in Overleaf.

---

## Chapter 1 — Introduction

**Contents:** 1.1 Smart Home Automation · 1.2 Problem Statement (manual operation, no remote access, no automation, energy wastage) · 1.3 Objectives · 1.4 Scope · 1.5 Motivation

**Image required:** `images/architecture/smart_home_concept.png` — generic smart-home / IoT concept illustration.

---

## Chapter 2 — Literature Survey

**Compare against:** Blynk, Tuya, Google Home, Home Assistant, Sinric Pro, ESPHome.

**Comparison table** (Parameter | Existing Systems | NexusFlow) — use real differentiators you actually built: offline-first Room cache + command queue, BLE-based first-time provisioning (no cloud dependency to pair), per-relay runtime tracking, presence-based sync-mode switching, single shared firmware across 4/6/8-channel variants.

**Research gaps to cite:** cloud dependency for basic control, recurring subscription models, limited on-device automation, poor offline resilience.

---

## Chapter 3 — System Architecture and Design *(most important chapter)*

### 3.1 Overall Architecture
```
Android App (Kotlin/Compose) ⇄ Firebase RTDB ⇄ ESP32 Firmware ⇄ Relays/Sensors
              ⇅ (BLE, first-time only)
```
Image: `images/architecture/system_architecture.png`

### 3.2 Device Provisioning Flow (matches your actual 5-step wizard)
```
Step 1: Prepare Device (power, BLE, WiFi checklist)
   ↓
Step 2: BLE Scan → discover "NexusFlow-XXXX"
   ↓
Step 3: Enter 6-digit Device PIN → Verify
   ↓
Step 4: Send WiFi SSID + Password over BLE (or Skip WiFi)
   ↓
Step 5: Pairing Complete → device summary (Hardware ID, Channels, Firmware, Paired Capacity)
```
Image: `images/flowcharts/provisioning_flow.png` — build this directly from your Step1–Step5 screenshots.

### 3.3 Presence Architecture
```
App opens/foregrounds → PresenceManager writes /presence/{deviceId}/{uid}/lastActive
   ↓
onDisconnect() cleanup on app close
   ↓
ESP32 reads presence node → switches sync interval:
   Active (recent lastActive) → 1-min sync
   Idle (stale lastActive)    → 15-min sync
```
Image: `images/architecture/presence_architecture.png`

### 3.4 Database Architecture
Two layers: Room (offline cache + command queue, source of truth for UI) and Firebase RTDB (real-time cloud sync). Include the full path map (`/devices`, `/device_states`, `/commands`, `/schedules`, `/scenes`, `/automation_rules`, `/presence`, `/billing_cycles`, `/logs`) as a figure — this can be taken almost directly from your `DATABASE.md`.
Image: `images/database/firebase_structure.png`

### 3.5 Data Flow Diagram
Cover both directions: (a) user toggles a relay in-app → Room write → Firebase `/commands` → ESP32 executes → `/device_states` updates → app listener updates Room → UI refreshes; (b) schedule/automation triggers device-side without the app open.
Image: `images/architecture/data_flow.png`

---

## Chapter 4 — Hardware Design

| Component | Image |
|---|---|
| ESP32 DevKit V1 (specs, pinout) | `images/hardware/esp32.jpg` |
| Relay module (active-LOW) | `images/hardware/relay.jpg` |
| TTP223 touch sensor | `images/hardware/ttp223.jpg` |
| AHT10 temp/humidity sensor | `images/hardware/aht10.jpg` |
| Full circuit diagram | `images/hardware/circuit_diagram.png` |
| PCB design (if available) | `images/hardware/pcb_design.png` |

**Include the locked GPIO table** as Appendix B and reference it here:

| Function | GPIOs |
|---|---|
| Relay 1–6 | 19, 18, 5, 17, 16, 25 |
| Touch 1–6 (TTP223, 3V3-powered) | 32, 33, 14, 13, 12, 15 |
| I2C SDA/SCL | 21 / 22 |
| Status LEDs (Green/Red) | 23 / 4 |
| Mode/pairing button | 0 |
| Mode-select slider | 27 |

Note the two hardware-safety callouts explicitly in the text: TTP223 must run off **3V3, not 5V** (GPIO overvoltage risk), and GPIO12/GPIO15 are strapping pins with a small but non-zero boot risk when used as touch inputs.

---

## Chapter 5 — Software Design and Implementation *(largest chapter)*

### 5.1 Android Architecture
MVVM + Clean Architecture: `UI (Compose) → ViewModel → UseCase (domain) → Repository → DataSource (Room / Firebase / BLE)`
Image: `images/app/app_architecture.png` and `images/app/mvvm.png`

### 5.2 Application Screenshots — updated to your actual captured screens

Use these exact groupings; each maps to one screenshot batch already captured:

**A. Onboarding (`01_onboarding/`)**
1. Splash screen ("Connecting your world. Intelligently.")
2. Welcome to NexusFlow
3. "Everything You Need" feature grid (Instant Control, Temp & Humidity, Scheduling, Scenes)
4. "Let's Get Started"

**B. Authentication (`02_auth/`)**
1. Continue with Google sign-in screen (Secure / Fast / Free badges)

**C. Home Dashboard (`03_home/`)**
1. Living Room — relay grid, environmental sensors, automation rules
2. Bedroom — relay grid, sensors, **Automation Rules panel expanded** (e.g. "AC ON if Temp > 30°C", "Fan OFF if Humidity < 40%")

**D. Add Device Wizard (`04_adddevice/`)** — all 5 steps
1. Step 1: Prepare Your Device
2. Step 2: Scanning for nearby devices (BLE list with RSSI)
3. Step 3: Enter Device PIN
4. Step 4: Connect to WiFi
5. Step 5: Pairing Complete (device summary card)

**E. Devices (`05_devices/`)**
1. My Devices list (4/4 devices, Wi-Fi/BLE status chips)
2. Device Details — Live Sensors + Relay Channels list
3. Device Details — scrolled to Device Information + action row (Rename / Restart / Update / Remove)
4. Edit Device Options bottom sheet (naming + icon picker)

**F. Schedules (`06_schedules/`)**
1. Schedules list (All / Active / Inactive filter pills)
2. Search bar in use ("light")
3. Filter by Device dropdown
4. Add Schedule — Steps 1–2 (Select Device *1 Schedule Max per Relay*, Action)
5. Add Schedule — device picker showing a relay grayed out as "Already Scheduled (Unavailable)"
6. Add Schedule — Trigger Type (Specific Time vs Duration Window) + Time Window
7. Add Schedule — Repeat & Days + Advanced Settings (Execution Notification, Enable Schedule, Set End Date)
8. Unsaved Changes confirmation dialog

**G. Scenes (`07_scenes/`)**
1. Scene Details — "Movie Night" (Devices in this Scene, Activate button)
2. Edit Scene Details sheet (name, description, icon picker)

**H. Settings (`08_settings/`)**
1. App Settings main screen (profile, theme mode, donate, rate, report issue, updates, terms)
2. Donate via UPI screen (QR code)
3. Logout confirmation dialog
4. Delete Account confirmation dialog (type-to-confirm)

> This gives you **~24 distinct app screenshots** — comfortably covers the "Application Screens" requirement in Section 5.2 and most of Appendix D.

### 5.3 Firmware Architecture
Image: `images/firmware/firmware_architecture.png`

**Modules to document** (one subsection each): `DeviceManager`, `RelayManager`, `TouchManager`, `SensorManager`, `BLEManager`, `WiFiManager`, `FirebaseManager`. For each, briefly note current implementation status vs. planned (e.g. `SensorManager` currently uses simulated AHT10 data pending real hardware wiring — worth an honest callout in a "Known Limitations" subsection).

### 5.4 Automation Engine
```
Sensor Reading (temp/humidity)
        ↓
Rule Evaluation (threshold + hysteresis, AND/OR for dual-sensor rules)
        ↓
Relay Action (ON/OFF) via applyCommand()
```
Note the design principle that **all** relay control paths — touch, BLE, Firebase, schedule, automation — funnel through a single `applyCommand()` function for consistent state handling. This is a good point to highlight as a design strength in viva.

---

## Chapter 6 — Testing and Validation

**Hardware tests:** relay switching test (photo: `images/testing/relay_test.jpg`), touch sensor response test (`images/testing/touch_test.jpg`), AHT10 sensor accuracy test (`images/testing/sensor_test.jpg`).

**Software tests:** Google sign-in test, device pairing/provisioning test (walk through Steps 1–5), realtime sync latency test, offline command-queue test (toggle relay with WiFi off, confirm it queues and drains on reconnect), one-schedule-per-relay constraint test (attempt duplicate schedule, confirm it's blocked/grayed out as shown in your screenshots).

**Table:** Test Case | Expected Result | Actual Result | Pass/Fail

---

## Chapter 7 — Results and Discussion

**Metrics to measure and graph** (store in `images/testing/graphs/`):
- Relay response time (command → physical switch, ms)
- BLE pairing time (scan → PIN verify → WiFi send, seconds)
- WiFi provisioning success rate
- Firebase sync latency (command write → `/device_states` update)
- AHT10 sensor accuracy vs. reference thermometer/hygrometer

---

## Chapter 8 — Future Scope

Matter protocol support · Home Assistant bridge · Voice assistant integration (Google Assistant/Alexa) · AI-based automation suggestions · Daily/monthly energy analytics with cost trend charts · Multiple time slots per schedule (superseding the current one-schedule-per-relay V1 constraint).

---

## Chapter 9 — Conclusion

Summarize completion status honestly per subsystem: Android app (9 screens, Phase 5 complete), ESP32 firmware (core modules complete; automation engine and real sensor wiring pending), cloud sync (Firebase RTDB integrated; schema alignment pending), BLE provisioning (functional; known Android-side scan bug being resolved). State which project objectives were fully met vs. partially met — examiners respond well to honest scoping over overclaiming.

---

## References

Target 15–25 sources: IEEE papers on IoT home automation, ESP32/Arduino official docs, Firebase RTDB docs, Android/Jetpack Compose docs, Bluetooth GATT/BLE specification docs.

---

## Appendix

- **Appendix A** — Firebase Database Structure (full path map + JSON examples, from `DATABASE.md`)
- **Appendix B** — ESP32 Pin Mapping (table above)
- **Appendix C** — API / Command Structure (`/commands/{deviceId}/{timestamp}` schema)
- **Appendix D** — Application Screenshots (all ~24 screens listed in 5.2, in order)
- **Appendix E** — Source Code Snippets (`applyCommand()` funnel, presence heartbeat logic, schedule conflict check)

---

## Figures Checklist (target 20–30)

System Architecture · Provisioning Flow · Presence Architecture · Firebase Database Structure · Data Flow Diagram · Circuit Diagram · Firmware Architecture · MVVM Architecture · Automation Engine Flow · GPIO Pinout Diagram · Testing Setup Photos (×3) · Results Graphs (×4–5) · App Screenshots (×24, Appendix D covers these separately from in-chapter figures)

## Tables Checklist (target 10–15)

Component/BOM List · GPIO Pin Configuration · Room Table Schema Summary · Firebase Path Map Summary · Existing-System Comparison · Test Case Results · Performance Metrics · Screenshot Index (maps each Appendix D figure number to its screen name)

---

## Final Deliverables

1. Thesis Report (PDF, Overleaf-compiled)
2. Overleaf project (shared/exported)
3. Android source code (repo or zip)
4. ESP32 firmware source
5. Circuit diagram
6. Presentation PPT
7. Demonstration video

---

### What changed vs. the original draft
- Chapter 5.2 and Appendix D replaced generic screenshot placeholders with the **actual 24 screens** you've captured, grouped by feature area with correct filenames/folders.
- Chapter 3.2's provisioning flow now matches your real 5-step wizard copy exactly.
- Added an honest "Known Limitations" note under 5.3 for the simulated-sensor gap, since examiners tend to ask about it directly.
- Added the one-schedule-per-relay constraint as a specific testable case in Chapter 6, since it's a distinctive design decision worth demonstrating live.
