import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

// same deal as ThreadA but for B
public class ThreadB extends Thread {

    private final SharedData data;
    private final Semaphore a1Done;
    private final Semaphore b2Done;
    private final Semaphore a2Done;
    private final Semaphore b3Done;
    private final int iterations;
    private final AtomicInteger errorCount;

    public ThreadB(SharedData data, Semaphore a1Done, Semaphore b2Done,
                    Semaphore a2Done, Semaphore b3Done, int iterations, AtomicInteger errorCount) {
        super("ThreadB");
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
            // FuncB1: B1 = sum 0 to 250, no dependency so js go straight away
            long b1 = SumUtil.sumTo(250);
            data.b1 = b1;

            // FuncB2 needs A1 to be ready
            try {
                a1Done.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            long b2 = data.a1 + SumUtil.sumTo(200);
            data.b2 = b2;
            b2Done.release(); // safe to read B2 now

            // FuncB3 needs A2
            try {
                a2Done.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            long b3 = data.a2 + SumUtil.sumTo(400);
            data.b3 = b3;
            b3Done.release(); // safe to read B3 

            if (b1 != 31375L || b2 != 145350L || b3 != 270700L) {
                errorCount.incrementAndGet();
            }
        }
    }
}
