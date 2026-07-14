#ifndef DEVICE_MANAGER_H
#define DEVICE_MANAGER_H

#include <stdint.h>
#include "Config.h"

// ============================================================================
// DeviceManager: Hardware identity, device information, and uptime tracking
// ============================================================================

class DeviceManager {
public:
  DeviceManager();

  void begin();
  void update();

  const char* getHardwareID();
  const char* getMACAddress();
  const char* getDeviceName();
  const char* getFirmwareVersion();
  const char* getDeviceModel();
  uint8_t getRelayCount();
  uint32_t getUptimeSeconds();

private:
  char hardware_id_buffer[20];
  char mac_address_buffer[18];
  char device_name_buffer[20];

  uint32_t last_millis_snapshot;
  uint32_t accumulated_uptime_seconds;

  bool is_initialized;

  void generateHardwareID();
  void generateDeviceName();
  void readMACAddress();
  void extractMACLastFourHex(char* out_hex, uint8_t out_size);
  void printDeviceInfo();
};

#endif // DEVICE_MANAGER_H
