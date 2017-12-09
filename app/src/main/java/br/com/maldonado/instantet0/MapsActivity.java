package br.com.maldonado.instantet0;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;

//    private LocationManager locationManager;
    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;

    private FusedLocationProviderClient mFusedLocationClient;

    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
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

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }



        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                Log.d("DEBUG","Map clicked [" + latLng.latitude + " / " + latLng.longitude + "]");

                // Instantiate the RequestQueue.
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                final String latitude = String.valueOf(latLng.latitude);
                final String longitude = String.valueOf(latLng.longitude);

                Calendar c = Calendar.getInstance();
                c.setTime(new Date());
                c.add(Calendar.DATE, -20);
                Date date = new Date();
                date.setTime(c.getTime().getTime());
                Date cDate = new Date();

                String sDay = (String) DateFormat.format("dd",   date);
                String sMonth = (String) DateFormat.format("MM",   date);
                String sYear = (String) DateFormat.format("yyyy", date);

                String eDay = (String) DateFormat.format("dd",   cDate);
                String eMonth = (String) DateFormat.format("MM",   cDate);
                String eYear = (String) DateFormat.format("yyyy", cDate);

                String url ="https://power.larc.nasa.gov/cgi-bin/agro.cgi?email=&step=1&lat=" + latitude +
                        "&lon=" + longitude +
                        "&ms=" + sMonth +
                        "&ds=" + sDay +
                        "&ys=" + sYear +
                        "&me=" + eMonth +
                        "&de=" + eDay +
                        "&ye=" + eYear +
                        "&submit=Yes";

                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {

                                String lines[] = response.split("\\r?\\n");
                                int sLength = lines.length;

                                ArrayList<Integer> weday = new ArrayList<Integer>();
                                ArrayList<Double> srad = new ArrayList<Double>();
                                ArrayList<Double> tmax = new ArrayList<Double>();
                                ArrayList<Double> tmin = new ArrayList<Double>();
                                ArrayList<Double> wind = new ArrayList<Double>();
                                ArrayList<Double> t2m = new ArrayList<Double>();
                                ArrayList<Double> rh2m = new ArrayList<Double>();

                                for (int i = sLength - 20; i < sLength; i++) {
                                    String fields[] = lines[i].split(" +");
                                    weday.add(Integer.valueOf(fields[2]));
                                    srad.add(Double.valueOf(fields[3]));
                                    tmax.add(Double.valueOf(fields[4]));
                                    tmin.add(Double.valueOf(fields[5]));
                                    wind.add(Double.valueOf(fields[7]));
                                    t2m.add(Double.valueOf(fields[9]));
                                    rh2m.add(Double.valueOf(fields[10]));
                                }

                                for (int i = weday.size() - 1; i >= 0; i--) {
                                    if (weday.get(i) != -99 &&
                                            srad.get(i) != -99 &&
                                            tmax.get(i) != -99 &&
                                            tmin.get(i) != -99 &&
                                            wind.get(i) != -99 &&
                                            t2m.get(i) != -99 &&
                                            rh2m.get(i) != -99) {

                                        CoreEstimation ce = new CoreEstimation();
                                        Double etp = ce.PenmannMonteith(srad.get(i),
                                                tmin.get(i),
                                                tmax.get(i),
                                                wind.get(i),
                                                t2m.get(i),
                                                rh2m.get(i));

                                        mMap.clear();
                                        final LatLng pos = new LatLng(Double.valueOf(latitude), Double.valueOf(longitude));
                                        Marker melbourne = mMap.addMarker(new MarkerOptions()
                                                .position(pos)
                                                .title("ETP: " + String.format("%.02f", etp) + " mm/d"));
                                        melbourne.showInfoWindow();

                                        Log.d("DEBUG", response);


                                        break;
                                    }

                                }

                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("DEBUG","Volley error!!");
                    }
                });
                // Add the request to the RequestQueue.
                queue.add(stringRequest);

                Log.d("DEBUG",url);

            }
        });

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //move map camera
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,11));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }
}
