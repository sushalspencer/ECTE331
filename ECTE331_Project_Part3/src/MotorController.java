import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;

/* all threads use this, w/ only 1 controllng at a time using a ReentrantLock 
 * (cuz it'll check if its currently locked & whos holdinn it which synchronized wontr give access to)
 * also takes care of priority inheritance & ceiling logic in this rather than spreading it b/w the thread classes
 *side note that cost me a good hour of debugging: java.lang.Thread's internal priority field isnt guaranteed volatile
 * so when one thread boosts anothers priority theres no promise the other thread sees the new val straight away
 * so instead of having threads check Thread.currentThread().getPriority() to decide if theyre "starved"
 * we track the holders priority ourselves in an AtomicInteger which does give proper visibility guarantees across threads
*/ 

public class MotorController {

    private final ReentrantLock lock = new ReentrantLock();
    private volatile Thread holder = null;
    private volatile int boostedFrom = -1; // -1 = holder hasnt been boosted
    private volatile PriorityMode mode = PriorityMode.BASELINE;
    private final int ceilingPriority;
    private final AtomicInteger effectivePriority = new AtomicInteger(-1);

    public MotorController(int ceilingPriority) {
        this.ceilingPriority = ceilingPriority;
    }

    public void setMode(PriorityMode mode) {
        this.mode = mode;
    }

    // lets other threads check what priority current holder is running at, w/o the Thread.priority visibility issue
    public int effectiveHolderPriority() {
        return effectivePriority.get();
    }

    /**
     * grabs the motor, runs whatever criticalWork is, then lets go
     * returns {waitMs, totalMs} so the caller can log/measure stuff
     * label is just for the console logs so its readable
     */
    public long[] access(String label, Runnable criticalWork) {
        Thread self = Thread.currentThread();
        long t0 = System.nanoTime();

        // priority inheritance: if smn w/ a lower priority is currently holding the lock and we're about to block on it
        // bump them up to our level so they hurry and finish instead of getting stuck behind medium priority stuff
        if (mode == PriorityMode.INHERITANCE) {
            Thread currentHolder = holder;
            if (currentHolder != null && lock.isLocked() && !lock.isHeldByCurrentThread()
                    && self.getPriority() > currentHolder.getPriority()) {
                System.out.println(Log.ts() + " [INHERIT] " + currentHolder.getName()
                        + " bumped from " + currentHolder.getPriority() + " to " + self.getPriority()
                        + " because " + label + " is stuck waiting on it");
                boostedFrom = currentHolder.getPriority();
                currentHolder.setPriority(self.getPriority());
                effectivePriority.set(self.getPriority()); // matters for visibility
            }
        }

        lock.lock();
        long t1 = System.nanoTime();
        long waitMs = (t1 - t0) / 1_000_000;
        holder = self;
        int savedPriority = self.getPriority();
        effectivePriority.set(savedPriority);

        // priority ceiling: doesnt matter who you are the second you grab this resource you run at ceiling until youre done
        // stops the inversion from ever starting in the first place since nothing can outrank you while youve got the lock
        if (mode == PriorityMode.CEILING) {
            self.setPriority(ceilingPriority);
            effectivePriority.set(ceilingPriority);
            System.out.println(Log.ts() + " [CEILING] " + label + " raised to ceiling priority (" + ceilingPriority + ")");
        }

        System.out.println(Log.ts() + " [ACQUIRE] " + label + " has the motor (waited " + waitMs + " ms)");

        try {
            criticalWork.run();
        } finally {
            if (mode == PriorityMode.CEILING) {
                self.setPriority(savedPriority);
                effectivePriority.set(savedPriority);
                System.out.println(Log.ts() + " [CEILING] " + label + " dropped back to normal priority (" + savedPriority + ")");
            }
            if (mode == PriorityMode.INHERITANCE && boostedFrom != -1) {
                self.setPriority(boostedFrom);
                effectivePriority.set(boostedFrom);
                System.out.println(Log.ts() + " [INHERIT] " + label + " priority restored to " + boostedFrom);
                boostedFrom = -1;
            }
            holder = null;
            long t2 = System.nanoTime();
            long holdMs = (t2 - t1) / 1_000_000;
            System.out.println(Log.ts() + " [RELEASE] " + label + " let go of the motor (held for " + holdMs + " ms)");
            lock.unlock();
        }

        long t3 = System.nanoTime();
        return new long[] { waitMs, (t3 - t0) / 1_000_000 };
    }

    // handy for the inversion demo so we can tell if someone else already has it
    public int currentHolderPriority() {
        Thread h = holder;
        return h == null ? -1 : h.getPriority();
    }
}
