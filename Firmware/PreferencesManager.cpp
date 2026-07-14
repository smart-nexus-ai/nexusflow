#include "PreferencesManager.h"
#include <Preferences.h>
#include <Arduino.h>

static Preferences preferences;

PreferencesManager::PreferencesManager() : is_initialized(false) {}

void PreferencesManager::begin() {
  if (!preferences.begin("nexusflow", false)) {
    Serial.println("ERROR: Failed to open Preferences namespace 'nexusflow'");
    return;
  }

  is_initialized = true;
  Serial.println("Preferences Manager Initialized");
}

bool PreferencesManager::saveRelayState(uint8_t relay_index, bool state) {
  if (!is_initialized) {
    Serial.println("ERROR: PreferencesManager not initialized. Call begin() first.");
    return false;
  }

  if (!isValidRelayIndex(relay_index)) {
    return false;
  }

  char key[16];
  getKeyForRelay(relay_index, key, sizeof(key));

  try {
    size_t bytes_written = preferences.putUChar(key, state ? 0x1 : 0x0);
    if (bytes_written > 0) {
      Serial.print("DEBUG: NVS Save - Key '");
      Serial.print(key);
      Serial.print("' = ");
      Serial.print(state ? "ON" : "OFF");
      Serial.println(" (bytes written)");
      return true;
    } else {
      Serial.print("ERROR: NVS write returned 0 bytes for key '");
      Serial.print(key);
      Serial.println("'");
      return false;
    }
  } catch (...) {
    Serial.print("ERROR: Failed to save relay ");
    Serial.print(relay_index + 1);
    Serial.println(" state to NVS");
    return false;
  }
}

bool PreferencesManager::loadRelayState(uint8_t relay_index, bool& out_state) {
  if (!is_initialized) {
    Serial.println("ERROR: PreferencesManager not initialized. Call begin() first.");
    return false;
  }

  if (!isValidRelayIndex(relay_index)) {
    return false;
  }

  char key[16];
  getKeyForRelay(relay_index, key, sizeof(key));

  if (!preferences.isKey(key)) {
    Serial.print("DEBUG: NVS Load - Key '");
    Serial.print(key);
    Serial.println("' not found in NVS");
    return false;
  }

  try {
    uint8_t stored_value = preferences.getUChar(key, 0x0);
    out_state = (stored_value != 0x0);
    
    Serial.print("DEBUG: NVS Load - Key '");
    Serial.print(key);
    Serial.print("' = ");
    Serial.print(out_state ? "ON" : "OFF");
    Serial.println(" (loaded)");
    
    return true;
  } catch (...) {
    Serial.print("ERROR: Failed to load relay ");
    Serial.print(relay_index + 1);
    Serial.println(" state from NVS");
    return false;
  }
}

void PreferencesManager::saveAllStates(const bool states[], uint8_t count) {
  if (!is_initialized) {
    Serial.println("ERROR: PreferencesManager not initialized. Call begin() first.");
    return;
  }

  if (count > RELAY_COUNT) {
    Serial.print("WARNING: saveAllStates count ");
    Serial.print(count);
    Serial.print(" exceeds RELAY_COUNT ");
    Serial.println(RELAY_COUNT);
    count = RELAY_COUNT;
  }

  for (uint8_t i = 0; i < count; i++) {
    saveRelayState(i, states[i]);
  }

  Serial.println("All relay states saved to NVS");
}

uint8_t PreferencesManager::loadAllStates(bool states[], uint8_t count) {
  if (!is_initialized) {
    Serial.println("ERROR: PreferencesManager not initialized. Call begin() first.");
    return 0;
  }

  if (count > RELAY_COUNT) {
    Serial.print("WARNING: loadAllStates count ");
    Serial.print(count);
    Serial.print(" exceeds RELAY_COUNT ");
    Serial.println(RELAY_COUNT);
    count = RELAY_COUNT;
  }

  uint8_t loaded_count = 0;

  for (uint8_t i = 0; i < count; i++) {
    if (loadRelayState(i, states[i])) {
      loaded_count++;
    } else {
      states[i] = false;
    }
  }

  return loaded_count;
}

void PreferencesManager::clearAllStates() {
  if (!is_initialized) {
    Serial.println("ERROR: PreferencesManager not initialized. Call begin() first.");
    return;
  }

  preferences.clear();
  Serial.println("All relay states cleared from NVS (factory reset)");
}

void PreferencesManager::end() {
  if (is_initialized) {
    preferences.end();
    is_initialized = false;
    Serial.println("Preferences Manager closed");
  }
}

bool PreferencesManager::saveRelayRuntime(uint8_t relay_index, uint32_t total_minutes, uint32_t remainder_seconds) {
  if (!is_initialized) return false;
  if (!isValidRelayIndex(relay_index)) return false;

  char key_min[16];
  char key_sec[16];
  snprintf(key_min, sizeof(key_min), "rt_m_%d", relay_index);
  snprintf(key_sec, sizeof(key_sec), "rt_s_%d", relay_index);

  try {
    preferences.putUInt(key_min, total_minutes);
    preferences.putUInt(key_sec, remainder_seconds);
    return true;
  } catch (...) {
    Serial.println("ERROR: NVS runtime write failed");
    return false;
  }
}

bool PreferencesManager::loadRelayRuntime(uint8_t relay_index, uint32_t& out_total_minutes, uint32_t& out_remainder_seconds) {
  if (!is_initialized) return false;
  if (!isValidRelayIndex(relay_index)) return false;

  char key_min[16];
  char key_sec[16];
  snprintf(key_min, sizeof(key_min), "rt_m_%d", relay_index);
  snprintf(key_sec, sizeof(key_sec), "rt_s_%d", relay_index);

  try {
    out_total_minutes = preferences.getUInt(key_min, 0);
    out_remainder_seconds = preferences.getUInt(key_sec, 0);
    return true;
  } catch (...) {
    out_total_minutes = 0;
    out_remainder_seconds = 0;
    return false;
  }
}

void PreferencesManager::getKeyForRelay(uint8_t relay_index, char* key_buffer, uint8_t buffer_size) {
  snprintf(key_buffer, buffer_size, "relay_%u", relay_index);
}

bool PreferencesManager::isValidRelayIndex(uint8_t relay_index) {
  if (relay_index >= RELAY_COUNT) {
    Serial.print("ERROR: Relay index ");
    Serial.print(relay_index);
    Serial.print(" out of range [0-");
    Serial.print(RELAY_COUNT - 1);
    Serial.println("]");
    return false;
  }

  return true;
}
