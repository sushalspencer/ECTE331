import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PerformanceEvaluation {

    private final int repeats;
    private final long unitsPerMs;

    public PerformanceEvaluation(int repeats, long unitsPerMs) {
        this.repeats = repeats;
        this.unitsPerMs = unitsPerMs;
    }

    static class Summary {
        PriorityMode mode;
        double avgWaitMs;
        double avgResponseMs;
        double avgLowHoldMs;
    }

    public List<Summary> run() throws InterruptedException, IOException {
        List<Summary> summaries = new ArrayList<>();

        System.out.println();
        System.out.println("TASK 6: PERFORMANCE EVALUATION");

        try (FileWriter csv = new FileWriter("performance_results.csv")) {
            csv.write("mode,run,waitMs,responseMs,lowHoldMs\n");

            for (PriorityMode mode : PriorityMode.values()) {
                long waitSum = 0, responseSum = 0, holdSum = 0;

                for (int i = 0; i < repeats; i++) {
                    PriorityInversionScenario scenario = new PriorityInversionScenario(unitsPerMs);
                    PriorityInversionScenario.Result r = scenario.run(mode);

                    waitSum += r.highWaitMs;
                    responseSum += r.highResponseMs;
                    holdSum += r.lowHoldMs;

                    csv.write(mode + "," + (i + 1) + "," + r.highWaitMs + "," + r.highResponseMs + "," + r.lowHoldMs + "\n");

                    // gap b/w runs so the jvm/gc settles before the next one
                    Thread.sleep(150);
                }

                Summary s = new Summary();
                s.mode = mode;
                s.avgWaitMs = waitSum / (double) repeats;
                s.avgResponseMs = responseSum / (double) repeats;
                s.avgLowHoldMs = holdSum / (double) repeats;
                summaries.add(s);
            }
        }

        System.out.println();
        System.out.println("mode        | avg high wait (ms) | avg high response (ms) | avg low hold (ms)");
        System.out.println("------------|---------------------|--------------------------|-------------------");
        for (Summary s : summaries) {
            System.out.printf("%-11s | %19.1f | %24.1f | %17.1f%n",
                    s.mode, s.avgWaitMs, s.avgResponseMs, s.avgLowHoldMs);
        }
        System.out.println();
        System.out.println("results also written to performance_results.csv (" + repeats + " runs per mode)");

        return summaries;
    }
}
