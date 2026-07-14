#ifndef FIREBASE_MANAGER_H
#define FIREBASE_MANAGER_H

#include <stdint.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include "Config.h"

// Forward declarations
class WiFiManager;
class RelayManager;
class DeviceManager;
class SensorManager;
class ScheduleManager;

struct AutomationRule {
  char id[37];
  char relayId[16];
  char sensorMode[16];      // "temperature", "humidity", "both"
  char logicalOperator[8];  // "AND", "OR", or empty
  char tempCondition[8];    // "above", "below"
  float tempThreshold;
  char humidityCondition[8];// "above", "below"
  float humidityThreshold;
  bool action;
  bool isEnabled;
};

class FirebaseManager {
public:
  FirebaseManager(WiFiManager* wifi_manager, RelayManager* relay_manager, DeviceManager* device_manager,
                  SensorManager* sensor_manager, ScheduleManager* schedule_manager);
  
  void begin();
  void update();

private:
  WiFiManager* wifi_manager;
  RelayManager* relay_manager;
  DeviceManager* device_manager;
  SensorManager* sensor_manager;
  ScheduleManager* schedule_manager;

  uint32_t last_sync_time_ms;
  uint32_t last_schedule_sync_time_ms;
  bool is_initialized;
  bool device_initialized;

  WiFiClientSecure client;
  HTTPClient http;

  uint32_t ble_stop_time_ms;
  bool last_relay_states[RELAY_COUNT];
  bool needs_sync;

  AutomationRule rules[10];
  uint8_t rule_count;

  void clearRules();
  void addRule(const char* id, const char* relayId, const char* sensorMode,
               const char* logicalOperator, const char* tempCondition, float tempThreshold,
               const char* humidityCondition, float humidityThreshold, bool action, bool isEnabled);
  void evaluateRules();

  void syncWithFirebase(bool local_change);
};

#endif // FIREBASE_MANAGER_H
