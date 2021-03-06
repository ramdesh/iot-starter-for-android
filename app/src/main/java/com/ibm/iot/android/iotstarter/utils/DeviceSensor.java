/*******************************************************************************
 * Copyright (c) 2014-2015 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Mike Robertson - initial contribution
 *******************************************************************************/
package com.ibm.iot.android.iotstarter.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

import com.ibm.iot.android.iotstarter.IoTStarterApplication;
import com.ibm.iot.android.iotstarter.iot.IoTClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class implements the SensorEventListener interface. When the application creates the MQTT
 * connection, it registers listeners for the accelerometer and magnetometer sensors.
 * Output from these sensors is used to publish accel event messages.
 */
public class DeviceSensor implements SensorEventListener {
    private final String TAG = DeviceSensor.class.getName();
    private static DeviceSensor instance;
    private final IoTStarterApplication app;
    private final SensorManager sensorManager;
    private final Sensor linearAcceleration;
    private final Sensor accelerometer;
    private final Sensor magnetometer;
    private final Sensor gyroscope;
    private final Sensor pressureSensor;
    private final Sensor temperatureSensor;
    private final Sensor humiditySensor;
    private final Context context;
    private Timer timer;
    private SharedPreferences settings;
    private boolean isEnabled = false;


    public static ArrayList<String> sensorValues;

    private DeviceSensor(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        sensorValues = new ArrayList<>();
        app = (IoTStarterApplication) context.getApplicationContext();
        settings = app.getSharedPreferences(Constants.SETTINGS, 0);
    }

    /**
     * @param context The application context for the object.
     * @return The MqttHandler object for the application.
     */
    public static DeviceSensor getInstance(Context context) {
        if (instance == null) {
            Log.i(DeviceSensor.class.getName(), "Creating new DeviceSensor");
            instance = new DeviceSensor(context);
        }
        return instance;
    }

