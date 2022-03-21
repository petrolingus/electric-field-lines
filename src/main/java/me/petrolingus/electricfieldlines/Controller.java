package me.petrolingus.electricfieldlines;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import me.petrolingus.electricfieldlines.core.DataGenerator;
import me.petrolingus.electricfieldlines.core.Triangulation;
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
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Controller {

    @FXML
    private Canvas canvas;

    @FXML
    private Pane plot3d;

    private static final boolean SHOW_TRIANGULATION = true;

    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;

    public static List<Point3d> points;
    public static List<Triangle> triangles;

    public void initialize() {
        generationOfPoints();
        triangulation();
        process();
        draw();

        // Jzy3d
        JavaFXChartFactory factory = new JavaFXChartFactory();
        AWTChart chart = getDemoChart(factory, "offscreen");
        ImageView imageView = factory.bindImageView(chart);
        plot3d.getChildren().add(imageView);
        factory.addSceneSizeChangedListener(chart, plot3d);
    }

    private void generationOfPoints() {
        DataGenerator generator = new DataGenerator();
        points = generator.generate();
    }

    private void triangulation() {
        Triangulation triangulation = new Triangulation();
        triangles = triangulation.create(points);
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
                        double s = normal.getNorm() / 2.0;
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