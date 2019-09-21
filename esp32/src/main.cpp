#include <Arduino.h>
#include "BluetoothSerial.h"

#define ADC_RESOLUTION 4096

const float VCC = 3.3;
const float V_DIV_RESISTANCE = 9900.0;

// The GPIO pins to which each pressure sensor is connected
// n.b. these pins must support analog-digital conversion
const int pressureSensors[] = {
  15, 2, 4
};

BluetoothSerial SerialBT;

float getPressure(int adcReading) {
  Serial.printf("ADC: %d\n", adcReading);

  const float v = VCC * adcReading / ADC_RESOLUTION;
  Serial.printf("Voltage: %.2f v\n", v);

  const float r = V_DIV_RESISTANCE * (VCC / v - 1.0);
  Serial.printf("Resistance: %.2f Ohms\n", r);

  const float conductance = 1.0 / r;
  const float force = r <= 600 ?
    (conductance - 0.00075) / 0.00000032639 :
    conductance / 0.000000642857;
  Serial.printf("Force: %.2f g\n", force);
  Serial.println();

  return force;
}

void setup() {
  Serial.begin(9600);
  SerialBT.begin("my-foot");

  // TODO: because Bluetooth makes use of the channel 2 ADC, this needs to use other inputs
  // see: https://rntlab.com/wp-content/uploads/2018/01/ESP32-DOIT-DEVKIT-V1-Board-Pinout-36-GPIOs-Copy.png
  // and: https://github.com/espressif/arduino-esp32/issues/2557
  //for (auto ps : pressureSensors)
  //  pinMode(ps, INPUT);
}

void loop() {
  for (auto ps : pressureSensors) {
    // TODO: replace this once the sensor inputs are using the right pins
    const int fsrAdcReading = 100;//analogRead(ps);
    if (fsrAdcReading != 0) {
      Serial.printf("Sensor number %d\n", ps);
      Serial.println("----------------");
      const float force = getPressure(fsrAdcReading);
      SerialBT.printf("%.2f", force);
      delay(500);
    }
  }
}
