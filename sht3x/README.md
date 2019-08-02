SHT3x driver for Android Things
================================

This driver supports the Sensirion [SHT3x][product_sht3x] environmental (temperature and humidity) sensor.

How to use the driver
---------------------

### Gradle dependency

To use the `sht3x` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    implementation 'com.sensirion.android.things.drivers:driver-sht3x:<version>'
}
```

### Sample usage

```java
import com.sensirion.android.things.drivers.sht3x.Sht3x;

// Access the environmental sensor:

Sht3x sht3x;

try {
    sht3x = new Sht3x(i2cBusName);
} catch (IOException e) {
    finish();
}

// Read the current temperature and humidity:

try {
    float temperature = sht3x.readTemperature();
} catch (IOException e) {
    // error reading temperature sensor data
}

// Close the environmental sensor when finished:

try {
    sht3x.close();
} catch (IOException e) {
    // error closing sensor
}
```

If you need to read sensor values continuously, you can register the Sht3x with the system and
listen for sensor values using the [Sensor APIs][sensors]:
```java
SensorManager mSensorManager = getSystemService(Context.SENSOR_SERVICE);
SensorEventListener mListener = ...;
Sht3xSensorDriver mSensorDriver;

mSensorManager.registerDynamicSensorCallback(new SensorManager.DynamicSensorCallback() {
    @Override
    public void onDynamicSensorConnected(Sensor sensor) {
        if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            mSensorManager.registerListener(mListener, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
});

try {
    mSensorDriver = new Sht3xSensorDriver(i2cBusName);
    mSensorDriver.registerTemperatureSensor();
} catch (IOException e) {
    // Error configuring sensor
}

// Unregister and close the driver when finished:

mSensorManager.unregisterListener(mListener);
mSensorDriver.unregisterTemperatureSensor();
try {
    mSensorDriver.close();
} catch (IOException e) {
    // error closing sensor
}
```

License
-------

Copyright 2019 Sensirion AG.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


[product_sht3x]: http://www.sensirion.com/sht3x
[jcenter]: https://bintray.com/sensirion/AndroidThingsDrivers/driver-sht3x/_latestVersion
[sensors]: https://developer.android.com/guide/topics/sensors/sensors_overview.html
