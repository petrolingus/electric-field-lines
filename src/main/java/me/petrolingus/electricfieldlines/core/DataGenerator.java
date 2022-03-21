package me.petrolingus.electricfieldlines.core;

import me.petrolingus.electricfieldlines.util.Point;

import java.util.ArrayList;
import java.util.List;

public class DataGenerator {

    private static final double COS135 = -Math.sqrt(2) / 2;

    private final int n;

    private final double radius;
    private final double cx;
    private final double cy;

    private final double outerCharge;
    private final double innerCharge;



    public DataGenerator(int n, double outerCharge, double innerCharge, double cx, double cy, double radius) {
        this.n = n;
        this.outerCharge = outerCharge;
        this.innerCharge = innerCharge;
        this.cx = cx;
        this.cy = cy;
        this.radius = radius;
    }

    public List<Point> generate() {

        final List<Point> points = new ArrayList<>();

        double step = 2.0 / (n - 1);
        int indexOuter135 = -1;
        int indexInner135xLeft = -1;
        int indexInner135xRight = -1;
        int indexInner135yLeft = -1;
        int indexInner135yRight = -1;

        // Add inner points
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double x = -1 + j * step;
                double y = -1 + i * step;
                double d = Math.sqrt(x * x + y * y);
                double d2 = Math.sqrt(Math.pow(cx - x, 2) + Math.pow(cy - y, 2));
                boolean isOuterContain = d < 1;
                boolean isCloseToOuterCircle = Math.abs(1 - d) < (step / 4);
                boolean isInnerCircleContain = d2 < radius;
                boolean isCloseToInnerCircle = Math.abs(radius - d2) < (step / 4);
                boolean condition = isOuterContain && !isCloseToOuterCircle && !isInnerCircleContain && !isCloseToInnerCircle;
                if (condition) {
                    points.add(new Point(x, y, 0));
                }
                indexOuter135 = (indexOuter135 == -1 && x > COS135) ? j : indexOuter135;
                indexInner135xLeft = (indexInner135xLeft == -1 && x > cx + radius * COS135) ? j : indexInner135xLeft;
                indexInner135xRight = (indexInner135xRight == -1 && x > cx - radius * COS135) ? j : indexInner135xRight;
                indexInner135yLeft = (indexInner135yLeft == -1 && y > cy + radius * COS135) ? i : indexInner135yLeft;
                indexInner135yRight = (indexInner135yRight == -1 && y > cy - radius * COS135) ? i : indexInner135yRight;
            }
        }

        // Add outer edge points
        for (int i = indexOuter135; i < n - indexOuter135; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(1 - a * a);
            points.add(new Point(a, -b, 0, true, outerCharge));
            points.add(new Point(a, b, 0, true, outerCharge));
            points.add(new Point(b, a, 0, true, outerCharge));
            points.add(new Point(-b, a, 0, true, outerCharge));
        }

        // Add inner edge points
        for (int i = indexInner135xLeft; i < indexInner135xRight; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(radius * radius - (a - cx) * (a - cx));
            points.add(new Point(a, -b + cy, 0, true, innerCharge));
            points.add(new Point(a, b + cy, 0, true, innerCharge));
        }
        for (int i = indexInner135yLeft; i < indexInner135yRight; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(radius * radius - (a - cy) * (a - cy));
            points.add(new Point(b + cx, a, 0, true, innerCharge));
            points.add(new Point(-b + cx, a, 0, true, innerCharge));
        }

        return points;
    }
}
