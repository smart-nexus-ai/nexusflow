#include "RelayManager.h"
#include "PreferencesManager.h"
#include <Arduino.h>

// ============================================================================
// RelayManager Implementation with Preferences Integration
// ============================================================================

RelayManager::RelayManager(PreferencesManager* prefs_manager)
    : prefs_manager(prefs_manager) {
  // Initialize all relay states and runtime to OFF/0.
  for (uint8_t i = 0; i < RELAY_COUNT; i++) {
    relayStates[i] = false;
    relayOnStartMillis[i] = 0;
    totalRuntimeMinutes[i] = 0;
    remainderRuntimeSeconds[i] = 0;
  }
}

void RelayManager::begin() {
  // Initialize all relay pins as OUTPUT.
  for (uint8_t i = 0; i < RELAY_COUNT; i++) {
    pinMode(RELAY_PINS[i], OUTPUT);
    digitalWrite(RELAY_PINS[i], 0x1);  // HIGH = OFF in active-LOW mode
    relayStates[i] = false;
  }

  Serial.println("\nRestoring Relay States...");

  // Load all saved relay states and runtime values from NVS
  uint8_t loaded_count = prefs_manager->loadAllStates(relayStates, RELAY_COUNT);
  for (uint8_t i = 0; i < RELAY_COUNT; i++) {
    prefs_manager->loadRelayRuntime(i, totalRuntimeMinutes[i], remainderRuntimeSeconds[i]);
  }

  // Restore each relay to its saved state and print status.
  for (uint8_t i = 0; i < RELAY_COUNT; i++) {
    setRelayInternal(i, relayStates[i], false, false);

    Serial.print("Relay ");
    Serial.print(i + 1);
    Serial.print(" = ");
    Serial.print(relayStates[i] ? "ON" : "OFF");
    Serial.print(" (Runtime: ");
    Serial.print(totalRuntimeMinutes[i]);
    Serial.print(" min, remainder ");
    Serial.print(remainderRuntimeSeconds[i]);
    Serial.println(" sec)");
  }

  Serial.println("Restore Complete\n");
  Serial.println("Relay Manager Initialized");
}

void RelayManager::setRelay(uint8_t index, bool state) {
  setRelayInternal(index, state, true, true);
}

void RelayManager::toggleRelay(uint8_t index) {
  if (!isValidIndex(index)) {
    return;
  }
  setRelayInternal(index, !relayStates[index], true, true);
}

bool RelayManager::getRelayState(uint8_t index) {
  if (!isValidIndex(index)) {
    return false;
  }
  return relayStates[index];
}

void RelayManager::turnAllOff() {
  for (uint8_t i = 0; i < RELAY_COUNT; i++) {
    setRelayInternal(i, false, false, true);
  }
}

void RelayManager::turnAllOn() {
  for (uint8_t i = 0; i < RELAY_COUNT; i++) {
    setRelayInternal(i, true, false, true);
  }
}

void RelayManager::setRelayInternal(uint8_t index, bool state, bool log, bool save) {
  if (!isValidIndex(index)) {
    return;
  }

  if (relayStates[index] != state) {
    bool old_state = relayStates[index];
    relayStates[index] = state;

    digitalWrite(RELAY_PINS[index], state ? 0x0 : 0x1);

    if (log) {
      Serial.print("Relay ");
      Serial.print(index + 1);
      Serial.println(state ? " ON" : " OFF");
    }

    // Handle runtime timer start and stop
    if (state) {
      relayOnStartMillis[index] = millis();
    } else if (old_state) {
      uint32_t current_time = millis();
      uint32_t elapsed_ms = 0;
      if (current_time < relayOnStartMillis[index]) {
        elapsed_ms = (UINT32_MAX - relayOnStartMillis[index]) + current_time;
      } else {
        elapsed_ms = current_time - relayOnStartMillis[index];
      }
      uint32_t elapsed_seconds = elapsed_ms / 1000;

      remainderRuntimeSeconds[index] += elapsed_seconds;
      if (remainderRuntimeSeconds[index] >= 60) {
        totalRuntimeMinutes[index] += remainderRuntimeSeconds[index] / 60;
        remainderRuntimeSeconds[index] %= 60;
      }

      if (prefs_manager != nullptr) {
        prefs_manager->saveRelayRuntime(index, totalRuntimeMinutes[index], remainderRuntimeSeconds[index]);
      }
    }

    if (save && prefs_manager != nullptr) {
      if (log) {
        Serial.print("Saving Relay ");
        Serial.print(index + 1);
        Serial.println(" State...");
      }

      if (prefs_manager->saveRelayState(index, state)) {
        if (log) {
          Serial.println("Saved");
        }
      }
    }
  }
}

void RelayManager::update() {
  uint32_t current_time = millis();
  static uint32_t last_periodic_check = 0;

  if (current_time - last_periodic_check >= 10000) {
    last_periodic_check = current_time;

    for (uint8_t i = 0; i < RELAY_COUNT; i++) {
      if (relayStates[i]) {
        uint32_t elapsed_ms = 0;
        if (current_time < relayOnStartMillis[i]) {
          elapsed_ms = (UINT32_MAX - relayOnStartMillis[i]) + current_time;
        } else {
          elapsed_ms = current_time - relayOnStartMillis[i];
        }

        uint32_t elapsed_seconds = elapsed_ms / 1000;
        if (elapsed_seconds >= 60) {
          uint32_t elapsed_minutes = elapsed_seconds / 60;
          totalRuntimeMinutes[i] += elapsed_minutes;
          relayOnStartMillis[i] += elapsed_minutes * 60 * 1000;

          if (prefs_manager != nullptr) {
            prefs_manager->saveRelayRuntime(i, totalRuntimeMinutes[i], remainderRuntimeSeconds[i]);
          }
        }
      }
    }
  }
}

uint32_t RelayManager::getRelayRuntime(uint8_t index) {
  if (!isValidIndex(index)) return 0;
  uint32_t total_seconds = totalRuntimeMinutes[index] * 60 + remainderRuntimeSeconds[index];
  if (relayStates[index]) {
    uint32_t elapsed_ms = millis() - relayOnStartMillis[index];
    total_seconds += elapsed_ms / 1000;
  }
  return total_seconds;
}

bool RelayManager::isValidIndex(uint8_t index) {
  if (index >= RELAY_COUNT) {
    Serial.print("ERROR: Relay index ");
    Serial.print(index);
    Serial.print(" out of range [0-");
    Serial.print(RELAY_COUNT - 1);
    Serial.println("]");
    return false;
  }
  return true;
}
