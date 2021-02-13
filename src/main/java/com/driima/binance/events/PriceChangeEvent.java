package com.driima.binance.events;

import com.dreamburst.sed.Event;
import com.driima.binance.wrapper.Symbol;
import lombok.Data;

@Data
public class PriceChangeEvent implements Event {
    private final Symbol symbol;
    private final Double oldPrice;
    private final Double newPrice;
    private final Double change;
}
