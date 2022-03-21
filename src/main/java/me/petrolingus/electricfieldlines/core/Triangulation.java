package me.petrolingus.electricfieldlines.core;

import me.petrolingus.electricfieldlines.util.Point;
import me.petrolingus.electricfieldlines.util.Triangle;

import java.util.ArrayList;
import java.util.List;

public class Triangulation {

    private double cx;
    private double cy;

    private final List<Triangle> triangles = new ArrayList<>();
    private final List<Point> vertices = new ArrayList<>();

    public Triangulation(double cx, double cy) {
        this.cx = cx;
        this.cy = cy;
    }

    public List<Triangle> create(List<Point> points) {

        Point p0 = new Point(-1, -1, 0);
        Point p1 = new Point(1, -1, 0);
        Point p2 = new Point(1, 1, 0);
        Point p3 = new Point(-1, 1, 0);
        Point p4 = new Point(cx, cy, 0);

        vertices.add(p0);
        vertices.add(p1);
        vertices.add(p2);
        vertices.add(p3);

        Triangle triangleOne = new Triangle(0, 1, 2);
        triangleOne.calcCircle(p0, p1, p2);
        triangles.add(triangleOne);

        Triangle triangleTwo = new Triangle(0, 2, 3);
        triangleTwo.calcCircle(p0, p2, p3);
        triangles.add(triangleTwo);

        methodBowerWatson(p4);

        for (Point p : points) {
            methodBowerWatson(p);
        }

        // Remove super-structure triangles and Remove triangles in inner circle
        triangles.removeIf(t -> {
            boolean res = false;
            for (int i = 0; i < 5; i++) {
                boolean c0 = t.getIndexA() == i;
                boolean c1 = t.getIndexB() == i;
                boolean c2 = t.getIndexC() == i;
                res |= c0 || c1 || c2;
            }
            return res;
        });

        // Change all indexes
        for (Triangle triangle : triangles) {
            triangle.setIndexA(triangle.getIndexA() - 5);
            triangle.setIndexB(triangle.getIndexB() - 5);
            triangle.setIndexC(triangle.getIndexC() - 5);
        }

        return triangles;
    }

    private void methodBowerWatson(Point point) {

        vertices.add(point);

        List<Triangle> badTriangles = new ArrayList<>();
        for (Triangle value : triangles) {
            if (value.isContain(point.x(), point.y())) {
                badTriangles.add(value);
            }
        }

        List<List<Integer>> polygon = new ArrayList<>();

        for (int i = 0; i < badTriangles.size(); i++) {
            List<List<Integer>> edges = badTriangles.get(i).getEdges();
            for (int j = 0; j < 3; j++) {
                List<Integer> edge = edges.get(j);
                boolean edgeIsNotShared = true;
                for (int k = 0; k < badTriangles.size(); k++) {
                    if (i == k) continue;
                    List<List<Integer>> otherEdges = badTriangles.get(k).getEdges();
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

        triangles.removeAll(badTriangles);

        for (List<Integer> edge : polygon) {
            Triangle triangle = new Triangle(edge.get(0), edge.get(1), vertices.size() - 1);
            Point a = vertices.get(edge.get(0));
            Point b = vertices.get(edge.get(1));
            Point c = vertices.get(vertices.size() - 1);
            triangle.calcCircle(a, b, c);
            triangles.add(triangle);
        }
    }

}
