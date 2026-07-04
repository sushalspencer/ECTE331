import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// just spits out a HH:mm:ss.SSS timestamp so every log line looks consistent
// didnt feel like typing this format string out 15 times
public class Log {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static String ts() {
        return "[" + LocalTime.now().format(FMT) + "]";
    }
}
