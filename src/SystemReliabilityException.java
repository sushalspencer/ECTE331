/**
 * when to go to safe mode
 *
 * @author  Sushal Spencer 7641564
 * @see     SensorReadException
 */
public class SystemReliabilityException extends Exception {

    /**
     * @param message to user saying why safe mode
     */
    public SystemReliabilityException(String message) {
        super(message);
    }
} // class
