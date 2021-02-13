package com.driima.binance.wrapper;

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
    }

    public String getName() {
        return symbolInfo.getSymbol();
    }

}
