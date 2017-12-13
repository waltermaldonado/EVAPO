package br.com.maldonado.instantet0;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by walter on 04/07/17.
 */

public class CoreEstimation {

    public Double estimatePenmannMonteith(Double srad, Double tmin, Double tmax, Double wind, Double t2m,
                                  Double rh2m) {

        final Double gama = 0.063;

        Double esTmax = 0.6108 * Math.pow(10,(7.5 * tmax) / (237.3 + tmax));
        Double esTmin = 0.6108 * Math.pow(10,(7.5 * tmin) / (237.3 + tmin));

        Double es = (esTmax + esTmin) / 2;

        Double t = (tmax + tmin) / 2;

        Double s = (4098 * es) / Math.pow(t + 237.3, 2);

        Double ea = (es * rh2m) / 100;

        Double De = es - ea;

        Double etp = (0.408 * s * (srad - 0.8) + (gama * wind * De) / (t + 275)) /
                (s + gama * (1 + 0.34 * wind));

        return etp;

    }


}
