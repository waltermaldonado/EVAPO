package br.com.maldonado.instantet0;

/**
 * Created by Walter Maldonado Jr. on 04/07/17.
 */

class CoreEstimation {

    float estimatePenmannMonteith(Double qg, Double q0, Double tMin, Double tMax, Double ws, Double t2m,
                                  Double rh2m, Double elevation) {

        final double gamma = 0.063;
        final Double qgConv = qg * (Math.pow(10, 6) / 86400);
        final Double q0Conv = q0 * (Math.pow(10, 6) / 86400);
        final Double q0Corr = (0.75 + 0.00002 * elevation) * q0Conv;
        final Double alpha = 0.2;
        final Double sigma = 5.67 * Math.pow(10, -8);
        final Double ac = 1.35;
        final Double bc = -0.35;
        final Double a1 = 0.35;
        final Double b1 = -0.14;


        Double esTMax = 0.6108 * Math.pow(10, (7.5 * tMax) / (237.3 + tMax));
        Double esTMin = 0.6108 * Math.pow(10, (7.5 * tMin) / (237.3 + tMin));

        Double es = (esTMax + esTMin) / 2;

        double s = (4098 * es) / Math.pow(t2m + 237.3, 2);

        Double ea = (es * rh2m) / 100;

        double de = es - ea;

        // Rn calculations
        Double rn = (1 - alpha) * qgConv - (ac * (qgConv / q0Corr) + bc) *
                (a1 + b1 * Math.sqrt(es)) *
                sigma * ((Math.pow(tMax, 4) + Math.pow(tMin, 4)) / 2);
        double rnConv = rn * (86400 / Math.pow(10, 6));

        return (float) ((0.408 * s * (rnConv - 0.8) + (gamma * 900 * ws * de) / (t2m + 273)) /
                (s + gamma * (1 + 0.34 * ws)));

    }
}
