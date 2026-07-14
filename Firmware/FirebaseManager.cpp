#include "FirebaseManager.h"
#include "WiFiManager.h"
#include "RelayManager.h"
#include "DeviceManager.h"
#include "SensorManager.h"
#include "ScheduleManager.h"
#include "BLEManager.h"
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <Arduino.h>

FirebaseManager::FirebaseManager(WiFiManager* wifi_manager, RelayManager* relay_manager, DeviceManager* device_manager,
                                 SensorManager* sensor_manager, ScheduleManager* schedule_manager)
    : wifi_manager(wifi_manager), relay_manager(relay_manager), device_manager(device_manager),
      sensor_manager(sensor_manager), schedule_manager(schedule_manager),
      last_sync_time_ms(0), last_schedule_sync_time_ms(0), is_initialized(false), device_initialized(false), ble_stop_time_ms(0), needs_sync(false), rule_count(0) {
  for (uint8_t i = 0; i < RELAY_COUNT; i++) {
    last_relay_states[i] = false;
  }
  clearRules();
}

void FirebaseManager::begin() {
  client.setInsecure();
  client.setHandshakeTimeout(10); // 10 seconds timeout in ESP32 Core
  is_initialized = true;
  Serial.println("Firebase Manager Initialized");
}

void FirebaseManager::update() {
  if (!is_initialized || !wifi_manager->isConnected()) {
    return;
  }

  String deviceId = wifi_manager->getDeviceID();
  if (deviceId.length() == 0) {
    String deviceNameStr = device_manager->getHardwareID();
    deviceNameStr.toLowerCase();
    deviceNameStr.replace("-", "_");
    deviceId = deviceNameStr;
  }

  if (!device_initialized) {
    time_t now;
    time(&now);
    Serial.print("Current system time: ");
    Serial.println(ctime(&now));

    // 1. ESP -> Update devices/{deviceId} using PATCH (non-destructive)
    String dev_url = String("https://") + FIREBASE_HOST + "/devices/" + deviceId + ".json?auth=" + FIREBASE_SECRET;
    Serial.print("PATCH /devices/ URL: ");
    Serial.println(dev_url);
    if (http.begin(client, dev_url)) {
      time_t now;
      time(&now);
      
      String defaultName = "Smart Relay";
      if (device_manager->getRelayCount() == 8) defaultName = "Living Room";
      else if (device_manager->getRelayCount() == 6) defaultName = "Bedroom";
      else if (device_manager->getRelayCount() == 4) defaultName = "Guestroom";

      String payload = "{";
      payload += "\"hardwareId\":\"" + String(device_manager->getHardwareID()) + "\"";
      payload += ",\"hardware_id\":\"" + String(device_manager->getHardwareID()) + "\"";
      payload += ",\"relayCount\":" + String(device_manager->getRelayCount());
      payload += ",\"relay_count\":" + String(device_manager->getRelayCount());
      payload += ",\"firmwareVersion\":\"" + String(device_manager->getFirmwareVersion()) + "\"";
      payload += ",\"firmware_version\":\"" + String(device_manager->getFirmwareVersion()) + "\"";
      payload += ",\"createdAt\":" + String((uint64_t)now * 1000);
      payload += ",\"created_at\":" + String((uint64_t)now * 1000);
      payload += ",\"lastSeen\":" + String((uint64_t)now * 1000);
      payload += ",\"last_seen\":" + String((uint64_t)now * 1000);
      payload += ",\"name\":\"" + defaultName + "\"";
      payload += "}";

      http.addHeader("Content-Type", "application/json");
      int code = http.PATCH(payload);
      Serial.print("PATCH /devices/ Response Code: ");
      Serial.println(code);
      if (code > 0) {
        Serial.print("Response: ");
        Serial.println(http.getString());
      } else {
        Serial.print("Error: ");
        Serial.println(http.errorToString(code));
        char err_buf[100];
        client.lastError(err_buf, sizeof(err_buf));
        Serial.print("SSL Last Error: ");
        Serial.println(err_buf);
      }
      http.end();
    }

    // 1b. Write ownership links directly if ownerId is set
    String ownerId = String(wifi_manager->getOwnerID());
    if (ownerId.length() > 0) {
      Serial.print("Writing ownership links for ownerId: ");
      Serial.println(ownerId);

      // Write device_access/{deviceId}/{ownerId} = "owner" (matches app's database_access lowercase target)
      String access_url = String("https://") + FIREBASE_HOST + "/device_access/" + deviceId + "/" + ownerId + ".json?auth=" + FIREBASE_SECRET;
      if (http.begin(client, access_url)) {
        http.addHeader("Content-Type", "application/json");
        int code = http.PUT("\"owner\"");
        Serial.print("PUT /device_access/ Response Code: ");
        Serial.println(code);
        http.end();
      }

      // Write devices/{deviceId}/owner = "{ownerId}"
      String owner_url = String("https://") + FIREBASE_HOST + "/devices/" + deviceId + "/owner.json?auth=" + FIREBASE_SECRET;
      if (http.begin(client, owner_url)) {
        http.addHeader("Content-Type", "application/json");
        int code = http.PUT("\"" + ownerId + "\"");
        Serial.print("PUT /devices/owner Response Code: ");
        Serial.println(code);
        http.end();
      }

      // Write users/{ownerId}/devices/{deviceId} = true
      String user_devices_url = String("https://") + FIREBASE_HOST + "/users/" + ownerId + "/devices/" + deviceId + ".json?auth=" + FIREBASE_SECRET;
      if (http.begin(client, user_devices_url)) {
        http.addHeader("Content-Type", "application/json");
        int code = http.PUT("true");
        Serial.print("PUT /users/devices Response Code: ");
        Serial.println(code);
        http.end();
      }
    }

    // 2. ESP -> Update device_states/{deviceId} & Set online=true using PATCH
    String state_url = String("https://") + FIREBASE_HOST + "/device_states/" + deviceId + ".json?auth=" + FIREBASE_SECRET;
    Serial.print("PATCH /device_states/ URL: ");
    Serial.println(state_url);
    if (http.begin(client, state_url)) {
      time_t now;
      time(&now);

      String payload = "{";
      payload += "\"online\":true";
      payload += ",\"updated_at\":" + String((uint64_t)now * 1000);
      payload += ",\"relays\":{";
      for (uint8_t i = 0; i < RELAY_COUNT; i++) {
        payload += "\"relay_" + String(i + 1) + "\":" + (relay_manager->getRelayState(i) ? "true" : "false");
        if (i < RELAY_COUNT - 1) payload += ",";
      }
      payload += "}";
      payload += "}";

      http.addHeader("Content-Type", "application/json");
      int code = http.PATCH(payload);
      Serial.print("PATCH /device_states/ Response Code: ");
      Serial.println(code);
      if (code > 0) {
        Serial.print("Response: ");
        Serial.println(http.getString());
      } else {
        Serial.print("Error: ");
        Serial.println(http.errorToString(code));
        char err_buf[100];
        client.lastError(err_buf, sizeof(err_buf));
        Serial.print("SSL Last Error: ");
        Serial.println(err_buf);
      }
      http.end();
    }

    device_initialized = true;
    Serial.println("ESP initialized /devices and /device_states with online=true");

    if (wifi_manager != nullptr && wifi_manager->getBLEManager() != nullptr) {
      wifi_manager->getBLEManager()->notifyStatus("{\"status\":\"registered\"}");
    }
    ble_stop_time_ms = millis();
  }

  if (ble_stop_time_ms > 0 && millis() - ble_stop_time_ms >= 5000) {
    if (wifi_manager != nullptr && wifi_manager->getBLEManager() != nullptr) {
      wifi_manager->getBLEManager()->stop();
    }
    ble_stop_time_ms = 0;
  }

  // Detect any relay state change → immediate sync before next scheduled interval
  for (uint8_t i = 0; i < RELAY_COUNT; i++) {
    bool current_state = relay_manager->getRelayState(i);
    if (current_state != last_relay_states[i]) {
      last_relay_states[i] = current_state;
      needs_sync = true;
    }
  }

  uint32_t current_time_sync = millis();
  bool interval_due = (current_time_sync - last_sync_time_ms >= FIREBASE_SYNC_INTERVAL_MS);
  if (interval_due || needs_sync) {
    last_sync_time_ms = current_time_sync;
    bool local_change = needs_sync;
    needs_sync = false;
    syncWithFirebase(local_change);
  }

  // Evaluate local automation rules based on latest sensor readings
  evaluateRules();
}

