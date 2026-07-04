import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

// thread A does FuncA1 -> FuncA2 -> FuncA3 in order (thats automatic cause its all in one thread) but A2 needs B2 to be done 1st and A3 needs B3
// to be done 1st so those two spots have to wait on B 
// using semaphores for that instead of sleep() or a spinny while loop since both of those are banned for this part
public class ThreadA extends Thread {

    private final SharedData data;
    private final Semaphore a1Done;   // we release once A1 done B waits
    private final Semaphore b2Done;   // we wait on this b4 doing A2 
    private final Semaphore a2Done;   // we release once A2 done B waits
    private final Semaphore b3Done;   // we wait on this b4 doing A3
    private final int iterations;
    private final AtomicInteger errorCount;

    public ThreadA(SharedData data, Semaphore a1Done, Semaphore b2Done,
                    Semaphore a2Done, Semaphore b3Done, int iterations, AtomicInteger errorCount) {
        super("ThreadA");
        this.data = data;
        this.a1Done = a1Done;
        this.b2Done = b2Done;
        this.a2Done = a2Done;
        this.b3Done = b3Done;
        this.iterations = iterations;
        this.errorCount = errorCount;
    }

    @Override
    public void run() {
        for (int i = 0; i < iterations; i++) {
            // FuncA1: A1 = sum til 500, doesnt depend on anything so just go
            long a1 = SumUtil.sumTo(500);
            data.a1 = a1;
            a1Done.release(); // tell B safe to read A1 now

            // FuncA2 needs B2 so wait till B says its ready
            try {
                b2Done.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            long a2 = data.b2 + SumUtil.sumTo(300);
            data.a2 = a2;
            a2Done.release(); // tell B its safe to read A2 now

            // FuncA3 needs B3
            try {
                b3Done.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            long a3 = data.b3 + SumUtil.sumTo(400);
            data.a3 = a3;

            // quick sanity check every loop cuz expected values alr worked out by hand
            if (a1 != 125250L || a2 != 190500L || a3 != 350900L) {
                errorCount.incrementAndGet();
            }
        }
    }
}
