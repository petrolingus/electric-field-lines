package me.petrolingus.electricfieldlines;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import me.petrolingus.electricfieldlines.core.DataGenerator;
import me.petrolingus.electricfieldlines.core.Triangulation;
import me.petrolingus.electricfieldlines.measure.Timer;
import me.petrolingus.electricfieldlines.util.Isoline;
import me.petrolingus.electricfieldlines.util.Point;
import me.petrolingus.electricfieldlines.util.Triangle;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Controller {

    // Configuration
    public CheckBox configurationShowCheckBox;
    public Slider configurationInnerRadiusSlider;
    public Slider configurationCenterShiftSlider;
    public Slider configurationClockwiseAngleSlider;

    // Triangulation
    public CheckBox triangulationCheckBox;
    public Slider pointsSlider;

    // Isoline
    public CheckBox isolineCheckBox;
    public Slider isolineCountSlider;

    // Field line
    public CheckBox fieldLineCheckBox;
    public Slider fieldLineCountSlider;

    // Solution
    public CheckBox pointsCheckBox;

    // Scene
    public Canvas canvas;


    // Solution config
    public static Vector3D config;
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;

    // Free shared collections
    public static List<Point> points;
    public static List<Triangle> triangles;


    public void initialize() {
        calcConfig();

        setupSlider(configurationInnerRadiusSlider);
        setupSlider(configurationCenterShiftSlider);
        setupSlider(configurationClockwiseAngleSlider);

        setupCheckBox(configurationShowCheckBox);
        setupCheckBox(triangulationCheckBox);
        setupCheckBox(pointsCheckBox);
        setupCheckBox(isolineCheckBox);

        draw();
    }

    private void calcConfig() {
        double radius = 0.2 + 0.7 * configurationInnerRadiusSlider.getValue();
        double shift = (1 - radius) * (0.8 * configurationCenterShiftSlider.getValue());
        double angle = 2 * Math.PI * configurationClockwiseAngleSlider.getValue();
        double x = (shift) * Math.cos(angle);
        double y = (shift) * Math.sin(angle);
        config = new Vector3D(x, -y, radius);
    }

    private void setupSlider(Slider slider) {
        slider.valueProperty().addListener((changed, oldValue, newValue) -> {
            calcConfig();
            draw();
        });
    }

    private void setupCheckBox(CheckBox checkBox) {
        checkBox.selectedProperty().addListener((value) -> {
            draw();
        });
    }

    public void onFindSolution() {

        if (triangles != null) {
            triangles.clear();
            min = Double.POSITIVE_INFINITY;
            max = Double.NEGATIVE_INFINITY;
        }

        if (points != null) {
            points.clear();
        }

        Timer timer = new Timer();

        generationOfPoints();
        timer.measure("generationOfPoints");

        triangulation();
        timer.measure("triangulation");

        process();
        timer.measure("process");

        draw();
        timer.measure("draw");

        System.out.println("######################################################################");
    }

    private void generationOfPoints() {
        int n = (int) pointsSlider.getValue();
        double outerCharge = 1;
        double innerCharge = -1;
        double cx = config.getX();
        double cy = config.getY();
        double radius = config.getZ();
        DataGenerator generator = new DataGenerator(n, outerCharge, innerCharge, cx, cy, radius);
        points = generator.generate();
    }

    private void triangulation() {
        Triangulation triangulation = new Triangulation(config.getX(), config.getY());
        triangles = triangulation.create(points);
    }

    private void process() {

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
            List<Integer> iTriangles =  points.get(i).getTriangleList();
            for (int j = whitePointsCount; j < points.size(); j++) {
                List<Integer> jTriangles =  points.get(j).getTriangleList();
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
        min = solution.getMinValue();
        max = solution.getMaxValue();
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
        int isolineCount = (int) Math.round(isolineCountSlider.getValue());
        for (Triangle t : triangles) {
            Point a = points.get(t.getIndexA());
            Point b = points.get(t.getIndexB());
            Point c = points.get(t.getIndexC());
            t.createIsoline(a, b, c, isolineCount);
        }
        timer.measure("\t", "createIsoline");

        // Create force line
        for (int i = 0; i < 100_000; i++) {
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
        if (triangles != null && triangulationCheckBox.isSelected()) {
            graphicsContext.setLineWidth(0.5 * (1 / zoom));
            for (Triangle t : triangles) {
                graphicsContext.setStroke(Color.GREEN);
                if (t.color != null) {
                    graphicsContext.setStroke(t.color);
                }
                Point a = points.get(t.getIndexA());
                Point b = points.get(t.getIndexB());
                Point c = points.get(t.getIndexC());
                graphicsContext.strokeLine(a.x(), a.y(), b.x(), b.y());
                graphicsContext.strokeLine(b.x(), b.y(), c.x(), c.y());
                graphicsContext.strokeLine(c.x(), c.y(), a.x(), a.y());
            }
        }

        // Draw points
        if (points != null && pointsCheckBox.isSelected()) {
            double r = 0.01;
            for (Point p : points) {
                double value = p.getValue();
                if (value > 0.5) {
                    graphicsContext.setFill(Color.BLACK.interpolate(Color.RED, 2 * (value - 0.5)));
                } else {
                    graphicsContext.setFill(Color.BLUE.interpolate(Color.BLACK, 2 * value));
                }
                graphicsContext.fillOval(p.x() - r, p.y() - r, 2 * r, 2 * r);
            }
        }

        // Draw configuration
        if (configurationShowCheckBox.isSelected()) {
            graphicsContext.setLineWidth(0.5 * (1 / zoom));
            graphicsContext.setStroke(Color.WHITE);
            graphicsContext.strokeOval(-1, -1, 2, 2);
            double x = config.getX();
            double y = config.getY();
            double r = config.getZ();
            graphicsContext.strokeOval(x - r, y - r, 2 * r, 2 * r);
        }

        // Draw force line
        if (triangles != null) {
            graphicsContext.setLineWidth(0.5 * (1 / zoom));
            graphicsContext.setStroke(Color.LIGHTGREEN);
            for (Triangle t : triangles) {
                if (t.forceLine.isEmpty()) continue;
                for (int i = 0; i < t.forceLine.size() / 2; i += 2) {
                    double x1 = t.forceLine.get(i).getX();
                    double y1 = t.forceLine.get(i).getY();
                    double x2 = t.forceLine.get(i + 1).getX();
                    double y2 = t.forceLine.get(i + 1).getY();
                    graphicsContext.strokeLine(x1, y1, x2, y2);
                }
            }
        }

        // Draw isoline
        if (isolineCheckBox.isSelected() && triangles != null) {
            graphicsContext.setLineWidth(0.8 * (1 / zoom));
            graphicsContext.setStroke(Color.YELLOW);
            for (Triangle t : triangles) {
                if (t.isoline.isEmpty()) {
                    continue;
                }
                for (Isoline isoline : t.isoline) {

//                    double value = isoline.getValue();
//                    if (value > 0.5) {
//                        graphicsContext.setStroke(Color.BLACK.interpolate(Color.RED, 2 * (value - 0.5)));
//                    } else {
//                        graphicsContext.setStroke(Color.BLUE.interpolate(Color.BLACK, 2 * value));
//                    }

                    double x1 = isoline.getX1();
                    double y1 = isoline.getY1();
                    double x2 = isoline.getX2();
                    double y2 = isoline.getY2();
                    graphicsContext.strokeLine(x1, y1, x2, y2);
                }
            }
        }

        graphicsContext.restore();

    }
}