void FirebaseManager::syncWithFirebase(bool local_change) {
  String deviceId = wifi_manager->getDeviceID();
  if (deviceId.length() == 0) {
    String deviceNameStr = device_manager->getHardwareID();
    deviceNameStr.toLowerCase();
    deviceNameStr.replace("-", "_");
    deviceId = deviceNameStr;
  }

  // Sync target relay states from /device_states/{deviceId}/relays.json
  // Skip fetching remote states if this sync run was triggered by a local touch state change
  if (!local_change) {
    String relays_url = String("https://") + FIREBASE_HOST + "/device_states/" + deviceId + "/relays.json?auth=" + FIREBASE_SECRET;
    if (http.begin(client, relays_url)) {
      int httpCode = http.GET();
      if (httpCode != HTTP_CODE_OK && httpCode != HTTP_CODE_NOT_FOUND) {
        Serial.print("GET /device_states/relays failed, code: ");
        Serial.println(httpCode);
      }
      if (httpCode == HTTP_CODE_OK) {
        String payload = http.getString();
        // Parse payload e.g. {"relay_1":true,"relay_2":false,...}
        for (uint8_t i = 0; i < RELAY_COUNT; i++) {
          String relay_key = "\"relay_" + String(i + 1) + "\"";
          int key_pos = payload.indexOf(relay_key);
          if (key_pos != -1) {
            int colon_pos = payload.indexOf(":", key_pos);
            if (colon_pos != -1) {
              int true_pos = payload.indexOf("true", colon_pos);
              int false_pos = payload.indexOf("false", colon_pos);
              bool target_state = false;
              bool found = false;
              
              if (true_pos != -1 && (false_pos == -1 || true_pos < false_pos)) {
                if (true_pos < colon_pos + 10) {
                  target_state = true;
                  found = true;
                }
              } else if (false_pos != -1) {
                if (false_pos < colon_pos + 10) {
                  target_state = false;
                  found = true;
                }
              }
              
              if (found) {
                // Loop prevention: Only update physical outputs if different.
                // Do NOT trigger immediate sync (needs_sync) or write back if the source was network sync.
                if (relay_manager->getRelayState(i) != target_state) {
                  Serial.printf("Syncing relay %d to state: %s\n", i + 1, target_state ? "ON" : "OFF");
                  relay_manager->setRelay(i, target_state);
                  last_relay_states[i] = target_state; // update local snapshot to prevent echo loops
                }
              }
            }
          }
        }
      }
      http.end();
    }
  }

  // Sync schedules from /schedules/{deviceId}.json (limited to once per minute)
  uint32_t current_time = millis();
  if (last_schedule_sync_time_ms == 0 || current_time - last_schedule_sync_time_ms >= 60000) {
    last_schedule_sync_time_ms = current_time;
    String schedules_url = String("https://") + FIREBASE_HOST + "/schedules/" + deviceId + ".json?auth=" + FIREBASE_SECRET;
    if (http.begin(client, schedules_url)) {
      int httpCode = http.GET();
      if (httpCode != HTTP_CODE_OK && httpCode != HTTP_CODE_NOT_FOUND) {
        Serial.print("GET /schedules/ failed, code: ");
        Serial.println(httpCode);
      }
      if (httpCode == HTTP_CODE_OK) {
        String payload = http.getString();
        if (payload != "null" && payload.length() > 2) {
          schedule_manager->clearSchedules();
          int search_pos = 0;
          while (true) {
            int id_pos = payload.indexOf("\"id\"", search_pos);
            if (id_pos == -1) break;

            int colon_pos = payload.indexOf(":", id_pos);
            if (colon_pos == -1) break;
            int quote_start = payload.indexOf("\"", colon_pos);
            if (quote_start == -1) break;
            int quote_end = payload.indexOf("\"", quote_start + 1);
            if (quote_end == -1) break;
            String id = payload.substring(quote_start + 1, quote_end);

            int next_id_pos = payload.indexOf("\"id\"", quote_end);
            int end_search = (next_id_pos != -1) ? next_id_pos : payload.length();

            String relayId = "relay_1";
            int relayId_pos = payload.indexOf("\"relayId\"", quote_end);
            if (relayId_pos != -1 && relayId_pos < end_search) {
              int r_colon = payload.indexOf(":", relayId_pos);
              int r_start = payload.indexOf("\"", r_colon);
              int r_end = payload.indexOf("\"", r_start + 1);
              if (r_start != -1 && r_end != -1) {
                relayId = payload.substring(r_start + 1, r_end);
              }
            }

            bool action = false;
            int action_pos = payload.indexOf("\"action\"", quote_end);
            if (action_pos != -1 && action_pos < end_search) {
              int a_colon = payload.indexOf(":", action_pos);
              int true_pos = payload.indexOf("true", a_colon);
              int false_pos = payload.indexOf("false", a_colon);
              if (true_pos != -1 && true_pos < a_colon + 15 && (false_pos == -1 || true_pos < false_pos)) {
                action = true;
              }
            }

            bool isEnabled = false;
            int enabled_pos = payload.indexOf("\"isEnabled\"", quote_end);
            if (enabled_pos != -1 && enabled_pos < end_search) {
              int e_colon = payload.indexOf(":", enabled_pos);
              int true_pos = payload.indexOf("true", e_colon);
              int false_pos = payload.indexOf("false", e_colon);
              if (true_pos != -1 && true_pos < e_colon + 15 && (false_pos == -1 || true_pos < false_pos)) {
                isEnabled = true;
              }
            }

            uint32_t startTime = 0;
            int start_time_pos = payload.indexOf("\"startTime\"", quote_end);
            if (start_time_pos != -1 && start_time_pos < end_search) {
              int st_colon = payload.indexOf(":", start_time_pos);
              String st_val_str = payload.substring(st_colon + 1, payload.indexOf(",", st_colon));
              st_val_str.trim();
              startTime = st_val_str.toInt();
            }

            int32_t endTime = -1;
            int end_time_pos = payload.indexOf("\"endTime\"", quote_end);
            if (end_time_pos != -1 && end_time_pos < end_search) {
              int et_colon = payload.indexOf(":", end_time_pos);
              String et_val_str = payload.substring(et_colon + 1, payload.indexOf(",", et_colon));
              et_val_str.trim();
              if (et_val_str != "null" && et_val_str.length() > 0) {
                endTime = et_val_str.toInt();
              }
            }

            uint8_t daysMask = 0;
            int days_pos = payload.indexOf("\"daysOfWeek\"", quote_end);
            if (days_pos != -1 && days_pos < end_search) {
              int arr_start = payload.indexOf("[", days_pos);
              int arr_end = payload.indexOf("]", days_pos);
              if (arr_start != -1 && arr_end != -1 && arr_end > arr_start + 1) {
                String days_str = payload.substring(arr_start + 1, arr_end);
                int comma_pos = 0;
                while (true) {
                  int next_comma = days_str.indexOf(",", comma_pos);
                  String day_val_str = (next_comma != -1) ? days_str.substring(comma_pos, next_comma) : days_str.substring(comma_pos);
                  day_val_str.trim();
                  int day_val = day_val_str.toInt();
                  if (day_val >= 1 && day_val <= 7) {
                    daysMask |= (1 << day_val);
                  }
                  if (next_comma == -1) break;
                  comma_pos = next_comma + 1;
                }
              }
            }

            schedule_manager->addSchedule(id.c_str(), relayId.c_str(), action, startTime, endTime, daysMask, isEnabled);
            search_pos = quote_end;
          }
        }
      }
      http.end();
    }

    // Sync automation rules from /automation_rules/{deviceId}.json
    String rules_url = String("https://") + FIREBASE_HOST + "/automation_rules/" + deviceId + ".json?auth=" + FIREBASE_SECRET;
    if (http.begin(client, rules_url)) {
      int httpCode = http.GET();
      if (httpCode == HTTP_CODE_OK) {
        String payload = http.getString();
        if (payload != "null" && payload.length() > 2) {
          clearRules();
          int search_pos = 0;
          while (true) {
            // Find key (ruleId) using relayId as anchor
            int id_pos = payload.indexOf("\"relayId\"", search_pos);
            if (id_pos == -1) break;

            int obj_start = payload.lastIndexOf("{", id_pos);
            if (obj_start == -1) break;

            int key_end = payload.lastIndexOf("\"", obj_start);
            int key_start = payload.lastIndexOf("\"", key_end - 1);
            if (key_start == -1 || key_end == -1) break;
            String ruleId = payload.substring(key_start + 1, key_end);

            int next_obj = payload.indexOf("}", id_pos);
            int end_search = (next_obj != -1) ? next_obj : payload.length();

            String relayId = "relay_1";
            int r_colon = payload.indexOf(":", id_pos);
            int r_start = payload.indexOf("\"", r_colon);
            int r_end = payload.indexOf("\"", r_start + 1);
            if (r_start != -1 && r_end != -1) relayId = payload.substring(r_start + 1, r_end);

            String sensorMode = "temperature";
            int sm_pos = payload.indexOf("\"sensorMode\"", obj_start);
            if (sm_pos != -1 && sm_pos < end_search) {
              int colon = payload.indexOf(":", sm_pos);
              int start = payload.indexOf("\"", colon);
              int end = payload.indexOf("\"", start + 1);
              if (start != -1 && end != -1) sensorMode = payload.substring(start + 1, end);
            }

            String logicalOperator = "";
            int lo_pos = payload.indexOf("\"logicalOperator\"", obj_start);
            if (lo_pos != -1 && lo_pos < end_search) {
              int colon = payload.indexOf(":", lo_pos);
              int start = payload.indexOf("\"", colon);
              int end = payload.indexOf("\"", start + 1);
              if (start != -1 && end != -1) logicalOperator = payload.substring(start + 1, end);
            }

            String tempCondition = "";
            int tc_pos = payload.indexOf("\"tempCondition\"", obj_start);
            if (tc_pos != -1 && tc_pos < end_search) {
              int colon = payload.indexOf(":", tc_pos);
              int start = payload.indexOf("\"", colon);
              int end = payload.indexOf("\"", start + 1);
              if (start != -1 && end != -1) tempCondition = payload.substring(start + 1, end);
            }

            float tempThreshold = 0.0f;
            int tt_pos = payload.indexOf("\"tempThreshold\"", obj_start);
            if (tt_pos != -1 && tt_pos < end_search) {
              int colon = payload.indexOf(":", tt_pos);
              String val_str = payload.substring(colon + 1, payload.indexOf(",", colon));
              val_str.trim();
              tempThreshold = val_str.toFloat();
            }

            String humidityCondition = "";
            int hc_pos = payload.indexOf("\"humidityCondition\"", obj_start);
            if (hc_pos != -1 && hc_pos < end_search) {
              int colon = payload.indexOf(":", hc_pos);
              int start = payload.indexOf("\"", colon);
              int end = payload.indexOf("\"", start + 1);
              if (start != -1 && end != -1) humidityCondition = payload.substring(start + 1, end);
            }

            float humidityThreshold = 0.0f;
            int ht_pos = payload.indexOf("\"humidityThreshold\"", obj_start);
            if (ht_pos != -1 && ht_pos < end_search) {
              int colon = payload.indexOf(":", ht_pos);
              String val_str = payload.substring(colon + 1, payload.indexOf(",", colon));
              val_str.trim();
              humidityThreshold = val_str.toFloat();
            }

            bool action = false;
            int action_pos = payload.indexOf("\"action\"", obj_start);
            if (action_pos != -1 && action_pos < end_search) {
              int colon = payload.indexOf(":", action_pos);
              int true_pos = payload.indexOf("true", colon);
              if (true_pos != -1 && true_pos < colon + 15) action = true;
            }

            bool isEnabled = true;
            int enabled_pos = payload.indexOf("\"isEnabled\"", obj_start);
            if (enabled_pos != -1 && enabled_pos < end_search) {
              int colon = payload.indexOf(":", enabled_pos);
              int false_pos = payload.indexOf("false", colon);
              if (false_pos != -1 && false_pos < colon + 15) isEnabled = false;
            }

            addRule(ruleId.c_str(), relayId.c_str(), sensorMode.c_str(), logicalOperator.c_str(),
                    tempCondition.c_str(), tempThreshold, humidityCondition.c_str(), humidityThreshold,
                    action, isEnabled);

            search_pos = id_pos + 10;
          }
        }
      }
      http.end();
    }
  }

  // Update telemetry, state, runtimes and uptime under /device_states/{deviceId}.json
  String update_url = String("https://") + FIREBASE_HOST + "/device_states/" + deviceId + ".json?auth=" + FIREBASE_SECRET;
  if (http.begin(client, update_url)) {
    time_t now;
    time(&now);
    uint32_t uptime_sec = device_manager->getUptimeSeconds();

    String payload = "{";
    payload += "\"online\":true";
    payload += ",\"updated_at\":" + String((uint64_t)now * 1000);
    payload += ",\"uptime_seconds\":" + String(uptime_sec);
    payload += ",\"relays\":{";
    for (uint8_t i = 0; i < RELAY_COUNT; i++) {
      payload += "\"relay_" + String(i + 1) + "\":" + (relay_manager->getRelayState(i) ? "true" : "false");
      if (i < RELAY_COUNT - 1) payload += ",";
    }
    payload += "}";
    // Runtimes under "runtime" node as per DATABASE.md spec
    payload += ",\"runtime\":{";
    for (uint8_t i = 0; i < RELAY_COUNT; i++) {
      uint32_t runtime_min = relay_manager->getRelayRuntime(i) / 60;
      payload += "\"relay_" + String(i + 1) + "\":{";
      payload += "\"lifetimeMinutes\":" + String(runtime_min);
      payload += ",\"lastUpdated\":" + String((uint64_t)now * 1000);
      payload += "}";
      if (i < RELAY_COUNT - 1) payload += ",";
    }
    payload += "}";
    payload += ",\"sensors\":{";
    payload += "\"temperature\":" + String(sensor_manager->getTemperature(), 1);
    payload += ",\"humidity\":" + String(sensor_manager->getHumidity(), 1);
    payload += ",\"humidex\":" + String(sensor_manager->getHumidex(), 1);
    payload += ",\"updated_at\":" + String((uint64_t)now * 1000);
    payload += "}";
    payload += "}";

    http.addHeader("Content-Type", "application/json");
    int code = http.PATCH(payload);
    Serial.print("PATCH /device_states/ Response Code: ");
    Serial.println(code);
    if (code < 0) {
      Serial.print("Error: ");
      Serial.println(http.errorToString(code));
    }
    http.end();
  }

  // Update lastSeen under /devices/{deviceId}.json
  String dev_update_url = String("https://") + FIREBASE_HOST + "/devices/" + deviceId + ".json?auth=" + FIREBASE_SECRET;
  if (http.begin(client, dev_update_url)) {
    time_t now;
    time(&now);
    String payload = "{\"lastSeen\":" + String((uint64_t)now * 1000) + "}";
    http.addHeader("Content-Type", "application/json");
    int code = http.PATCH(payload);
    if (code < 0) {
      Serial.print("PATCH /devices/ lastSeen failed, code: ");
      Serial.println(code);
    }
    http.end();
  }
}

