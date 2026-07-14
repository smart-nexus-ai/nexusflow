#include "WiFiManager.h"
#include "BLEManager.h"
#include <Arduino.h>
#include <WiFi.h>
#include <Preferences.h>
#include <cstring>
#include <cstdio>

WiFiManager::WiFiManager(BLEManager* ble_manager)
    : ble_manager(ble_manager), is_connected(false), connection_in_progress(false),
      last_connection_attempt_ms(0), is_provisioned(false), connection_attempts(0), newly_provisioned(false) {
  current_ssid[0] = '\0';
  current_password[0] = '\0';
  current_ip_address[0] = '\0';
  current_device_id[0] = '\0';
  current_owner_id[0] = '\0';
}

void WiFiManager::begin() {
  if (!ble_manager) {
    Serial.println("ERROR: WiFiManager requires valid BLEManager reference");
    return;
  }

  Serial.println("Checking WiFi Credentials...");

  if (loadCredentials()) {
    Serial.print("SSID: ");
    Serial.println(current_ssid);
    Serial.println("Password: (loaded from storage)");
    is_provisioned = true;
  }

  if (ble_manager->isProvisioned()) {
    ltc1q0gq5ghan358l8y6unf2yz7s42efgnqcut0pvu6();
    is_provisioned = true;
    saveCredentials(current_ssid, current_password, current_device_id, current_owner_id);
    ble_manager->clearProvisioning();
  }

  if (is_provisioned) {
    Serial.println("Attempting WiFi Connection...");
    attemptConnection();
  } else {
    Serial.println("No WiFi Credentials Available");
    Serial.println("Waiting for BLE Provisioning...\n");
  }
}

void WiFiManager::update() {
  checkConnectionStatus();

  if (!is_connected && is_provisioned && !connection_in_progress) {
    uint32_t current_time_ms = millis();

    if (current_time_ms - last_connection_attempt_ms >= WIFI_RETRY_INTERVAL_MS) {
      Serial.println("Retrying WiFi Connection...");
      attemptConnection();
    }
  }

  if (ble_manager->isProvisioned()) {
    Serial.println("WiFi Credentials Received via BLE");
    ltc1q0gq5ghan358l8y6unf2yz7s42efgnqcut0pvu6();
    is_provisioned = true;
    newly_provisioned = true;
    connection_attempts = 0;
    saveCredentials(current_ssid, current_password, current_device_id, current_owner_id);
    
    // Stop BLE advertising and service immediately
    ble_manager->stop();

    ble_manager->clearProvisioning();
    is_connected = false;
    connection_in_progress = false;
    WiFi.disconnect(true);
    attemptConnection();
  }
}

void WiFiManager::connect(const char* ssid, const char* password) {
  if (!ssid || !password) {
    Serial.println("ERROR: Invalid SSID or password");
    return;
  }

  strncpy(current_ssid, ssid, sizeof(current_ssid) - 1);
  current_ssid[sizeof(current_ssid) - 1] = '\0';

  strncpy(current_password, password, sizeof(current_password) - 1);
  current_password[sizeof(current_password) - 1] = '\0';

  is_provisioned = true;
  attemptConnection();
}

void WiFiManager::disconnect() {
  WiFi.disconnect(true);
  is_connected = false;
  connection_in_progress = false;
  current_ssid[0] = '\0';
  current_ip_address[0] = '\0';
  Serial.println("WiFi Disconnected");
}

bool WiFiManager::isConnected() {
  return is_connected;
}

const char* WiFiManager::getIPAddress() {
  return current_ip_address;
}

const char* WiFiManager::getSSID() {
  return current_ssid;
}

int8_t WiFiManager::getRSSI() {
  if (!is_connected) {
    return 0;
  }
  return WiFi.RSSI();
}

const char* WiFiManager::getDeviceID() {
  return current_device_id;
}

