# Wearable my foot - ESP32 software

ESP32 is an Arduino-compatible microcontroller. The software is built and deployed using PlatformIO.

## Prerequisites

Python 2 and optionally pip + virtualenv.

### Using a virtual environment (optional)

Feel free to skip if you're installing / have installed PlatformIO using a different method. To initialise the environment using `pip`:

```
virtualenv env
source env/bin/activate
pip install -r requirements.txt
```

To activate the environment, `source env/bin/activate`.

### Building and deploying

*Just build*

```
platformio run
```

*Build and upload*

```
platformio upload
```

*Run the tests* - this builds a new image and uploads it so that tests are run on the device

```
platformio test
```
