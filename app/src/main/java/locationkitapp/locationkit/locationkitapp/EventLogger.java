/*
 * Copyright (c) 2015. SocialRadar. All rights reserved.  Code provided subject to the terms of the SocialRadar LocationKit SDK License.  See https://locationkit.io/ for more details.
 */

package locationkitapp.locationkit.locationkitapp;

import android.util.Log;

import java.util.Map;

import socialradar.locationkit.util.LKDiagnostics;
import socialradar.locationkit.util.LKRemoteLogger;

/**
 * LKRemoteLogger implementation to allow for event capture when trying to diagnose specific setup or runtime issues.
 * Created by johnfontaine on 12/1/15.
 */
public class EventLogger implements LKRemoteLogger {
    private static final String LOG_TAG = "LocationKitEvents";
    private static EventLogger instance;
    public static EventLogger getInstance() {
        if (instance == null) {
            Log.v(LOG_TAG, "starting event logger");
            instance = new EventLogger();
        }
        return instance;
    }
    private EventLogger() {
    }

    @Override
    public void logException(Throwable throwable) {
        Log.e(LOG_TAG, throwable.getMessage(), throwable);
    }

    @Override
    public void logRemoteMessage(String s) {
//        Log.v(LOG_TAG, s);
    }

    @Override
    public void performDiagnosticsSnapshot() {
//        Log.v(LOG_TAG, "requested diagnostic data");
//        Log.v(LOG_TAG, mapToString(LKDiagnostics.getDiagnosticAttributes()));
    }

    @Override
    public void logRemoteEvent(String s, Map<String, Object> map) {
//        Log.v(LOG_TAG, s);
//        if (map != null) {
//            Log.v(LOG_TAG, mapToString(map));
//        }
    }
    private String mapToString(Map<String, Object> map) {
        if (map == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String s : map.keySet()) {

            builder.append(String.format("\t{%s : %s }\n", s, map.get(s)));
        }
        builder.append("\n");
        return builder.toString();
    }
}
