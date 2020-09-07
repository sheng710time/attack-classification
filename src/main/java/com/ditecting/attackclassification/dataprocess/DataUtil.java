package com.ditecting.attackclassification.dataprocess;

import java.math.RoundingMode;
import java.text.NumberFormat;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/8/7 9:29
 */
public class DataUtil {

    /**
     * keep the last two decimal palces
     * @param value
     * @return
     */
    public static String formatDouble(double value, int place) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(place);
//        nf.setMinimumFractionDigits(place);
        nf.setRoundingMode(RoundingMode.HALF_UP);
        nf.setGroupingUsed(false);
        return nf.format(value);
    }
}