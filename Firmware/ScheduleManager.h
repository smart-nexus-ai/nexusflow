#ifndef SCHEDULE_MANAGER_H
#define SCHEDULE_MANAGER_H

#include <stdint.h>
#include "Config.h"

struct DeviceSchedule {
  char id[40];
  char relayId[16];
  bool action;
  uint32_t startTime;
  int32_t endTime;
  uint8_t daysOfWeek;
  bool isEnabled;
  bool last_triggered_today;
};

class RelayManager;

class ScheduleManager {
public:
  ScheduleManager(RelayManager* relay_manager);

  void begin();
  void update();

  void clearSchedules();
  void addSchedule(const char* id, const char* relayId, bool action, uint32_t startTime, int32_t endTime, uint8_t daysMask, bool isEnabled);

private:
  RelayManager* relay_manager;
  DeviceSchedule schedules[10];
  uint8_t schedule_count;
  uint32_t last_check_time_ms;
  int last_checked_minute;

  void checkSchedules();
};

#endif // SCHEDULE_MANAGER_H
