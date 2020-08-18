#include <Arduino.h>
#include <math.h>
//#include "BluetoothSerial.h"

#define countof(arg) ((unsigned int) (sizeof (arg) / sizeof (arg [0])))
#define ANALOG_READ_DELAY 30

const float VCC = 3.3;
const float V_DIV_RESISTANCE = 9900.0;
const float ADC_MAX = pow(2, ADC_RESOLUTION);

// The GPIO pins to which each pressure sensor is connected
// n.b. these pins must support analog-digital conversion
const int pressureSensors[] = {
  A7, // Large toe
  A6, // Small toe
  A3  // Heel
};
int pressureSamples[] = {0, 0, 0};
int currentSensorIndex = 0;

// TODO: there are two ADC channels, each with 4 GPIO pins
// Bluetooth makes use of the channel 2 ADC, which prevents us using those pins for analog read at the same time
// This limits us to at most 4 direct ADC inputs
// see: https://rntlab.com/wp-content/uploads/2018/01/ESP32-DOIT-DEVKIT-V1-Board-Pinout-36-GPIOs-Copy.png
// and: https://github.com/espressif/arduino-esp32/issues/2557
//BluetoothSerial SerialBT;

float getPressure(int adcReading) {
  const float v = VCC * adcReading / ADC_MAX;
  const float r = V_DIV_RESISTANCE * (VCC / v - 1.0);
  const float conductance = 1.0 / r;
  const float force = r <= 600 ?
    (conductance - 0.00075) / 0.00000032639 :
    conductance / 0.000000642857;
  return force;
}

void setup() {
  Serial.begin(9600);
  //SerialBT.begin("my-foot");
  for (auto ps : pressureSensors)
    pinMode(ps, INPUT);
}

void loop() {
  const int ps = pressureSensors[currentSensorIndex];
  const int fsrAdcReading = analogRead(ps);
  const float force = getPressure(fsrAdcReading);
  pressureSamples[currentSensorIndex] = force;

  // When we've sampled every sensor, send a message over Bluetooth
  if (currentSensorIndex == countof(pressureSensors) - 1) {
    char serialBuffer[64];
    sprintf(serialBuffer, "Millis %lu sensor %i pin %i adc %i force %f\n", millis(), currentSensorIndex, ps, fsrAdcReading, force);
    Serial.print(buffer);
    //SerialBT.printf("%d:%.2f\n", ps, force);
    delay(1000 - ANALOG_READ_DELAY * countof(pressureSensors)); // Wait 30 seconds before resuming sampling
  }

  currentSensorIndex = (currentSensorIndex + 1) % countof(pressureSensors);
  delay(ANALOG_READ_DELAY); // We allow 30 ms between analog reads to allow the hardware to settle
}
