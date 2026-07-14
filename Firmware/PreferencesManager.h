#ifndef PREFERENCES_MANAGER_H
#define PREFERENCES_MANAGER_H

#include <stdint.h>
#include "Config.h"

// ============================================================================
// PreferencesManager: Relay state persistence using ESP32 NVS (Preferences)
// ============================================================================
// Manages persistent storage of relay states and runtime across power cycles (Phase 2).
// - Uses ESP32 Preferences library with namespace "nexusflow"
// - Key format: "relay_N" where N is the relay index (0-5)
// - Runtime tracking: "rt_m_N" for minutes, "rt_s_N" for seconds
// - Storage: Non-Volatile Storage (NVS) flash memory
// - Automatic save on every relay state change
// - Automatic restore on boot via RelayManager integration
// ============================================================================

class PreferencesManager {
public:
  PreferencesManager();

  void begin();

  bool saveRelayState(uint8_t relay_index, bool state);
  bool loadRelayState(uint8_t relay_index, bool& out_state);

  void saveAllStates(const bool states[], uint8_t count);
  uint8_t loadAllStates(bool states[], uint8_t count);

  void clearAllStates();

  bool saveRelayRuntime(uint8_t relay_index, uint32_t total_minutes, uint32_t remainder_seconds);
  bool loadRelayRuntime(uint8_t relay_index, uint32_t& out_total_minutes, uint32_t& out_remainder_seconds);

  void end();

private:
  bool is_initialized;

  void getKeyForRelay(uint8_t relay_index, char* key_buffer, uint8_t buffer_size);
  bool isValidRelayIndex(uint8_t relay_index);
};

#endif // PREFERENCES_MANAGER_H
