package me.petrolingus.electricfieldlines.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class Triangle {

    private int indexA;
    private int indexB;
    private int indexC;

    private double cx;
    private double cy;
    private double r;

    public List<List<Point>> isolines = new ArrayList<>();

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

    public void createIsoline(Point a, Point b, Point c) {

        List<Point> points = Stream.of(a, b, c).sorted(Comparator.comparingDouble(Point::getValue)).toList();

        // USED FOR DEBUG
        for (int i = 0; i < 3; i++) {
            for (int j = i + 1; j < 3; j++) {
                if (points.get(i).getValue() > points.get(j).getValue()) {
                    System.err.println("POINTS NOT SORTED!");
                    System.exit(-1);
                }
            }
        }

        double z1 = points.get(0).getValue();
        double z2 = points.get(1).getValue();
        double z3 = points.get(2).getValue();

        double x1 = points.get(0).x();
        double y1 = points.get(0).y();
        double x2 = points.get(1).x();
        double y2 = points.get(1).y();
        double x3 = points.get(2).x();
        double y3 = points.get(2).y();

        int n = 10;
        double begin = 0.1;
        double end = 0.9;
        double step = (end - begin) / (n - 1);

        for (int i = 0; i < n; i++) {

            double zc = begin + i * step;

            if (zc > z1 && zc < z3) {
                List<Point> isoline = new ArrayList<>();

                if (zc < z2) {
                    double c21 = (z2 - zc) / (z2 - z1);
                    double xc1 = x2 - c21 * (x2 - x1);
                    double yc1 = y2 - c21 * (y2 - y1);
                    isoline.add(new Point(xc1, yc1, 0));
                } else {
                    double c32 = (z3 - zc) / (z3 - z2);
                    double xc1 = x3 - c32 * (x3 - x2);
                    double yc1 = y3 - c32 * (y3 - y2);
                    isoline.add(new Point(xc1, yc1, 0));
                }

                double c31 = (z3 - zc) / (z3 - z1);
                double xc2 = x3 - c31 * (x3 - x1);
                double yc2 = y3 - c31 * (y3 - y1);
                isoline.add(new Point(xc2, yc2, 0));

                isolines.add(isoline);
            }
        }

    }
}
