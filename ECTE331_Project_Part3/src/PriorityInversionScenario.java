import java.util.concurrent.atomic.AtomicBoolean;

// controlled scenario for tasks 3, 4 , 5. same setup reused for all 3 w/ MotorControllers mode switched

// java thread priorities are only a hint to OS & on normal desktop/linux scheduler bumping thread to MAX_PRIORITY doesnt
// get it more cpu time unless run w/ special permissions

// so instead this models the effect directly: while medium is busy low grinds the critical section til its priority is below medium's 
// when low gets boosted (inheritance)/starts at ceiling (ceiling protocol) it stops being starved & finishes 
// baseline never boosts anything so its stuck behind medium the whole time

public class PriorityInversionScenario {

    public static final int HIGH_PRIORITY = Thread.MAX_PRIORITY;   // 10
    public static final int MEDIUM_PRIORITY = Thread.NORM_PRIORITY; // 5
    public static final int LOW_PRIORITY = Thread.MIN_PRIORITY;    // 1
    public static final int CEILING_PRIORITY = Thread.MAX_PRIORITY; // 10

    private final long unitsPerMs;
    private final long lowBaseWorkMs = 1200;   // how long lows job would take if left alone
    private final long mediumBusyMs = 2500;    // how long medium hogs the cpu for
    private final long highWorkMs = 50;        // highs own job is quick once it gets in

    public PriorityInversionScenario(long unitsPerMs) {
        this.unitsPerMs = unitsPerMs;
    }

    public static class Result {
        public long highWaitMs;
        public long highResponseMs;
        public long lowHoldMs;
    }

    public Result run(PriorityMode mode) throws InterruptedException {
        MotorController motor = new MotorController(CEILING_PRIORITY);
        motor.setMode(mode);

        AtomicBoolean mediumBusy = new AtomicBoolean(false); 
        Result result = new Result();

        System.out.println();
        System.out.println(Log.ts() + " scenario start, mode = " + mode);

        Thread low = new Thread(() -> {
            long[] times = motor.access("Logger(low)", () -> {
                long endTime = System.currentTimeMillis() + lowBaseWorkMs;
                long chunk = Math.max(1, unitsPerMs * 20); // does work in small chunks so we can keep checking the flag
                while (true) {
                    boolean starved = mediumBusy.get() && motor.effectiveHolderPriority() < MEDIUM_PRIORITY;
                    if (System.currentTimeMillis() >= endTime && !starved) {
                        break;
                    }
                    Work.burn(chunk);
                }
            });
            result.lowHoldMs = times[1];
        }, "Logger(low)");
        low.setPriority(LOW_PRIORITY);

        Thread high = new Thread(() -> {
            long[] times = motor.access("SafetyMonitor(high)", () -> Work.burn(unitsPerMs * highWorkMs));
            result.highWaitMs = times[0];
            result.highResponseMs = times[1];
        }, "SafetyMonitor(high)");
        high.setPriority(HIGH_PRIORITY);

        Thread medium = new Thread(() -> {
            mediumBusy.set(true);
            System.out.println(Log.ts() + " MotionPlanner(medium) started its own cpu heavy job, doesnt need the motor for this");
            Work.burn(unitsPerMs * mediumBusyMs);
            mediumBusy.set(false);
            System.out.println(Log.ts() + " MotionPlanner(medium) finished its job");
        }, "MotionPlanner(medium)");
        medium.setPriority(MEDIUM_PRIORITY);

        // stagger the starts a bit so low reliably grabs the lock first then high reliably blocks on it then medium jumps in 
        // this part is orchestration for the demo not part of the actual synchronisation mechanism
        low.start();
        Thread.sleep(80);
        high.start();
        Thread.sleep(80);
        medium.start();

        low.join();
        high.join();
        medium.join();

        System.out.println(Log.ts() + " scenario end, mode = " + mode
                + " | high waited " + result.highWaitMs + " ms, low held for " + result.lowHoldMs + " ms");

        return result;
    }
}
