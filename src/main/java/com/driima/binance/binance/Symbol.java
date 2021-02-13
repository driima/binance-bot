package com.driima.binance.binance;

import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolInfo;
import com.driima.binance.util.Utils;
import lombok.Data;

@Data
public class Symbol {

    private final SymbolInfo symbolInfo;

    private final int stepSize;
    private final double minNotional;

    public Symbol(SymbolInfo symbolInfo) {
        this.symbolInfo = symbolInfo;

        this.stepSize = Utils.getDecimals(symbolInfo.getSymbolFilter(FilterType.LOT_SIZE).getStepSize());
        this.minNotional = Double.parseDouble(symbolInfo.getSymbolFilter(FilterType.MIN_NOTIONAL).getMinNotional());

//        System.out.println(symbolInfo.getSymbol() + ":");
//        for (SymbolFilter filter : symbolInfo.getFilters()) {
//            if (filter.getFilterType() == FilterType.MAX_NUM_ALGO_ORDERS || filter.getFilterType() == FilterType.MAX_NUM_ORDERS || filter.getFilterType() == FilterType.ICEBERG_PARTS) {
//                continue;
//            }
//
//            System.out.println("    " + filter.getFilterType() + "  ->  Step Size (" + filter.getStepSize() + ")    " +
//                            "Min Notional (" + filter.getMinNotional() + ")    " +
//                            "Min Price (" + filter.getMinPrice() + ")    " +
//                            "Max Price (" + filter.getMaxPrice() + ")    " +
//                            "Min Qty (" + filter.getMinQty() + ")    " +
//                            "Max Qty (" + filter.getMaxQty() + ")    " +
//                            "Tick Size (" + filter.getTickSize() + ")    " +
//                            "Limit (" + filter.getLimit() + ")");
//        }
    }

}
