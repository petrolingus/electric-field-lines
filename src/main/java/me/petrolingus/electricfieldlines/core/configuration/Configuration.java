package me.petrolingus.electricfieldlines.core.configuration;

import me.petrolingus.electricfieldlines.util.Point;

import java.util.List;

public interface Configuration {

    List<Point> generate(int n);

    boolean containsPoint(double x, double y);

    List<Point> getPivotPoints();

}
