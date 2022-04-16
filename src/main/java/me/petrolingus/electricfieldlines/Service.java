package me.petrolingus.electricfieldlines;

import javafx.concurrent.Task;
import me.petrolingus.electricfieldlines.core.Algorithm;
import me.petrolingus.electricfieldlines.core.Triangulation;
import me.petrolingus.electricfieldlines.core.configuration.Configuration;
import me.petrolingus.electricfieldlines.core.generator.DataGenerator;
import me.petrolingus.electricfieldlines.measure.Timer;
import me.petrolingus.electricfieldlines.util.Point;
import me.petrolingus.electricfieldlines.util.Triangle;

import java.util.List;

public class Service extends javafx.concurrent.Service<Void> {

    private Configuration configuration;
    private List<Point> points;
    private List<Triangle> triangles;

    // Solution config
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;

    private double pointsSliderValue;
    private double isolineCountSliderValue;
    private double fieldLineCountSliderValue;

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {

                if (triangles != null) {
                    triangles.clear();
                    min = Double.POSITIVE_INFINITY;
                    max = Double.NEGATIVE_INFINITY;
                }

                if (points != null) {
                    points.clear();
                }

                Timer timer = new Timer();

                int n = (int) pointsSliderValue;
                points = new DataGenerator(n).generate(configuration);
                timer.measure("generationOfPoints");

                triangles = new Triangulation().create(points, configuration);
                timer.measure("triangulation");

                int isolineCount = (int) Math.round(isolineCountSliderValue);
                int forceLineCount = (int) Math.round(fieldLineCountSliderValue);
                Algorithm algorithm = new Algorithm(configuration, points, triangles, isolineCount, forceLineCount);
                algorithm.process();
                timer.measure("process");

                System.out.println("######################################################################");

                return null;
            }
        };
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setPointsSliderValue(double pointsSliderValue) {
        this.pointsSliderValue = pointsSliderValue;
    }

    public void setIsolineCountSliderValue(double isolineCountSliderValue) {
        this.isolineCountSliderValue = isolineCountSliderValue;
    }

    public void setFieldLineCountSliderValue(double fieldLineCountSliderValue) {
        this.fieldLineCountSliderValue = fieldLineCountSliderValue;
    }

    public List<Point> getPoints() {
        return points;
    }

    public List<Triangle> getTriangles() {
        return triangles;
    }
}
