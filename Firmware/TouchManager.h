#ifndef TOUCH_MANAGER_H
#define TOUCH_MANAGER_H

#include <stdint.h>
#include "Config.h"
#include "RelayManager.h"

// ============================================================================
// TouchManager: Capacitive touch sensing with debounce and toggle control
// ============================================================================
// Manages 6 TTP223 capacitive touch pads connected to GPIO pins.
// - Pure digital sensing: digitalRead(pin) == HIGH when touched (Phase 1).
// - Per-pad state machine: IDLE → CANDIDATE → TOUCHED → IDLE.
// - Debounce: pad must read HIGH continuously for TOUCH_DEBOUNCE_MS before toggle fires.
// - Exactly one toggle per press on confirmed leading edge; no repeat while held.
// - Polling via millis(): no delay() in loop, efficient sampling at TOUCH_SAMPLE_INTERVAL_MS.
// - 1:1 mapping: Touch N → Relay N (direct index-based toggle).
// ============================================================================

class TouchManager {
public:
  explicit TouchManager(RelayManager* relay_manager);

  void begin();
  void update();

private:
  enum TouchState : uint8_t {
    IDLE = 0,
    CANDIDATE = 1,
    TOUCHED = 2
  };

  struct TouchPadState {
    TouchState state;
    uint32_t debounce_timer_ms;
    uint8_t last_raw_read;
    uint32_t last_log_time_ms;
  };

  RelayManager* relay_manager;
  TouchPadState pad_states[TOUCH_COUNT];
  uint32_t last_update_time_ms;

  void updatePad(uint8_t pad_index);
  void logPadState(uint8_t pad_index, uint8_t raw_read);
};

#endif // TOUCH_MANAGER_H
