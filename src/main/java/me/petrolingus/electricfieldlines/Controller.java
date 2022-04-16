package me.petrolingus.electricfieldlines;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import me.petrolingus.electricfieldlines.core.Algorithm;
import me.petrolingus.electricfieldlines.core.Triangulation;
import me.petrolingus.electricfieldlines.core.configuration.TwoCircleConfiguration;
import me.petrolingus.electricfieldlines.core.generator.DataGenerator;
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
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;

    // Free shared collections
    public static List<Point> points;
    public static List<Triangle> triangles;

    private static TwoCircleConfiguration configuration = new TwoCircleConfiguration();

    public void initialize() {

        calculateConfiguration();

        setupSlider(configurationInnerRadiusSlider);
        setupSlider(configurationCenterShiftSlider);
        setupSlider(configurationClockwiseAngleSlider);

        configurationShowCheckBox.selectedProperty().addListener((value) -> draw());
        triangulationCheckBox.selectedProperty().addListener((value) -> draw());
        pointsCheckBox.selectedProperty().addListener((value) -> draw());
        isolineCheckBox.selectedProperty().addListener((value) -> draw());

        draw();
    }

    private void calculateConfiguration() {
        double innerCircleRadius = configurationInnerRadiusSlider.getValue();
        double innerCircleCenterShift = configurationCenterShiftSlider.getValue();
        double innerCircleCenterRotation = configurationClockwiseAngleSlider.getValue();
        configuration.calculateParameters(innerCircleRadius, innerCircleCenterShift, innerCircleCenterRotation);
    }

    private void setupSlider(Slider slider) {
        slider.valueProperty().addListener((changed, oldValue, newValue) -> {
            calculateConfiguration();
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

        int n = (int) pointsSlider.getValue();
        points = new DataGenerator(n).generate(configuration);
        timer.measure("generationOfPoints");

        triangles = new Triangulation().create(points, configuration);
        timer.measure("triangulation");

        process();
        timer.measure("process");

        draw();
        timer.measure("draw");

        System.out.println("######################################################################");
    }

    private void process() {
        int isolineCount = (int) Math.round(isolineCountSlider.getValue());
        int forceLineCount = (int) Math.round(isolineCountSlider.getValue());
        Algorithm algorithm = new Algorithm(points, triangles, isolineCount, forceLineCount);
        algorithm.process();
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
            double x = configuration.getInnerCircleCenterX();
            double y = configuration.getInnerCircleCenterY();
            double r = configuration.getInnerCircleRadius();
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