    /**
     * Register the listeners for the sensors the application is interested in.
     */
    public void enableSensor() {
        Log.i(TAG, ".enableSensor() entered");
        if (!isEnabled) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, linearAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, humiditySensor, SensorManager.SENSOR_DELAY_NORMAL);
            timer = new Timer();
            timer.scheduleAtFixedRate(new SendTimerTask(), app.getTimeInterval(), app.getTimeInterval());
            isEnabled = true;
        }
    }

    /**
     * Disable the listeners.
     */
    public void disableSensor() {
        Log.d(TAG, ".disableSensor() entered");
        if (timer != null && isEnabled) {
            timer.cancel();
            sensorManager.unregisterListener(this);
            isEnabled = false;
        }
    }

    // Values used for accelerometer, magnetometer, orientation sensor data
    private float[] A = new float[3]; // gravity x,y,z
    private float[] M = new float[3]; // geomagnetic field x,y,z
    private float[] G = new float[3]; // gyroscope values x,y,z
    private float[] accel = new float[3];
    private float pressure; // Barometric Pressure
    private float temperature; //Ambient Temperature
    private float humidity; // Relative humidity
    private final float[] ROT = new float[9]; // rotation matrix
    private final float[] I = new float[9]; // inclination matrix
    private float[] O = new float[3]; // orientation azimuth, pitch, roll
    private float yaw;

    /**
     * Callback for processing data from the registered sensors. Accelerometer and magnetometer
     * data are used together to get orientation data.
     *
     * @param sensorEvent The event containing the sensor data values.
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.v(TAG, "onSensorChanged() entered");



        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //Log.v(TAG, "Accelerometer -- x: " + sensorEvent.values[0] + " y: "
            //        + sensorEvent.values[1] + " z: " + sensorEvent.values[2]);
            A = sensorEvent.values;

        } else if(sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            //Log.v(TAG, "Linear Acceleration -- x: " + sensorEvent.values[0] + " y: "
            //        + sensorEvent.values[1] + " z: " + sensorEvent.values[2]);
            accel = sensorEvent.values;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            //Log.v(TAG, "Magnetometer -- x: " + sensorEvent.values[0] + " y: "
            //        + sensorEvent.values[1] + " z: " + sensorEvent.values[2]);
            M = sensorEvent.values;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE) {
            //Log.v(TAG, "Barometer -- : " + sensorEvent.values[0]);
            pressure = sensorEvent.values[0];
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            //Log.v(TAG, "Gyroscope -- x: " + sensorEvent.values[0] + " y: "
                    //+ sensorEvent.values[1] + " z: " + sensorEvent.values[2]);
            G = sensorEvent.values;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            //Log.v(TAG, "Temperature " + sensorEvent.values[0]);
            temperature = sensorEvent.values[0];
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
            //Log.v(TAG, "Humidity " + sensorEvent.values[0]);
            humidity = sensorEvent.values[0];
        }
        if (A != null && M != null) {
            if (SensorManager.getRotationMatrix(ROT, I, A, M)) {
                float[] previousO = O.clone();
                O = SensorManager.getOrientation(ROT, O);
                yaw = O[0] - previousO[0];
                //Log.v(TAG, "Orientation: azimuth: " + O[0] + " pitch: " + O[1] + " roll: " + O[2] + " yaw: " + yaw);
            }
        }
    }

    public void sendData() {
        app.setWriteState(true);
        ArrayList<String> dataToSend = (ArrayList<String>)DeviceSensor.sensorValues.clone();
        DeviceSensor.sensorValues.removeAll(dataToSend);
        app.setWriteState(false);
        Iterator<String> dataIterator = dataToSend.iterator();
        //}
        while(dataIterator.hasNext()) {
            String messageData = dataIterator.next();
            try {
                // create ActionListener to handle message published results
                MyIoTActionListener listener = new MyIoTActionListener(context, Constants.ActionStateStatus.PUBLISH);
                IoTClient iotClient = IoTClient.getInstance(context);
                if (app.getConnectionType() == Constants.ConnectionType.QUICKSTART) {
                    iotClient.publishEvent(Constants.STATUS_EVENT, "json", messageData, 0, false, listener);
                } else {
                    iotClient.publishEvent(Constants.ACCEL_EVENT, "json", messageData, 0, false, listener);
                }
                //String runningActivity = app.getCurrentRunningActivity();
                //if (runningActivity != null && runningActivity.equals(IoTPagerFragment.class.getName())) {

                //}
            } catch (MqttException e) {
                Toast.makeText(context, "Encountered an issue when publishing message " + messageData, Toast.LENGTH_SHORT );
                Log.d(TAG, ".sendData() received exception on publishEvent()");
            }
        }
        Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
        actionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_DATA_PUBLISHED);
        context.sendBroadcast(actionIntent);
        app.setPublishCount(0);
        Toast.makeText(context, "Published data to IoTF", Toast.LENGTH_SHORT).show();
    }

    /**
     * Callback for the SensorEventListener interface. Unused.
     *
     * @param sensor The sensor that changed.
     * @param i The change in accuracy for the sensor.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(TAG, "onAccuracyChanged() entered");
    }

    /**
     * Timer task for sending accel data on 1000ms intervals
     */
    private class SendTimerTask extends TimerTask {

        /**
         * Publish an accel event message.
         */
        @Override
        public void run() {
            Log.v(TAG, "SendTimerTask.run() entered");
            double lon = 0.0;
            double lat = 0.0;
            if (app.getCurrentLocation() != null) {
                lon = app.getCurrentLocation().getLongitude();
                lat = app.getCurrentLocation().getLatitude();
            }
            String deviceId = settings.getString(Constants.DEVICE_ID, null);
            String messageData = MessageFactory.getAccelMessage(A, accel, G, M, lon, lat, pressure, yaw, temperature, humidity,
                    IoTStarterApplication.getCurrentActivityType());
            try {
                // create ActionListener to handle message published results
                MyIoTActionListener listener = new MyIoTActionListener(context, Constants.ActionStateStatus.PUBLISH);
                IoTClient iotClient = IoTClient.getInstance(context);
                if (app.getConnectionType() == Constants.ConnectionType.QUICKSTART) {
                    iotClient.publishEvent(Constants.STATUS_EVENT, "json", messageData, 0, false, listener);
                } else {
                    iotClient.publishEvent(Constants.ACCEL_EVENT, "json", messageData, 0, false, listener);
                }
                //String runningActivity = app.getCurrentRunningActivity();
                //if (runningActivity != null && runningActivity.equals(IoTPagerFragment.class.getName())) {

                //}
                Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
                actionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_DATA_PUBLISHED);
                context.sendBroadcast(actionIntent);
            } catch (MqttException e) {
                Toast.makeText(context, "Encountered an issue when publishing message " + messageData, Toast.LENGTH_SHORT ).show();
                Log.d(TAG, ".sendData() received exception on publishEvent()");
            }
            //sensorValues.add(messageData);

            int count = app.getPublishCount();
            app.setPublishCount(++count);
            app.setAccelData(A);

            Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
            actionIntent.putExtra(Constants.INTENT_DATA, Constants.ACCEL_EVENT);
            context.sendBroadcast(actionIntent);

        }
    }
}
