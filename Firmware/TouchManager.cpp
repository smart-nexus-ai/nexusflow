#include "TouchManager.h"
#include <Arduino.h>

TouchManager::TouchManager(RelayManager* relay_manager)
    : relay_manager(relay_manager), last_update_time_ms(0) {
  for (uint8_t i = 0; i < TOUCH_COUNT; i++) {
    pad_states[i].state = IDLE;
    pad_states[i].debounce_timer_ms = 0;
    pad_states[i].last_raw_read = LOW;
    pad_states[i].last_log_time_ms = 0;
  }
}

void TouchManager::begin() {
  for (uint8_t i = 0; i < TOUCH_COUNT; i++) {
    pinMode(TOUCH_PINS[i], INPUT);
  }
  Serial.println("Touch Manager Initialized");
}

void TouchManager::update() {
  uint32_t current_time_ms = millis();

  if (current_time_ms - last_update_time_ms < TOUCH_SAMPLE_INTERVAL_MS) {
    return;
  }

  last_update_time_ms = current_time_ms;

  for (uint8_t i = 0; i < TOUCH_COUNT; i++) {
    updatePad(i);
  }
}

void TouchManager::updatePad(uint8_t pad_index) {
  uint8_t raw_read = digitalRead(TOUCH_PINS[pad_index]);
  logPadState(pad_index, raw_read);

  TouchPadState& pad = pad_states[pad_index];
  uint32_t current_time_ms = millis();

  switch (pad.state) {
    case IDLE:
      if (raw_read == TOUCH_ACTIVE_STATE) {
        pad.state = CANDIDATE;
        pad.debounce_timer_ms = current_time_ms;
      }
      break;

     case CANDIDATE:
      if (raw_read == TOUCH_ACTIVE_STATE) {
        if (current_time_ms - pad.debounce_timer_ms >= TOUCH_DEBOUNCE_MS) {
          pad.state = TOUCHED;
          relay_manager->toggleRelay(pad_index);
        }
      } else {
        pad.state = IDLE;
      }
      break;

    case TOUCHED:
      if (raw_read != TOUCH_ACTIVE_STATE) {
        pad.state = IDLE;
      }
      break;

    default:
      pad.state = IDLE;
      break;
  }

  pad.last_raw_read = raw_read;
}

void TouchManager::logPadState(uint8_t pad_index, uint8_t raw_read) {
  // No-op: Do not print touch status on serial print
}
