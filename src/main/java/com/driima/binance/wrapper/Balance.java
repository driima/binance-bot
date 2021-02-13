package com.driima.binance.wrapper;

import com.binance.api.client.domain.account.AssetBalance;
import lombok.Data;

@Data
public class Balance {
    private String asset;
    private Double free;
    private Double locked;

    public Balance(AssetBalance assetBalance) {
        this.asset = assetBalance.getAsset();
        this.free = Double.valueOf(assetBalance.getFree());
        this.locked = Double.valueOf(assetBalance.getLocked());
    }
}
