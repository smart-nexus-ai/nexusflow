#include "SensorManager.h"
#include <Arduino.h>
#include <Wire.h>
#include <math.h>

SensorManager::SensorManager()
    : temperature(25.0f), humidity(60.0f), humidex(25.0f), aht_state(0), trigger_time_ms(0) {}

void SensorManager::begin() {
  Wire.begin(); // Join I2C bus (defaults to GPIO 21 SDA, 22 SCL on ESP32 DevKit V1)
  
  // Send system calibration command to AHT10 (required after power on)
  Wire.beginTransmission(0x38);
  Wire.write(0xE1);
  Wire.write(0x08);
  Wire.write(0x00);
  Wire.endTransmission();
  
  trigger_time_ms = millis();
  aht_state = 0;
}

void SensorManager::update() {
  uint32_t current_time = millis();

  if (aht_state == 0) {
    // Check if 5 seconds have elapsed to trigger a new reading
    if (current_time - trigger_time_ms >= 5000) {
      // Trigger measurement command
      Wire.beginTransmission(0x38);
      Wire.write(0xAC);
      Wire.write(0x33);
      Wire.write(0x00);
      Wire.endTransmission();
      
      trigger_time_ms = current_time;
      aht_state = 1;
    }
  } else if (aht_state == 1) {
    // Wait at least 80ms for conversion to complete
    if (current_time - trigger_time_ms >= 80) {
      Wire.requestFrom(0x38, 6);
      if (Wire.available() >= 6) {
        uint8_t data[6];
        for (int i = 0; i < 6; i++) {
          data[i] = Wire.read();
        }
        
        // Status bit 7: 0 = idle, 1 = busy
        if ((data[0] & 0x80) == 0) {
          uint32_t raw_hum = (((uint32_t)data[1]) << 12) | (((uint32_t)data[2]) << 4) | (((uint32_t)data[3]) >> 4);
          uint32_t raw_temp = ((((uint32_t)data[3]) & 0x0F) << 16) | (((uint32_t)data[4]) << 8) | ((uint32_t)data[5]);
          
          humidity = ((float)raw_hum * 100.0f) / 1048576.0f;
          temperature = (((float)raw_temp * 200.0f) / 1048576.0f) - 50.0f;
          
          // Apply bounds check
          if (temperature < -40.0f) temperature = -40.0f;
          if (temperature > 85.0f) temperature = 85.0f;
          if (humidity < 0.0f) humidity = 0.0f;
          if (humidity > 100.0f) humidity = 100.0f;
          
          humidex = calculateHumidex(temperature, humidity);
        }
      }
      aht_state = 0; // Go back to waiting for next 5s interval
    }
  }
}

float SensorManager::getTemperature() {
  return temperature;
}

float SensorManager::getHumidity() {
  return humidity;
}

float SensorManager::getHumidex() {
  return humidex;
}

float SensorManager::calculateHumidex(float temp, float hum) {
  double kelvin = 273.15 + temp;
  double e = 6.11 * exp(5417.7530 * (1.0 / 273.16 - 1.0 / kelvin)) * (hum / 100.0);
  double h = temp + (0.5555 * (e - 10.0));
  return (float)h;
}
