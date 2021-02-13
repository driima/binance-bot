package com.driima.binance.util;

public class Utils {

    public static int getDecimals(String stepSize) {
        String formatted = stepSize
                .replaceAll("0*$", "")
                .replaceAll("\\.$", "");

        if (Double.parseDouble(formatted) % 1 == 0) {
            return 0;
        } else {
            return formatted.length() - formatted.indexOf('.') - 1;
        }
    }

    public static String reduceDecimals(double amount, int notional) {
        return String.format("%." + notional + "f", amount);
    }
}
