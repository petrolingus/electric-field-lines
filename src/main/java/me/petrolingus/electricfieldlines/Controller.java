package me.petrolingus.electricfieldlines;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import me.petrolingus.electricfieldlines.util.Point;
import me.petrolingus.electricfieldlines.util.Point3d;
import me.petrolingus.electricfieldlines.util.Triangle;
import org.apache.commons.math3.linear.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Controller {

    @FXML
    private Canvas canvas;

    private static final double RADIUS = 0.5;
    private static final double SHIFT = 0.2;
    private static final double ANGLE = 0.5;
    private static final int N = 32;

    private static final double COS135 = -Math.sqrt(2) / 2;

    public static final List<Triangle> triangles = new ArrayList<>();
    public static final List<Point3d> vertices = new ArrayList<>();

    public double cx = 0;
    public double cy = 0;
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    double outerQ = 1;
    double innerQ = 0;

    public static final List<Point3d> points = new ArrayList<>();

    public void initialize() {
        generationOfPoints();
        triangulation();
        process();
        draw();
    }

    private void generationOfPoints() {

        double step = 2.0 / (N - 1);
        int indexOuter135 = -1;
        int indexInner135xLeft = -1;
        int indexInner135xRight = -1;
        int indexInner135yLeft = -1;
        int indexInner135yRight = -1;

        // Add inner points
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                double x = -1 + j * step;
                double y = -1 + i * step;
                double d = Math.sqrt(x * x + y * y);
                double d2 = Math.sqrt(Math.pow(cx - x, 2) + Math.pow(cy - y, 2));
                boolean isOuterContain = d < 1;
                boolean isCloseToOuterCircle = Math.abs(1 - d) < (step / 2);
                boolean isInnerCircleContain = d2 < RADIUS;
                boolean isCloseToInnerCircle = Math.abs(RADIUS - d2) < (step / 2);
                boolean condition = isOuterContain && !isCloseToOuterCircle && !isInnerCircleContain && !isCloseToInnerCircle;
                if (condition) {
                    points.add(new Point3d(x, y, 0));
                }
                indexOuter135 = (indexOuter135 == -1 && x > COS135) ? j : indexOuter135;
                indexInner135xLeft = (indexInner135xLeft == -1 && x > cx + RADIUS * COS135) ? j : indexInner135xLeft;
                indexInner135xRight = (indexInner135xRight == -1 && x > cx - RADIUS * COS135) ? j : indexInner135xRight;
                indexInner135yLeft = (indexInner135yLeft == -1 && y > cy + RADIUS * COS135) ? i : indexInner135yLeft;
                indexInner135yRight = (indexInner135yRight == -1 && y > cy - RADIUS * COS135) ? i : indexInner135yRight;
            }
        }

        // Add outer edge points
        for (int i = indexOuter135; i < N - indexOuter135; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(1 - a * a);
            points.add(new Point3d(a, -b, 0, true, outerQ));
            points.add(new Point3d(a, b, 0, true, outerQ));
            points.add(new Point3d(b, a, 0, true, outerQ));
            points.add(new Point3d(-b, a, 0, true, outerQ));
        }

        // Add inner edge points
        for (int i = indexInner135xLeft; i < indexInner135xRight; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(RADIUS * RADIUS - (a - cx) * (a - cx));
            points.add(new Point3d(a, -b + cy, 0, true, innerQ));
            points.add(new Point3d(a, b + cy, 0, true, innerQ));
        }
        for (int i = indexInner135yLeft; i < indexInner135yRight; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(RADIUS * RADIUS - (a - cy) * (a - cy));
            points.add(new Point3d(b + cx, a, 0, true, innerQ));
            points.add(new Point3d(-b + cx, a, 0, true, innerQ));
        }
    }

    private void methodBowerWatson(Point3d point3d) {

        vertices.add(point3d);

        List<Integer> badTriangles = new ArrayList<>();
        for (int i = 0; i < triangles.size(); i++) {
            if (triangles.get(i).isContain(point3d.x(), point3d.y())) {
                badTriangles.add(i);
                triangles.get(i).setBad(true);
            }
        }

        List<List<Integer>> polygon = new ArrayList<>();

        for (int i = 0; i < badTriangles.size(); i++) {
            List<List<Integer>> edges = triangles.get(badTriangles.get(i)).getEdges();
            for (int j = 0; j < 3; j++) {
                List<Integer> edge = edges.get(j);
                boolean edgeIsNotShared = true;
                for (int k = 0; k < badTriangles.size(); k++) {
                    if (i == k) continue;
                    List<List<Integer>> otherEdges = triangles.get(badTriangles.get(k)).getEdges();
                    for (int w = 0; w < 3; w++) {
                        List<Integer> otherEdge = otherEdges.get(w);
                        boolean c1 = edge.get(0).equals(otherEdge.get(0)) && edge.get(1).equals(otherEdge.get(1));
                        edgeIsNotShared &= !c1;
                    }
                }
                if (edgeIsNotShared) {
                    polygon.add(edge);
                }
            }
        }

        triangles.removeIf(Triangle::isBad);

        for (List<Integer> edge : polygon) {
            triangles.add(new Triangle(edge.get(0), edge.get(1), vertices.size() - 1));
        }
    }

    private void triangulation() {

        Point3d p0 = new Point3d(-1, -1, 0);
        Point3d p1 = new Point3d(1, -1, 0);
        Point3d p2 = new Point3d(1, 1, 0);
        Point3d p3 = new Point3d(-1, 1, 0);
        Point3d p4 = new Point3d(cx, cy, 0);

        vertices.add(p0);
        vertices.add(p1);
        vertices.add(p2);
        vertices.add(p3);

        triangles.add(new Triangle(0, 1, 2));
        triangles.add(new Triangle(0, 2, 3));

        methodBowerWatson(p4);

        for (Point3d p : points) {
            methodBowerWatson(p);
        }

        // Remove super-structure triangles
        triangles.removeIf(t -> {
            boolean res = false;
            for (int i = 0; i < 4; i++) {
                boolean c0 = t.getAid() == i;
                boolean c1 = t.getBid() == i;
                boolean c2 = t.getCid() == i;
                res |= c0 || c1 || c2;
            }
            return res;
        });

        // Remove triangles in inner circle
        triangles.removeIf(t -> {
            boolean c0 = t.getAid() == 4;
            boolean c1 = t.getBid() == 4;
            boolean c2 = t.getCid() == 4;
            return c0 || c1 || c2;
        });

    }

    private void process() {

        System.out.println("Triangles:" + triangles.size());
        System.out.println("Points:" + points.size());

        int whitePointsCount = 0;
        for (Point3d p : points) {
            if (!p.isEdge()) {
                whitePointsCount++;
            }
        }
        System.out.println("WhitePoints:" + whitePointsCount);

        int redPointsCount = points.size() - whitePointsCount;
        System.out.println("RedPoints:" + redPointsCount);

        // Link points with triangles
        for (int i = 0; i < points.size(); i++) {
            List<Integer> dependencyTriangles = new ArrayList<>();
            for (int j = 0; j < triangles.size(); j++) {
                Triangle t = triangles.get(j);
                int indexV0 = t.getRaid();
                int indexV1 = t.getRbid();
                int indexV2 = t.getRcid();
                boolean cond = (i == indexV0) || (i == indexV1) || (i == indexV2);
                if (cond) {
                    dependencyTriangles.add(j);
                }
            }
            points.get(i).setTriangleList(dependencyTriangles);
        }

        // USES FOR DEBUG NEIGHBOURS
//        for (int i = 0; i < triangles.size(); i++) {
//            Triangle t = triangles.get(i);
//            int a = t.getRaid();
//            int b = t.getRbid();
//            int c = t.getRcid();
//
//            Point3d p0 = points.get(a);
//            boolean cond0 = false;
//            for (Integer index0 : p0.getTriangleList()) {
//                cond0 |= index0 == i;
//            }
//
//            Point3d p1 = points.get(b);
//            boolean cond1 = false;
//            for (Integer index0 : p1.getTriangleList()) {
//                cond1 |= index0 == i;
//            }
//
//            Point3d p2 = points.get(c);
//            boolean cond2 = false;
//            for (Integer index0 : p2.getTriangleList()) {
//                cond2 |= index0 == i;
//            }
//
//            if (!(cond0 && cond1 && cond2)) {
//                System.err.println("WRONHG!!!");
//                System.exit(-1);
//            }
//        }

        // WHAT THE FUCK HAPPEN THESE
        double[][] A = new double[whitePointsCount][whitePointsCount];
        for (int i = 0; i < whitePointsCount; i++) {
            Point3d p0 = points.get(i);
            List<Integer> n0 = p0.getTriangleList();
            for (int j = 0; j < whitePointsCount; j++) {
                Point3d p1 = points.get(j);
                List<Integer> n1 = p1.getTriangleList();

                if (i == j) {
                    double value = 0;
                    for (Integer triangleIndex : n0) {
                        double[] ABS = triangles.get(triangleIndex).magicCalc(i);
                        double valueA = ABS[0];
                        double valueB = ABS[1];
                        double valueS = ABS[2];
                        value += (valueA * valueA + valueB * valueB) * valueS;
                    }
                    A[i][j] = -value;

                } else {
                    Set<Integer> neighTrianglesIndices = new HashSet<>();
                    for (Integer index0 : n0) {
                        for (Integer index1 : n1) {
                            if (index0.equals(index1)) {
                                neighTrianglesIndices.add(index0);
                            }
                        }
                    }

                    if (!neighTrianglesIndices.isEmpty()) {

                        if (neighTrianglesIndices.size() != 2) {
                            System.err.println("WFT!!");
                            System.exit(-1);
                        }

                        double value = 0;
                        for (Integer triangleIndex : neighTrianglesIndices) {
                            double[] ABSi = triangles.get(triangleIndex).magicCalc(i);
                            double[] ABSj = triangles.get(triangleIndex).magicCalc(j);
                            value += (ABSi[0] * ABSj[0] + ABSi[1] * ABSj[1]) * ABSi[2];
//                            value += (ABSi[0] * ABSj[0] + ABSj[1] * ABSj[1]) * ABSj[2];
                        }
                        A[i][j] = -value;

                    } else {
                        A[i][j] = 0;
                    }

                }

            }
        }

        double[] B = new double[whitePointsCount];
        for (int i = 0; i < whitePointsCount; i++) {
            double value = 0;
            for (int j = 0; j < redPointsCount; j++) {
                value += points.get(j + whitePointsCount).getValue() * A[j][i];
            }
            B[i] = value;
        }

        RealMatrix coefficients = new Array2DRowRealMatrix(A, false);
        DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();

        RealVector constants = new ArrayRealVector(B, false);
        RealVector solution = solver.solve(constants);

        for (int i = 0; i < whitePointsCount; i++) {
            double value = solution.getEntry(i);
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }

        System.out.println("MIN: " + min);
        System.out.println("MAX: " + max);

        for (int i = 0; i < whitePointsCount; i++) {
            double value = valueMapper(Math.abs(solution.getEntry(i)), min, max, 0, 1);
            // System.out.println(value);
            points.get(i).setValue(value);
        }
    }

    double valueMapper(double value,double smin,double smax,double dmin, double dmax) {
        return ((value-smin) / (smax-smin)) * (dmax-dmin) + dmin;
    }
    
    private void draw() {

        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();

        graphicsContext.save();

        graphicsContext.setFill(Color.BLACK);
        graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        graphicsContext.translate(canvas.getBoundsInLocal().getCenterX(), canvas.getBoundsInLocal().getCenterY());
        double zoom = 0.4 * canvas.getHeight();
        graphicsContext.scale(zoom, zoom);

        // Draw triangles
//        graphicsContext.setStroke(Color.GREEN);
//        graphicsContext.setLineWidth(0.5 * (1 / zoom));
//        for (Triangle t : triangles) {
//            Point3d a = points.get(t.getRaid());
//            Point3d b = points.get(t.getRbid());
//            Point3d c = points.get(t.getRcid());
//            graphicsContext.strokeLine(a.x(), a.y(), b.x(), b.y());
//            graphicsContext.strokeLine(b.x(), b.y(), c.x(), c.y());
//            graphicsContext.strokeLine(c.x(), c.y(), a.x(), a.y());
//        }

        // Draw points
        double r = 0.01;

        for (Point3d p : points) {
            graphicsContext.setFill(Color.WHITE.interpolate(Color.RED, p.getValue()));
            graphicsContext.fillOval(p.x() - r, p.y() - r, 2 * r, 2 * r);
        }

        graphicsContext.restore();

    }

}