// ============================================================================
// NexusFlow Smart Switch — Phase 4 + Phase 5 Firmware
// ESP32 DevKit V1 | 6 TTP223 Touch Pads → 6 Active-LOW Relays
// BLE Pairing + WiFi Provisioning + Persistent State + Firebase Integration
// ============================================================================
// Phase 4 + Phase 5 Features:
// - Local touch-to-relay control (Phase 1 baseline)
// - Relay state persistence across power cycles (Phase 2)
// - Hardware identity and device information (Phase 3)
// - BLE server for Android app pairing and WiFi credential provisioning (Phase 4)
// - WiFi connectivity with auto-reconnect and credential storage (Phase 5)
// - Firebase RTDB synchronization for cloud control and telemetry (Phase 6)
// - Schedule management with time-based automation (Phase 8)
// - Environmental sensor telemetry (temperature, humidity, humidex) (Phase 9)
// - Future-proof for OTA/Presence in next phases
// ============================================================================

#include "Config.h"
#include "DeviceManager.h"
#include "PreferencesManager.h"
#include "RelayManager.h"
#include "TouchManager.h"
#include "BLEManager.h"
#include "WiFiManager.h"
#include "SensorManager.h"
#include "ScheduleManager.h"
#include "FirebaseManager.h"

// Global instances.
DeviceManager device_manager;
PreferencesManager prefs_manager;
RelayManager relay_manager(&prefs_manager);
TouchManager touch_manager(&relay_manager);
BLEManager ble_manager(&device_manager);
WiFiManager wifi_manager(&ble_manager);
SensorManager sensor_manager;
ScheduleManager schedule_manager(&relay_manager);
FirebaseManager firebase_manager(&wifi_manager, &relay_manager, &device_manager, &sensor_manager, &schedule_manager);

// ============================================================================
// setup(): Initialize hardware, managers, and connectivity
// ============================================================================
void setup() {
  // Initialize serial communication at configured baud rate.
  Serial.begin(SERIAL_BAUD_RATE);

  // Brief stabilization delay (one-time startup delay is acceptable).
  delay(500);

  // Print startup banner.
  Serial.println("\n================================");
  Serial.println("NexusFlow Firmware");
  Serial.println("Phase 7 + Phase 8 Enabled");
  Serial.println("================================\n");

  // Initialize DeviceManager first: generates hardware ID and device info.
  device_manager.begin();

  // Initialize PreferencesManager: open NVS namespace "nexusflow".
  prefs_manager.begin();

  // Initialize RelayManager: loads saved states and restores relay outputs.
  relay_manager.begin();

  // Initialize TouchManager: poll-based state machine with local debounce.
  touch_manager.begin();

  // Initialize SensorManager: environmental metrics
  sensor_manager.begin();

  // Initialize ScheduleManager: timer and duration rules
  schedule_manager.begin();

  // Initialize BLEManager: start BLE server for app pairing.
  ble_manager.begin();

  // Initialize WiFiManager: load credentials and auto-connect.
  wifi_manager.begin();

  // Initialize FirebaseManager: cloud RTDB synchronizer
  firebase_manager.begin();

  // Create background FreeRTOS task for Firebase updates on Core 0
  // This keeps synchronous network/HTTP operations from blocking capacitive touch sensors on Core 1
  xTaskCreatePinnedToCore(
      [](void* pvParameters) {
          for (;;) {
              firebase_manager.update();
              vTaskDelay(pdMS_TO_TICKS(100));
          }
      },
      "FirebaseTask",
      10000,
      NULL,
      1,
      NULL,
      0
  );

  Serial.println("System Ready\n");
}

// ============================================================================
// loop(): Main control loop with all manager updates
// ============================================================================
void loop() {
  // Update DeviceManager for uptime tracking and future enhancements.
  device_manager.update();

  // Update RelayManager to track runtimes and execute periodic NVS saves
  relay_manager.update();

  // Poll all touch pads for presses and fire toggles on debounced confirmation.
  touch_manager.update();

  // Update SensorManager for environmental readings
  sensor_manager.update();

  // Update ScheduleManager to check and fire automation rules
  schedule_manager.update();

  // Update BLEManager for characteristic updates and future enhancements.
  ble_manager.update();

  // Update WiFiManager for connection polling and retry logic.
  wifi_manager.update();

  // Yield to allow system tasks (watchdog, etc.) to execute.
  delay(1);
}

