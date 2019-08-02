/*
 * Copyright 2019 Sensirion AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensirion.android.things.drivers.sht3x;

import android.hardware.Sensor;

import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.sensor.UserSensor;
import com.google.android.things.userdriver.sensor.UserSensorDriver;
import com.google.android.things.userdriver.sensor.UserSensorReading;

import java.io.IOException;
import java.util.UUID;

public class Sht3xSensorDriver implements AutoCloseable {

    // DRIVER parameters
    // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
    private static final String DRIVER_VENDOR = "Sensirion";
    private static final String DRIVER_NAME = "SHT3x";
    private static final int DRIVER_MIN_DELAY_US = Math.round(1000000.f / Sht3x.MAX_FREQ_HZ);
    private static final int DRIVER_MAX_DELAY_US = Math.round(1000000.f / Sht3x.MIN_FREQ_HZ);

    private Sht3x mDevice;

    private TemperatureUserDriver mTemperatureUserDriver;
    private HumidityUserDriver mHumidityUserDriver;

    /**
     * Create a new framework sensor driver connected on the given bus.
     * The driver emits {@link Sensor} with temperature and humidity data when
     * registered.
     *
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     * @see #registerTemperatureSensor()
     */
    public Sht3xSensorDriver(String bus) throws IOException {
        mDevice = new Sht3x(bus);
    }

    /**
     * Create a new framework sensor driver connected on the given bus and address.
     * The driver emits {@link Sensor} with temperature and humidity data when
     * registered.
     *
     * @param bus     I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException
     * @see #registerTemperatureSensor()
     */
    public Sht3xSensorDriver(String bus, int address) throws IOException {
        mDevice = new Sht3x(bus, address);
    }

    /**
     * Close the driver and the underlying device.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        unregisterTemperatureSensor();
        unregisterHumiditySensor();
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Register a {@link UserSensor} that pipes temperature readings into the Android SensorManager.
     *
     * @see #unregisterTemperatureSensor()
     */
    public void registerTemperatureSensor() {
        if (mDevice == null) {
            throw new IllegalStateException("cannot register closed driver");
        }

        if (mTemperatureUserDriver == null) {
            mTemperatureUserDriver = new TemperatureUserDriver();
            UserDriverManager.getInstance().registerSensor(mTemperatureUserDriver.getUserSensor());
        }
    }

    /**
     * Register a {@link UserSensor} that pipes humidity readings into the Android SensorManager.
     *
     * @see #unregisterHumiditySensor()
     */
    public void registerHumiditySensor() {
        if (mDevice == null) {
            throw new IllegalStateException("cannot register closed driver");
        }

        if (mHumidityUserDriver == null) {
            mHumidityUserDriver = new HumidityUserDriver();
            UserDriverManager.getInstance().registerSensor(mHumidityUserDriver.getUserSensor());
        }
    }

    /**
     * Unregister the temperature {@link UserSensor}.
     */
    public void unregisterTemperatureSensor() {
        if (mTemperatureUserDriver != null) {
            UserDriverManager.getInstance().unregisterSensor(mTemperatureUserDriver.getUserSensor());
            mTemperatureUserDriver = null;
        }
    }

    /**
     * Unregister the humidity {@link UserSensor}.
     */
    public void unregisterHumiditySensor() {
        if (mHumidityUserDriver != null) {
            UserDriverManager.getInstance().unregisterSensor(mHumidityUserDriver.getUserSensor());
            mHumidityUserDriver = null;
        }
    }

    private class TemperatureUserDriver implements UserSensorDriver {
        // DRIVER parameters
        // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
        private static final float DRIVER_MAX_RANGE = Sht3x.MAX_TEMP_C;
        private static final float DRIVER_RESOLUTION = 0.005f;
        private static final float DRIVER_POWER = Sht3x.MAX_POWER_CONSUMPTION_TEMP_UA / 1000.f;
        private static final int DRIVER_VERSION = 1;

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = new UserSensor.Builder()
                        .setType(Sensor.TYPE_AMBIENT_TEMPERATURE)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setVersion(DRIVER_VERSION)
                        .setMaxRange(DRIVER_MAX_RANGE)
                        .setResolution(DRIVER_RESOLUTION)
                        .setPower(DRIVER_POWER)
                        .setMinDelay(DRIVER_MIN_DELAY_US)
                        .setMaxDelay(DRIVER_MAX_DELAY_US)
                        .setUuid(UUID.randomUUID())
                        .setDriver(this)
                        .build();
            }
            return mUserSensor;
        }

        @Override
        public UserSensorReading read() throws IOException {
            return new UserSensorReading(new float[]{mDevice.readTemperature()});
        }

        @Override
        public void setEnabled(boolean enabled) {
            mEnabled = enabled;
        }

        private boolean isEnabled() {
            return mEnabled;
        }
    }

    private class HumidityUserDriver implements UserSensorDriver {
        // DRIVER parameters
        // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
        private static final float DRIVER_MAX_RANGE = Sht3x.MAX_HUM_RH;
        private static final float DRIVER_RESOLUTION = 0.005f;
        private static final float DRIVER_POWER = Sht3x.MAX_POWER_CONSUMPTION_HUMIDITY_UA / 1000.f;
        private static final int DRIVER_VERSION = 1;

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = new UserSensor.Builder()
                        .setType(Sensor.TYPE_RELATIVE_HUMIDITY)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setVersion(DRIVER_VERSION)
                        .setMaxRange(DRIVER_MAX_RANGE)
                        .setResolution(DRIVER_RESOLUTION)
                        .setPower(DRIVER_POWER)
                        .setMinDelay(DRIVER_MIN_DELAY_US)
                        .setMaxDelay(DRIVER_MAX_DELAY_US)
                        .setUuid(UUID.randomUUID())
                        .setDriver(this)
                        .build();
            }
            return mUserSensor;
        }

        @Override
        public UserSensorReading read() throws IOException {
            return new UserSensorReading(new float[]{mDevice.readHumidity()});
        }

        @Override
        public void setEnabled(boolean enabled) throws IOException {
            mEnabled = enabled;
        }

        private boolean isEnabled() {
            return mEnabled;
        }
    }
}
