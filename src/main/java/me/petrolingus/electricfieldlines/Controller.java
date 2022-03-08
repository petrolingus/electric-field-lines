package me.petrolingus.electricfieldlines;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Controller {

    @FXML
    private Canvas canvas;

    private double r1 = 0.7;
    private double shift = 0.2;
    private double angle = 0.5;

    private static final double COS135 = -Math.sqrt(2) / 2;

    private double timer = 0;

    record Point(double x, double y, Color color) {

    }

    public void initialize() {
        draw();

        KeyFrame rotate = new KeyFrame(
                Duration.seconds(0.1),
                event -> {
                    timer += 0.05;
                    r1 = 0.1 + 0.8 * (1 + Math.cos(timer)) / 2;
                    draw();
                }
        );

        Timeline timeline = new Timeline(rotate);
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

    }

    private void draw() {

        double cx = 0;
        double cy = 0;

        int n = 32;
        double step = 2.0 / (n - 1);
        int indexOuter135 = -1;
        int indexInner135xLeft = -1;
        int indexInner135xRight = -1;
        int indexInner135yLeft = -1;
        int indexInner135yRight = -1;
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double x = -1 + j * step;
                double y = -1 + i * step;
                double d = Math.sqrt(x * x + y * y);
                double d2 = Math.sqrt(Math.pow(cx - x, 2) + Math.pow(cy - y, 2));
                boolean isOuterContain = d < 1;
                boolean isCloseToOuterCircle = Math.abs(1 - d) < (step / 2);
                boolean isInnerCircleContain = d2 < r1;
                boolean isCloseToInnerCircle = Math.abs(r1 - d2) < (step / 2);
                boolean condition = isOuterContain && !isCloseToOuterCircle && !isInnerCircleContain && !isCloseToInnerCircle;
                if (condition) {
                    points.add(new Point(x, y, Color.WHITE));
                }
                indexOuter135 = (indexOuter135 == -1 && x > COS135) ? j : indexOuter135;
                indexInner135xLeft = (indexInner135xLeft == -1 && x > cx + r1 * COS135) ? j : indexInner135xLeft;
                indexInner135xRight = (indexInner135xRight == -1 && x > cx - r1 * COS135) ? j : indexInner135xRight;
                indexInner135yLeft = (indexInner135yLeft == -1 && y > cy + r1 * COS135) ? i : indexInner135yLeft;
                indexInner135yRight = (indexInner135yRight == -1 && y > cy - r1 * COS135) ? i : indexInner135yRight;
            }
        }

        List<Point> redPoints = new ArrayList<>();
        for (int i = indexOuter135; i < n - indexOuter135; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(1 - a * a);
            redPoints.add(new Point(a, -b, Color.RED));
            redPoints.add(new Point(a, b, Color.RED));
            redPoints.add(new Point(b, a, Color.RED));
            redPoints.add(new Point(-b, a, Color.RED));
        }
//        double corner = Math.sqrt(2) / 2;
//        redPoints.add(new Point(-corner, -corner, Color.RED));
//        redPoints.add(new Point(corner, -corner, Color.RED));
//        redPoints.add(new Point(corner, corner, Color.RED));
//        redPoints.add(new Point(-corner, corner, Color.RED));

        List<Point> bluePoints = new ArrayList<>();
        for (int i = indexInner135xLeft; i < indexInner135xRight; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(r1 * r1 - (a - cx) * (a - cx));
            bluePoints.add(new Point(a, -b + cy, Color.BLUE));
            bluePoints.add(new Point(a, b + cy, Color.BLUE));
        }

        for (int i = indexInner135yLeft; i < indexInner135yRight; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(r1 * r1 - (a - cy) * (a - cy));
            bluePoints.add(new Point(b + cx, a, Color.BLUE));
            bluePoints.add(new Point(-b + cx, a, Color.BLUE));
        }

        points.addAll(redPoints);
        points.addAll(bluePoints);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();

        graphicsContext.save();

        graphicsContext.setFill(Color.BLACK);
        graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        graphicsContext.translate(canvas.getBoundsInLocal().getCenterX(), canvas.getBoundsInLocal().getCenterY());
        double zoom = 0.4 * canvas.getHeight();
        graphicsContext.scale(zoom, zoom);

        graphicsContext.setStroke(Color.BLUE);
        graphicsContext.setLineWidth(1 / zoom);
        double const1 = Math.sqrt(2) / 2;
        graphicsContext.strokeLine(-const1, -1, -const1, 1);
        graphicsContext.strokeLine(const1, -1, const1, 1);
        graphicsContext.setStroke(Color.GREEN);
        graphicsContext.strokeLine(-1, -const1, 1, -const1);
        graphicsContext.strokeLine(-1, const1, 1, const1);

        graphicsContext.setStroke(Color.BLUE);
        graphicsContext.setLineWidth(1 / zoom);
        double const2 = r1 * COS135;
        graphicsContext.strokeLine(-const2 + cx, -1, -const2 + cx, 1);
        graphicsContext.strokeLine(const2 + cx, -1, const2 + cx, 1);
        graphicsContext.setStroke(Color.GREEN);
        graphicsContext.strokeLine(-1, -const2 + cy, 1, -const2 + cy);
        graphicsContext.strokeLine(-1, const2 + cy, 1, const2 + cy);

        double r = 0.01;
        for (Point p : points) {
            graphicsContext.setFill(p.color);
            graphicsContext.fillOval(p.x - r, p.y - r, 2 * r, 2 * r);
        }

        graphicsContext.restore();

    }

    private void generateObjects() {


    }

}