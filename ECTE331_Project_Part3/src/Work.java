public class Work {

    public static void burn(long units) {
        double x = 0.0001;
        for (long i = 0; i < units; i++) {
            x = Math.sqrt(x + i) * 1.0000001;
        }
        if (x == -1) {
            System.out.println("nope");
        }
    }

    // roughly how many burn() units correspond to 1ms on this machine
    // measured once at startup so the demo timings are comparable instead of being made up
    public static long calibrateUnitsPerMs() {
        long testUnits = 2_000_000;
        long t0 = System.nanoTime();
        burn(testUnits);
        long t1 = System.nanoTime();
        double ms = (t1 - t0) / 1_000_000.0;
        if (ms <= 0) ms = 1;
        return (long) (testUnits / ms);
    }
}