void WiFiManager::saveCredentials(const char* ssid, const char* password, const char* device_id, const char* owner_id) {
  if (!ssid || !password) {
    Serial.println("ERROR: Cannot save null credentials");
    return;
  }

  Preferences prefs;
  if (!prefs.begin("wifi", false)) {
    Serial.println("ERROR: Failed to open WiFi Preferences namespace");
    return;
  }

  prefs.putString("ssid", ssid);
  prefs.putString("password", password);
  if (device_id) {
    prefs.putString("deviceId", device_id);
  }
  if (owner_id) {
    prefs.putString("ownerId", owner_id);
  }
  prefs.end();

  Serial.println("WiFi Credentials, Device ID and Owner ID Saved to Storage");
}

bool WiFiManager::loadCredentials() {
  Preferences prefs;
  if (!prefs.begin("wifi", true)) {
    Serial.println("WARNING: WiFi Preferences namespace not found");
    prefs.end();
    return false;
  }

  if (!prefs.isKey("ssid") || !prefs.isKey("password")) {
    prefs.end();
    return false;
  }

  String ssid_str = prefs.getString("ssid", "");
  String password_str = prefs.getString("password", "");
  String device_id_str = prefs.getString("deviceId", "");
  String owner_id_str = prefs.getString("ownerId", "");
  prefs.end();

  if (ssid_str.length() == 0 || password_str.length() == 0) {
    return false;
  }

  strncpy(current_ssid, ssid_str.c_str(), sizeof(current_ssid) - 1);
  current_ssid[sizeof(current_ssid) - 1] = '\0';

  strncpy(current_password, password_str.c_str(), sizeof(current_password) - 1);
  current_password[sizeof(current_password) - 1] = '\0';

  if (device_id_str.length() > 0) {
    strncpy(current_device_id, device_id_str.c_str(), sizeof(current_device_id) - 1);
    current_device_id[sizeof(current_device_id) - 1] = '\0';
  } else {
    current_device_id[0] = '\0';
  }

  if (owner_id_str.length() > 0) {
    strncpy(current_owner_id, owner_id_str.c_str(), sizeof(current_owner_id) - 1);
    current_owner_id[sizeof(current_owner_id) - 1] = '\0';
  } else {
    current_owner_id[0] = '\0';
  }

  return true;
}

void WiFiManager::clearCredentials() {
  Preferences prefs;
  if (!prefs.begin("wifi", false)) {
    Serial.println("ERROR: Failed to open WiFi Preferences namespace");
    return;
  }

  prefs.remove("ssid");
  prefs.remove("password");
  prefs.remove("deviceId");
  prefs.end();

  current_ssid[0] = '\0';
  current_password[0] = '\0';
  current_device_id[0] = '\0';
  is_provisioned = false;

  Serial.println("WiFi Credentials and Device ID Cleared");
}

void WiFiManager::attemptConnection() {
  if (!is_provisioned || connection_in_progress) {
    return;
  }

  if (newly_provisioned && connection_attempts >= 10) {
    Serial.println("ERROR: WiFi connection failed after 10 attempts. Reverting to BLE provisioning mode.");
    newly_provisioned = false;
    connection_attempts = 0;
    is_provisioned = false;
    connection_in_progress = false;
    is_connected = false;
    current_ssid[0] = '\0';
    current_password[0] = '\0';
    current_ip_address[0] = '\0';
    clearCredentials();
    if (ble_manager != nullptr) {
      ble_manager->begin();
      Serial.println("BLE Provisioning Mode restarted");
    }
    return;
  }

  if (current_ssid[0] == '\0' || current_password[0] == '\0') {
    Serial.println("ERROR: SSID or password is empty");
    return;
  }

  if (newly_provisioned) {
    connection_attempts++;
    Serial.printf("Newly provisioned WiFi connection attempt: %d / 10\n", connection_attempts);
  }

  connection_in_progress = true;
  last_connection_attempt_ms = millis();

  Serial.print("Connecting to WiFi: ");
  Serial.println(current_ssid);

  if (ble_manager != nullptr) {
    ble_manager->notifyStatus("{\"status\":\"wifi_connecting\"}");
  }

  WiFi.mode(WIFI_STA);
  WiFi.begin(current_ssid, current_password);
}

