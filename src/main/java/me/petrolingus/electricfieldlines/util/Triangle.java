package me.petrolingus.electricfieldlines.util;

import java.util.List;

public class Triangle {

    private int indexA;
    private int indexB;
    private int indexC;

    private double cx;
    private double cy;
    private double r;

    public Triangle(int indexA, int indexB, int indexC) {
        this.indexA = indexA;
        this.indexB = indexB;
        this.indexC = indexC;
    }

    public void calcCircle(Point a, Point b, Point c) {
        double aa = a.mod();
        double bb = b.mod();
        double cc = c.mod();

        double d = 2 * (a.x() * (b.y() - c.y()) + b.x() * (c.y() - a.y()) + c.x() * (a.y() - b.y()));
        this.cx = (aa * (b.y() - c.y()) + bb * (c.y() - a.y()) + cc * (a.y() - b.y())) / d;
        this.cy = (aa * (c.x() - b.x()) + bb * (a.x() - c.x()) + cc * (b.x() - a.x())) / d;

        double dx = cx - a.x();
        double dy = cy - a.y();
        this.r = Math.sqrt(dx * dx + dy * dy);
    }

    public boolean isContain(double x, double y) {
        double dx = cx - x;
        double dy = cy - y;
        return this.r - Math.sqrt(dx * dx + dy * dy) > 1e-10;
    }

    public List<List<Integer>> getEdges() {
        List<Integer> edge0 = List.of(Math.min(indexA, indexB), Math.max(indexA, indexB));
        List<Integer> edge1 = List.of(Math.min(indexB, indexC), Math.max(indexB, indexC));
        List<Integer> edge2 = List.of(Math.min(indexC, indexA), Math.max(indexC, indexA));
        return List.of(edge0, edge1, edge2);
    }

    public int getIndexA() {
        return indexA;
    }

    public void setIndexA(int indexA) {
        this.indexA = indexA;
    }

    public int getIndexB() {
        return indexB;
    }

    public void setIndexB(int indexB) {
        this.indexB = indexB;
    }

    public int getIndexC() {
        return indexC;
    }

    public void setIndexC(int indexC) {
        this.indexC = indexC;
    }
}
