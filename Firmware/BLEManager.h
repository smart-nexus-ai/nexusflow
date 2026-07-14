#ifndef BLE_MANAGER_H
#define BLE_MANAGER_H

#include <stdint.h>
#include <stddef.h>
#include "Config.h"

class DeviceManager;

// ============================================================================
// BLEManager: Bluetooth Low Energy for device pairing and WiFi provisioning
// ============================================================================

class BLEManager {
public:
  explicit BLEManager(DeviceManager* device_manager);

  void begin();
  void update();
  void stop();

  bool isConnected();
  bool isProvisioned();
  uint8_t getConnectedDeviceCount();

  const char* getProvisionedSSID();
  const char* getProvisionedPassword();
  const char* getProvisionedDeviceID();
  const char* getProvisionedOwnerID();

  void clearProvisioning();
  bool parseWiFiProvisionJSON(const char* json_str);
  void notifyStatus(const char* status_json);

private:
  DeviceManager* device_manager;
  void* pStatusChar; // store BLECharacteristic pointer generically to avoid deep include issues in header

  char provisioned_ssid[33];
  char provisioned_password[64];
  char provisioned_device_id[33];
  char provisioned_owner_id[64];

  bool ble_initialized;
  bool is_provisioned;
  bool pin_verified;

  uint32_t last_pin_check_time_ms;

  void generateDeviceInfoJSON(char* out_buffer, size_t buffer_size);
  bool verifyPIN(const char* pin_str);
};

#endif // BLE_MANAGER_H
