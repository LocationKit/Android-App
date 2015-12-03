/*
 * Copyright (c) 2015. SocialRadar. All rights reserved.  Code provided subject to the terms of the SocialRadar LocationKit SDK License.  See https://locationkit.io/ for more details.
 */

package locationkitapp.locationkit.locationkitapp;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.List;

import locationkitapp.locationkit.locationkitapp.sugar.Visit;
import socialradar.locationkit.LocationKitResult;
import socialradar.locationkit.internal.util.LKPlaceUtils;
import socialradar.locationkit.logger.AndroidLogger;
import socialradar.locationkit.model.LKPlace;
import socialradar.locationkit.model.LKVisit;

public class ServiceReceiver extends BroadcastReceiver {
    private boolean useNotifications = true;
    private static final String LOG_TAG = "ServiceReceiver";
    private int notifyId = 0;
    static {
        AndroidLogger.registerRemoteLogger(EventLogger.getInstance());
    }
    public ServiceReceiver() {
        Log.v(LOG_TAG, "initialize service receiver");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        useNotifications = context.getSharedPreferences(AppConstants.PREFS_FILE, Context.MODE_PRIVATE).getBoolean(AppConstants.NOTIFICATIONS, true);
        LocationKitResult result = null;
        try {
            result = LocationKitResult.extractResult(intent);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "invalid argument for intent", e);
            return;
        }
        Log.v(LOG_TAG, String.format("Got result type %s", result.getResultType()));
        switch(result.getResultType()) {

            case LOCATION:
                saveLocation(context, result.getLocation());
                break;
            case START_VISIT:
                startVisit(context, result.getVisit());
                break;
            case END_VISIT:
                endVisit(context, result.getVisit());
                break;
            case ERROR:
                Log.e(LOG_TAG, result.getErrorMessage());
                break;
            case LOCATION_DISABLED:
                break;
            case LOCATION_ENABLED:
                break;
            case NETWORK_DISABLED:
                break;
            case NETWORK_ENABLED:
                break;
            case ACTIVITY_CHANGED:
                break;
            case PERMISSSION_DENIED:
                Log.e(LOG_TAG, "Permission Denied");
                break;
        }

    }

    private void endVisit(Context context, LKVisit visit) {
        List<Visit> visits = Visit.find(Visit.class, "visit_id = ?", LKPlaceUtils.generateVisitId(visit));
        Visit update = null;

        if (visits.isEmpty()) {
            Log.v(LOG_TAG, "creating new visit");
            update = new Visit(visit);
            update.save();
        } else {
            update = visits.get(0);
            update.departureDate = visit.getDepartureDate();
            update.save();
        }
        sendNotification(context, "Visit Ended", update.venueName);
        notifyVisitUpdated(context);
    }

    private void startVisit(Context context, LKVisit visit) {
        List<Visit> visits = Visit.find(Visit.class, "visit_id = ?", LKPlaceUtils.generateVisitId(visit));
        if (visits.isEmpty()) {
            Log.v(LOG_TAG, "creating new visit");
            Visit update = new Visit(visit);
            update.save();
            sendNotification(context, "Visit Started", update.venueName);
        } else {
            Log.v(LOG_TAG, "Visit already exists");
        }

        notifyVisitUpdated(context);

    }
    private void notifyVisitUpdated(Context context) {
        Intent i = new Intent(AppConstants.VISIT_UPDATED);
        context.sendBroadcast(i);
    }

    private void saveLocation(Context context, Location location) {
        SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putFloat(AppConstants.LATITUDE, (float)location.getLatitude());
        edit.putFloat(AppConstants.LONGITUDE, (float)location.getLongitude());
        edit.apply();
    }

    private void sendNotification(Context context, String title, String text) {
        if (!useNotifications) { return; }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentText(text);
        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notifyId == Integer.MAX_VALUE) {
            notifyId = 1;
        } else {
            notifyId++;
        }
        manager.notify(notifyId, builder.build());
    }


}
