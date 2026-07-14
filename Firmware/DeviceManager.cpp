#include "DeviceManager.h"
#include <Arduino.h>
#include <cstring>
#include <esp_mac.h>

DeviceManager::DeviceManager()
    : last_millis_snapshot(0), accumulated_uptime_seconds(0), is_initialized(false) {
  hardware_id_buffer[0] = '\0';
  mac_address_buffer[0] = '\0';
  device_name_buffer[0] = '\0';
}

void DeviceManager::begin() {
  readMACAddress();
  generateHardwareID();
  generateDeviceName();

  last_millis_snapshot = millis();
  accumulated_uptime_seconds = 0;

  is_initialized = true;

  printDeviceInfo();
}

void DeviceManager::update() {
  uint32_t current_millis = millis();
  uint32_t elapsed_ms = 0;

  if (current_millis < last_millis_snapshot) {
    elapsed_ms = (UINT32_MAX - last_millis_snapshot) + current_millis;
  } else {
    elapsed_ms = current_millis - last_millis_snapshot;
  }

  static uint32_t accumulated_ms = 0;
  accumulated_ms += elapsed_ms;

  if (accumulated_ms >= 1000) {
    accumulated_uptime_seconds += accumulated_ms / 1000;
    accumulated_ms %= 1000;
  }

  last_millis_snapshot = current_millis;
}

const char* DeviceManager::getHardwareID() {
  return hardware_id_buffer;
}

const char* DeviceManager::getMACAddress() {
  return mac_address_buffer;
}

const char* DeviceManager::getDeviceName() {
  return device_name_buffer;
}

const char* DeviceManager::getFirmwareVersion() {
  return FIRMWARE_VERSION;
}

const char* DeviceManager::getDeviceModel() {
  return DEVICE_MODEL;
}

uint8_t DeviceManager::getRelayCount() {
  return RELAY_COUNT;
}

uint32_t DeviceManager::getUptimeSeconds() {
  update();
  return accumulated_uptime_seconds;
}

void DeviceManager::readMACAddress() {
  uint8_t mac_bytes[6];
  esp_efuse_mac_get_default(mac_bytes);

  snprintf(mac_address_buffer, sizeof(mac_address_buffer),
           "%02X:%02X:%02X:%02X:%02X:%02X",
           mac_bytes[0], mac_bytes[1], mac_bytes[2],
           mac_bytes[3], mac_bytes[4], mac_bytes[5]);
}

void DeviceManager::generateHardwareID() {
  char last_four[5];
  extractMACLastFourHex(last_four, sizeof(last_four));

  uint8_t relay_count = getRelayCount();
  snprintf(hardware_id_buffer, sizeof(hardware_id_buffer),
           "SN-%uCH-%s", relay_count, last_four);
}

void DeviceManager::generateDeviceName() {
  char last_four[5];
  extractMACLastFourHex(last_four, sizeof(last_four));

  snprintf(device_name_buffer, sizeof(device_name_buffer),
           "NexusFlow-%s", last_four);
}

void DeviceManager::extractMACLastFourHex(char* out_hex, uint8_t out_size) {
  if (out_size < 5) {
    out_hex[0] = '\0';
    return;
  }

  if (strlen(mac_address_buffer) >= 17) {
    snprintf(out_hex, out_size, "%c%c%c%c",
             mac_address_buffer[12], mac_address_buffer[13],
             mac_address_buffer[15], mac_address_buffer[16]);
  } else {
    out_hex[0] = '\0';
  }
}

void DeviceManager::printDeviceInfo() {
  Serial.println("\n================================");
  Serial.println("NexusFlow Firmware");
  Serial.println("Phase 3 Device Manager");
  Serial.println("================================\n");

  Serial.print("Device Name:\n");
  Serial.print(getDeviceName());
  Serial.println("\n");

  Serial.print("Hardware ID:\n");
  Serial.print(getHardwareID());
  Serial.println("\n");

  Serial.print("MAC Address:\n");
  Serial.print(getMACAddress());
  Serial.println("\n");

  // Generate and display PIN based on MAC and relay count
  const char* dev_name = getDeviceName();
  int len = strlen(dev_name);
  if (len >= 4) {
    const char* last_four = dev_name + (len - 4);
    uint8_t channels = getRelayCount();
    const char* prefix = "1";
    if (channels == 6) prefix = "9";
    else if (channels == 8) prefix = "5";
    else if (channels == 4) prefix = "8";

    char hex_str[16];
    snprintf(hex_str, sizeof(hex_str), "%s%s", prefix, last_four);
    long val = strtol(hex_str, nullptr, 16);

    char pin_str[16];
    snprintf(pin_str, sizeof(pin_str), "%ld", val);

    Serial.print("Device PIN:\n");
    Serial.print(pin_str);
    Serial.println("\n");
  }

  Serial.print("Firmware Version:\n");
  Serial.print(getFirmwareVersion());
  Serial.println("\n");

  Serial.print("Device Model:\n");
  Serial.print(getDeviceModel());
  Serial.println("\n");

  Serial.print("Relay Count:\n");
  Serial.print(getRelayCount());
  Serial.println("\n");

  Serial.println("Device Manager Initialized\n");
}
