#ifndef CONFIG_H
#define CONFIG_H

#include <stdint.h>

// ============================================================================
// NexusFlow Smart Switch — Phase 1 Configuration
// Hardware: ESP32 DevKit V1, 6x TTP223 Touch Sensors, 6x Active-LOW Relays
// ============================================================================

// Relay Configuration
// 6-Channel Relay Config:
// constexpr uint8_t RELAY_COUNT = 6;
// constexpr uint8_t RELAY_PINS[RELAY_COUNT] = {19, 18, 5, 17, 16, 25};
// 4-Channel Relay Config:
constexpr uint8_t RELAY_COUNT = 4;
constexpr uint8_t RELAY_PINS[RELAY_COUNT] = {19, 18, 5, 17};

// Touch Sensor Configuration
// 6-Channel Touch Config:
// constexpr uint8_t TOUCH_COUNT = 6;
// constexpr uint8_t TOUCH_PINS[TOUCH_COUNT] = {32, 33, 14, 13, 12, 15};
// 4-Channel Touch Config:
constexpr uint8_t TOUCH_COUNT = 4;
constexpr uint8_t TOUCH_PINS[TOUCH_COUNT] = {32, 33, 14, 13};

// TTP223 Output Behavior
// In momentary/direct mode (default, no TOG jumper soldered):
// - OUT pin is HIGH (0x1) only while a finger is touching the pad
// - OUT pin is LOW (0x0) when no touch is detected
constexpr uint8_t TOUCH_ACTIVE_STATE = 0x1;  // HIGH literal value

// Debounce Configuration
// TOUCH_DEBOUNCE_MS: Minimum time the pad must read HIGH continuously before a press is accepted.
// This deglitches noise and ensures clean, intentional touches are registered.
// NOT a cooldown after release — the pad immediately re-enters IDLE when it reads LOW.
constexpr uint16_t TOUCH_DEBOUNCE_MS = 300;

// Polling Interval
// How often each touch pad is sampled in update() loop.
// 25ms = 40 Hz sampling, sufficient for capacitive touch response (>100 Hz hardware, but UI doesn't need that).
constexpr uint16_t TOUCH_SAMPLE_INTERVAL_MS = 25;

// Serial Communication
constexpr uint32_t SERIAL_BAUD_RATE = 115200;

// Device Information
constexpr char DEVICE_MODEL[] = "NexusFlow Smart Switch";
constexpr char FIRMWARE_VERSION[] = "v0.5.0";

// BLE Security
constexpr char DEVICE_PIN[] = "654321";

// WiFi Configuration
constexpr uint32_t WIFI_RETRY_INTERVAL_MS = 30000;  // 30 second retry interval

// Firebase Configuration
constexpr char FIREBASE_HOST[] = "your-project-id-default-rtdb.firebaseio.com";
constexpr char FIREBASE_SECRET[] = "YOUR_FIREBASE_DATABASE_SECRET";
constexpr uint32_t FIREBASE_SYNC_INTERVAL_MS = 5000;  // 5 seconds sync interval

// ============================================================================
// HARDWARE NOTES
// ============================================================================
// IMPORTANT: TTP223 modules must be powered from ESP32 3.3V rail (NOT 5V/VIN).
// Powering from 5V can drive OUT pin above safe GPIO input levels and damage the ESP32.
//
// STRAPPING PINS CAUTION:
// GPIO 12: Used as TOUCH_5 (Touch pin).
// GPIO 15: Used as TOUCH_6 (Touch pin).
// Both are ESP32 strapping pins affecting boot mode selection.
// TTP223 output is an externally-driven signal (not ESP32 internal touch sensing).
// Confirm the TTP223's idle/power-up state (LOW) does not interfere with boot-mode selection.
// In practice: GPIO12 and GPIO15 default to HIGH on power-up via internal pull-ups, but the external
// TTP223 OUT signal drives LOW at power-up → may impact JTAG/download mode if not handled.
// Recommendation: Test boot behavior with connected TTP223 modules, or use GPIO50/GPIO48 if available.
// ============================================================================

#endif // CONFIG_H
