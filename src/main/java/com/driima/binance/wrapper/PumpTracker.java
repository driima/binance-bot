package com.driima.binance.wrapper;

import lombok.Data;

@Data
public class PumpTracker {
    private final double entry;
    private double profit;
    private double top;
}
