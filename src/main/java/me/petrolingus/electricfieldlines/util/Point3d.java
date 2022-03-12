package me.petrolingus.electricfieldlines.util;

import java.util.List;

public class Point3d {

    private final double x;
    private final double y;
    private final double z;
    private final boolean isEdge;

    private double value;
    private List<Integer> triangleList;

    public Point3d(double x, double y, double z) {
        this(x, y, z, false, 0);
    }

    public Point3d(double x, double y, double z, boolean isEdge, double value) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.isEdge = isEdge;
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

    public boolean isEdge() {
        return isEdge;
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
