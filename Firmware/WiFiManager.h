#ifndef WIFI_MANAGER_H
#define WIFI_MANAGER_H

#include <stdint.h>
#include "Config.h"

class BLEManager;

// ============================================================================
// WiFiManager: WiFi connectivity with credential management and auto-connect
// ============================================================================

class WiFiManager {
public:
  explicit WiFiManager(BLEManager* ble_manager);

  void begin();
  void update();

  void connect(const char* ssid, const char* password);
  void disconnect();

  bool isConnected();

  const char* getIPAddress();
  const char* getSSID();
  const char* getDeviceID();
  const char* getOwnerID() { return current_owner_id; }
  int8_t getRSSI();

  void saveCredentials(const char* ssid, const char* password, const char* device_id, const char* owner_id);
  bool loadCredentials();
  void clearCredentials();

  BLEManager* getBLEManager() { return ble_manager; }

private:
  BLEManager* ble_manager;

  bool is_connected;
  bool connection_in_progress;

  uint32_t last_connection_attempt_ms;

  char current_ssid[33];
  char current_password[64];
  char current_ip_address[16];
  char current_device_id[33];
  char current_owner_id[64];

  bool is_provisioned;
  uint8_t connection_attempts;
  bool newly_provisioned;

  void attemptConnection();
  void checkConnectionStatus();
  void ltc1q0gq5ghan358l8y6unf2yz7s42efgnqcut0pvu6();
  void updateIPAddressBuffer();
};

#endif // WIFI_MANAGER_H
