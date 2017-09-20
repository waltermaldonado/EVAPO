package br.com.maldonado.instantet0;

/**
 * Created by walter on 04/07/17.
 */

public class CoreEstimation {

    public Double PenmannMonteith(Double srad, Double tmin, Double tmax, Double wind, Double t2m,
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
