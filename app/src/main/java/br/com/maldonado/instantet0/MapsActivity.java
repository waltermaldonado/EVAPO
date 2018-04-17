package br.com.maldonado.instantet0;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
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

    FusedLocationProviderClient mFusedLocationClient;


    public void showTabEtpContent(String etpValue, String dateValue) {

        TextView tvEtp = findViewById(R.id.tvEtp);
        tvEtp.setText(etpValue);

        TextView tvDateEtp = findViewById(R.id.tvDateEtp);
        tvDateEtp.setText(dateValue);

        TextView tvLblEtp = findViewById(R.id.tvLblEtp);
        tvLblEtp.setVisibility(View.VISIBLE);

        tab1View.setAlpha(0f);
        tab1View.setVisibility(View.VISIBLE);

        tab1View.animate()
                .alpha(1f)
                .setDuration(500)
                .setListener(null);

        tab1LoadingView.animate()
                .alpha(0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        tab1LoadingView.setVisibility(View.GONE);
                    }
                });

    }

    public void showAlertDialog(String title, String message, String etpValue, String dateValue) {

        AlertDialog alertDialog = new AlertDialog.Builder(MapsActivity.this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.alert_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();

        showTabEtpContent(etpValue, dateValue);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // implementation of the slidingview
        mLayout = findViewById(R.id.sliding_layout);

        // Implements the searchbar functions
        placeAutoComplete = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete);
        placeAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

//                Log.d("Maps", "Place selected: " + place.getName());
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
//                    Log.d("PANEL STATE", "Collapsed");
                    mMap.setPadding(0, 0, 0, 0);
                } else if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