void FirebaseManager::clearRules() {
  rule_count = 0;
}

void FirebaseManager::addRule(const char* id, const char* relayId, const char* sensorMode,
                             const char* logicalOperator, const char* tempCondition, float tempThreshold,
                             const char* humidityCondition, float humidityThreshold, bool action, bool isEnabled) {
  if (rule_count >= 10) return;

  AutomationRule& rule = rules[rule_count];
  strncpy(rule.id, id, sizeof(rule.id) - 1);
  rule.id[sizeof(rule.id) - 1] = '\0';
  
  strncpy(rule.relayId, relayId, sizeof(rule.relayId) - 1);
  rule.relayId[sizeof(rule.relayId) - 1] = '\0';
  
  strncpy(rule.sensorMode, sensorMode, sizeof(rule.sensorMode) - 1);
  rule.sensorMode[sizeof(rule.sensorMode) - 1] = '\0';
  
  strncpy(rule.logicalOperator, logicalOperator, sizeof(rule.logicalOperator) - 1);
  rule.logicalOperator[sizeof(rule.logicalOperator) - 1] = '\0';
  
  strncpy(rule.tempCondition, tempCondition, sizeof(rule.tempCondition) - 1);
  rule.tempCondition[sizeof(rule.tempCondition) - 1] = '\0';
  
  rule.tempThreshold = tempThreshold;
  
  strncpy(rule.humidityCondition, humidityCondition, sizeof(rule.humidityCondition) - 1);
  rule.humidityCondition[sizeof(rule.humidityCondition) - 1] = '\0';
  
  rule.humidityThreshold = humidityThreshold;
  rule.action = action;
  rule.isEnabled = isEnabled;

  rule_count++;
}

