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

    public Double estimatePenmannMonteith(Double qg, Double q0, Double tmin, Double tmax, Double ws, Double t2m,
                                  Double rh2m, Double elevation) {


        // TODO: SRAD should be corrected according to the Rn correction (Rsi = Qg = srad)


        final Double gamma   = 0.063;
        final Double qg_conv = qg * (Math.pow(10, 6) / 86400);
        final Double q0_conv = q0 * (Math.pow(10, 6) / 86400);
        final Double q0_corr = (0.75 + 0.00002 * elevation) * q0_conv;
        final Double alpha   = 0.2;
        final Double sigma   = 5.67 * Math.pow(10, -8);
        final Double ac      = 1.35;
        final Double bc      = -0.35;
        final Double a1      = 0.35;
        final Double b1      = -0.14;


        Double esTmax = 0.6108 * Math.pow(10,(7.5 * tmax) / (237.3 + tmax));
        Double esTmin = 0.6108 * Math.pow(10,(7.5 * tmin) / (237.3 + tmin));

        Double es = (esTmax + esTmin) / 2;

        Double t = t2m;

        Double s = (4098 * es) / Math.pow(t + 237.3, 2);

        Double ea = (es * rh2m) / 100;

        Double De = es - ea;


        // Rn calculations
        Double rn = (1 - alpha) * qg_conv - ( ac * ( qg_conv / q0_corr ) + bc ) * ( a1 + b1 * Math.sqrt(es) ) * sigma * ( ( Math.pow(tmax, 4) + Math.pow(tmin, 4) ) / 2 );
        Double rn_conv = rn * ( 86400 / Math.pow(10, 6) );

        // TODO: Replace srad with Rn
        Double etp = (0.408 * s * (rn_conv - 0.8) + (gamma * 900 * ws * De) / (t + 273)) /
                (s + gamma * (1 + 0.34 * ws));

        return etp;

    }


}
