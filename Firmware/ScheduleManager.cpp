#include "ScheduleManager.h"
#include "RelayManager.h"
#include <Arduino.h>
#include <time.h>

ScheduleManager::ScheduleManager(RelayManager* relay_manager)
    : relay_manager(relay_manager), schedule_count(0), last_check_time_ms(0), last_checked_minute(-1) {
  for (uint8_t i = 0; i < 10; i++) {
    schedules[i].id[0] = '\0';
    schedules[i].relayId[0] = '\0';
    schedules[i].action = false;
    schedules[i].startTime = 0;
    schedules[i].endTime = -1;
    schedules[i].daysOfWeek = 0;
    schedules[i].isEnabled = false;
    schedules[i].last_triggered_today = false;
  }
}

void ScheduleManager::begin() {
  last_check_time_ms = millis();
}

void ScheduleManager::update() {
  uint32_t current_time = millis();
  if (current_time - last_check_time_ms >= 5000) {
    last_check_time_ms = current_time;

    time_t now;
    struct tm timeinfo;
    time(&now);
    localtime_r(&now, &timeinfo);

    if (timeinfo.tm_year > 70) {
      if (timeinfo.tm_min != last_checked_minute) {
        last_checked_minute = timeinfo.tm_min;
        checkSchedules();
      }
    } else {
      configTime(19800, 0, "pool.ntp.org", "time.nist.gov");
      Serial.println("NTP not synced, retrying sync...");
    }
  }
}

void ScheduleManager::clearSchedules() {
  schedule_count = 0;
}

void ScheduleManager::addSchedule(const char* id, const char* relayId, bool action, uint32_t startTime, int32_t endTime, uint8_t daysMask, bool isEnabled) {
  if (schedule_count >= 10) return;

  DeviceSchedule& sch = schedules[schedule_count];
  strncpy(sch.id, id, sizeof(sch.id) - 1);
  sch.id[sizeof(sch.id) - 1] = '\0';
  strncpy(sch.relayId, relayId, sizeof(sch.relayId) - 1);
  sch.relayId[sizeof(sch.relayId) - 1] = '\0';
  sch.action = action;
  sch.startTime = startTime;
  sch.endTime = endTime;
  sch.daysOfWeek = daysMask;
  sch.isEnabled = isEnabled;
  sch.last_triggered_today = false;

  schedule_count++;
}

void ScheduleManager::checkSchedules() {
  time_t now;
  struct tm timeinfo;
  time(&now);
  localtime_r(&now, &timeinfo);

  uint32_t currentSeconds = timeinfo.tm_hour * 3600 + timeinfo.tm_min * 60;
  uint8_t currentDayOfWeek = timeinfo.tm_wday + 1;
  uint8_t currentDayBit = (1 << currentDayOfWeek);

  for (uint8_t i = 0; i < schedule_count; i++) {
    DeviceSchedule& sch = schedules[i];
    if (!sch.isEnabled) continue;

    if ((sch.daysOfWeek & currentDayBit) == 0) {
      sch.last_triggered_today = false;
      continue;
    }

    if (timeinfo.tm_hour == 0 && timeinfo.tm_min == 0) {
      sch.last_triggered_today = false;
    }

    int channelIndex = -1;
    const char* relay_ptr = strstr(sch.relayId, "relay_");
    if (relay_ptr != nullptr) {
      if (sscanf(relay_ptr, "relay_%d", &channelIndex) == 1) {
        channelIndex -= 1;
      }
    } else {
      if (sscanf(sch.relayId, "%d", &channelIndex) == 1) {
        channelIndex -= 1;
      }
    }

    if (channelIndex < 0 || channelIndex >= RELAY_COUNT) continue;

    uint32_t startMinSeconds = sch.startTime - (sch.startTime % 60);
    if (currentSeconds == startMinSeconds && !sch.last_triggered_today) {
      sch.last_triggered_today = true;
      relay_manager->setRelay(channelIndex, sch.action);
      Serial.print("Schedule Triggered: Relay ");
      Serial.print(channelIndex + 1);
      Serial.println(sch.action ? " ON" : " OFF");
    }

    if (sch.endTime >= 0) {
      uint32_t endMinSeconds = sch.endTime - (sch.endTime % 60);
      if (currentSeconds == endMinSeconds) {
        bool endAction = !sch.action;
        if (relay_manager->getRelayState(channelIndex) != endAction) {
          relay_manager->setRelay(channelIndex, endAction);
          Serial.print("Schedule Duration Finished: Relay ");
          Serial.print(channelIndex + 1);
          Serial.println(endAction ? " ON" : " OFF");
        }
      }
    }
  }
}
