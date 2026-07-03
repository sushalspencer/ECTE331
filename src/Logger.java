import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * file logger for navigation
 *
 * @author  Sushal Spencer 7641564
 */
public class Logger implements AutoCloseable {

    /** timestamp format */
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** keeps file current after each line */
    private final PrintWriter writer;

    /** log file */
    private final String fileName;

    /**
     * @param fileName the name of log file to write to
     * @throws IOException if the file cant be opened for writing
     */
    public Logger(String fileName) throws IOException {
        this.fileName = fileName;
        this.writer = new PrintWriter(new FileWriter(fileName, true), true);
    }

    /**
     * @param event the message describing the event
     */
    public void log(String event) {
        writer.println("[" + LocalDateTime.now().format(STAMP) + "] " + event);
    }

    /**
     * @return the log file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * closes writer and releases file
     */
    @Override
    public void close() {
        writer.close();
    }
} // class
