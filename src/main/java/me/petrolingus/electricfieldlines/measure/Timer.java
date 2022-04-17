package me.petrolingus.electricfieldlines.measure;

public class Timer {

    private long start;

    public Timer() {
        start = System.nanoTime();
    }

    public void measure(String msg) {
        measure("", msg);
    }

    public void measure(String preMsg, String msg) {
        double durationImNanoseconds = System.nanoTime() - start;
        double durationInMillis = durationImNanoseconds / 1_000_000;
        System.out.println(preMsg + durationInMillis + " ms [" + msg + "]");
        start = System.nanoTime();
    }
}
