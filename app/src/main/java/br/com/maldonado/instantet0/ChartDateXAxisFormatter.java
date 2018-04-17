package br.com.maldonado.instantet0;


import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ChartDateXAxisFormatter implements IAxisValueFormatter {

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        SimpleDateFormat df = new SimpleDateFormat("dd/MM");
        String s = df.format(new Date(new Float(value).longValue()));
        return s;
    }
}