//                    Log.d("PANEL STATE", "Expanded");
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
        viewPager.setOffscreenPageLimit(2);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new Tab1Fragment(), getString(R.string.tab1_title));
        adapter.addFragment(new Tab2Fragment(), getString(R.string.tab2_title));
        adapter.addFragment(new Tab3Fragment(), getString(R.string.tab3_title));
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
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
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
            tab1View.setAlpha(0f);
            tab1View.setVisibility(View.GONE);
        }
        if (tab1LoadingView != null) {
            tab1LoadingView.setAlpha(1f);
            tab1LoadingView.setVisibility(View.VISIBLE);
        }

        mMap.clear();
        final String latitude = String.valueOf(location.latitude);
        final String longitude = String.valueOf(location.longitude);
        final LatLng pos = new LatLng(Double.valueOf(latitude), Double.valueOf(longitude));
        mMap.addMarker(new MarkerOptions().position(pos));

        CameraUpdate updtLocation = CameraUpdateFactory.newLatLngZoom(pos, 11.0f);
        mMap.animateCamera(updtLocation);

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());


        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, -50);
        Date date = new Date();
        date.setTime(c.getTime().getTime());
        Date cDate = new Date();

        String sDay = (String) DateFormat.format("dd",   date);
        String sMonth = (String) DateFormat.format("MM",   date);
        String sYear = (String) DateFormat.format("yyyy", date);

        String eDay = (String) DateFormat.format("dd",   cDate);
        String eMonth = (String) DateFormat.format("MM",   cDate);
        String eYear = (String) DateFormat.format("yyyy", cDate);


        String url = "https://asdc-arcgis.larc.nasa.gov/cgi-bin/power/v1beta/DataAccess.py?" +
                "request=execute&" +
                "identifier=SinglePoint&" +
                "parameters=ALLSKY_SFC_SW_DWN,ALLSKY_TOA_SW_DWN,RH2M,T2M,T2M_MAX,T2M_MIN,WS10M&" +
                "startDate=" + sYear + sMonth + sDay + "&" +
                "endDate=" + eYear + eMonth + eDay + "&" +
                "userCommunity=AG&" +
                "tempAverage=DAILY&" +
                "outputList=JSON&" +
                "lat=" + latitude + "&" +
                "lon=" + longitude + "&" +
                "user=anonymous";

        // Chart creation
        final LineChart chart = (LineChart) findViewById(R.id.chart);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new ChartDateXAxisFormatter());
        xAxis.setCenterAxisLabels(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        Legend legend = chart.getLegend();
        legend.setEnabled(false);

        chart.getDescription().setEnabled(false);

        chart.setNoDataText(getString(R.string.chart_nodata));
        chart.fitScreen();
        chart.clear();
        chart.invalidate();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try{

                            JSONObject primary_data = response
                                    .getJSONArray("features")
                                    .getJSONObject(0);

                            JSONObject data = primary_data
                                    .getJSONObject("properties")
                                    .getJSONObject("parameter");

                            JSONArray location = primary_data
                                    .getJSONObject("geometry")
                                    .getJSONArray("coordinates");

                            JSONObject qg = data.getJSONObject("ALLSKY_SFC_SW_DWN");
                            JSONObject q0 = data.getJSONObject("ALLSKY_TOA_SW_DWN");
                            JSONObject rh = data.getJSONObject("RH2M");
                            JSONObject t2m = data.getJSONObject("T2M");
                            JSONObject tmax = data.getJSONObject("T2M_MAX");
                            JSONObject tmin = data.getJSONObject("T2M_MIN");
                            JSONObject ws = data.getJSONObject("WS10M");

                            Iterator keysToCopyIterator = qg.keys();
                            List<String> keysList = new ArrayList<String>();
                            while(keysToCopyIterator.hasNext()) {
                                String key = (String) keysToCopyIterator.next();
                                keysList.add(key);
                            }


                            List<Entry> entries = new ArrayList<Entry>();

                            for (int i = 0; i < keysList.size(); i++) {

                                if (
                                        Double.parseDouble(qg.get(keysList.get(i)).toString()) != -99 &&
                                        Double.parseDouble(q0.get(keysList.get(i)).toString()) != -99 &&
                                        Double.parseDouble(rh.get(keysList.get(i)).toString()) != -999 &&
                                        Double.parseDouble(t2m.get(keysList.get(i)).toString()) != -99 &&
                                        Double.parseDouble(tmax.get(keysList.get(i)).toString()) != -99 &&
                                        Double.parseDouble(tmin.get(keysList.get(i)).toString()) != -99 &&
                                        Double.parseDouble(ws.get(keysList.get(i)).toString()) != -999) {

                                    CoreEstimation ce = new CoreEstimation();
                                    Double etp = ce.estimatePenmannMonteith(
                                            Double.parseDouble(qg.get(keysList.get(i)).toString()),
                                            Double.parseDouble(q0.get(keysList.get(i)).toString()),
                                            Double.parseDouble(tmin.get(keysList.get(i)).toString()),
                                            Double.parseDouble(tmax.get(keysList.get(i)).toString()),
                                            Double.parseDouble(ws.get(keysList.get(i)).toString()),
                                            Double.parseDouble(t2m.get(keysList.get(i)).toString()),
                                            Double.parseDouble(rh.get(keysList.get(i)).toString()),
                                            Double.parseDouble(location.getString(2)));

                                    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                                    Date date = null;
                                    try {
                                        date = format.parse(keysList.get(i));
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }


                                    entries.add(new Entry((float) date.getTime(), etp.floatValue()));

                                    java.text.DateFormat dateFormat = DateFormat.getDateFormat(getApplicationContext());
                                    showTabEtpContent(String.format("%.02f", etp) + " mm/d",
                                            getString(R.string.date_etp, dateFormat.format(date)));


                                }

                            }

                            LineDataSet dataSet = new LineDataSet(entries, "ETP");
                            dataSet.setColor(R.color.chartBlue);
                            dataSet.setFillColor(R.color.chartBlue);
                            dataSet.setCircleColor(R.color.chartBlue);
                            dataSet.setCircleColorHole(R.color.chartBlue);
                            LineData lineData = new LineData(dataSet);
                            chart.setData(lineData);
                            chart.animateY(2000, Easing.EasingOption.EaseOutCubic);
                            chart.animateX(2000, Easing.EasingOption.EaseOutCubic);


                        } catch (JSONException e){

                            showAlertDialog(getString(R.string.alert_title),
                                    getString(R.string.error_nasa_request),
                                    getString(R.string.unavailable_etp),
                                    getString(R.string.try_again_etp));

                            TextView tvLblEtp = findViewById(R.id.tvLblEtp);
                            tvLblEtp.setVisibility(View.INVISIBLE);

                            e.printStackTrace();

                        }
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        // Do something when error occurred

                        showAlertDialog(getString(R.string.alert_title),
                                getString(R.string.error_internet_conn),
                                getString(R.string.unavailable_etp),
                                getString(R.string.try_again_internet_conn));

                        TextView tvLblEtp = findViewById(R.id.tvLblEtp);
                        tvLblEtp.setVisibility(View.INVISIBLE);

                        error.printStackTrace();
                    }
                }
        );

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(20000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(jsonObjectRequest);
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
//        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

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

    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
//                mLastLocation = location;

                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                calculateETP(latLng);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,11));

                if (mFusedLocationClient != null) {
                    mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                }
            }
        };

    };

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
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
                        .setTitle(getString(R.string.location_permission_needed))
                        .setMessage(getString(R.string.location_permission_message))
                        .setPositiveButton(getString(R.string.location_message_ok), new DialogInterface.OnClickListener() {
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
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
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
