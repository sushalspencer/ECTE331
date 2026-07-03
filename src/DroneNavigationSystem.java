import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * main run file
 *
 * @author  Sushal Spencer 7641564
 */

public class DroneNavigationSystem {

    /** lowest valid altitude */
    public static final int ALT_MIN = 0;

    /** highest valid altitude */
    public static final int ALT_MAX = 200;

    /** single sensor reading for status */
    private enum Status { VALID, CORRUPTED, FAILED }

    /** the three sensors A B C */
    private final Sensor[] sensors;

    /** event logger*/
    private final Logger logger;

    /** last alt used, fallback as well */
    private int previousAltitude;

    /** reliability sensors */
    private int consecutiveFailures = 0;

    /**
     * nav control build for sensors
     *
     * @param sensors         three redundant sensors (A, B, C)
     * @param logger          event logger
     * @param initialAltitude good altitude to start from, first fallback
     */
    public DroneNavigationSystem(Sensor[] sensors, Logger logger, int initialAltitude) {
        this.sensors = sensors;
        this.logger = logger;
        this.previousAltitude = initialAltitude;
    }

    /**
     * valid val in alt range
     *
     * @param value candidate alt in m
     * @return {@code true} if {@code value} is within {@code [ALT_MIN, ALT_MAX]}
     */
    private static boolean inRange(int value) {
        return value >= ALT_MIN && value <= ALT_MAX;
    }

    /**
     * alt that comes twice
     *
     * @param validValues 
     * @return majority value/null
     */
    private static Integer findMajority(List<Integer> validValues) {
        for (int i = 0; i < validValues.size(); i++) {
            int count = 0;
            for (int candidate : validValues) {
                if (candidate == validValues.get(i)) {
                    count++;
                }
            }
            if (count >= 2) {
                return validValues.get(i);
            }
        }
        return null;
    }

    /**
     * one full voting cycle
     *
     * @param cycle cycle number
     * @throws SystemReliabilityException if second conseq error
     */
    public void runCycle(int cycle) throws SystemReliabilityException {
        System.out.println();
        System.out.println("=== Cycle " + cycle + " ===");

        String[] ids = { "A", "B", "C" };
        int[] values = new int[3];
        Status[] status = new Status[3];

        // reading every sensor & outcomes
        for (int i = 0; i < sensors.length; i++) {
            try {
                int reading = sensors[i].readSensor();
                if (inRange(reading)) {
                    status[i] = Status.VALID;
                    values[i] = reading;
                    System.out.println("Sensor " + ids[i] + ": " + reading + " m (valid)");
                } else {
                    status[i] = Status.CORRUPTED;
                    values[i] = reading;
                    System.out.println("Sensor " + ids[i] + ": " + reading + " m (CORRUPTED)");
                    logger.log("CORRUPTED READING: Sensor " + ids[i]
                            + " returned " + reading + " m (outside [" + ALT_MIN
                            + "," + ALT_MAX + "])");
                }
            } catch (SensorReadException e) {
                status[i] = Status.FAILED;
                System.out.println("Sensor " + ids[i] + ": FAILURE (" + e.getMessage() + ")");
                logger.log("SENSOR FAILURE: Sensor " + ids[i] + " - " + e.getMessage());
            }
        }

        // valid readings
        List<Integer> validValues = new ArrayList<>();
        List<String> validIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (status[i] == Status.VALID) {
                validValues.add(values[i]);
                validIds.add(ids[i]);
            }
        }

        boolean reliabilityFailure;