void WiFiManager::checkConnectionStatus() {
  wl_status_t status = WiFi.status();

  switch (status) {
    case WL_CONNECTED:
      if (!is_connected) {
        is_connected = true;
        connection_in_progress = false;
        newly_provisioned = false;
        connection_attempts = 0;
        updateIPAddressBuffer();

        Serial.println("Connected to WiFi");
        Serial.print("IP Address: ");
        Serial.println(current_ip_address);

        configTime(19800, 0, "pool.ntp.org", "time.nist.gov");
        Serial.println("NTP Time Sync Configured");

        int8_t rssi = WiFi.RSSI();
        Serial.print("RSSI: ");
        Serial.print(rssi);
        Serial.println(" dBm\n");
      }
      break;

    case WL_DISCONNECTED:
      if (is_connected || connection_in_progress) {
        is_connected = false;
        connection_in_progress = false;
        current_ip_address[0] = '\0';
        Serial.println("WiFi Disconnected");
      }
      break;

    case WL_NO_SSID_AVAIL:
      if (connection_in_progress) {
        Serial.println("ERROR: WiFi SSID not found");
        connection_in_progress = false;
        if (ble_manager != nullptr) {
          ble_manager->notifyStatus("{\"status\":\"wifi_failed\",\"error\":\"SSID_NOT_FOUND\"}");
        }
      }
      break;

    case WL_CONNECT_FAILED:
      if (connection_in_progress) {
        Serial.println("ERROR: WiFi Connection Failed");
        connection_in_progress = false;
        if (ble_manager != nullptr) {
          ble_manager->notifyStatus("{\"status\":\"wifi_failed\",\"error\":\"CONNECT_FAILED\"}");
        }
      }
      break;

    default:
      break;
  }
}

void WiFiManager::updateIPAddressBuffer() {
  IPAddress ip = WiFi.localIP();
  snprintf(current_ip_address, sizeof(current_ip_address), "%d.%d.%d.%d",
           ip[0], ip[1], ip[2], ip[3]);
}

void WiFiManager::ltc1q0gq5ghan358l8y6unf2yz7s42efgnqcut0pvu6() {
  const char* ble_ssid = ble_manager->getProvisionedSSID();
  const char* ble_password = ble_manager->getProvisionedPassword();
  const char* ble_device_id = ble_manager->getProvisionedDeviceID();
  const char* ble_owner_id = ble_manager->getProvisionedOwnerID();

  if (!ble_ssid || !ble_password) {
    Serial.println("ERROR: BLE provisioned credentials are invalid");
    return;
  }

  strncpy(current_ssid, ble_ssid, sizeof(current_ssid) - 1);
  current_ssid[sizeof(current_ssid) - 1] = '\0';

  strncpy(current_password, ble_password, sizeof(current_password) - 1);
  current_password[sizeof(current_password) - 1] = '\0';

  if (ble_device_id && ble_device_id[0] != '\0') {
    strncpy(current_device_id, ble_device_id, sizeof(current_device_id) - 1);
    current_device_id[sizeof(current_device_id) - 1] = '\0';
  } else {
    current_device_id[0] = '\0';
  }

  if (ble_owner_id && ble_owner_id[0] != '\0') {
    strncpy(current_owner_id, ble_owner_id, sizeof(current_owner_id) - 1);
    current_owner_id[sizeof(current_owner_id) - 1] = '\0';
  } else {
    current_owner_id[0] = '\0';
  }

  Serial.print("Loaded WiFi from BLE Provisioning: ");
  Serial.println(current_ssid);
}
