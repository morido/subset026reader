package helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * Logging Handler which caches all log events instead of forwarding them right away
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class DeferredLoggingHandler extends StreamHandler {
    private final ByteArrayOutputStream buffer; 
    
    /**
     * Create a Logging Handler which caches all logged events
     * <p>
     * The <tt>Handler</tt> is configured based on
     * <tt>LogManager</tt> properties (or their default values).
     * 
     * @param buffer buffer to use   
     */
    public DeferredLoggingHandler(final ByteArrayOutputStream buffer) {
        super(buffer, new SimpleFormatter());
        this.buffer = buffer;
    }
    
    /**
     * Flushes all collected messages to <tt>System.err</tt>
     */
    public void flushMessages() {
	System.err.flush();
	try {
	    this.buffer.writeTo(System.err);	    
	} catch (IOException e) {
	    throw new IllegalStateException("Unable to write logging data", e);	    
	}
	System.err.flush();
    }
    
    /**
     * Make sure to flush after each incoming message
     * 
     * @see java.util.logging.ConsoleHandler#publish(java.util.logging.LogRecord)
     */
    @Override
    public synchronized void publish(final LogRecord record) {
	super.publish(record);
	flush();
    }
}
