package com.polygongoogle.map;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity implements
        BaseActivity.PermCallback, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private View view;
    GoogleMap mGoogleMap;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    GoogleApiClient mGoogleApiClients;
    Location mLastLocation;
    private double lati, longi;
    private float mDeclination;
    private double latis, longis;
    double tolat, tolong;
    private String orderData;
    private Bitmap bmp;
    String image;
    double userLat, UserLong;
    View fram_map;
    Boolean Is_MAP_Moveable = true; // to detect map is movable
    private boolean screenLeave = false;
    int source = 0;
    int destination = 1;
    private ArrayList<LatLng> val = new ArrayList<LatLng>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapFrag = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
        fram_map = (View) findViewById(R.id.draggable);
    }



    @Override
    public void onPause() {
        super.onPause();

        if (mGoogleApiClient != null) {
            try {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            } catch (Exception e) {

            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private Polyline polyline;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        //Initialize Google Play Services

        fram_map.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                LatLng position = mGoogleMap.getProjection().fromScreenLocation(
                        new Point((int) motionEvent.getX(), (int) motionEvent.getY()));
                Log.e("polygon", "Latlng >> " + position);
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (polyline != null) {
                        polyline.remove();
                        polyline = null;

                        val.clear();
                    }
                    val.add(position);
                    polyline = mGoogleMap.addPolyline(new PolylineOptions().addAll(val));
                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    val.add(position);
                    polyline.setPoints(val);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    // Close the polyline?
                    // Send the polyline to make a search?
                }
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
         enableLoc();

    }

    final static int REQUEST_LOCATION = 199;
    /*This method is used to enableLoc of gps for getting current location of User*/

    private void enableLoc() {

        if (mGoogleApiClients == null) {
            mGoogleApiClients = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {

                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            mGoogleApiClients.connect();
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {

                            Log.d("Location error", "Location error " + connectionResult.getErrorCode());
                        }
                    }).build();
            mGoogleApiClients.connect();

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            builder.setAlwaysShow(true);

            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClients, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);


                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SUCCESS:
                            try {
                                setLocation();


                            } catch (Exception e) {
                                // Ignore the error.
                            }
                            break;
                    }
                }
            });
        } else {
            setLocation();
        }
    }

    /* Through it we check permission for get location
     * */
    private void setLocation() {
        if (checkPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, 123, this)) {
            buildGoogleApiClient();
        } else {
            buildGoogleApiClient();
        }
    }


    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (this.checkPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, 123, this)) {
            try {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            String result = null;
            try {
                List<Address> addressList = geocoder.getFromLocation(
                        mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
                if (addressList != null && addressList.size() > 0) {
                    Address address = addressList.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        sb.append(address.getAddressLine(i)).append("\n");
                    }
                    sb.append(address.getLocality()).append("\n");
                    sb.append(address.getPostalCode()).append("\n");
                    sb.append(address.getCountryName());
                    result = sb.toString();
                    lati = address.getLatitude();
                    longi = address.getLongitude();
                    setCurretLocation(lati, longi);
                    setRestaurantLocation();
                    setCustomerLocation();
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(new LatLng(address.getLatitude(), address.getLongitude()))      // Sets the center of the map to location user
                            .zoom(17)                   // Sets the zoo
                            .build();                   // Creates a CameraPosition from the builder
                    mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            buildGoogleApiClient();
        }
    }

    /* thorugh this method we get latLng of customer who placed the order
     add marker on his/her location
    * */
    private void setCustomerLocation() {
        LatLng userlatLng = new LatLng(userLat, UserLong);
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userLat, UserLong), 11));
        MarkerOptions useroptions = new MarkerOptions()
                .position(userlatLng)
                .title("Customer location");

        mGoogleMap.addMarker(useroptions);
    }

    /* thorugh this method we get latLng of Restaurant where order is placed
         add marker on  location
        * */
    private void setRestaurantLocation() {
        LatLng latLngs = new LatLng(tolat, tolong);

    }

    /* thorugh this method we get latLng of current location of Delivery agent
         add marker on his/her location
        * */
    private void setCurretLocation(double lati, double longi) {
        LatLng latLng = new LatLng(lati, longi);
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lati, longi), 13));
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("Current location");
        mGoogleMap.addMarker(options);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }


    @Override
    public void permGranted(int resultCode) {
        buildGoogleApiClient();
    }

    @Override
    public void permDenied(int resultCode) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("MAp", "OnLocationChange " + location.getLongitude());
        //   setCurretLocation(location.getLatitude(), location.getLongitude());
    }
}
