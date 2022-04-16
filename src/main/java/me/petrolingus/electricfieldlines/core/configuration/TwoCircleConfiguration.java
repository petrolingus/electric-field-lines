package me.petrolingus.electricfieldlines.core.configuration;

import me.petrolingus.electricfieldlines.util.Point;

import java.util.ArrayList;
import java.util.List;

public class TwoCircleConfiguration implements Configuration {

    private double innerCircleCenterX;
    private double innerCircleCenterY;
    private double innerCircleRadius;

    private static final double COS135 = -Math.sqrt(2) / 2;

    public void calculateParameters(double innerCircleRadius, double innerCircleCenterShift, double innerCircleCenterRotation) {
        this.innerCircleRadius = 0.2 + 0.7 * innerCircleRadius;
        double shift = (1 - this.innerCircleRadius) * (0.8 * innerCircleCenterShift);
        double angle = 2 * Math.PI * innerCircleCenterRotation;
        this.innerCircleCenterX = shift * Math.cos(angle);
        this.innerCircleCenterY = shift * Math.sin(angle);
    }

    public double getInnerCircleCenterX() {
        return innerCircleCenterX;
    }

    public double getInnerCircleCenterY() {
        return innerCircleCenterY;
    }

    public double getInnerCircleRadius() {
        return innerCircleRadius;
    }

    public List<Point> generate(int n) {

        final List<Point> points = new ArrayList<>();

        double step = 2.0 / (n - 1);

//        ##############################################################################################################
//        int indexOuter135 = -1;
//        int indexInner135xLeft = -1;
//        int indexInner135xRight = -1;
//        int indexInner135yLeft = -1;
//        int indexInner135yRight = -1;
//
//        for (int i = 0; i < n; i++) {
//            for (int j = 0; j < n; j++) {
//                double x = -1 + j * step;
//                double y = -1 + i * step;
//                indexOuter135 = (indexOuter135 == -1 && x > COS135) ? j : indexOuter135;
//                indexInner135xLeft = (indexInner135xLeft == -1 && x > innerCircleCenterX + innerCircleRadius * COS135) ? j : indexInner135xLeft;
//                indexInner135xRight = (indexInner135xRight == -1 && x > innerCircleCenterX - innerCircleRadius * COS135) ? j : indexInner135xRight;
//                indexInner135yLeft = (indexInner135yLeft == -1 && y > innerCircleCenterY + innerCircleRadius * COS135) ? i : indexInner135yLeft;
//                indexInner135yRight = (indexInner135yRight == -1 && y > innerCircleCenterY - innerCircleRadius * COS135) ? i : indexInner135yRight;
//            }
//        }
//
//        // Add outer edge points
//        for (int i = indexOuter135; i < n - indexOuter135; i++) {
//            double a = -1 + i * step;
//            double b = Math.sqrt(1 - a * a);
//            points.add(new Point(a, -b, 0, false, 1));
//            points.add(new Point(a, b, 0, false, 1));
//            points.add(new Point(b, a, 0, false, 1));
//            points.add(new Point(-b, a, 0, false, 1));
//        }
//
//        // Add inner edge points
//        for (int i = indexInner135xLeft; i < indexInner135xRight; i++) {
//            double a = -1 + i * step;
//            double b = Math.sqrt(Math.pow(innerCircleRadius, 2) - Math.pow(a - innerCircleCenterX, 2));
//            points.add(new Point(a, -b + innerCircleCenterY, 0, false, -1));
//            points.add(new Point(a, b + innerCircleCenterY, 0, false, -1));
//        }
//        for (int i = indexInner135yLeft; i < indexInner135yRight; i++) {
//            double a = -1 + i * step;
//            double b = Math.sqrt(Math.pow(innerCircleRadius, 2) - Math.pow(a - innerCircleCenterY, 2));
//            points.add(new Point(b + innerCircleCenterX, a, 0, false, -1));
//            points.add(new Point(-b + innerCircleCenterX, a, 0, false, -1));
//        }
//        ##############################################################################################################


        int count1 = (int) Math.floor(2 * Math.PI / step);
        for (int i = 0; i < count1; i++) {
            double phi = step * i;
            double x = Math.cos(phi);
            double y = Math.sin(phi);
            points.add(new Point(x, y, 0, false, 1));
        }

        int count2 = (int) Math.floor(2 * Math.PI * innerCircleRadius / step);
        for (int i = 0; i < count2; i++) {
            double phi = step * i / innerCircleRadius;
            double x = innerCircleCenterX + innerCircleRadius * Math.cos(phi);
            double y = innerCircleCenterY + innerCircleRadius * Math.sin(phi);
            points.add(new Point(x, y, 0, false, -1));
        }

        return points;
    }

    public boolean containsPoint(double x, double y) {
        double d = Math.sqrt(x * x + y * y);
        double d2 = Math.sqrt(Math.pow(innerCircleCenterX - x, 2) + Math.pow(innerCircleCenterY - y, 2));
        boolean isOuterContain = d < 1;
        boolean isInnerCircleContain = d2 < innerCircleRadius;
        return isOuterContain && !isInnerCircleContain;
    }

    @Override
    public List<Point> getPivotPoints() {
        return List.of(new Point(innerCircleCenterX, innerCircleCenterY, 0));
    }
}
