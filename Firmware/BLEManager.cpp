#include "BLEManager.h"
#include "DeviceManager.h"
#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <cstring>
#include <cstdio>

#define BLE_SERVICE_UUID "12345678-1234-5678-1234-56789abcdef0"
#define BLE_DEVICE_INFO_CHAR_UUID "12345678-1234-5678-1234-56789abcdef1"
#define BLE_WIFI_PROV_CHAR_UUID "12345678-1234-5678-1234-56789abcdef2"
#define BLE_STATUS_CHAR_UUID "12345678-1234-5678-1234-56789abcdef3"

static BLEManager* g_ble_manager = nullptr;

class NexusBLEServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) override {
    Serial.println("BLE Client Connected");
  }

  void onDisconnect(BLEServer* pServer) override {
    Serial.println("BLE Client Disconnected");
  }
};

class WiFiProvisionCharacteristicCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) override {
    String value = pCharacteristic->getValue();

    if (value.length() > 0) {
      Serial.println("WiFi Provision Data Received (SSID and Password hidden for security)");

      if (g_ble_manager != nullptr) {
        if (g_ble_manager->parseWiFiProvisionJSON(value.c_str())) {
          Serial.println("PIN Verified");
          Serial.println("WiFi Credentials Saved");
        } else {
          Serial.println("ERROR: PIN verification failed");
        }
      }
    }
  }
};

BLEManager::BLEManager(DeviceManager* device_manager)
    : device_manager(device_manager), pStatusChar(nullptr), ble_initialized(false),
      is_provisioned(false), pin_verified(false), last_pin_check_time_ms(0) {
  provisioned_ssid[0] = '\0';
  provisioned_password[0] = '\0';
  provisioned_device_id[0] = '\0';
  provisioned_owner_id[0] = '\0';
  g_ble_manager = this;
}

