package com.driima.binance;

import java.awt.*;

public enum EmbedType {
    STANDARD,
    INFO(Color.CYAN),
    WARNING(Color.ORANGE),
    ERROR(Color.RED);

    private Color color;

    EmbedType() {
    }

    EmbedType(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}
