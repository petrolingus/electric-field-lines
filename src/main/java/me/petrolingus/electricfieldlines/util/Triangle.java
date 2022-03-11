package me.petrolingus.electricfieldlines.util;

import me.petrolingus.electricfieldlines.Controller;

import java.util.List;

public class Triangle {

    private final int aid;
    private final int bid;
    private final int cid;

    private final int raid;
    private final int rbid;
    private final int rcid;

    private double cx;
    private double cy;
    private double r;

    private boolean isBad = false;

    public Triangle(int aid, int bid, int cid) {
        this.aid = aid;
        this.bid = bid;
        this.cid = cid;
        this.raid = aid - 5;
        this.rbid = bid - 5;
        this.rcid = cid - 5;
        calcCircle();
    }

    private void calcCircle() {

        Point3d a = Controller.vertices.get(aid);
        Point3d b = Controller.vertices.get(bid);
        Point3d c = Controller.vertices.get(cid);

        double aa = a.mod();
        double bb = b.mod();
        double cc = c.mod();

        double d = 2 * (a.x() * (b.y() - c.y()) + b.x() * (c.y() - a.y()) + c.x() * (a.y() - b.y()));
        this.cx = (aa * (b.y() - c.y()) + bb * (c.y() - a.y()) + cc * (a.y() - b.y())) / d;
        this.cy = (aa * (c.x() - b.x()) + bb * (a.x() - c.x()) + cc * (b.x() - a.x())) / d;

        double dx = cx - a.x();
        double dy = cy - a.y();
        this.r = Math.sqrt(dx * dx + dy * dy);
    }

    public boolean isContain(double x, double y) {
        double dx = cx - x;
        double dy = cy - y;
        return this.r - Math.sqrt(dx * dx + dy * dy) > 1e-10;
    }

    public List<List<Integer>> getEdges() {
        List<Integer> edge0 = List.of(Math.min(aid, bid), Math.max(aid, bid));
        List<Integer> edge1 = List.of(Math.min(bid, cid), Math.max(bid, cid));
        List<Integer> edge2 = List.of(Math.min(cid, aid), Math.max(cid, aid));
        return List.of(edge0, edge1, edge2);
    }

    public boolean isBad() {
        return isBad;
    }

    public void setBad(boolean bad) {
        isBad = bad;
    }

    public int getAid() {
        return aid;
    }

    public int getBid() {
        return bid;
    }

    public int getCid() {
        return cid;
    }

    public int getRaid() {
        return raid;
    }

    public int getRbid() {
        return rbid;
    }

    public int getRcid() {
        return rcid;
    }
}
