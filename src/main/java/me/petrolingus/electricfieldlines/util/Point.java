package me.petrolingus.electricfieldlines.util;

import java.util.List;

public class Point {

    private final double x;
    private final double y;
    private final double z;
    private final boolean isNotEdge;

    private double value;
    private List<Integer> triangleList;

    public Point(double x, double y, double z) {
        this(x, y, z, true, 0);
    }

    public Point(double x, double y, double z, boolean isNotEdge, double value) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.isNotEdge = isNotEdge;
        this.value = value;
    }

    double mod() {
        return x * x + y * y + z * z;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public boolean isNotEdge() {
        return isNotEdge;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public List<Integer> getTriangleList() {
        return List.copyOf(triangleList);
    }

    public void setTriangleList(List<Integer> triangleList) {
        this.triangleList = List.copyOf(triangleList);
    }
}
