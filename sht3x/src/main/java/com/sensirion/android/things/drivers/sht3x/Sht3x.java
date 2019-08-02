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

import android.support.annotation.IntDef;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static java.lang.Thread.sleep;

/**
 * Driver for the SHT3x temperature and humidity sensor.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Sht3x implements AutoCloseable {

    public static final int DEFAULT_I2C_ADDRESS = 0x44;
    @Deprecated
    public static final int I2C_ADDRESS = DEFAULT_I2C_ADDRESS;

    /**
     * Measurement accuracy mode. Higher accuracy comes with slower measurements.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ACCURACY_MODE_HIGH, ACCURACY_MODE_MEDIUM, ACCURACY_MODE_LOW})
    public @interface AccuracyMode {}
    public static final int ACCURACY_MODE_HIGH = 0;
    public static final int ACCURACY_MODE_MEDIUM = 1;
    public static final int ACCURACY_MODE_LOW = 2;

    // Sensor constants from the datasheet:
    // http://www.sensirion.com/file/datasheet_sht3x_digital
    /**
     * Mininum temperature in Celsius the sensor can measure.
     */
    public static final float MIN_TEMP_C = -40f;
    /**
     * Maximum temperature in Celsius the sensor can measure.
     */
    public static final float MAX_TEMP_C = 125f;
    /**
     * Mininum humidity in RH the sensor can measure.
     */
    public static final float MIN_HUM_RH = 0f;
    /**
     * Maximum humidity in RH the sensor can measure.
     */
    public static final float MAX_HUM_RH = 100f;
    /**
     * Maximum power consumption in micro-amperes when measuring temperature.
     */
    public static final float MAX_POWER_CONSUMPTION_TEMP_UA = 1500f;
    /**
     * Maximum power consumption in micro-amperes when measuring humidity.
     */
    public static final float MAX_POWER_CONSUMPTION_HUMIDITY_UA = 1500f;
    /**
     * Maximum frequency of the measurements.
     */
    public static final float MAX_FREQ_HZ = 20f;
    /**
     * Minimum frequency of the measurements.
     */
    public static final float MIN_FREQ_HZ = 0.00001f;

    // Commands
    private static class Sht3xCommand {
        final byte[] command;
        final int max_duration_ms;
        final int readLength;

        Sht3xCommand(final byte[] command, final int max_duration_ms, final int readLength) {
            this.command = command;
            this.max_duration_ms = max_duration_ms;
            this.readLength = readLength;
        }
    }

    private static final Sht3xCommand SHT3X_CMD_READ_TEMP_AND_HUMI_ACC_HIGH =
            new Sht3xCommand(new byte[]{0x24, 0x00}, 15, 6);
    private static final Sht3xCommand SHT3X_CMD_READ_TEMP_AND_HUMI_ACC_MED =
            new Sht3xCommand(new byte[]{0x24, 0x0B}, 6, 6);
    private static final Sht3xCommand SHT3X_CMD_READ_TEMP_AND_HUMI_ACC_LOW =
            new Sht3xCommand(new byte[]{0x24, 0x16}, 4, 6);
    private static final Sht3xCommand SHT3X_CMD_READ_STATUS_REG =
            new Sht3xCommand(new byte[]{(byte) 0xF3, 0x2D}, 4, 3);

    private I2cDevice mDevice;
    private final byte[] mBuffer = new byte[6]; // for reading sensor values
    private Sht3xCommand mMeasureCmd = SHT3X_CMD_READ_TEMP_AND_HUMI_ACC_HIGH;
    private float mTemperatureOffset = -45f;
    private float mTemperatureGain = 175f;
    private float mTemperatureScaling = 65535f;
    private float mHumidityGain = 100f;
    private float mHumidityScaling = 65535f;

    /**
     * Create a new Sht3x sensor driver connected on the given bus.
     *
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     */
    public Sht3x(String bus) throws IOException {
        this(bus, DEFAULT_I2C_ADDRESS);
    }

    /**
     * Create a new Sht3x sensor driver connected on the given bus and address.
     *
     * @param bus     I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException
     */
    public Sht3x(String bus, int address) throws IOException {
        PeripheralManager pioService = PeripheralManager.getInstance();
        I2cDevice device = pioService.openI2cDevice(bus, address);
        try {
            connect(device);
        } catch (IOException | RuntimeException e) {
            try {
                close();
            } catch (IOException | RuntimeException ignored) {
            }
            throw e;
        }
    }

    /**
     * Create a new Sht3x sensor driver connected to the given I2C device.
     *
     * @param device I2C device of the sensor.
     * @throws IOException
     */
    /*package*/  Sht3x(I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(I2cDevice device) throws IOException {
        mDevice = device;

        int status = readStatus();
    }

    /**
     * Close the driver and the underlying device.
     */
    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Set a specific accuracy mode that leads to faster/slower measurements.
     *
     * @param accuracyMode The desired accuracy mode.
     */
    public void setAccuracyMode(@AccuracyMode int accuracyMode) {
        switch (accuracyMode) {
            case ACCURACY_MODE_LOW:
                mMeasureCmd = SHT3X_CMD_READ_TEMP_AND_HUMI_ACC_LOW;
                break;
            case ACCURACY_MODE_MEDIUM:
                mMeasureCmd = SHT3X_CMD_READ_TEMP_AND_HUMI_ACC_MED;
                break;
            case ACCURACY_MODE_HIGH:
            default:
                mMeasureCmd = SHT3X_CMD_READ_TEMP_AND_HUMI_ACC_HIGH;
        }
    }

    /**
     * Read the sensor's status register, c.f. data sheet:
     * http://www.sensirion.com/file/datasheet_sht3x_digital
     *
     * @return The sensor's status as integer
     */
    public int readStatus() throws IOException {
        return readCommand(SHT3X_CMD_READ_STATUS_REG)[0];
    }

    /**
     * Read the current temperature.
     *
     * @return the current temperature in degrees Celsius
     */
    public float readTemperature() throws IOException {
        return readTemperatureAndHumidity()[0];
    }

    /**
     * Read the current humidity.
     *
     * @return the current relative humidity in RH percentage (100f means totally saturated air)
     */
    public float readHumidity() throws IOException {
        return readTemperatureAndHumidity()[1];
    }

    /**
     * Read the current temperature and relative humidity.
     *
     * @return a 2-element array. The first element is temperature in degrees Celsius, and the
     * second is relative humidity in RH percentage (100f means totally saturated air)
     * @throws IOException
     */
    public float[] readTemperatureAndHumidity() throws IOException {

        int[] res = readCommand(mMeasureCmd);
        return new float[]{convertTemperature(res[0]), convertHumidity(res[1])};
    }

    private float convertTemperature(int rawTemperature) {
        return mTemperatureOffset + mTemperatureGain * rawTemperature / mTemperatureScaling;
    }

    private float convertHumidity(int rawHumidity) {
        return mHumidityGain * rawHumidity / mHumidityScaling;
    }

    private int crc8(byte[] data) {
        final int POLY = 0x31;
        final int CARRY = 0x80;

        int crc = 0xFF;
        for (int i = 0; i < data.length; ++i) {
            crc ^= (data[i] & 0xFF);
            for (int bit = 8; bit > 0; --bit) {
                if ((crc & CARRY) != 0) {
                    crc = ((crc << 1) & 0xFF) ^ POLY;
                } else {
                    crc = (crc << 1) & 0xFF;
                }
            }
        }
        return crc;
    }

    private int[] readCommand(Sht3xCommand cmd) throws IOException {
        if (BuildConfig.DEBUG && !(cmd.readLength % 3 == 0)) {
            throw new AssertionError();
        }

        int i = 0;
        int w = 0;
        int[] words = new int[cmd.readLength * 2 / 3];

        synchronized (mBuffer) {
            mDevice.write(cmd.command, cmd.command.length);
            try {
                sleep(cmd.max_duration_ms);
            } catch (InterruptedException e) {
                throw new IOException("Sleep interrupted", e);
            }
            mDevice.read(mBuffer, cmd.readLength);
            while (i < cmd.readLength) {
                int crc = crc8(new byte[]{mBuffer[i], mBuffer[i + 1]});
                if (crc != (mBuffer[i + 2] & 0xFF)) {
                    throw new IOException("Checksum mismatch");
                }
                words[w++] = ((mBuffer[i] & 0xFF) << 8) + (mBuffer[i + 1] & 0xFF);
                i += 3;
            }
        }
        return words;
    }
}