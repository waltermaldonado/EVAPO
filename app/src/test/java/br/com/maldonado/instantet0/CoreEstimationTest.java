package br.com.maldonado.instantet0;


import org.junit.Test;

import static org.junit.Assert.*;

public class CoreEstimationTest {

    @Test
    public void et0_estimation_test1() throws Exception {

        CoreEstimation ce = new CoreEstimation();
        double etp = ce.estimatePenmannMonteith(
                13.1,
                29.2,
                6.09,
                19.09,
                4.35,
                12.37,
                71.76,
                22.0
        );

        assertEquals(3.0550, etp, 0.0001);
    }

    @Test
    public void et0_estimation_test2() throws Exception {

        CoreEstimation ce = new CoreEstimation();
        double etp = ce.estimatePenmannMonteith(
                1.22,
                21.38,
                8.37,
                15.14,
                2.4,
                12.81,
                88.49,
                22.0
        );

        assertEquals(0.4058, etp, 0.0001);
    }

    @Test
    public void et0_estimation_test3() throws Exception {

        CoreEstimation ce = new CoreEstimation();
        double etp = ce.estimatePenmannMonteith(
                25.56,
                40.54,
                25.45,
                34.9,
                3.69,
                29.38,
                62.39,
                22.0
        );

        assertEquals(8.0471, etp, 0.0001);
    }

}
