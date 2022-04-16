package me.petrolingus.electricfieldlines.core.generator;

import me.petrolingus.electricfieldlines.core.configuration.Configuration;
import me.petrolingus.electricfieldlines.util.Point;

import java.util.ArrayList;
import java.util.List;

public class DataGenerator {

    private final int n;

    public DataGenerator(int n) {
        this.n = n;
    }

    public List<Point> generate(Configuration configuration) {

        final List<Point> points = new ArrayList<>();

        double step = 2.0 / (n - 1);

        // Add inner points
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double x = -1 + j * step;
                double y = -1 + i * step;
                if (configuration.containsPoint(x, y)) {
                    points.add(new Point(x, y, 0));
                }
            }
        }

        points.addAll(configuration.generate(n));

        return points;
    }
}
