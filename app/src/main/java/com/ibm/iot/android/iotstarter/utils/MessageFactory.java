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

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Build messages to be published by the application.
 * This class is currently unused.
 */
public class MessageFactory {
    private static final String TAG = "SensorSim";
    /**
     * Construct a JSON formatted string accel event message
     * @param A Float array with accelerometer x, y, z data
     * @param G Float array with gyroscope roll, pitch data
     * @param M Float array representing magnetometer values
     * @param lon Double containing device longitude
     * @param lat Double containing device latitude
     * @param pressure Double containing barometric pressure
     * @param activity String describing the activity that is being executed
     * @return String containing JSON formatted message
     */
    public static String getAccelMessage(float A[], float G[], float M[], double lon, double lat, double pressure, String activity) {

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());

        JSONObject message = new JSONObject();
        JSONObject d = new JSONObject();

        try {
            d.put("accelerometer_x", A[0]);
            d.put("accelerometer_y", A[1]);
            d.put("accelerometer_z", A[2]);

            d.put("gyroscope_x", G[0]);
            d.put("gyroscope_y", G[1]);
            d.put("gyroscope_z", G[2]);

            d.put("lat", lat);
            d.put("lon", lon);

            d.put("magnetometer_x", M[0]);
            d.put("magnetometer_y", M[1]);
            d.put("magnetometer_z", M[2]);

            d.put("pressure", pressure);
            d.put("activity", activity);
            d.put("timestamp", nowAsISO);

            message.put("d", d);

        } catch(JSONException jsone) {
            Log.e(TAG, jsone.getMessage());
        }

        /*return "{ \"d\": {" +
                "\"acceleration_x\":" + G[0] + ", " +
                "\"acceleration_y\":" + G[1] + ", " +
                "\"acceleration_z\":" + G[2] + ", " +
                "\"roll\":" + O[2] + ", " +
                "\"pitch\":" + O[1] + ", " +
                "\"yaw\":" + yaw + ", " +
                "\"lon\":" + lon + ", " +
                "\"lat\":" + lat + ", " +
                "\"pressure\":" + pressure + " " +
                "} }";*/
        return message.toString();
    }

    /**
     * Construct a JSON formatted string text event message
     * @param text String of text message to send
     * @return String containing JSON formatted message
     */
    public static String getTextMessage(String text) {
        return "{\"d\":{" +
                "\"text\":\"" + text + "\"" +
                " } }";
    }

    /**
     * Construct a JSON formatted string touchmove event message
     * @param x Double of relative x position on screen
     * @param y Double of relative y position on screen
     * @param dX Double of relative x delta from previous position
     * @param dY Double of relative y delta from previous position
     * @param ended True if final message of the touch, false otherwise
     * @return String containing JSON formatted message
     */
    public static String getTouchMessage(double x, double y, double dX, double dY, boolean ended) {
        String endString;
        if (ended) {
            endString = ", \"ended\":1 } }";
        } else {
            endString = " } }";
        }

        return "{ \"d\": { " +
                "\"screenX\":" + x + ", " +
                "\"screenY\":" + y + ", " +
                "\"deltaX\":" + dX + ", " +
                "\"deltaY\":" + dY +
                endString;
    }

}
