#include <Arduino.h>
#include <Arduino_LSM6DS3.h>
#include <ArduinoBLE.h>

typedef struct{
  float x;
  float y;
  float z;
} vector3d_struct_t;

typedef struct{
  vector3d_struct_t acceleration;
  vector3d_struct_t gyroscope;
} imu_reading_struct_t;

typedef union{
  imu_reading_struct_t reading;
  uint8_t array[sizeof(imu_reading_struct_t)];
} imu_reading_t;
imu_reading_t reading = {};

BLEService powermetreService("1FFF");

BLECharacteristic imuReadingChar("2FFF", BLERead | BLENotify, sizeof(imu_reading_t));

#define IMU_POLLING_PERIOD 500
long lastMillis = 0;

void setup() {
  pinMode(LED_BUILTIN, OUTPUT); // initialize the built-in LED pin to indicate when a central is connected

  Serial.begin(9600);
  // while (!Serial) {
  //   ; // wait for serial port to connect. Needed for native USB port only
  // }
  if (!IMU.begin()) {
    Serial.println("Failed to initialize IMU!");
    while (true); // halt program
  } 
  Serial.println("IMU initialized!");

  if (!BLE.begin()) {
    Serial.println("starting BLE failed!");

    while (true);
  }
  Serial.println("BLE started");

  Serial.print("Size of vector: ");
  Serial.println(sizeof(imu_reading_t));

  BLE.setLocalName("Powermetre");
  BLE.setAdvertisedService(powermetreService);
  Serial.println(powermetreService.uuid());
  powermetreService.addCharacteristic(imuReadingChar);
  BLE.addService(powermetreService);
  imuReadingChar.writeValue(reading.array, sizeof(reading.array));

  BLE.advertise();

  Serial.println("BLE setup finished.");
}

void pollIMU() {
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

    reading = {
      .reading = {
        .acceleration = {aX, aY, aZ},
        .gyroscope = {gX, gY, gZ}
      }
    };

    if(imuReadingChar.writeValue(reading.array, sizeof(reading.array))) {
      Serial.print("Reading written to the characteristic: ");
      Serial.print(sizeof(reading.array));
      Serial.println(" bytes");
    }
  }
}

void loop() {
  // Serial.println("Wainting for connection");
  BLEDevice central = BLE.central(); // Wait for connection

  if(central) {
    Serial.print("Central device connected: ");
    Serial.println(central.address());

    digitalWrite(LED_BUILTIN, HIGH);

    while(central.connected()) {
      long currentMillis = millis();

      if (currentMillis - lastMillis >= IMU_POLLING_PERIOD) {
        lastMillis = currentMillis;
        pollIMU();
      }
    }

    digitalWrite(LED_BUILTIN, LOW);
    Serial.print("Disconnected from central: ");
    Serial.println(central.address());
  }
}