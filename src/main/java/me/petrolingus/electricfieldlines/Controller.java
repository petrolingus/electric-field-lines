package me.petrolingus.electricfieldlines;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import me.petrolingus.electricfieldlines.util.Point3d;
import me.petrolingus.electricfieldlines.util.Triangle;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.*;
import org.jzy3d.chart.AWTChart;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.javafx.JavaFXChartFactory;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.primitives.CompileableComposite;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Controller {

    @FXML
    private Canvas canvas;

    @FXML
    private Pane plot3d;

    private static final double RADIUS = 0.3;
    private static final double SHIFT = 0.2;
    private static final double ANGLE = 0.5;
    private static final int N = 32;
    private static final boolean SHOW_TRIANGULATION = false;

    private static final double COS135 = -Math.sqrt(2) / 2;

    public static final List<Triangle> triangles = new ArrayList<>();
    public static final List<Point3d> vertices = new ArrayList<>();

    public double cx = 0.1;
    public double cy = 0.45;
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    double outerQ = 9;
    double innerQ = -9;

    int outerEdgePoints = 0;
    int innerEdgePoints = 0;

    Random random = new Random();

    public static final List<Point3d> points = new ArrayList<>();

    private ImageView imageView;
    private AWTChart chart;
    private JavaFXChartFactory factory;

    public void initialize() {
        generationOfPoints();
        triangulation();
        process();
        draw();

        // Jzy3d
        factory = new JavaFXChartFactory();
        chart  = getDemoChart(factory, "offscreen");
        imageView = factory.bindImageView(chart);
        plot3d.getChildren().add(imageView);
        factory.addSceneSizeChangedListener(chart, plot3d);
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
            outerEdgePoints++;
        }

        // Add inner edge points
        for (int i = indexInner135xLeft; i < indexInner135xRight; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(RADIUS * RADIUS - (a - cx) * (a - cx));
            points.add(new Point3d(a, -b + cy, 0, true, innerQ));
            points.add(new Point3d(a, b + cy, 0, true, innerQ));
            innerEdgePoints++;
        }
        for (int i = indexInner135yLeft; i < indexInner135yRight; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(RADIUS * RADIUS - (a - cy) * (a - cy));
            points.add(new Point3d(b + cx, a, 0, true, innerQ));
            points.add(new Point3d(-b + cx, a, 0, true, innerQ));
            innerEdgePoints++;
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

        // USE FOR DEBUG NEIGHBOURS
        for (int i = 0; i < triangles.size(); i++) {
            Triangle t = triangles.get(i);
            int a = t.getRaid();
            int b = t.getRbid();
            int c = t.getRcid();

            Point3d p0 = points.get(a);
            boolean cond0 = false;
            for (Integer index0 : p0.getTriangleList()) {
                cond0 |= index0 == i;
            }

            Point3d p1 = points.get(b);
            boolean cond1 = false;
            for (Integer index0 : p1.getTriangleList()) {
                cond1 |= index0 == i;
            }

            Point3d p2 = points.get(c);
            boolean cond2 = false;
            for (Integer index0 : p2.getTriangleList()) {
                cond2 |= index0 == i;
            }

            if (!(cond0 && cond1 && cond2)) {
                System.err.println("WRONG NEIGHBOURS!");
                System.exit(-1);
            }
        }

        // WHAT THE FAQ HAPPEN THESE
        double[][] A = new double[whitePointsCount][whitePointsCount];
        for (int i = 0; i < whitePointsCount; i++) {

            for (int j = 0; j < whitePointsCount; j++) {

                double value = 0;

                List<Integer> tempTriangles = new ArrayList<>();

                if (i == j) {

                    for (int k = 0; k < triangles.size(); k++) {
                        Triangle triangle = triangles.get(k);
                        if (triangle.getRaid() == i || triangle.getRbid() == i || triangle.getRcid() == i) {
                            tempTriangles.add(k);
                        }
                    }

                    for (Integer triangleIndex : tempTriangles) {
                        Triangle triangle = triangles.get(triangleIndex);

                        int ida = triangle.getRaid();
                        int idb = triangle.getRbid();
                        int idc = triangle.getRcid();

                        Point3d a = points.get(ida);
                        Point3d b = points.get(idb);
                        Point3d c = points.get(idc);

                        if (ida == i) {
                            a = new Point3d(a.x(), a.y(), 1.0);
                        } else if (idb == i) {
                            b = new Point3d(b.x(), b.y(), 1.0);
                        } else if (idc == i) {
                            c = new Point3d(c.x(), c.y(), 1.0);
                        } else {
                            System.err.println("TRIANGLE IN 2D PLANE");
                            System.exit(-1);
                        }

                        Vector3D v0 = new Vector3D(b.x() - a.x(), b.y() - a.y(), b.z() - a.z());
                        Vector3D v1 = new Vector3D(c.x() - a.x(), c.y() - a.y(), c.z() - a.z());
                        Vector3D normal = v1.crossProduct(v0);
                        double ai = normal.getX();
                        double bi = normal.getY();
                        double s =  normal.getNorm() / 2.0;
                        value += s * (ai * ai + bi * bi);
                    }

                } else {

                    for (int k = 0; k < triangles.size(); k++) {
                        Triangle triangle = triangles.get(k);
                        boolean statement =
                                (triangle.getRaid() == i && triangle.getRbid() == j) ||
                                        (triangle.getRaid() == i && triangle.getRcid() == j) ||
                                        (triangle.getRbid() == i && triangle.getRaid() == j) ||
                                        (triangle.getRbid() == i && triangle.getRcid() == j) ||
                                        (triangle.getRcid() == i && triangle.getRaid() == j) ||
                                        (triangle.getRcid() == i && triangle.getRbid() == j);
                        if (statement) {
                            tempTriangles.add(k);
                        }
                    }

                    if (tempTriangles.size() != 0) {

                        if (tempTriangles.size() != 2) {
                            System.err.println("[matrix A]: i != j and neighbours triangle count not equal two");
                        }

                        for (Integer triangleIndex : tempTriangles) {

                            Triangle triangle = triangles.get(triangleIndex);

                            int ida = triangle.getRaid();
                            int idb = triangle.getRbid();
                            int idc = triangle.getRcid();

                            Point3d a = points.get(ida);
                            Point3d b = points.get(idb);
                            Point3d c = points.get(idc);

                            Point3d pt1 = new Point3d(a.x(), a.y(), a.z());
                            Point3d pt2 = new Point3d(b.x(), b.y(), b.z());
                            Point3d pt3 = new Point3d(c.x(), c.y(), c.z());

                            if (ida == i) {
                                if (idb == j) {
                                    pt1 = new Point3d(a.x(), a.y(), a.z());
                                    pt2 = new Point3d(b.x(), b.y(), b.z());
                                    pt3 = new Point3d(c.x(), c.y(), c.z());
                                }
                                if (idc == j) {
                                    pt1 = new Point3d(a.x(), a.y(), a.z());
                                    pt2 = new Point3d(c.x(), c.y(), c.z());
                                    pt3 = new Point3d(b.x(), b.y(), b.z());
                                }
                            }
                            if (idb == i) {
                                if (ida == j) {
                                    pt1 = new Point3d(b.x(), b.y(), b.z());
                                    pt2 = new Point3d(a.x(), a.y(), a.z());
                                    pt3 = new Point3d(c.x(), c.y(), c.z());
                                }
                                if (idc == j) {
                                    pt1 = new Point3d(b.x(), b.y(), b.z());
                                    pt2 = new Point3d(c.x(), c.y(), c.z());
                                    pt3 = new Point3d(a.x(), a.y(), a.z());
                                }
                            }
                            if (idc == i) {
                                if (idb == j) {
                                    pt1 = new Point3d(c.x(), c.y(), c.z());
                                    pt2 = new Point3d(b.x(), b.y(), b.z());
                                    pt3 = new Point3d(a.x(), a.y(), a.z());
                                }
                                if (ida == j) {
                                    pt1 = new Point3d(c.x(), c.y(), c.z());
                                    pt2 = new Point3d(a.x(), a.y(), a.z());
                                    pt3 = new Point3d(b.x(), b.y(), b.z());
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

                            value += s * (ai * aj + bi * bj);
                        }
                    }

                }

                A[i][j] = value;

            }
        }

        double[] B = new double[whitePointsCount];
        for (int i = 0; i < whitePointsCount; i++) {
            double value = 0;
            for (int j = 0; j < redPointsCount; j++) {

                if (true) {

                    List<Integer> tempTriangles = new ArrayList<>();
                    for (int k = 0; k < triangles.size(); k++) {
                        Triangle triangle = triangles.get(k);
                        boolean statement = (triangle.getRaid() == i && triangle.getRbid() == j + whitePointsCount) ||
                                (triangle.getRaid() == i && triangle.getRcid() == j + whitePointsCount) ||
                                (triangle.getRbid() == i && triangle.getRaid() == j + whitePointsCount) ||
                                (triangle.getRbid() == i && triangle.getRcid() == j + whitePointsCount) ||
                                (triangle.getRcid() == i && triangle.getRaid() == j + whitePointsCount) ||
                                (triangle.getRcid() == i && triangle.getRbid() == j + whitePointsCount);
                        if (statement) {
                            tempTriangles.add(k);
                        }
                    }

                    if (tempTriangles.size() != 0) {

                        if (tempTriangles.size() != 2) {
                            System.err.println("[vector B]: i != j and neighbours triangle count not equal two");
                        }

                        for (Integer triangleIndex : tempTriangles) {

                            Triangle triangle = triangles.get(triangleIndex);

                            int ida = triangle.getRaid();
                            int idb = triangle.getRbid();
                            int idc = triangle.getRcid();

                            Point3d a = points.get(ida);
                            Point3d b = points.get(idb);
                            Point3d c = points.get(idc);

                            Point3d pt1 = new Point3d(a.x(), a.y(), a.z());
                            Point3d pt2 = new Point3d(b.x(), b.y(), b.z());
                            Point3d pt3 = new Point3d(c.x(), c.y(), c.z());

                            if (ida == i) {
                                if (idb == j + whitePointsCount) {
                                    pt1 = new Point3d(a.x(), a.y(), a.z());
                                    pt2 = new Point3d(b.x(), b.y(), b.z());
                                    pt3 = new Point3d(c.x(), c.y(), c.z());
                                }
                                if (idc == j + whitePointsCount) {
                                    pt1 = new Point3d(a.x(), a.y(), a.z());
                                    pt2 = new Point3d(c.x(), c.y(), c.z());
                                    pt3 = new Point3d(b.x(), b.y(), b.z());
                                }
                            }
                            if (idb == i) {
                                if (ida == j + whitePointsCount) {
                                    pt1 = new Point3d(b.x(), b.y(), b.z());
                                    pt2 = new Point3d(a.x(), a.y(), a.z());
                                    pt3 = new Point3d(c.x(), c.y(), c.z());
                                }
                                if (idc == j + whitePointsCount) {
                                    pt1 = new Point3d(b.x(), b.y(), b.z());
                                    pt2 = new Point3d(c.x(), c.y(), c.z());
                                    pt3 = new Point3d(a.x(), a.y(), a.z());
                                }
                            }
                            if (idc == i) {
                                if (idb == j + whitePointsCount) {
                                    pt1 = new Point3d(c.x(), c.y(), c.z());
                                    pt2 = new Point3d(b.x(), b.y(), b.z());
                                    pt3 = new Point3d(a.x(), a.y(), a.z());
                                }
                                if (ida == j + whitePointsCount) {
                                    pt1 = new Point3d(c.x(), c.y(), c.z());
                                    pt2 = new Point3d(a.x(), a.y(), a.z());
                                    pt3 = new Point3d(b.x(), b.y(), b.z());
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

                            value += s * (ai * aj + bi * bj);

                            value += points.get(j + whitePointsCount).getValue() * s * (ai * aj + bi * bj);
                        }
                    }
                }
            }
            B[i] = value * (-1);
        }

        RealMatrix coefficients = new Array2DRowRealMatrix(A, false);
        DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();

        RealVector constants = new ArrayRealVector(B, false);
        RealVector solution = solver.solve(constants);

        System.out.println("Sol. Dim: " + solution.getDimension());

        min = solution.getMinValue();
        max = solution.getMaxValue();

        System.out.println("MIN: " + min);
        System.out.println("MAX: " + max);

        for (int i = 0; i < whitePointsCount; i++) {
            double value = valueMapper(solution.getEntry(i), min, max);
            points.get(i).setValue(value);
        }

        for (int i = whitePointsCount; i < points.size(); i++) {
            double value = valueMapper(points.get(i).getValue(), min, max);
            points.get(i).setValue(value);
        }
    }

    double valueMapper(double value, double min, double max) {
        return ((value - min) / (max - min));
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
        if (SHOW_TRIANGULATION) {
            graphicsContext.setStroke(Color.GREEN);
            graphicsContext.setLineWidth(0.5 * (1 / zoom));
            for (Triangle t : triangles) {
                graphicsContext.setStroke(Objects.requireNonNullElse(t.neighColor, Color.GREEN));
                Point3d a = points.get(t.getRaid());
                Point3d b = points.get(t.getRbid());
                Point3d c = points.get(t.getRcid());
                graphicsContext.strokeLine(a.x(), a.y(), b.x(), b.y());
                graphicsContext.strokeLine(b.x(), b.y(), c.x(), c.y());
                graphicsContext.strokeLine(c.x(), c.y(), a.x(), a.y());
            }
        }

        // Draw points
        double r = 0.01;
        for (Point3d p : points) {
            double value = p.getValue();

            if (value > 0.5) {
                graphicsContext.setFill(Color.BLACK.interpolate(Color.RED, 2 * (p.getValue() - 0.5)));
            } else {
                graphicsContext.setFill(Color.BLUE.interpolate(Color.BLACK, 2 * p.getValue()));
            }

//            graphicsContext.setFill(Color.BLUE.interpolate(Color.RED, value));

            graphicsContext.fillOval(p.x() - r, p.y() - r, 2 * r, 2 * r);
        }

        graphicsContext.restore();

    }

    private AWTChart getDemoChart(JavaFXChartFactory factory, String toolkit) {

        List<Coord3d> cord3dList = new ArrayList<>();
        for (Point3d p : points) {
            double x = p.x();
            double y = p.y();
            double z = p.getValue();
            cord3dList.add(new Coord3d(x, y, z));
        }

        final Shape surface = Builder.buildDelaunay(cord3dList);
        surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new org.jzy3d.colors.Color(1, 1, 1, 1.0f)));
        surface.setFaceDisplayed(true);
        surface.setWireframeColor(new org.jzy3d.colors.Color(0, 0, 0, 0.5f));
        surface.setWireframeDisplayed(true);

        // -------------------------------
        // Create a chart
        Quality quality = Quality.Nicest;
        quality.setSmoothPolygon(true);
//        quality.setAnimated(true);

        // let factory bind mouse and keyboard controllers to JavaFX node
        AWTChart chart = (AWTChart) factory.newChart(quality, toolkit);
        chart.getScene().getGraph().add(surface);

        return chart;
    }

}