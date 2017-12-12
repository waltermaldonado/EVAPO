package br.com.maldonado.instantet0;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private SlidingUpPanelLayout mLayout;
    private ViewPager viewPager;
    private TabLayout tabLayout;

    private int mapsHeight;

    PlaceAutocompleteFragment placeAutoComplete;

    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;

    // ETP internet request relate stuff
    private View tab1View;
    private View tab1LoadingView;
    private int mAnimationDuration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // implementation of the slidingview
        mLayout = findViewById(R.id.sliding_layout);

        // Implements the searchbar functions
        placeAutoComplete = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete);
        placeAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                Log.d("Maps", "Place selected: " + place.getName());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(),15));
            }

            @Override
            public void onError(Status status) {
                Log.d("Maps", "An error occurred: " + status);
            }
        });

        // Implements the sliding panel listener
        mLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {

                tab1View = findViewById(R.id.tab1_content);
                tab1LoadingView = findViewById(R.id.tab1_spinner);

                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    Log.d("PANEL STATE", "Collapsed");
                    mMap.setPadding(0, 0, 0, 0);
                } else if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    Log.d("PANEL STATE", "Expanded");
                    mMap.setPadding(0, 0, 0, mapsHeight/2);
                }

            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Tabs on slide panel initialization
        viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new Tab1Fragment(), "ETP");
        adapter.addFragment(new Tab2Fragment(), "HISTORY");
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
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
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        mGoogleApiClient.connect();
    }


    private void calculateETP(LatLng location) {
        mapsHeight = findViewById(R.id.map).getHeight() - 200;
        mMap.setPadding(0, 0, 0, mapsHeight/2);

        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

        if (tab1View != null) {
            tab1View.setVisibility(View.GONE);
        }
        if (tab1LoadingView != null) {
            tab1LoadingView.setVisibility(View.VISIBLE);
        }

        mMap.clear();
        final String latitude = String.valueOf(location.latitude);
        final String longitude = String.valueOf(location.longitude);
        final LatLng pos = new LatLng(Double.valueOf(latitude), Double.valueOf(longitude));
        mMap.addMarker(new MarkerOptions().position(pos));

        CameraUpdate updtLocation = CameraUpdateFactory.newLatLngZoom(pos, 11.0f);
        mMap.animateCamera(updtLocation);



        Log.d("DEBUG","Map clicked [" + location.latitude + " / " + location.longitude + "]");

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());


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

                                CoreEstimationTask ce = new CoreEstimationTask();
                                Double etp = ce.estimatePenmannMonteith(srad.get(i),
                                        tmin.get(i),
                                        tmax.get(i),
                                        wind.get(i),
                                        t2m.get(i),
                                        rh2m.get(i));

                                Log.d("DEBUG", response);
                                Log.d("DEBUG", "ETP: " + String.format("%.02f", etp) + " mm/d");

                                TextView tvEtp = findViewById(R.id.tvEtp);
                                tvEtp.setText(String.format("%.02f", etp) + " mm/d");

                                tab1View.setVisibility(View.VISIBLE);
                                tab1LoadingView .setVisibility(View.GONE);

//                                        // Animate the "show" view to 100% opacity, and clear any animation listener set on the view.
//                                        tab1View.animate()
//                                                .alpha(1f)
//                                                .setDuration(1000)
//                                                .setListener(null);
//
//                                        // Animate the "hide" view to 0% opacity.
//                                        tab1LoadingView.animate()
//                                                .alpha(0f)
//                                                .setDuration(1000)
//                                                .setListener(new AnimatorListenerAdapter() {
//                                                    @Override
//                                                    public void onAnimationEnd(Animator animation) {
//                                                        tab1LoadingView.setVisibility(View.GONE);
//                                                    }
//                                                });

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


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
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

                calculateETP(latLng);

            }
        });

    }

    @Override
    public void onLocationChanged(Location location) {
        //move map camera
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        calculateETP(latLng);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,11));

        // Stop updating the current location
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
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
//            // Stop updating the current location
//            if (mGoogleApiClient != null) {
//                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
//            }
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

    @Override
    public void onBackPressed() {
        if (mLayout != null &&
                (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED || mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }
}
