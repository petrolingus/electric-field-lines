package me.petrolingus.electricfieldlines.core;

import me.petrolingus.electricfieldlines.util.Point3d;
import me.petrolingus.electricfieldlines.util.Triangle;

import java.util.ArrayList;
import java.util.List;

public class Triangulation {

    private double cx = 0.1;
    private double cy = 0.45;

    private static final List<Triangle> triangles = new ArrayList<>();
    private static final List<Point3d> vertices = new ArrayList<>();

    public List<Triangle> create(List<Point3d> points) {

        Point3d p0 = new Point3d(-1, -1, 0);
        Point3d p1 = new Point3d(1, -1, 0);
        Point3d p2 = new Point3d(1, 1, 0);
        Point3d p3 = new Point3d(-1, 1, 0);
        Point3d p4 = new Point3d(cx, cy, 0);

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

        return triangles;
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
            Triangle triangle = new Triangle(edge.get(0), edge.get(1), vertices.size() - 1);
            Point3d a = vertices.get(edge.get(0));
            Point3d b = vertices.get(edge.get(1));
            Point3d c = vertices.get(vertices.size() - 1);
            triangle.calcCircle(a, b, c);
            triangles.add(triangle);
        }
    }

}