        if (validValues.size() < 2) {
            // reliability failure cuz of too few valid orders
            reliabilityFailure = true;
            List<String> outliers = collectNonValid(ids, status);
            logger.log("OUTLIER DETECTION: non-valid sensor(s) " + outliers
                    + "; only " + validValues.size() + " valid reading(s)");
            System.out.println("Reliability: FAILURE - fewer than two valid readings");
            System.out.println("Holding previous altitude: " + previousAltitude + " m");
        } else {
            Integer majority = findMajority(validValues);

            if (majority != null) {
                // majority discovered so thats new alt
                reliabilityFailure = false;
                previousAltitude = majority;

                List<String> agree = new ArrayList<>();
                for (int i = 0; i < validValues.size(); i++) {
                    if (validValues.get(i).equals(majority)) {
                        agree.add(validIds.get(i));
                    }
                }
                List<String> outliers = collectOutliers(ids, status, values, majority);
                if (!outliers.isEmpty()) {
                    logger.log("OUTLIER DETECTION: sensor(s) " + outliers
                            + " disagree with majority " + majority + " m");
                }
                logger.log("MAJORITY DECISION: altitude = " + majority
                        + " m, sensor(s) " + agree + " in agreement");
                System.out.println("Voting: MAJORITY " + majority + " m (sensors "
                        + agree + " agree)");
                System.out.println("Reliability: OK");
            } else {
                // differed valid readings, so fallback
                reliabilityFailure = true;
                logger.log("FALLBACK DECISION: valid readings " + validValues
                        + " all differ, no majority; reusing previous altitude "
                        + previousAltitude + " m");
                System.out.println("Voting: NO MAJORITY - valid readings " + validValues
                        + " all differ");
                System.out.println("Fallback: reusing previous altitude "
                        + previousAltitude + " m");
                System.out.println("Reliability: FAILURE - no majority");
            }
        }

        // consecutive failure tracking + escalation
        if (reliabilityFailure) {
            consecutiveFailures++;
        } else {
            consecutiveFailures = 0;
        }
        System.out.println("Altitude in use: " + previousAltitude
                + " m | consecutive failures: " + consecutiveFailures);

        if (consecutiveFailures >= 2) {
            throw new SystemReliabilityException(
                    "Two consecutive reliability failures (last at cycle " + cycle + ")");
        }
    }

    /**
     * ids with invalid readings
     */
    private static List<String> collectNonValid(String[] ids, Status[] status) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            if (status[i] != Status.VALID) {
                out.add(ids[i]);
            }
        }
        return out;
    }

    /**
     * which ids failed
     */
    private static List<String> collectOutliers(String[] ids, Status[] status,
                                                int[] values, int majority) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            if (status[i] != Status.VALID || values[i] != majority) {
                out.add(ids[i]);
            }
        }
        return out;
    }

    /**
     * runs sim till safe mode
     */
    public static void main(String[] args) {
        long seed = -1;
        int cycles = 12;
        int spread = 5;
        int baseline = 98;

        if (args.length >= 1) seed = Long.parseLong(args[0]);
        if (args.length >= 2) cycles = Integer.parseInt(args[1]);
        if (args.length >= 3) spread = Integer.parseInt(args[2]);
        if (args.length >= 4) baseline = Integer.parseInt(args[3]);

        Random rng = (seed >= 0) ? new Random(seed) : new Random();

        // Dynamic, per-run log file name with a random numeric suffix.
        String logName = "log_" + new Random().nextInt(90000) + ".txt";

        Sensor[] sensors = {
                new Sensor("A", rng, baseline, spread),
                new Sensor("B", rng, baseline, spread),
                new Sensor("C", rng, baseline, spread)
        };

        int initialAltitude = baseline; // baseline starting altitude

        try (Logger logger = openLogger(logName)) {
            logger.log("RUN START: seed=" + (seed >= 0 ? seed : "random")
                    + ", cycles=" + cycles + ", spread=" + spread
                    + ", baseline=" + baseline);
            System.out.println("Drone navigation system starting (log file: "
                    + logger.getFileName() + ")");

            DroneNavigationSystem system =
                    new DroneNavigationSystem(sensors, logger, initialAltitude);

            try {
                for (int cycle = 1; cycle <= cycles; cycle++) {
                    system.runCycle(cycle);
                }
                logger.log("RUN COMPLETE: finished " + cycles
                        + " cycles without entering SAFE MODE");
                System.out.println("\nSimulation completed normally after "
                        + cycles + " cycles.");
            } catch (SystemReliabilityException e) {
                logger.log("SAFE MODE ACTIVATION: " + e.getMessage());
                System.out.println("\n*** SAFE MODE ACTIVATED ***");
                System.out.println("Reason: " + e.getMessage());
                System.out.println("Execution halted.");
            }
        } catch (IOException e) {
            System.out.println("Fatal: could not open log file - " + e.getMessage());
        }
    }

    /**
     * @param log file name
     * @return an open logger
     * @throws IOException if file cant be created
     */
    private static Logger openLogger(String name) throws IOException {
        return new Logger(name);
    }
} // class
