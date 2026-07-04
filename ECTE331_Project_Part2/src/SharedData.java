/* box that holds the vars so both threads can see them 
 * marked volatile just to be safe w/ visibility altho semaphore acquire/release guarantees 
 * other thread'll see latest value which happens before & all */
public class SharedData {
    volatile long a1, a2, a3;
    volatile long b1, b2, b3;
}