void FirebaseManager::evaluateRules() {
  float current_temp = sensor_manager->getTemperature();
  float current_hum = sensor_manager->getHumidity();

  // Guard against initial/invalid sensor readings
  if (current_temp < -39.0f || current_hum <= 0.0f) return;

  for (uint8_t i = 0; i < rule_count; i++) {
    AutomationRule& rule = rules[i];
    if (!rule.isEnabled) continue;

    bool temp_met = false;
    if (strcmp(rule.tempCondition, "above") == 0) {
      temp_met = (current_temp > rule.tempThreshold);
    } else if (strcmp(rule.tempCondition, "below") == 0) {
      temp_met = (current_temp < rule.tempThreshold);
    }

    bool hum_met = false;
    if (strcmp(rule.humidityCondition, "above") == 0) {
      hum_met = (current_hum > rule.humidityThreshold);
    } else if (strcmp(rule.humidityCondition, "below") == 0) {
      hum_met = (current_hum < rule.humidityThreshold);
    }

    bool condition_met = false;
    if (strcmp(rule.sensorMode, "temperature") == 0) {
      condition_met = temp_met;
    } else if (strcmp(rule.sensorMode, "humidity") == 0) {
      condition_met = hum_met;
    } else if (strcmp(rule.sensorMode, "both") == 0) {
      if (strcmp(rule.logicalOperator, "OR") == 0 || strcmp(rule.logicalOperator, "or") == 0) {
        condition_met = (temp_met || hum_met);
      } else {
        condition_met = (temp_met && hum_met);
      }
    }

    if (condition_met) {
      int channelIndex = -1;
      if (sscanf(rule.relayId, "relay_%d", &channelIndex) == 1) {
        channelIndex -= 1;
      }
      if (channelIndex >= 0 && channelIndex < RELAY_COUNT) {
        // Only set and log if it triggers a state change to prevent spamming
        if (relay_manager->getRelayState(channelIndex) != rule.action) {
          Serial.printf("Automation Rule [%s] Triggered: Setting Relay %d to %s\n", rule.id, channelIndex + 1, rule.action ? "ON" : "OFF");
          relay_manager->setRelay(channelIndex, rule.action);
          
          // Trigger immediate telemetry sync to reflect the rule execution back to the App
          needs_sync = true;
        }
      }
    }
  }
}
