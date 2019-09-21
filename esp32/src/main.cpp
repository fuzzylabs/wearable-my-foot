#include <Arduino.h>

#define ADC_RESOLUTION 4096

const float VCC = 3.3;
const float V_DIV_RESISTANCE = 9900.0;

// The GPIO pins to which each pressure sensor is connected
// n.b. these pins must support analog-digital conversion
const int pressureSensors[] = {
  15, 2, 4
};

void getPressure(int adcReading) {
  Serial.printf("ADC: %d\n", adcReading);

  const float v = VCC * adcReading / ADC_RESOLUTION;
  Serial.printf("Voltage: %f v\n", v);

  const float r = V_DIV_RESISTANCE * (VCC / v - 1.0);
  Serial.printf("Resistance: %f Ohms\n", r);

  const float conductance = 1.0 / r;
  const float force = r <= 600 ?
    (conductance - 0.00075) / 0.00000032639 :
    conductance / 0.000000642857;
  Serial.printf("Force: %d\n g", force);
  Serial.println();
}

void setup() {
  Serial.begin(9600);
  for (auto ps : pressureSensors)
    pinMode(ps, INPUT);
}

void loop() {
  for (auto ps : pressureSensors) {
    const int fsrAdcReading = analogRead(ps);
    if (fsrAdcReading != 0) {
      Serial.printf("Sensor number %d\n", ps);
      Serial.println("----------------");
      getPressure(fsrAdcReading);
      delay(500);
    }
  }
}
