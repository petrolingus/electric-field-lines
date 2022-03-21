package me.petrolingus.electricfieldlines.core;

import me.petrolingus.electricfieldlines.util.Point3d;

import java.util.ArrayList;
import java.util.List;

public class DataGenerator {

    private static final double RADIUS = 0.3;
    private static final double SHIFT = 0.2;
    private static final double ANGLE = 0.5;
    private static final int N = 32;

    private static final double COS135 = -Math.sqrt(2) / 2;

    private double cx = 0.1;
    private double cy = 0.45;
    private double outerQ = 9;
    private double innerQ = -9;

    public List<Point3d> generate() {

        final List<Point3d> points = new ArrayList<>();

        double step = 2.0 / (N - 1);
        int indexOuter135 = -1;
        int indexInner135xLeft = -1;
        int indexInner135xRight = -1;
        int indexInner135yLeft = -1;
        int indexInner135yRight = -1;

        // Add inner points
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                double x = -1 + j * step;
                double y = -1 + i * step;
                double d = Math.sqrt(x * x + y * y);
                double d2 = Math.sqrt(Math.pow(cx - x, 2) + Math.pow(cy - y, 2));
                boolean isOuterContain = d < 1;
                boolean isCloseToOuterCircle = Math.abs(1 - d) < (step / 4);
                boolean isInnerCircleContain = d2 < RADIUS;
                boolean isCloseToInnerCircle = Math.abs(RADIUS - d2) < (step / 4);
                boolean condition = isOuterContain && !isCloseToOuterCircle && !isInnerCircleContain && !isCloseToInnerCircle;
                if (condition) {
                    points.add(new Point3d(x, y, 0));
                }
                indexOuter135 = (indexOuter135 == -1 && x > COS135) ? j : indexOuter135;
                indexInner135xLeft = (indexInner135xLeft == -1 && x > cx + RADIUS * COS135) ? j : indexInner135xLeft;
                indexInner135xRight = (indexInner135xRight == -1 && x > cx - RADIUS * COS135) ? j : indexInner135xRight;
                indexInner135yLeft = (indexInner135yLeft == -1 && y > cy + RADIUS * COS135) ? i : indexInner135yLeft;
                indexInner135yRight = (indexInner135yRight == -1 && y > cy - RADIUS * COS135) ? i : indexInner135yRight;
            }
        }

        // Add outer edge points
        for (int i = indexOuter135; i < N - indexOuter135; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(1 - a * a);
            points.add(new Point3d(a, -b, 0, true, outerQ));
            points.add(new Point3d(a, b, 0, true, outerQ));
            points.add(new Point3d(b, a, 0, true, outerQ));
            points.add(new Point3d(-b, a, 0, true, outerQ));
        }

        // Add inner edge points
        for (int i = indexInner135xLeft; i < indexInner135xRight; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(RADIUS * RADIUS - (a - cx) * (a - cx));
            points.add(new Point3d(a, -b + cy, 0, true, innerQ));
            points.add(new Point3d(a, b + cy, 0, true, innerQ));
        }
        for (int i = indexInner135yLeft; i < indexInner135yRight; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(RADIUS * RADIUS - (a - cy) * (a - cy));
            points.add(new Point3d(b + cx, a, 0, true, innerQ));
            points.add(new Point3d(-b + cx, a, 0, true, innerQ));
        }

        return points;
    }
}
