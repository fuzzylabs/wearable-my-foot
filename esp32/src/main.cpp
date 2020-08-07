#include <Arduino.h>
//#include "BluetoothSerial.h"

#define countof(arg) ((unsigned int) (sizeof (arg) / sizeof (arg [0])))
#define ADC_RESOLUTION 4096 // The resolution of the hardware analogue-digital convertor

const float VCC = 3.3;
const float V_DIV_RESISTANCE = 9900.0;

// The GPIO pins to which each pressure sensor is connected
// n.b. these pins must support analog-digital conversion
const int pressureSensors[] = {
  12 // Large toe
  //  11, // Small toe
  //10  // Heel
};

// TODO: there are two ADC channels, each with 4 GPIO pins
// Bluetooth makes use of the channel 2 ADC, which prevents us using those pins for analog read at the same time
// This limits us to at most 4 direct ADC inputs
// see: https://rntlab.com/wp-content/uploads/2018/01/ESP32-DOIT-DEVKIT-V1-Board-Pinout-36-GPIOs-Copy.png
// and: https://github.com/espressif/arduino-esp32/issues/2557

//BluetoothSerial SerialBT;

float getPressure(int adcReading) {
  const float v = VCC * adcReading / ADC_RESOLUTION;
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

int currentSensorIndex = 0;

void loop() {
  const int ps = pressureSensors[currentSensorIndex];
  const int fsrAdcReading = analogRead(ps);
  if (fsrAdcReading > 200) {
    const float force = getPressure(fsrAdcReading);
    //Serial.printf("Millis %lu sensor %i pin %i adc %i force %f\n", millis(), currentSensorIndex, ps, fsrAdcReading, force);
    //SerialBT.printf("%d:%.2f\n", ps, force);
    Serial.print(force);
    Serial.print("\n");
  }
  currentSensorIndex = (currentSensorIndex + 1) % countof(pressureSensors);
  delay(50);
}