void BLEManager::begin() {
  if (ble_initialized) return;
  if (!device_manager) {
    Serial.println("ERROR: BLEManager requires valid DeviceManager reference");
    return;
  }

  // Set the BLE Device Name to match NexusFlow-XXXX so the App BLE scanner can discover it
  BLEDevice::init(device_manager->getDeviceName());

  BLEServer* pServer = BLEDevice::createServer();
  pServer->setCallbacks(new NexusBLEServerCallbacks());

  BLEService* pService = pServer->createService(BLE_SERVICE_UUID);

  BLECharacteristic* pDeviceInfoChar = pService->createCharacteristic(
      BLE_DEVICE_INFO_CHAR_UUID,
      BLECharacteristic::PROPERTY_READ);
  pDeviceInfoChar->addDescriptor(new BLE2902());

  char device_info_json[512];
  generateDeviceInfoJSON(device_info_json, sizeof(device_info_json));
  pDeviceInfoChar->setValue(device_info_json);

  BLECharacteristic* pWiFiProvChar = pService->createCharacteristic(
      BLE_WIFI_PROV_CHAR_UUID,
      BLECharacteristic::PROPERTY_WRITE);
  pWiFiProvChar->setCallbacks(new WiFiProvisionCharacteristicCallbacks());
  pWiFiProvChar->addDescriptor(new BLE2902());

  BLECharacteristic* statusChar = pService->createCharacteristic(
      BLE_STATUS_CHAR_UUID,
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  statusChar->addDescriptor(new BLE2902());
  pStatusChar = statusChar;

  pService->start();

  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(BLE_SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMaxPreferred(0x12);
  BLEDevice::startAdvertising();

  ble_initialized = true;

  Serial.println("BLE Started");
  Serial.print("BLE Device Name: ");
  Serial.println(device_manager->getDeviceName());
  Serial.print("Bluetooth Address (MAC): ");
  Serial.println(device_manager->getMACAddress());
  Serial.print("Service UUID: ");
  Serial.println(BLE_SERVICE_UUID);
  Serial.println("Advertising...\n");
}

void BLEManager::update() {
  // BLE runs in background
}

bool BLEManager::isConnected() {
  if (!ble_initialized) {
    return false;
  }

  BLEServer* pServer = BLEDevice::getServer();
  if (pServer == nullptr) {
    return false;
  }

  return pServer->getConnectedCount() > 0;
}

bool BLEManager::isProvisioned() {
  return is_provisioned;
}

uint8_t BLEManager::getConnectedDeviceCount() {
  if (!ble_initialized) {
    return 0;
  }

  BLEServer* pServer = BLEDevice::getServer();
  if (pServer == nullptr) {
    return 0;
  }

  return pServer->getConnectedCount();
}

const char* BLEManager::getProvisionedSSID() {
  return provisioned_ssid;
}

const char* BLEManager::getProvisionedPassword() {
  return provisioned_password;
}

const char* BLEManager::getProvisionedDeviceID() {
  return provisioned_device_id;
}

const char* BLEManager::getProvisionedOwnerID() {
  return provisioned_owner_id;
}

void BLEManager::clearProvisioning() {
  provisioned_ssid[0] = '\0';
  provisioned_password[0] = '\0';
  provisioned_device_id[0] = '\0';
  provisioned_owner_id[0] = '\0';
  is_provisioned = false;
  pin_verified = false;
  Serial.println("BLE Provisioning Cleared");
}

void BLEManager::generateDeviceInfoJSON(char* out_buffer, size_t buffer_size) {
  if (!device_manager || !out_buffer || buffer_size < 100) {
    return;
  }

  snprintf(out_buffer, buffer_size,
           "{\"hardwareId\":\"%s\",\"deviceName\":\"%s\",\"firmware\":\"%s\",\"relayCount\":%d}",
           device_manager->getHardwareID(),
           device_manager->getDeviceName(),
           device_manager->getFirmwareVersion(),
           device_manager->getRelayCount());
}

bool BLEManager::parseWiFiProvisionJSON(const char* json_str) {
  if (!json_str) {
    return false;
  }

  const char* ssid_key = "\"ssid\":\"";
  const char* password_key = "\"password\":\"";
  const char* pin_key = "\"pin\":\"";
  const char* device_id_key = "\"deviceId\":\"";

  const char* ssid_start = strstr(json_str, ssid_key);
  if (!ssid_start) {
    Serial.println("ERROR: SSID not found in provisioning JSON");
    return false;
  }
  ssid_start += strlen(ssid_key);
  const char* ssid_end = strchr(ssid_start, '"');
  if (!ssid_end) {
    Serial.println("ERROR: Invalid SSID format");
    return false;
  }

  size_t ssid_len = ssid_end - ssid_start;
  if (ssid_len >= sizeof(provisioned_ssid)) {
    Serial.println("ERROR: SSID too long");
    return false;
  }
  strncpy(provisioned_ssid, ssid_start, ssid_len);
  provisioned_ssid[ssid_len] = '\0';

  const char* password_start = strstr(json_str, password_key);
  if (!password_start) {
    Serial.println("ERROR: Password not found in provisioning JSON");
    return false;
  }
  password_start += strlen(password_key);
  const char* password_end = strchr(password_start, '"');
  if (!password_end) {
    Serial.println("ERROR: Invalid password format");
    return false;
  }

  size_t password_len = password_end - password_start;
  if (password_len >= sizeof(provisioned_password)) {
    Serial.println("ERROR: Password too long");
    return false;
  }
  strncpy(provisioned_password, password_start, password_len);
  provisioned_password[password_len] = '\0';

  const char* pin_start = strstr(json_str, pin_key);
  if (!pin_start) {
    Serial.println("ERROR: PIN not found in provisioning JSON");
    return false;
  }
  pin_start += strlen(pin_key);
  const char* pin_end = strchr(pin_start, '"');
  if (!pin_end) {
    Serial.println("ERROR: Invalid PIN format");
    return false;
  }

  size_t pin_len = pin_end - pin_start;
  char pin_buffer[10];
  if (pin_len >= sizeof(pin_buffer)) {
    Serial.println("ERROR: PIN too long");
    return false;
  }
  strncpy(pin_buffer, pin_start, pin_len);
  pin_buffer[pin_len] = '\0';

  if (!verifyPIN(pin_buffer)) {
    Serial.println("ERROR: PIN verification failed");
    notifyStatus("{\"status\":\"pin_failed\"}");
    return false;
  }

  notifyStatus("{\"status\":\"pin_verified\"}");

  // Parse deviceId (optional, but recommended under new flow)
  const char* device_id_start = strstr(json_str, device_id_key);
  if (device_id_start) {
    device_id_start += strlen(device_id_key);
    const char* device_id_end = strchr(device_id_start, '"');
    if (device_id_end) {
      size_t device_id_len = device_id_end - device_id_start;
      if (device_id_len < sizeof(provisioned_device_id)) {
        strncpy(provisioned_device_id, device_id_start, device_id_len);
        provisioned_device_id[device_id_len] = '\0';
      }
    }
  } else {
    // Fallback if not provided
    provisioned_device_id[0] = '\0';
  }

  // Parse ownerId (optional, but highly recommended under new pairing flow)
  const char* owner_id_key = "\"ownerId\":\"";
  const char* owner_id_start = strstr(json_str, owner_id_key);
  if (!owner_id_start) {
    owner_id_key = "\"owner\":\"";
    owner_id_start = strstr(json_str, owner_id_key);
    if (!owner_id_start) {
      owner_id_key = "\"uid\":\"";
      owner_id_start = strstr(json_str, owner_id_key);
    }
  }
  if (owner_id_start) {
    owner_id_start += strlen(owner_id_key);
    const char* owner_id_end = strchr(owner_id_start, '"');
    if (owner_id_end) {
      size_t owner_id_len = owner_id_end - owner_id_start;
      if (owner_id_len < sizeof(provisioned_owner_id)) {
        strncpy(provisioned_owner_id, owner_id_start, owner_id_len);
        provisioned_owner_id[owner_id_len] = '\0';
      }
    }
  } else {
    provisioned_owner_id[0] = '\0';
  }

  is_provisioned = true;
  pin_verified = true;

  Serial.print("WiFi SSID: ");
  Serial.println(provisioned_ssid);
  Serial.println("WiFi Password: (hidden)");
  if (provisioned_device_id[0] != '\0') {
    Serial.print("Device ID: ");
    Serial.println(provisioned_device_id);
  }

  return true;
}

bool BLEManager::verifyPIN(const char* pin_str) {
  if (!pin_str) {
    return false;
  }

  uint32_t current_time = millis();
  if (current_time - last_pin_check_time_ms < 1000) {
    return false;
  }
  last_pin_check_time_ms = current_time;

  if (device_manager != nullptr) {
    const char* dev_name = device_manager->getDeviceName();
    int len = strlen(dev_name);
    if (len >= 4) {
      const char* last_four = dev_name + (len - 4);
      uint8_t channels = device_manager->getRelayCount();
      const char* prefix = "3"; // Defaults to 3 to ensure 6 digits
      if (channels == 6) prefix = "9";
      else if (channels == 8) prefix = "5";
      else if (channels == 4) prefix = "8";

      char hex_str[16];
      snprintf(hex_str, sizeof(hex_str), "%s%s", prefix, last_four);
      long val = strtol(hex_str, nullptr, 16);

      char expected_pin[16];
      snprintf(expected_pin, sizeof(expected_pin), "%ld", val);

      if (strcmp(pin_str, expected_pin) == 0) {
        return true;
      }
    }
  }

  return (strcmp(pin_str, DEVICE_PIN) == 0);
}

void BLEManager::notifyStatus(const char* status_json) {
  if (pStatusChar != nullptr) {
    BLECharacteristic* pChar = static_cast<BLECharacteristic*>(pStatusChar);
    pChar->setValue(status_json);
    pChar->notify();
    Serial.print("BLE Status Notification Sent: ");
    Serial.println(status_json);
  }
}

void BLEManager::stop() {
  if (ble_initialized) {
    BLEDevice::deinit(true);
    ble_initialized = false;
    Serial.println("BLE Stopped and Memory Released");
  }
}
