/* just a lil helper class so both threads can grab their sum w/o
 * copy pasting the same for loop everywhere. nothing clever going on here lol */
public class SumUtil {

    // adds up nums using a plain loop
    // (they specifically said dont just use the n*(n+1)/2 formula in the code, has to be a loop, so thats what this is)
    public static long sumTo(int n) {
        long total = 0;
        for (int i = 0; i <= n; i++) {
            total += i;
        }
        return total;
    }
}
