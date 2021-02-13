package com.driima.binance.binance;

import com.binance.api.client.domain.market.TickerPrice;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Trade {
    private String symbol;
    private Double price;

    public Trade(TickerPrice tickerPrice) {
        update(tickerPrice);
    }

    public void update(TickerPrice tickerPrice) {
        this.symbol = tickerPrice.getSymbol();
        this.price = Double.valueOf(tickerPrice.getPrice());
    }
}
