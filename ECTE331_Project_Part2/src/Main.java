import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/* runs threadA and threadB back to back and checks every time that final values come out correct
 *  if sync was broken (like if we used sleep() & hoped for best) wud fail once scheduler did smt unlucky
 *  running it a ton of times is only way to be sure scheduling cant break it
*/ 
public class Main {

    public static void main(String[] args) throws InterruptedException {

        int iterations = 200_000; // high num of iter

        SharedData data = new SharedData();
        AtomicInteger errorCount = new AtomicInteger(0);

        // one semaphore per cross thread dependency arrow in fig
        // starts at 0 permits so waiting side blocks 1st time
        Semaphore a1Done = new Semaphore(0);
        Semaphore b2Done = new Semaphore(0);
        Semaphore a2Done = new Semaphore(0);
        Semaphore b3Done = new Semaphore(0);

        ThreadA threadA = new ThreadA(data, a1Done, b2Done, a2Done, b3Done, iterations, errorCount);
        ThreadB threadB = new ThreadB(data, a1Done, b2Done, a2Done, b3Done, iterations, errorCount);

        System.out.println("running " + iterations + " iterations of the handshake...");
        long start = System.currentTimeMillis();

        threadA.start();
        threadB.start();
        threadA.join();
        threadB.join();

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("done in " + elapsed + " ms");
        System.out.println("expected final values -> A1=125250 B1=31375 B2=145350 A2=190500 B3=270700 A3=350900");
        System.out.println("last values seen       -> A1=" + data.a1 + " B1=" + data.b1 + " B2=" + data.b2
                + " A2=" + data.a2 + " B3=" + data.b3 + " A3=" + data.a3);
        System.out.println("mismatches across all iterations: " + errorCount.get());

        if (errorCount.get() == 0) {
            System.out.println("PASS - synchronisation held up for all " + iterations + " runs");
        } else {
            System.out.println("FAIL - something raced, check the semaphore logic");
        }
    }
}
