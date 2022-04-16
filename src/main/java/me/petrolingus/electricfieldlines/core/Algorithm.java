package me.petrolingus.electricfieldlines.core;

import me.petrolingus.electricfieldlines.measure.Timer;
import me.petrolingus.electricfieldlines.util.Point;
import me.petrolingus.electricfieldlines.util.Triangle;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Algorithm {

    private final List<Point> points;
    private final List<Triangle> triangles;
    private final int isolineCount;
    private final int forceLineCount;

    public Algorithm(List<Point> points, List<Triangle> triangles, int isolineCount, int forceLineCount) {
        this.points = points;
        this.triangles = triangles;
        this.isolineCount = isolineCount;
        this.forceLineCount = forceLineCount;
    }

    public void process() {

        int whitePointsCount = (int) points.stream().filter(Point::isNotEdge).count();
        System.out.println(whitePointsCount);

        Timer timer = new Timer();

        // Link points with triangles
        for (int i = 0; i < points.size(); i++) {
            List<Integer> dependencyTriangles = new ArrayList<>();
            for (int j = 0; j < triangles.size(); j++) {
                Triangle t = triangles.get(j);
                int indexV0 = t.getIndexA();
                int indexV1 = t.getIndexB();
                int indexV2 = t.getIndexC();
                boolean cond = (i == indexV0) || (i == indexV1) || (i == indexV2);
                if (cond) {
                    dependencyTriangles.add(j);
                }
            }
            points.get(i).setTriangleList(dependencyTriangles);
        }
        timer.measure("\t", "link");

        double[][] A = new double[whitePointsCount][whitePointsCount];
        for (int i = 0; i < whitePointsCount; i++) {
            for (Integer triangleIndex : points.get(i).getTriangleList()) {

                Triangle triangle = triangles.get(triangleIndex);

                int ida = triangle.getIndexA();
                int idb = triangle.getIndexB();
                int idc = triangle.getIndexC();

                Point a = points.get(ida);
                Point b = points.get(idb);
                Point c = points.get(idc);

                if (ida == i) {
                    a = new Point(a.x(), a.y(), 1.0);
                } else if (idb == i) {
                    b = new Point(b.x(), b.y(), 1.0);
                } else if (idc == i) {
                    c = new Point(c.x(), c.y(), 1.0);
                } else {
                    System.err.println("TRIANGLE IN 2D PLANE");
                    System.exit(-1);
                }

                Vector3D v0 = new Vector3D(b.x() - a.x(), b.y() - a.y(), b.z() - a.z());
                Vector3D v1 = new Vector3D(c.x() - a.x(), c.y() - a.y(), c.z() - a.z());
                Vector3D normal = v1.crossProduct(v0);
                double ai = normal.getX();
                double bi = normal.getY();
                double s = normal.getNorm() / 2.0;
                A[i][i] += s * (ai * ai + bi * bi);
            }
        }
        for (int i = 0; i < whitePointsCount; i++) {

            List<Integer> iTriangles = points.get(i).getTriangleList();

            for (int j = 0; j < whitePointsCount; j++) {

                if (i == j) continue;

                List<Integer> jTriangles = points.get(j).getTriangleList();

                List<Integer> tempTriangles = iTriangles.stream().filter(jTriangles::contains).toList();

                if (tempTriangles.size() == 0) continue;

                for (Integer triangleIndex : tempTriangles) {

                    Triangle triangle = triangles.get(triangleIndex);

                    int ida = triangle.getIndexA();
                    int idb = triangle.getIndexB();
                    int idc = triangle.getIndexC();

                    Point a = points.get(ida);
                    Point b = points.get(idb);
                    Point c = points.get(idc);

                    Point pt1 = new Point(a.x(), a.y(), a.z());
                    Point pt2 = new Point(b.x(), b.y(), b.z());
                    Point pt3 = new Point(c.x(), c.y(), c.z());

                    if (ida == i) {
                        if (idb == j) {
                            pt1 = new Point(a.x(), a.y(), a.z());
                            pt2 = new Point(b.x(), b.y(), b.z());
                            pt3 = new Point(c.x(), c.y(), c.z());
                        }
                        if (idc == j) {
                            pt1 = new Point(a.x(), a.y(), a.z());
                            pt2 = new Point(c.x(), c.y(), c.z());
                            pt3 = new Point(b.x(), b.y(), b.z());
                        }
                    }
                    if (idb == i) {
                        if (ida == j) {
                            pt1 = new Point(b.x(), b.y(), b.z());
                            pt2 = new Point(a.x(), a.y(), a.z());
                            pt3 = new Point(c.x(), c.y(), c.z());
                        }
                        if (idc == j) {
                            pt1 = new Point(b.x(), b.y(), b.z());
                            pt2 = new Point(c.x(), c.y(), c.z());
                            pt3 = new Point(a.x(), a.y(), a.z());
                        }
                    }
                    if (idc == i) {
                        if (idb == j) {
                            pt1 = new Point(c.x(), c.y(), c.z());
                            pt2 = new Point(b.x(), b.y(), b.z());
                            pt3 = new Point(a.x(), a.y(), a.z());
                        }
                        if (ida == j) {
                            pt1 = new Point(c.x(), c.y(), c.z());
                            pt2 = new Point(a.x(), a.y(), a.z());
                            pt3 = new Point(b.x(), b.y(), b.z());
                        }
                    }

                    Vector3D p21i = new Vector3D(pt2.x() - pt1.x(), pt2.y() - pt1.y(), -1);
                    Vector3D p31i = new Vector3D(pt3.x() - pt1.x(), pt3.y() - pt1.y(), -1);
                    Vector3D vi = p21i.crossProduct(p31i);
                    double ai = vi.getX();
                    double bi = vi.getY();

                    Vector3D p21j = new Vector3D(pt2.x() - pt1.x(), pt2.y() - pt1.y(), 1);
                    Vector3D p31j = new Vector3D(pt3.x() - pt1.x(), pt3.y() - pt1.y(), 0);
                    Vector3D vj = p21j.crossProduct(p31j);
                    double aj = vj.getX();
                    double bj = vj.getY();

                    double s = vi.getNorm() / 2.0;

                    A[i][j] += s * (ai * aj + bi * bj);
                }
            }
        }
        timer.measure("\t", "generateA");

        double[] B = new double[whitePointsCount];
        for (int i = 0; i < whitePointsCount; i++) {
            List<Integer> iTriangles = points.get(i).getTriangleList();
            for (int j = whitePointsCount; j < points.size(); j++) {
                List<Integer> jTriangles = points.get(j).getTriangleList();
                List<Integer> tempTriangles = iTriangles.stream().filter(jTriangles::contains).toList();
                if (tempTriangles.size() == 0) continue;
                for (Integer triangleIndex : tempTriangles) {

                    Triangle triangle = triangles.get(triangleIndex);

                    int ida = triangle.getIndexA();
                    int idb = triangle.getIndexB();
                    int idc = triangle.getIndexC();

                    Point a = points.get(ida);
                    Point b = points.get(idb);
                    Point c = points.get(idc);

                    Point pt1 = new Point(a.x(), a.y(), a.z());
                    Point pt2 = new Point(b.x(), b.y(), b.z());
                    Point pt3 = new Point(c.x(), c.y(), c.z());

                    if (ida == i) {
                        if (idb == j) {
                            pt1 = new Point(a.x(), a.y(), a.z());
                            pt2 = new Point(b.x(), b.y(), b.z());
                            pt3 = new Point(c.x(), c.y(), c.z());
                        }
                        if (idc == j) {
                            pt1 = new Point(a.x(), a.y(), a.z());
                            pt2 = new Point(c.x(), c.y(), c.z());
                            pt3 = new Point(b.x(), b.y(), b.z());
                        }
                    }
                    if (idb == i) {
                        if (ida == j) {
                            pt1 = new Point(b.x(), b.y(), b.z());
                            pt2 = new Point(a.x(), a.y(), a.z());
                            pt3 = new Point(c.x(), c.y(), c.z());
                        }
                        if (idc == j) {
                            pt1 = new Point(b.x(), b.y(), b.z());
                            pt2 = new Point(c.x(), c.y(), c.z());
                            pt3 = new Point(a.x(), a.y(), a.z());
                        }
                    }
                    if (idc == i) {
                        if (idb == j) {
                            pt1 = new Point(c.x(), c.y(), c.z());
                            pt2 = new Point(b.x(), b.y(), b.z());
                            pt3 = new Point(a.x(), a.y(), a.z());
                        }
                        if (ida == j) {
                            pt1 = new Point(c.x(), c.y(), c.z());
                            pt2 = new Point(a.x(), a.y(), a.z());
                            pt3 = new Point(b.x(), b.y(), b.z());
                        }
                    }

                    Vector3D p21i = new Vector3D(pt2.x() - pt1.x(), pt2.y() - pt1.y(), -1);
                    Vector3D p31i = new Vector3D(pt3.x() - pt1.x(), pt3.y() - pt1.y(), -1);
                    Vector3D vi = p21i.crossProduct(p31i);
                    double ai = vi.getX();
                    double bi = vi.getY();

                    Vector3D p21j = new Vector3D(pt2.x() - pt1.x(), pt2.y() - pt1.y(), 1);
                    Vector3D p31j = new Vector3D(pt3.x() - pt1.x(), pt3.y() - pt1.y(), 0);
                    Vector3D vj = p21j.crossProduct(p31j);
                    double aj = vj.getX();
                    double bj = vj.getY();

                    double s = vi.getNorm() / 2.0;

                    B[i] -= points.get(j).getValue() * s * (ai * aj + bi * bj);
                }
            }
        }
        timer.measure("\t", "generateB");

        // Searching of solution
        RealMatrix coefficients = new Array2DRowRealMatrix(A, false);
        DecompositionSolver solver = new QRDecomposition(coefficients).getSolver();
        RealVector constants = new ArrayRealVector(B, false);
        RealVector solution = solver.solve(constants);
        timer.measure("\t", "findSolution");

        // Mapping solution to points value
        double min = solution.getMinValue();
        double max = solution.getMaxValue();
        for (int i = 0; i < whitePointsCount; i++) {
            double value = valueMapper(solution.getEntry(i), min, max);
            points.get(i).setValue(value);
        }
        for (int i = whitePointsCount; i < points.size(); i++) {
            double value = valueMapper(points.get(i).getValue(), min, max);
            points.get(i).setValue(value);
        }
        timer.measure("\t", "mappingSolution");

        // Creating isoline
        for (Triangle t : triangles) {
            Point a = points.get(t.getIndexA());
            Point b = points.get(t.getIndexB());
            Point c = points.get(t.getIndexC());
            t.createIsoline(a, b, c, isolineCount);
        }
        timer.measure("\t", "createIsoline");

        // Create force line
        for (int i = 0; i < 1000; i++) {
            double x = ThreadLocalRandom.current().nextDouble(-1, 1);
            double y = ThreadLocalRandom.current().nextDouble(-1, 1);
            Triangle triangle = null;
            boolean flag = true;
            while (flag) {
                x = ThreadLocalRandom.current().nextDouble(-1, 1);
                y = ThreadLocalRandom.current().nextDouble(-1, 1);
                for (Triangle t : triangles) {
                    if (t.containsPoint(x, y)) {
                        triangle = t;
                        flag = false;
                        break;
                    }
                }
            }
            triangle.createForceLine(x, y);
        }
        timer.measure("\t", "createForceLine");
    }

    private double valueMapper(double value, double min, double max) {
        return ((value - min) / (max - min));
    }

}
