package br.com.maldonado.instantet0;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdate;
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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;

    private LocationManager locationManager;
    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION  }, 10 );

        } else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
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

// Request a string response from the provided URL.
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

        //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-21.24, -48.29);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        mMap.animateCamera(cameraUpdate);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