// ============================================================================
// ARCHITECTURE NOTES — Phase 4 + Phase 5 + Phase 6 + Phase 8 + Phase 9
// ============================================================================
// Manager Hierarchy:
// 1. DeviceManager: Hardware identity, uptime, firmware info (highest level)
// 2. PreferencesManager: NVS storage abstraction
// 3. RelayManager: GPIO control + state tracking + NVS integration + runtime tracking
// 4. TouchManager: Touch sensing + debounce + RelayManager toggle
// 5. SensorManager: Temperature, humidity, humidex calculations
// 6. ScheduleManager: Time-based automation rules (start/end times, days of week)
// 7. BLEManager: Bluetooth connectivity, provisioning, device discovery
// 8. WiFiManager: WiFi connectivity, auto-reconnect, credential storage
// 9. FirebaseManager: Cloud RTDB sync, remote control, telemetry push (highest level)
//
// Initialization Order (CRITICAL):
// 1. Serial (in setup())
// 2. DeviceManager.begin() — generates IDs, prints device info
// 3. PreferencesManager.begin() — opens NVS namespace "nexusflow"
// 4. RelayManager.begin() — loads saved states, restores outputs, loads runtimes
// 5. TouchManager.begin() — initializes touch pins
// 6. SensorManager.begin() — initializes sensor readings
// 7. ScheduleManager.begin() — prepares schedule engine
// 8. BLEManager.begin() — starts BLE server with device name from DeviceManager
// 9. WiFiManager.begin() — loads credentials and attempts auto-connect
// 10. FirebaseManager.begin() — prepares cloud sync
//
// Main Loop:
// 1. DeviceManager.update() — updates uptime tracking
// 2. RelayManager.update() — handles runtime accumulation and periodic NVS saves
// 3. TouchManager.update() — polls touch sensors, fires toggles
// 4. SensorManager.update() — updates sensor readings
// 5. ScheduleManager.update() — checks and fires automation schedules
// 6. BLEManager.update() — handles BLE events (passive in this phase)
// 7. WiFiManager.update() — handles WiFi connection polling and retries
// 8. FirebaseManager.update() — syncs with cloud on interval
//
// Data Flow on Touch:
// TTP223 output → TouchManager.update() → RelayManager.toggleRelay()
//                                        → RelayManager.setRelayInternal()
//                                        → digitalWrite() (GPIO change)
//                                        → PreferencesManager.saveRelayState() (NVS save)
//                                        → PreferencesManager.saveRelayRuntime() (NVS save)
//
// Data Flow on BLE Provisioning:
// Android App → BLE Write (WiFi JSON + PIN)
//             → BLEManager.parseWiFiProvisionJSON()
//             → WiFiManager.connect(ssid, password)
//             → WiFi.begin() (background connection)
//
// Data Flow on Firebase Sync:
// FirebaseManager.update() checks WiFi connection
//   if (wifi_manager.isConnected())
//     → FirebaseManager.syncWithFirebase()
//       → HTTP GET: fetch relay states + schedules from Firebase
//       → Parse JSON: extract relay isOn, schedule rules
//       → Apply local changes: RelayManager.setRelay() if state differs
//       → Schedule manager: clearSchedules(), addSchedule() for each rule
//       → HTTP PATCH: push device status (lastSeen, uptime, temp, humidity)
//
// Future Phases can add:
// - OTAUpdateManager (Phase 7): Over-the-air firmware updates
//   - Uses device_manager.getFirmwareVersion() for version checking
//   - Needs WiFi: checks wifi_manager.isConnected()
//   - Downloads and verifies firmware over HTTPS
//
// - PresenceManager (Phase 10): WiFi-based presence detection
//   - Detects device proximity by WiFi signal strength
//   - Triggers presence-based automation
//   - Integrates with ScheduleManager for flexible triggers
//
// This modular design allows each manager to:
// 1. Initialize independently in setup().
// 2. Poll independently in loop() (non-blocking via millis() timers).
// 3. Coordinate via shared manager references.
// 4. Scale to 15+ features without touching existing code.
// ============================================================================

// ============================================================================
// SERIAL OUTPUT EXAMPLE — Phase 7 + Phase 8
// ============================================================================
// ================================
// NexusFlow Firmware
// Phase 7 + Phase 8 Enabled
// ================================
//
// ================================
// NexusFlow Firmware
// Phase 3 Device Manager
// ================================
//
// Device Name:
// NexusFlow-2233
//
// Hardware ID:
// SN-86CH-2233
//
// MAC Address:
// A8:42:E3:11:22:33
//
// Firmware Version:
// v0.5.0
//
// Device Model:
// NexusFlow Smart Switch
//
// Relay Count:
// 6
//
// Device Manager Initialized
//
// Preferences Manager Initialized
//
// Restoring Relay States...
// Relay 1 = OFF (Runtime: 0 min, remainder 0 sec)
// Relay 2 = ON (Runtime: 120 min, remainder 45 sec)
// Relay 3 = OFF (Runtime: 0 min, remainder 0 sec)
// Relay 4 = OFF (Runtime: 0 min, remainder 0 sec)
// Relay 5 = OFF (Runtime: 0 min, remainder 0 sec)
// Relay 6 = ON (Runtime: 87 min, remainder 23 sec)
// Restore Complete
//
// Relay Manager Initialized
// Touch Manager Initialized
// BLE Started
// BLE Device Name: NexusFlow-2233
// Service UUID: 12345678-1234-5678-1234-56789abcdef0
// Advertising...
//
// Checking WiFi Credentials...
// SSID: HomeWiFi
// Password: (loaded from storage)
// Attempting WiFi Connection...
// Connecting to WiFi: HomeWiFi
// Connected to WiFi
// IP Address: 192.168.1.105
// RSSI: -52 dBm
// NTP Time Sync Configured
//
// Firebase Manager Initialized
// System Ready
//
// (During Firebase Sync)
// Firebase Command: Switch Relay 1 ON
// Relay 1 ON
// Saving Relay 1 State...
// DEBUG: NVS Save - Key 'relay_0' = ON (bytes written)
// Saved
// Schedule Triggered: Relay 3 ON
// Relay 3 ON
// Saving Relay 3 State...
// DEBUG: NVS Save - Key 'relay_2' = ON (bytes written)
// Saved
//
// ============================================================================
