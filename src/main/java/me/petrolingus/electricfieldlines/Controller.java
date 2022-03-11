package me.petrolingus.electricfieldlines;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import me.petrolingus.electricfieldlines.util.Point;
import me.petrolingus.electricfieldlines.util.Point3d;
import me.petrolingus.electricfieldlines.util.Triangle;

import java.util.ArrayList;
import java.util.List;

public class Controller {

    @FXML
    private Canvas canvas;

    private static final double RADIUS = 0.5;
    private static final double SHIFT = 0.2;
    private static final double ANGLE = 0.5;
    private static final int N = 16;

    private static final double COS135 = -Math.sqrt(2) / 2;

    public static final List<Triangle> triangles = new ArrayList<>();

    public static final List<Point> vertices = new ArrayList<>();

    public static final List<Point> points = new ArrayList<>();
    public double cx = 0.3;
    public double cy = 0;

    public static final List<Point3d> whitePoints = new ArrayList<>();
    public static final List<Point3d> edgePoints = new ArrayList<>();

    public void initialize() {
        generationOfPoints();
        triangulation();
        draw();
    }

    private void generationOfPoints() {

        double step = 2.0 / (N - 1);
        int indexOuter135 = -1;
        int indexInner135xLeft = -1;
        int indexInner135xRight = -1;
        int indexInner135yLeft = -1;
        int indexInner135yRight = -1;
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
                    points.add(new Point(x, y, Color.WHITE));
                    whitePoints.add(new Point3d(x, y, 0));
                }
                indexOuter135 = (indexOuter135 == -1 && x > COS135) ? j : indexOuter135;
                indexInner135xLeft = (indexInner135xLeft == -1 && x > cx + RADIUS * COS135) ? j : indexInner135xLeft;
                indexInner135xRight = (indexInner135xRight == -1 && x > cx - RADIUS * COS135) ? j : indexInner135xRight;
                indexInner135yLeft = (indexInner135yLeft == -1 && y > cy + RADIUS * COS135) ? i : indexInner135yLeft;
                indexInner135yRight = (indexInner135yRight == -1 && y > cy - RADIUS * COS135) ? i : indexInner135yRight;
            }
        }

        // Add red points (outer circle)
        List<Point> redPoints = new ArrayList<>();
        for (int i = indexOuter135; i < N - indexOuter135; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(1 - a * a);
            redPoints.add(new Point(a, -b, Color.RED));
            redPoints.add(new Point(a, b, Color.RED));
            redPoints.add(new Point(b, a, Color.RED));
            redPoints.add(new Point(-b, a, Color.RED));
            edgePoints.add(new Point3d(a, -b, 0));
            edgePoints.add(new Point3d(a, b, 0));
            edgePoints.add(new Point3d(b, a, 0));
            edgePoints.add(new Point3d(-b, a, 0));
        }

        // Add blue points (inner circle)
        List<Point> bluePoints = new ArrayList<>();
        for (int i = indexInner135xLeft; i < indexInner135xRight; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(RADIUS * RADIUS - (a - cx) * (a - cx));
            bluePoints.add(new Point(a, -b + cy, Color.BLUE));
            bluePoints.add(new Point(a, b + cy, Color.BLUE));
            edgePoints.add(new Point3d(a, -b + cy, 0));
            edgePoints.add(new Point3d(a, b + cy, 0));
        }
        for (int i = indexInner135yLeft; i < indexInner135yRight; i++) {
            double a = -1 + i * step;
            double b = Math.sqrt(RADIUS * RADIUS - (a - cy) * (a - cy));
            bluePoints.add(new Point(b + cx, a, Color.BLUE));
            bluePoints.add(new Point(-b + cx, a, Color.BLUE));
            edgePoints.add(new Point3d(b + cx, a, 0));
            edgePoints.add(new Point3d(-b + cx, a, 0));
        }

        points.addAll(redPoints);
        points.addAll(bluePoints);
    }

    private void methodBowerWatson(double x, double y) {

        vertices.add(new Point(x, y, Color.WHITE));

        List<Integer> badTriangles = new ArrayList<>();
        for (int i = 0; i < triangles.size(); i++) {
            if (triangles.get(i).isContain(x, y)) {
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

        Point p0 = new Point(-1, -1, Color.WHITE);
        Point p1 = new Point(1, -1, Color.WHITE);
        Point p2 = new Point(1, 1, Color.WHITE);
        Point p3 = new Point(-1, 1, Color.WHITE);

        vertices.add(p0);
        vertices.add(p1);
        vertices.add(p2);
        vertices.add(p3);

        triangles.add(new Triangle(0, 1, 2));
        triangles.add(new Triangle(0, 2, 3));

        methodBowerWatson(cx, cy);

        for (Point p : points) {
            double x = p.x();
            double y = p.y();
            methodBowerWatson(x, y);
        }

        // Remove super-structure triangles
//        triangles.removeIf(t -> {
//            boolean res = false;
//            for (int i = 0; i < 4; i++) {
//                boolean c0 = t.getAid() == i;
//                boolean c1 = t.getBid() == i;
//                boolean c2 = t.getCid() == i;
//                res |= c0 || c1 || c2;
//            }
//            return res;
//        });

        // Remove triangles in inner circle
//        triangles.removeIf(t -> {
//            boolean c0 = t.getAid() == vertices.size() - 1;
//            boolean c1 = t.getBid() == vertices.size() - 1;
//            boolean c2 = t.getCid() == vertices.size() - 1;
//            return c0 || c1 || c2;
//        });

//        vertices.remove(p0);
//        vertices.remove(p1);
//        vertices.remove(p2);
//        vertices.remove(p3);

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
//        graphicsContext.setLineWidth(2 * (1 / zoom));
//        for (Triangle t : triangles) {
//            Point a = vertices.get(t.getAid());
//            Point b = vertices.get(t.getBid());
//            Point c = vertices.get(t.getCid());
//            graphicsContext.strokeLine(a.x(), a.y(), b.x(), b.y());
//            graphicsContext.strokeLine(b.x(), b.y(), c.x(), c.y());
//            graphicsContext.strokeLine(c.x(), c.y(), a.x(), a.y());
//        }

        // Draw points
        double r = 0.01;
//        for (Point p : vertices) {
//            graphicsContext.setFill(p.color());
//            graphicsContext.fillOval(p.x() - r, p.y() - r, 2 * r, 2 * r);
//        }

        graphicsContext.setFill(Color.WHITE);
        for (Point3d p : whitePoints) {
            graphicsContext.fillOval(p.x() - r, p.y() - r, 2 * r, 2 * r);
        }

        graphicsContext.setFill(Color.RED);
        for (Point3d p : edgePoints) {
            graphicsContext.fillOval(p.x() - r, p.y() - r, 2 * r, 2 * r);
        }

        graphicsContext.restore();

    }

}