package com.driima.binance.binance;

import lombok.Data;

@Data
public class PumpTracker {
    private final double entry;
    private double profit;
    private double top;
    private double lossHits;
}
