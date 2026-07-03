import java.util.Random;

/**
 * altitude sensor
 * @author  Sushal Spencer 7641564
 */
public class Sensor {

    private final String id;

    /** random seed */
    private final Random random;

    /** lowest valid altitude */
    private final int baseline;

    /** valid reading window, small spread means agreement majority */
    private final int spread;

    /* sensor creation */
    public Sensor(String id, Random random, int baseline, int spread) {
        this.id = id;
        this.random = random;
        this.baseline = baseline;
        this.spread = spread;
    }

    /**
     * @return sensor id
     */
    public String getId() {
        return id;
    }

    /**
     * @return valid altitude in m/corrupted value that falls outta range
     * @throws SensorReadException if the fault model decides a fail
     */
    public int readSensor() throws SensorReadException {
        int chance = random.nextInt(100); // 0..99 inclusive

        if (chance < 15) {
            throw new SensorReadException("Sensor " + id + " failed to respond");
        } else if (chance < 30) {
            // corrupted values
            if (random.nextBoolean()) {
                return -(1 + random.nextInt(50));   // below floor
            } else {
                return 201 + random.nextInt(100);   // above ceil
            }
        } else {
            // valid reading
            return baseline + random.nextInt(spread);
        }
    }
} // class
