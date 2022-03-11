package me.petrolingus.electricfieldlines.util;

public class Point3d {

    double x;
    double y;
    double z;

    boolean isEdge = false;

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
}
