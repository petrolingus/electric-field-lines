package me.petrolingus.electricfieldlines.util;

import java.util.ArrayList;
import java.util.List;

public class Point3d {

    double x;
    double y;
    double z;

    boolean isEdge = false;

    List<Integer> triangleList;

    public Point3d(double x, double y, double z) {
        this(x, y, z, false);
    }

    public Point3d(double x, double y, double z, boolean isEdge) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.isEdge = isEdge;
    }

    double mod() {
        return x * x + y * y;
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

    public void setEdge(boolean edge) {
        isEdge = edge;
    }

    public void setTriangleList(List<Integer> triangleList) {
        this.triangleList = triangleList;
    }

    public List<Integer> getTriangleList() {
        return triangleList;
    }
}
