#ifndef RELAY_MANAGER_H
#define RELAY_MANAGER_H

#include <stdint.h>
#include "Config.h"

// Forward declaration: PreferencesManager class.
class PreferencesManager;

// ============================================================================
// RelayManager: Hardware abstraction for active-LOW relay control with persistence
// ============================================================================
// Manages 6 relays connected to GPIO pins with runtime tracking.
// - All relays are forced OFF during begin() before any other logic runs.
// - Active-LOW mode: setRelay(i, true) drives pin LOW; false drives pin HIGH.
// - State is tracked in a bool array for efficient queries.
// - Relay state changes are automatically persisted via PreferencesManager (Phase 2).
// - Runtime tracking: measures total ON time for each relay with NVS persistence.
// - All methods include bounds-checking with error logging.
// - Loads saved states on boot and restores relay outputs.
// ============================================================================

class RelayManager {
public:
  // Constructor: Initialize with PreferencesManager reference for persistence.
  explicit RelayManager(PreferencesManager* prefs_manager);

  // Initialize all relay pins as OUTPUT, load saved states, and restore outputs.
  // Must be called after PreferencesManager.begin().
  void begin();

  // Set a specific relay ON (true) or OFF (false).
  // Active-LOW: true → pin LOW, false → pin HIGH.
  // Saves state to NVS, logs state change to Serial. Bounds-checks index.
  void setRelay(uint8_t index, bool state);

  // Toggle a specific relay (ON → OFF, OFF → ON).
  // Saves state to NVS, logs state change to Serial. Bounds-checks index.
  void toggleRelay(uint8_t index);

  // Query the current state of a specific relay.
  // Returns: true (ON), false (OFF), or false if index out of bounds.
  bool getRelayState(uint8_t index);

  // Get total runtime in seconds for a specific relay.
  uint32_t getRelayRuntime(uint8_t index);

  // Turn all relays OFF (forces all pins HIGH).
  // Saves all states to NVS.
  void turnAllOff();

  // Turn all relays ON (forces all pins LOW).
  // Saves all states to NVS.
  void turnAllOn();

  // Update runtime tracking for active relays (non-blocking).
  void update();

private:
  // Reference to PreferencesManager for persistent state storage.
  PreferencesManager* prefs_manager;

  // Track relay states: true = ON (pin LOW), false = OFF (pin HIGH).
  bool relayStates[RELAY_COUNT];

  // Runtime tracking members
  uint32_t relayOnStartMillis[RELAY_COUNT];
  uint32_t totalRuntimeMinutes[RELAY_COUNT];
  uint32_t remainderRuntimeSeconds[RELAY_COUNT];

  // Internal helper: Set relay with optional logging and persistence.
  void setRelayInternal(uint8_t index, bool state, bool log, bool save);

  // Bounds-check and log error if index invalid.
  bool isValidIndex(uint8_t index);
};

#endif // RELAY_MANAGER_H
