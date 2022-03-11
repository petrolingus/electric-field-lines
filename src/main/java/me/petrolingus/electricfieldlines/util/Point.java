package me.petrolingus.electricfieldlines.util;

import javafx.scene.paint.Color;

public record Point(double x, double y, Color color) {
    double mod() {
        return x * x + y * y;
    }
}
