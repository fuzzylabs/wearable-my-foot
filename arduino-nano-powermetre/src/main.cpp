#include <Arduino.h>
#include <Arduino_LSM6DS3.h>

void setup() {
  Serial.begin(9600);
  while (!Serial) {
    ; // wait for serial port to connect. Needed for native USB port only
  }
  if (!IMU.begin()) {
    Serial.println("Failed to initialize IMU!");
    while (true); // halt program
  } 
  Serial.println("IMU initialized!");
}

void loop() {
  float aX, aY, aZ;
  float gX, gY, gZ;
  const char * spacer = ", ";
 
  if (
    IMU.accelerationAvailable() 
    && IMU.gyroscopeAvailable()
  ) {      
    IMU.readAcceleration(aX, aY, aZ);
    IMU.readGyroscope(gX, gY, gZ);
    Serial.print(aX); Serial.print(spacer);
    Serial.print(aY); Serial.print(spacer);
    Serial.print(aZ); Serial.print(spacer);
    Serial.print(gX); Serial.print(spacer);
    Serial.print(gY); Serial.print(spacer);
    Serial.println(gZ);
    delay(100);
  }
}