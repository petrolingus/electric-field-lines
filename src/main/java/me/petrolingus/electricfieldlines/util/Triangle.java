package me.petrolingus.electricfieldlines.util;

import javafx.scene.paint.Color;
import me.petrolingus.electricfieldlines.Controller;
import me.petrolingus.electricfieldlines.core.Triangulation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

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

    private double x3, y3;
    private double y23, x32, y31, x13;
    private double det, minD, maxD;

    public Color color;

    public List<Isoline> isoline = new ArrayList<>();

    public List<Vector2D> forceLine = new ArrayList<>();

    public Triangle(int indexA, int indexB, int indexC) {
        this.indexA = indexA;
        this.indexB = indexB;
        this.indexC = indexC;

        Point a = Triangulation.vertices.get(indexA);
        Point b = Triangulation.vertices.get(indexB);
        Point c = Triangulation.vertices.get(indexC);

        x3 = c.x();
        y3 = c.y();
        y23 = b.y() - c.y();
        x32 = c.x() - b.x();
        y31 = c.y() - a.y();
        x13 = a.x() - c.x();
        det = y23 * x13 - x32 * y31;
        minD = Math.min(det, 0);
        maxD = Math.max(det, 0);
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
        List<Integer> points = Stream.of(indexA, indexB, indexC).sorted().toList();
        List<Integer> edge0 = List.of(points.get(0), points.get(1));
        List<Integer> edge1 = List.of(points.get(1), points.get(2));
        List<Integer> edge2 = List.of(points.get(0), points.get(2));
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

    public void createIsoline(Point a, Point b, Point c, int n) {

        List<Point> points = Stream.of(a, b, c).sorted(Comparator.comparingDouble(Point::getValue)).toList();

        double z1 = points.get(0).getValue();
        double z2 = points.get(1).getValue();
        double z3 = points.get(2).getValue();

        double x1 = points.get(0).x();
        double y1 = points.get(0).y();
        double x2 = points.get(1).x();
        double y2 = points.get(1).y();
        double x3 = points.get(2).x();
        double y3 = points.get(2).y();

        double begin = 0;
        double end = 1;
        double step = (end - begin) / (n - 1);

        for (int i = 0; i < n; i++) {

            double zc = begin + i * step;

            if (zc > z1 && zc < z3) {

                Isoline tempIsoline = new Isoline();

                if (zc < z2) {
                    double c21 = (z2 - zc) / (z2 - z1);
                    tempIsoline.x1 = x2 - c21 * (x2 - x1);
                    tempIsoline.y1 = y2 - c21 * (y2 - y1);
                } else {
                    double c32 = (z3 - zc) / (z3 - z2);
                    tempIsoline.x1 = x3 - c32 * (x3 - x2);
                    tempIsoline.y1 = y3 - c32 * (y3 - y2);
                }

                double c31 = (z3 - zc) / (z3 - z1);
                tempIsoline.x2 = x3 - c31 * (x3 - x1);
                tempIsoline.y2 = y3 - c31 * (y3 - y1);

                tempIsoline.setValue(zc);

                this.isoline.add(tempIsoline);
            }
        }

    }

    public void createForceLine(double x, double y) {
        Point a = Controller.points.get(indexA);
        Point b = Controller.points.get(indexB);
        Point c = Controller.points.get(indexC);

//        Vector3D a = new Vector3D(p1.x(), p1.y(), p1.getValue());
//        Vector3D b = new Vector3D(p2.x(), p2.y(), p2.getValue());
//        Vector3D c = new Vector3D(p3.x(), p3.y(), p3.getValue());
        Vector3D v0 = new Vector3D(b.x() - a.x(), b.y() - a.y(), b.getValue() - a.getValue());
        Vector3D v1 = new Vector3D(c.x() - a.x(), c.y() - a.y(), c.getValue() - a.getValue());
        Vector3D normal = v1.crossProduct(v0).normalize().scalarMultiply(0.05);
//        Vector3D normal = v1.crossProduct(v0);
        double ai = normal.getX();
        double bi = normal.getY();
        if (normal.getZ() < 0) {
            ai = -ai;
            bi = -bi;
        }
//        System.out.println(ai + ":" + bi);
        forceLine.add(new Vector2D(x, y));
        forceLine.add(new Vector2D(x + ai, y + bi));
    }

    public boolean containsPoint(double x, double y) {
        double dx = x - x3;
        double dy = y - y3;
        double a = y23 * dx + x32 * dy;
        if (a < minD || a > maxD) return false;
        double b = y31 * dx + x13 * dy;
        if (b < minD || b > maxD) return false;
        double c = det - a - b;
        return !(c < minD) && !(c > maxD);
    }
}
