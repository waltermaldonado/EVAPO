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
import android.os.Looper;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
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
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.apache.commons.collections4.queue.CircularFifoQueue;
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
import java.util.Queue;

/**
 * @author Walter Maldonado Jr.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private SlidingUpPanelLayout mLayout;
    private ViewPager viewPager;

    private int mapsHeight;

    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;

    private View tab1View;
    private View tab1LoadingView;
    private TextView tvLblEtp;
    private TextView tvDateEtp;
    private TextView tvEtp;

    FusedLocationProviderClient mFusedLocationClient;

    private int mapType = 0;

    private RequestQueue vQueue;
    private LatLng lastLatLng;


    public void showTabEtpContent(String etpValue, String dateValue) {

        tab1View = findViewById(R.id.tab1_content);
        tab1LoadingView = findViewById(R.id.tab1_spinner);
        tvLblEtp = findViewById(R.id.tvLblEtp);
        tvDateEtp = findViewById(R.id.tvDateEtp);
        tvEtp = findViewById(R.id.tvEtp);

        tvEtp.setText(etpValue);
        tvDateEtp.setText(dateValue);
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
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();

        showTabEtpContent(etpValue, dateValue);

    }

    private void initializePlaces() {
        // Initialize the AutocompleteSupportFragment.
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        }

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.place_autocomplete);

        assert autocompleteFragment != null;
        autocompleteFragment.setPlaceFields(Collections.singletonList(Place.Field.LAT_LNG));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Log.i("PLACES AUTOCOMPLETE", "Place: " + place.getName() + ", " + place.getId());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15));
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.i("PLACES AUTOCOMPLETE", "An error occurred: " + status);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        tab1View = findViewById(R.id.tab1_content);
        tab1LoadingView = findViewById(R.id.tab1_spinner);
        tvLblEtp = findViewById(R.id.tvLblEtp);
        tvDateEtp = findViewById(R.id.tvDateEtp);
        tvEtp = findViewById(R.id.tvEtp);
        vQueue = Volley.newRequestQueue(getApplicationContext());

        final Button btnTerrain = findViewById(R.id.btnTerrain);
        btnTerrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapType == 0) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    btnTerrain.setPressed(true);
                    btnTerrain.setText(R.string.btn_terrain_normal);
                    mapType = 1;
                } else {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    btnTerrain.setPressed(false);
                    btnTerrain.setText(R.string.btn_terrain_satellite);
                    mapType = 0;
                }

            }
        });

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // implementation of the slidingview
        mLayout = findViewById(R.id.sliding_layout);

        // initializes Google Places Services and SearchBar
        initializePlaces();

        // Implements the sliding panel listener
        mLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    Log.d("PANEL STATE", "Collapsed");
                    mMap.setPadding(0, 0, 0, 0);
                } else if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    Log.d("PANEL STATE", "Expanded");
                    mMap.setPadding(0, 0, 0, mapsHeight / 2);
                }
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // Tabs on slide panel initialization
        viewPager = findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(2);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tabs);
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

        ViewPagerAdapter(FragmentManager manager) {
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

        void addFragment(Fragment fragment, String title) {
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
                .build();
        mGoogleApiClient.connect();
    }


    private void calculateETP(LatLng location) {
        mapsHeight = findViewById(R.id.map).getHeight() - 200;
        mMap.setPadding(0, 0, 0, mapsHeight / 2);

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
        viewPager.setCurrentItem(0);

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, -49);
        Date date = new Date();
        date.setTime(c.getTime().getTime());
        Date cDate = new Date();

        String sDay = (String) DateFormat.format("dd", date);
        String sMonth = (String) DateFormat.format("MM", date);
        String sYear = (String) DateFormat.format("yyyy", date);

        String eDay = (String) DateFormat.format("dd", cDate);
        String eMonth = (String) DateFormat.format("MM", cDate);
        String eYear = (String) DateFormat.format("yyyy", cDate);

        String url = "https://power.larc.nasa.gov/cgi-bin/v1/DataAccess.py?" +
                "request=execute&" +
                "identifier=SinglePoint&" +
                "parameters=ALLSKY_SFC_SW_DWN,ALLSKY_TOA_SW_DWN,RH2M,T2M,T2M_MAX,T2M_MIN,WS2M&" +
                "startDate=" + sYear + sMonth + sDay + "&" +
                "endDate=" + eYear + eMonth + eDay + "&" +
                "userCommunity=AG&" +
                "tempAverage=DAILY&" +
                "outputList=JSON&" +
                "lat=" + latitude + "&" +
                "lon=" + longitude + "&" +
                "user=anonymous";

        // Chart creation
        final LineChart chart = findViewById(R.id.chart);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new ChartDateXAxisFormatter());
        xAxis.setCenterAxisLabels(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        Legend legend = chart.getLegend();
        legend.setEnabled(false);

        chart.getDescription().setText(getString(R.string.mm_d));

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
                        try {

                            JSONObject primaryData = response
                                    .getJSONArray("features")
                                    .getJSONObject(0);

                            JSONObject data = primaryData
                                    .getJSONObject("properties")
                                    .getJSONObject("parameter");

                            JSONArray location = primaryData
                                    .getJSONObject("geometry")
                                    .getJSONArray("coordinates");

                            JSONObject qg = data.getJSONObject("ALLSKY_SFC_SW_DWN");
                            JSONObject q0 = data.getJSONObject("ALLSKY_TOA_SW_DWN");
                            JSONObject rh = data.getJSONObject("RH2M");
                            JSONObject t2m = data.getJSONObject("T2M");
                            JSONObject tMax = data.getJSONObject("T2M_MAX");
                            JSONObject tMin = data.getJSONObject("T2M_MIN");
                            JSONObject ws = data.getJSONObject("WS2M");

                            Iterator keysToCopyIterator = qg.keys();
                            List<String> keysList = new ArrayList<>();
                            while (keysToCopyIterator.hasNext()) {
                                String key = (String) keysToCopyIterator.next();
                                keysList.add(key);
                            }


                            List<Entry> entries = new ArrayList<>();

                            Queue<Double> qgQueue = new CircularFifoQueue<>(7);
                            Queue<Double> q0Queue = new CircularFifoQueue<>(7);
                            Queue<Double> rhQueue = new CircularFifoQueue<>(7);
                            Queue<Double> t2mQueue = new CircularFifoQueue<>(7);
                            Queue<Double> tMaxQueue = new CircularFifoQueue<>(7);
                            Queue<Double> tMinQueue = new CircularFifoQueue<>(7);
                            Queue<Double> wsQueue = new CircularFifoQueue<>(7);

                            for (int i = 0; i < keysList.size(); i++) {

                                if (Double.parseDouble(qg.get(keysList.get(i)).toString()) > -99) {
                                    qgQueue.add(Double.parseDouble(qg.get(keysList.get(i)).toString()));
                                } else {
                                    qg.put(keysList.get(i), Collections.max(qgQueue));
                                }

                                if (Double.parseDouble(q0.get(keysList.get(i)).toString()) > -99) {
                                    q0Queue.add(Double.parseDouble(q0.get(keysList.get(i)).toString()));
                                } else {
                                    q0.put(keysList.get(i), Collections.max(q0Queue));
                                }

                                if (Double.parseDouble(rh.get(keysList.get(i)).toString()) > -999) {
                                    rhQueue.add(Double.parseDouble(rh.get(keysList.get(i)).toString()));
                                } else {
                                    rh.put(keysList.get(i), Collections.min(rhQueue));
                                }

                                if (Double.parseDouble(t2m.get(keysList.get(i)).toString()) > -99) {
                                    t2mQueue.add(Double.parseDouble(t2m.get(keysList.get(i)).toString()));
                                } else {
                                    t2m.put(keysList.get(i), Collections.max(t2mQueue));
                                }

                                if (Double.parseDouble(tMax.get(keysList.get(i)).toString()) > -99) {
                                    tMaxQueue.add(Double.parseDouble(tMax.get(keysList.get(i)).toString()));
                                } else {
                                    tMax.put(keysList.get(i), Collections.max(tMaxQueue));
                                }

                                if (Double.parseDouble(tMin.get(keysList.get(i)).toString()) > -99) {
                                    tMinQueue.add(Double.parseDouble(tMin.get(keysList.get(i)).toString()));
                                } else {
                                    tMin.put(keysList.get(i), Collections.max(tMinQueue));
                                }

                                if (Double.parseDouble(ws.get(keysList.get(i)).toString()) > -999) {
                                    wsQueue.add(Double.parseDouble(ws.get(keysList.get(i)).toString()));
                                } else {
                                    ws.put(keysList.get(i), Collections.max(wsQueue));
                                }
                            }

                            for (int i = 0; i < keysList.size(); i++) {

                                CoreEstimation ce = new CoreEstimation();
                                float etp = ce.estimatePenmannMonteith(
                                        Double.parseDouble(qg.get(keysList.get(i)).toString()),
                                        Double.parseDouble(q0.get(keysList.get(i)).toString()),
                                        Double.parseDouble(tMin.get(keysList.get(i)).toString()),
                                        Double.parseDouble(tMax.get(keysList.get(i)).toString()),
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


                                assert date != null;
                                entries.add(new Entry((float) date.getTime(), etp));


                                java.text.DateFormat dateFormat = DateFormat
                                        .getDateFormat(getApplicationContext());
                                showTabEtpContent(String.format("%.02f", etp) + " mm/d",
                                        getString(R.string.date_etp, dateFormat.format(date)));

                            }

                            LineDataSet dataSet = new LineDataSet(entries, "ETP");
                            dataSet.setColor(R.color.colorAccent);
                            dataSet.setFillColor(R.color.colorAccent);
                            dataSet.setCircleColor(R.color.colorAccent);
                            dataSet.setCircleColorHole(R.color.colorAccent);
                            LineData lineData = new LineData(dataSet);
                            chart.setData(lineData);
                            chart.animateY(2000, Easing.EasingOption.EaseOutCubic);
                            chart.animateX(2000, Easing.EasingOption.EaseOutCubic);


                        } catch (JSONException e) {

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
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
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

        vQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
        vQueue.add(jsonObjectRequest);
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

        // Initialize Google Play Services
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
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                lastLatLng = latLng;
                calculateETP(latLng);

            }
        });

    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                Log.i("MapsActivity", "Location: "
                        + location.getLatitude() + " " + location.getLongitude());

                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                calculateETP(latLng);
                lastLatLng = latLng;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));

                if (mFusedLocationClient != null) {
                    mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                }
            }
        }
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
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback,
                    Looper.myLooper());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        vQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (lastLatLng != null) {
            if (tab1LoadingView != null) {
                if (tab1LoadingView.getVisibility() == View.VISIBLE) {
                    calculateETP(lastLatLng);
                }
            } else {
                calculateETP(lastLatLng);
            }
        }
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
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
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
                Toast.makeText(this, R.string.location_permission_denied,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        boolean expandedOrAnchored =
                (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED
                        || mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED);
        if (mLayout != null && expandedOrAnchored) {
            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }
}
