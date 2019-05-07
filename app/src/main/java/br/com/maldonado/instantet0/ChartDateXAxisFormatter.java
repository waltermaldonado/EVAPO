package br.com.maldonado.instantet0;


import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Walter Maldonado Jr.
 */
public class ChartDateXAxisFormatter implements IAxisValueFormatter {
    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        SimpleDateFormat df = new SimpleDateFormat("dd/MM", Locale.getDefault());
        return df.format(new Date(Float.valueOf(value).longValue()));
    }
}
