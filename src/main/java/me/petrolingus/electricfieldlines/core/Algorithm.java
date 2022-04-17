package me.petrolingus.electricfieldlines.core;

import me.petrolingus.electricfieldlines.core.configuration.Configuration;
import me.petrolingus.electricfieldlines.measure.Timer;
import me.petrolingus.electricfieldlines.util.Point;
import me.petrolingus.electricfieldlines.util.Triangle;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.linear.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Algorithm {

    public static enum Type {
        LU,
        QR,
        RRQR
    }

    private final List<Point> points;
    private final List<Triangle> triangles;
    private final Configuration configuration;
    private final int isolineCount;
    private final int forceLineCount;

    private Type type;

    public Algorithm(Configuration configuration, List<Point> points, List<Triangle> triangles, int isolineCount, int forceLineCount, Type type) {
        this.configuration = configuration;
        this.points = points;
        this.triangles = triangles;
        this.isolineCount = isolineCount;
        this.forceLineCount = forceLineCount;
        this.type = type;
    }

    public void process() {

        int whitePointsCount = (int) points.stream().filter(Point::isNotEdge).count();

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
        timer.measure("\t", "generateA-part1");
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

                    double ai = (pt2.y() - pt1.y()) * (-1) - (pt3.y() - pt1.y()) * (-1);
                    double bi = (pt3.x() - pt1.x()) * (-1) - (pt2.x() - pt1.x()) * (-1);
                    double ci = (pt3.x() - pt1.x()) * (pt2.y() - pt1.y()) - (pt3.y() - pt1.y()) * (pt2.x() - pt1.x());
                    double s = Math.sqrt(ai * ai + bi * bi + ci * ci) / 2.0;

                    double aj = (pt2.y() - pt1.y()) * (0) - (pt3.y() - pt1.y()) * (1);
                    double bj = (pt3.x() - pt1.x()) * (1) - (pt2.x() - pt1.x()) * (0);

//                    Vector3D p21i = new Vector3D(pt2.x() - pt1.x(), pt2.y() - pt1.y(), -1);
//                    Vector3D p31i = new Vector3D(pt3.x() - pt1.x(), pt3.y() - pt1.y(), -1);
//                    Vector3D vi = p21i.crossProduct(p31i);
//                    double ai = vi.getX();
//                    double bi = vi.getY();
//
//                    Vector3D p21j = new Vector3D(pt2.x() - pt1.x(), pt2.y() - pt1.y(), 1);
//                    Vector3D p31j = new Vector3D(pt3.x() - pt1.x(), pt3.y() - pt1.y(), 0);
//                    Vector3D vj = p21j.crossProduct(p31j);
//                    double aj = vj.getX();
//                    double bj = vj.getY();

//                    double s = vi.getNorm() / 2.0;

                    A[i][j] += s * (ai * aj + bi * bj);
                }
            }
        }
        timer.measure("\t", "generateA-part2");

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
        System.out.println("vectorB size: " + B.length);
        timer.measure("\t", "generateB");

        // Searching of solution
        RealMatrix coefficients = new Array2DRowRealMatrix(A, false);
        DecompositionSolver solver = switch (type) {
            case LU -> new LUDecomposition(coefficients).getSolver();
            case QR -> new QRDecomposition(coefficients).getSolver();
            case RRQR -> new RRQRDecomposition(coefficients).getSolver();
        };
        RealVector constants = new ArrayRealVector(B, false);
        RealVector solution = solver.solve(constants);
        timer.measure("\t", "findSolution");

        // Mapping solution to points value
        double min = solution.getMinValue();
        double max = solution.getMaxValue();
        for (int i = 0; i < whitePointsCount; i++) {
            double value = valueMapper(solution.getEntry(i), -1, 1);
            points.get(i).setValue(value);
        }

        for (int i = whitePointsCount; i < points.size(); i++) {
            double value = valueMapper(points.get(i).getValue(), -1, 1);
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
//        for (int i = 0; i < 10_000; i++) {
//            double x = ThreadLocalRandom.current().nextDouble(-1, 1);
//            double y = ThreadLocalRandom.current().nextDouble(-1, 1);
//            Triangle triangle = null;
//            boolean flag = true;
//            while (flag) {
//                x = ThreadLocalRandom.current().nextDouble(-1, 1);
//                y = ThreadLocalRandom.current().nextDouble(-1, 1);
//                for (Triangle t : triangles) {
//                    if (t.containsPoint(x, y)) {
//                        triangle = t;
//                        flag = false;
//                        break;
//                    }
//                }
//            }
//            triangle.createForceLine(x, y);
//        }

        List<Point> innerBound = configuration.getInnerBound(forceLineCount);
        for (Point p : innerBound) {
            double x = p.x();
            double y = p.y();
            Vector2D end = new Vector2D(x, y);
            while (end.getNorm() < configuration.getOuterBoundDistance()) {
                Vector2D finalEnd = end;
                Triangle triangle = triangles.stream().parallel().filter(t -> t.containsPoint(finalEnd.getX(), finalEnd.getY())).findFirst().orElse(null);
                if (triangle != null) {
                    end = triangle.createForceLine(end.getX(), end.getY());
                } else {
                    break;
                }
            }
        }
        timer.measure("\t", "createForceLine");
    }

    private double valueMapper(double value, double min, double max) {
        return ((value - min) / (max - min));
    }

    public double[] methodKaczmarz(double[][] a, double[] b) {

        Timer timer = new Timer();

        int n = b.length;

        double[] tempRow = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                tempRow[i] += a[i][j] * a[i][j];
            }
        }

        double[] x = new double[n];
        double eps = 1e-5;
        double s1, s2;
        double[] x1 = new double[n];

        x[0] = 0.5;
        for (int i = 1; i < n; i++) {
            x[i] = 0.0;
        }

        double sumA = 0;
        double sumB = 0;
        double sumC = 0;
        int count = 0;

        s1 = s2 = 1;
        while (s1 > eps * s2) {


            long startA = System.nanoTime();
            for (int i = 0; i < n; i++) {
                x1[i] = x[i];
            }
            sumA += System.nanoTime() - startA;

            long startB = System.nanoTime();
            for (int i = 0; i < n; i++) {
                s1 = 0;
                s2 = tempRow[i];
                for (int j = 0; j < n; j++) {
                    double fa1 = a[i][j];
                    s1 += fa1 * x[j];
                }
                double t = (b[i] - s1) / s2;
                for (int j = 0; j < n; j++) {
                    x[j] += a[i][j] * t;
                }
            }
            sumB += System.nanoTime() - startB;

            long startC = System.nanoTime();
            s1 = 0;
            s2 = 0;
            for (int i = 0; i < n; i++) {
                s1 += (x[i] - x1[i]) * (x[i] - x1[i]);
                s2 += x[i] * x[i];
            }
            s1 = Math.sqrt(s1);
            s2 = Math.sqrt(s2);
            sumC += System.nanoTime() - startC;

            count++;
        }

        System.out.println("\t\t" + sumA / 1_000_000 + " ms [partA]");
        System.out.println("\t\t" + sumB / 1_000_000 + " ms [partB]");
        System.out.println("\t\t" + sumC / 1_000_000 + " ms [partC]");
        System.out.println("\t\t" + count + " count");

        timer.measure("\t\t", "innerFindSolution");

        return x;
    }

}
