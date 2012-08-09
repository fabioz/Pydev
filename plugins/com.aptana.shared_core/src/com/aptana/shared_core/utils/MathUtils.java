package com.aptana.shared_core.utils;

public class MathUtils {

    /**
     * Log with base is missing in java!
     */
    public static double log(double a, double base) {
        return Math.log(a) / Math.log(base);
    }

}
