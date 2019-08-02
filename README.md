Sensirion's Android Things user-space drivers
=================================

Peripheral drivers for Android Things for Sensirion sensors.

The general project layout was borrowed from the Android Things contrib-project:
https://github.com/androidthings/contrib-drivers

Like the AndroidThings contrib drivers we offer no guarantee of correctness, completeness, robustness, or suitability for any particular purpose.

How to use a driver
===================

Add the appropriate dependency for the driver you want to use, e.g.:

```
dependencies {
    implementation 'com.sensirion.android.things.drivers:driver-sht3x:1.0'
    ...
}
```

Current drivers
----------------

<!-- DRIVER_LIST_START -->
Driver | Type | Usage (add to your gradle dependencies) | Note
:---:|:---:| --- | ---
[sht3x](sht2x) | Temperature and Humidity Sensor | `com.sensirion.android.things.drivers:sht3x:1.0` | [changelog](sht3x/CHANGELOG.md)
<!-- DRIVER_LIST_END -->

