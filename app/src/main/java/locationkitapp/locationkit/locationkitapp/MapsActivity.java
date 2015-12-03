/*
 * Copyright (c) 2015. SocialRadar. All rights reserved.  Code provided subject to the terms of the SocialRadar LocationKit SDK License.  See https://locationkit.io/ for more details.
 */

package locationkitapp.locationkit.locationkitapp;


import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.support.design.widget.Snackbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import locationkitapp.locationkit.locationkitapp.sugar.Visit;
import locationkitapp.locationkit.locationkitapp.util.CategoryUtil;
import socialradar.locationkit.ILocationKitBinder;
import socialradar.locationkit.ILocationKitCallback;
import socialradar.locationkit.LocationKitService;
import socialradar.locationkit.LocationKitServiceOptions;
import socialradar.locationkit.internal.manager.LKDataManager;
import socialradar.locationkit.logger.AndroidLogger;
import socialradar.locationkit.model.LKPlace;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String BUNDLE_KEY_LOCATION = "currentLocation";

    private static final HandlerThread THREAD;
    private static final Handler BG_HANDLER;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static DateFormat AMPM_FORMAT = new SimpleDateFormat("h:mm a");
    private static DateFormat DAY_MON_FORMAT = new SimpleDateFormat("MMM d, yyyy");
    static {
        THREAD = new HandlerThread("bg-thread");
        THREAD.start();
        BG_HANDLER = new Handler(THREAD.getLooper());

            AndroidLogger.registerRemoteLogger(EventLogger.getInstance());
    }
    private static final String LOG_TAG = "MapsActivity";
    private static final String API_TOKEN = "f0838784beb72a13";
    private final String ACTION = "locationkitapp.locationkit.update";
    private GoogleMap mMap;
    private ListView mListView;
    private LatLng mCurrentLocation;
    private ILocationKitBinder mLocationKit;
    private LocationKitServiceOptions options;
    private AtomicBoolean mBound = new AtomicBoolean(false);
    private ArrayAdapter<Visit> visitArrayAdapter;
    private List<Visit> mVisits = new ArrayList<>();
    private Marker mCurrentVisitMarker;
    private List<Marker> mPreviousVisitMarkers = new ArrayList<>();
    private boolean stopShowingLocationUpdates = false;
    private boolean trackingEnabled = true;
    private boolean notificationEnabled = true;
    private View mLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mLayout = findViewById(R.id.main_layout);
        loadPrefs();
        updateLocation();
        mListView = (ListView)findViewById(R.id.listView);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setSubtitle(String.format(Locale.ENGLISH, "Version %s", BuildConfig.VERSION_NAME));
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        reloadVisits();
        visitArrayAdapter = new VisitAdapter(this, mVisits);
        mListView.setAdapter(visitArrayAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.v(LOG_TAG, String.format("onItemClicked %d %d", position, id));
                Visit v = mVisits.get((int)id);
                hideInfoWindows();
                if (id == 0) {
                    if (mMap != null) {
                        if (mCurrentVisitMarker == null) {
                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.title(TextUtils.isEmpty(v.venueName) ? formatAddress(v) : v.venueName);
                            markerOptions.position(new LatLng(v.latitude, v.longitude));
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_pin));
                            mCurrentVisitMarker = mMap.addMarker(markerOptions);
                            mCurrentVisitMarker.showInfoWindow();

                        } else {
                            mCurrentVisitMarker.setPosition(new LatLng(v.latitude, v.longitude));
                            mCurrentVisitMarker.setTitle(TextUtils.isEmpty(v.venueName) ? formatAddress(v) : v.venueName);
//                            mCurrentVisitMarker.setVisible(true);
                            mCurrentVisitMarker.showInfoWindow();
                        }

                    }
                } else {

                    for (Marker marker : mPreviousVisitMarkers) {
                        if (marker.getPosition().latitude == v.latitude && marker.getPosition().longitude == v.longitude) {
                            marker.showInfoWindow();
                            break;
                        }
                    }
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(v.latitude, v.longitude), AppConstants.MAP_ZOOM));
//                if (id < mVisits.size()) {
//                    Visit v = mVisits.get((int) id);
//                //    Log.v(LOG_TAG, "items is ".concat(v.venueName));
//
//                    stopShowingLocationUpdates = true;
//                } else {
//                    stopShowingLocationUpdates = false;
//                }
            }
        });

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
  //              Log.v(LOG_TAG, "scroll state changed");
               hideInfoWindows();
                stopShowingLocationUpdates = false;
                updateLocation();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//                Log.v(LOG_TAG, "scrolling");

            }
        });
    }
    private void hideInfoWindows() {
        if (mCurrentVisitMarker != null) {
            mCurrentVisitMarker.hideInfoWindow();
        }
        for (Marker marker : mPreviousVisitMarkers) {
            marker.hideInfoWindow();
        }

    }
    private void loadPrefs() {
        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_FILE, Context.MODE_PRIVATE);
        trackingEnabled = prefs.getBoolean(AppConstants.TRACKING, true);
        notificationEnabled = prefs.getBoolean(AppConstants.NOTIFICATIONS, true);
    }


    private void updateLocation() {
        if (!stopShowingLocationUpdates) {
            SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_FILE, Context.MODE_PRIVATE);
            if (prefs.contains(AppConstants.LATITUDE)) {
                mCurrentLocation = new LatLng(prefs.getFloat(AppConstants.LATITUDE, 0f), prefs.getFloat(AppConstants.LONGITUDE, 0f));
                if (mMap != null) {
//                    mLocationMarker.setPosition(mCurrentLocation);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLocation, AppConstants.MAP_ZOOM));
                }
            }
        }
    }
    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter(AppConstants.VISIT_UPDATED);
        registerReceiver(visitUpdateReceiver, filter);
        if (!mBound.get()) {
            options =  new LocationKitServiceOptions.Builder().withInterval(null).build();
            Intent i = new Intent(this, LocationKitService.class);
            bindService(i, connection, Service.BIND_AUTO_CREATE);
        }
        getSharedPreferences(AppConstants.PREFS_FILE, Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(changedListener);
        super.onResume();

    }

    @Override
    protected void onPause() {
        unregisterReceiver(visitUpdateReceiver);
        getSharedPreferences(AppConstants.PREFS_FILE, Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(changedListener);
        if (mBound.get() && mLocationKit != null) {
            try {
                unbindService(connection);
            } catch (IllegalArgumentException e) {
                mBound.set(false);
                mLocationKit = null;
            }
        }
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem notification = menu.findItem(R.id.action_enable_disable_notifications);
        MenuItem tracking = menu.findItem(R.id.action_enable_disable_tracking);
        if (notificationEnabled) {
            notification.setTitle(getResources().getString(R.string.disable_notifications));
        } else {
            notification.setTitle(getResources().getString(R.string.enable_notifications));
        }
        if (trackingEnabled) {
            tracking.setTitle(getResources().getString(R.string.disable_tracking));
        } else {
            tracking.setTitle(getResources().getString(R.string.enable_tracking));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(LOG_TAG, String.format("selected menu item %s", item.getTitle()));
        if (item.hasSubMenu()) {
            return true;
        }

        if (item.getItemId() == R.id.action_getplace) {
            getPlace();
            return true;
        }
        if (item.getItemId() == R.id.action_clear) {
            clearVisits();
            return true;
        }
        if (item.getItemId() == R.id.action_email) {
            emailVisitHistory();
            return true;
        }
        if (item.getItemId() == R.id.action_enable_disable_notifications) {
            flipNotifications(item);
            return true;
        }
        if (item.getItemId() == R.id.action_enable_disable_tracking) {
            flipTracking(item);
            return true;
        }
        if (item.getItemId() == R.id.action_show_info) {
            String url = "https://locationkit.io/";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void flipTracking(MenuItem item) {
        SharedPreferences.Editor editor = getSharedPreferences(AppConstants.PREFS_FILE, Context.MODE_PRIVATE).edit();
        trackingEnabled = !trackingEnabled;
        editor.putBoolean(AppConstants.TRACKING, trackingEnabled);
        editor.apply();
        if (trackingEnabled) {
            item.setTitle(getResources().getString(R.string.disable_tracking));
        } else {
            item.setTitle(getResources().getString(R.string.enable_tracking));
        }
    }

    private void flipNotifications(MenuItem item) {
        SharedPreferences.Editor editor = getSharedPreferences(AppConstants.PREFS_FILE, Context.MODE_PRIVATE).edit();
        notificationEnabled = !notificationEnabled;
        editor.putBoolean(AppConstants.NOTIFICATIONS, notificationEnabled);
        editor.apply();
        if (trackingEnabled) {
            item.setTitle(getResources().getString(R.string.disable_notifications));
        } else {
            item.setTitle(getResources().getString(R.string.enable_notifications));
        }
    }

    private void clearVisits() {
        Visit.deleteAll(Visit.class);
        mVisits.clear();
        for (Marker m : mPreviousVisitMarkers) {
            m.remove();
        }
        mPreviousVisitMarkers.clear();
        if (mCurrentVisitMarker != null) {
            mCurrentVisitMarker.remove();
        }
        mCurrentVisitMarker = null;
        if (visitArrayAdapter != null) {
            visitArrayAdapter.notifyDataSetChanged();
        }

    }
    private String visitRow(Visit visit) {
        return "\"" + visit.arrivalDate +
                "\",\"" + visit.detectedTime +
                "\",\"" + visit.visitId +
                "\",\"" + visit.departureDate +
                "\",\"" + visit.category + '\'' +
                "\",\"" + visit.subcategory + '\'' +
                "\",\"" + visit.venueName + '\'' +
                "\",\"" + visit.street + '\'' +
                "\",\"" + visit.city + '\'' +
                "\",\"" + visit.state + '\'' +
                "\",\"" + visit.zip + '\'' +
                "\",\"" + visit.detectionMethod + '\'' +
                "\",\"" + visit.latitude +
                "\",\""+ visit.longitude +
                "\",\"" + visit.fromPlace +
                "\"\n";
    }
    private void emailVisitHistory() {
        ArrayList<Uri> uris = new ArrayList<Uri>();
        String filename = String.format(Locale.ENGLISH, "data_%d.csv", System.currentTimeMillis());
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(root, filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.e(LOG_TAG, "file failed", e);
                return;
            }
        }
        try {
            FileOutputStream os = new FileOutputStream(file);
            os.write("arrival_date,detected_time,visit_id,departure_date,category,subcategory,venue_name,street,city,state,zip,detection_method,latitude,longitude,from_place\n".getBytes());
            for (Visit v: mVisits) {
                os.write(visitRow(v).getBytes());
            }
            os.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "ioexception on file", e);
            return;
        }
        uris.add(LKDataManager.EXPORT_DB(this));
        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "LocationKitApp ");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Visits Recorded by LocationKitApp");
        uris.add(Uri.fromFile(file));
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        startActivity(Intent.createChooser(emailIntent, "Pick an Email provider"));
    }
    private void getPlace() {

        if (mBound.get() && mLocationKit != null) {
            mLocationKit.getCurrentPlace(new ILocationKitCallback<LKPlace>() {
                @Override
                public void onError(Exception e, String s) {
                    Log.e(LOG_TAG, s, e);
                }

                @Override
                public void onReceivedData(LKPlace lkPlace) {
                    if (lkPlace != null) {
                        Log.v(LOG_TAG, "got current place");
                        Visit visit = new Visit(lkPlace);
                        visit.save();
                        reloadVisits();
                        if (visitArrayAdapter != null) {
                            visitArrayAdapter.notifyDataSetChanged();
                        }
                        saveLocation(lkPlace.getLocation().getLatitude(), lkPlace.getLocation().getLongitude());
                    }
                }
            });
        } else {
            Log.v(LOG_TAG, "service is not bound");
        }
    }
    private void getCurrentLocation() {
        if (mBound.get() && mLocationKit != null) {
            Log.v(LOG_TAG, "requesting current location");
            mLocationKit.getCurrentLocation(new ILocationKitCallback<Location>() {
                @Override
                public void onError(Exception e, String s) {
                    Log.e(LOG_TAG, s, e);
                }

                @Override
                public void onReceivedData(Location location) {
                    Log.v(LOG_TAG, "got current location");
                    if (location != null) {
                        saveLocation(location.getLatitude(), location.getLongitude());
                    }
                }
            });
        } else {
            Log.e(LOG_TAG, "service is not yet bound");
        }
    }

    private void saveLocation(double latitude, double longitude) {
        SharedPreferences.Editor editor = getSharedPreferences(AppConstants.PREFS_FILE, Context.MODE_PRIVATE).edit();
        editor.putFloat(AppConstants.LATITUDE, (float)latitude);
        editor.putFloat(AppConstants.LONGITUDE,(float)longitude);
        editor.apply();
    }
    private void reloadVisits() {
        mVisits.clear();
        mVisits.addAll(Visit.findWithQuery(Visit.class, "select * from Visit order by arrival_date desc limit 100"));
        for (Marker marker : mPreviousVisitMarkers) {
            marker.remove();
        }
        if (mVisits.isEmpty()) {
            findViewById(R.id.no_data).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.no_data).setVisibility(View.GONE);
        }
        int i = 0;
        if (mMap != null) {
            for (Visit visit : mVisits) {
                i++;
                if (i == 1) {
                    continue;
                }
                MarkerOptions options = new MarkerOptions()
                                            .position(new LatLng(visit.latitude, visit.longitude))
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_green))
                                            .title(TextUtils.isEmpty(visit.venueName) ? formatAddress(visit) : visit.venueName);
                mPreviousVisitMarkers.add(mMap.addMarker(options));
            }
            if (!mVisits.isEmpty()) {
                Visit visit = mVisits.get(0);
                if (mCurrentVisitMarker != null) {
                    mCurrentVisitMarker.setPosition(new LatLng(visit.latitude, visit.longitude));
                    mCurrentVisitMarker.setTitle(TextUtils.isEmpty(visit.venueName) ? formatAddress(visit) : visit.venueName);
                } else {
                    mCurrentVisitMarker = mMap.addMarker(new MarkerOptions().title(visit.venueName).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_orange_nav_pin)).position(new LatLng(visit.latitude, visit.longitude)));
                }
            }
        }
    }

    private String formatAddress(Visit visit) {
        return visit.street;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.v(LOG_TAG, "map is ready");
        boolean hasLocationPermissions = PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (hasLocationPermissions) {
            Log.v(LOG_TAG,"has location permissions");
            mMap.setMyLocationEnabled(true);
        } else {
            Log.v(LOG_TAG, "permission denied");
               Snackbar.make(mLayout, "We need access to your location to function.",
                        Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Request the permission
                        ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);;
                    }
                }).show();
        }
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                hideInfoWindows();
                updateLocation();
                stopShowingLocationUpdates = false;
            }
        });
        // Add a marker in Sydney and move the camera

        if (mCurrentLocation == null) {
            LatLng latLng = new LatLng(38.9047, -77.0164);
//            mLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_current_location)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f));
        } else {
//            mLocationMarker = mMap.addMarker(new MarkerOptions().position(mCurrentLocation).title("").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_current_location)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLocation, AppConstants.MAP_ZOOM));
        }

    }

    protected ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(LOG_TAG, "Bound to service");
            MapsActivity.this.mBound.set(true);
            mBound.set(true);
            mLocationKit = (ILocationKitBinder) service;
            try {
                if (options != null) {
                    mLocationKit.startWithApiToken(API_TOKEN, options, ACTION);
                    Log.v(LOG_TAG, String.format("starting with options api token = %s action %s", API_TOKEN, ACTION));
                } else {
                    Log.v(LOG_TAG, String.format("starting without options api token = %s action %s", API_TOKEN, ACTION));
                    mLocationKit.startWithApiToken(API_TOKEN, ACTION);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Service connection failed", e);
            }
            if (mCurrentLocation == null) {
                getCurrentLocation();
            }
            reloadVisits();
            if (visitArrayAdapter != null) {
                visitArrayAdapter.notifyDataSetChanged();
            }
            Log.v(LOG_TAG, "service is connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(LOG_TAG, "service disconnected");
            mLocationKit = null;
            mBound.set(false);
            //MainActivity.this.mBound = false;
        }
    };

    private class VisitAdapter extends ArrayAdapter<Visit> {
        public VisitAdapter(Context context, List<Visit> objects) {
            super(context, R.layout.visit_layout, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Visit visit = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.visit_layout, parent, false);
            }

            if (position == 0) {
                convertView.findViewById(R.id.date_sub_header).setVisibility(View.VISIBLE);
                updateTextView(convertView, R.id.date_sub_header, DAY_MON_FORMAT.format(new Date(visit.arrivalDate)));
            } else {
                String lastArrivalDate =DAY_MON_FORMAT.format(new Date(mVisits.get(position-1).arrivalDate));
                String currentArrivalDate =  DAY_MON_FORMAT.format(new Date(visit.arrivalDate));
                if (lastArrivalDate.equals(currentArrivalDate)) {
                    convertView.findViewById(R.id.date_sub_header).setVisibility(View.GONE);
                } else {
                    convertView.findViewById(R.id.date_sub_header).setVisibility(View.VISIBLE);
                    updateTextView(convertView, R.id.date_sub_header, currentArrivalDate);
                }
            }
            updateTextView(convertView, R.id.visit_venue_name, visit.venueName);
            if (visit.subcategory != null && !TextUtils.isEmpty(visit.subcategory)) {
                updateTextView(convertView, R.id.visit_category_subcategory, String.format("Categories: %s - %s", visit.category, visit.subcategory));
            } else {
                updateTextView(convertView, R.id.visit_category_subcategory, String.format("Categories: %s", visit.category));
            }
            if (visit.fromPlace) {
                convertView.findViewById(R.id.info_block_from_place).setVisibility(View.VISIBLE);
                convertView.findViewById(R.id.info_block_from_visit).setVisibility(View.INVISIBLE);
                setTime(convertView, R.id.place_time, visit.arrivalDate);
            } else {
                convertView.findViewById(R.id.info_block_from_place).setVisibility(View.INVISIBLE);
                convertView.findViewById(R.id.info_block_from_visit).setVisibility(View.VISIBLE);
                setTime(convertView, R.id.visit_start_time, visit.arrivalDate);
                setTime(convertView, R.id.visit_end_time, visit.departureDate);
            }

            updateTextView(convertView, R.id.visit_street, visit.street);
            updateTextView(convertView, R.id.visit_city_state_zip, String.format("%s %s,  %s", visit.city, visit.state, visit.zip));
            updateTextView(convertView, R.id.visit_detection_method, String.format("Detection Method: %s", visit.detectionMethod));
            ImageView r = (ImageView)convertView.findViewById(R.id.visit_icon);
            r.setImageBitmap(BitmapFactory.decodeResource(getResources(), CategoryUtil.getIconForCategorySubcategory(visit.category, visit.subcategory)));
            return convertView;
        }
        private void updateTextView(View convertView, int id, String text) {
            ((TextView)convertView.findViewById(id)).setText(text);
        }
        private void setTime(View convertView, int id, Long time) {
            if (time != null && time > 1l) {
                convertView.findViewById(id).setVisibility(View.VISIBLE);
                updateTextView(convertView, id,AMPM_FORMAT.format(new Date(time)) );
            } else {
                convertView.findViewById(id).setVisibility(View.INVISIBLE);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 100) {
            boolean granted = true;
            for (int i :grantResults) {
                if (i == PackageManager.PERMISSION_DENIED) {
                    granted = false;
                }
            }
            if (!granted) {
                Snackbar.make(mLayout, "Location permission request was denied.",
                        Snackbar.LENGTH_SHORT)
                        .show();
                Log.e(LOG_TAG, "Permission was denied");
            } else {
                mMap.setMyLocationEnabled(true);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private BroadcastReceiver visitUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadVisits();
            visitArrayAdapter.notifyDataSetChanged();
        }

    };
    private SharedPreferences.OnSharedPreferenceChangeListener changedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(AppConstants.LATITUDE)) {
                updateLocation();
            }
        }
    };
}
