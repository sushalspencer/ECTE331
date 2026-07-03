import java.io.IOException;

/**
 * sensor failure
 * @author  Sushal Spencer 7641564
 * @see     SystemReliabilityException
 */
public class SensorReadException extends IOException {

    /**
     * @param sensor failure message
     */
    public SensorReadException(String message) {
        super(message);
    }
} // class
