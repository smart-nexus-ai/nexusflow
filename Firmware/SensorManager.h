#ifndef SENSOR_MANAGER_H
#define SENSOR_MANAGER_H

#include <stdint.h>

class SensorManager {
public:
  SensorManager();

  void begin();
  void update();

  float getTemperature();
  float getHumidity();
  float getHumidex();

private:
  float temperature;
  float humidity;
  float humidex;

  uint8_t aht_state; // 0 = idle/wait next poll, 1 = wait measurement
  uint32_t trigger_time_ms;
  
  float calculateHumidex(float temp, float hum);
};

#endif // SENSOR_MANAGER_H
