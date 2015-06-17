package helper;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;

/**
 * Helper class to filter console output from various (thread-) sources
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class ConsoleOutputFilter {    
    private final HashSet<Long> allowedthreadIDs = new HashSet<>();
    private final PrintStream system_err_Original = System.err;
    private final PrintStream system_out_Original = System.out;
    private PrintStream system_err_old = null;
    private PrintStream system_out_old = null;
    
    private static class AllowedThreadOnlyOutputStream extends FilterOutputStream
    {
	private final HashSet<Long> allowedThreadIDs;

	public AllowedThreadOnlyOutputStream(final PrintStream out, final HashSet<Long> threadIDs)
	{
	    super(out);
	    this.allowedThreadIDs = new HashSet<>(threadIDs);
	}

	@Override
	public void write(int b) throws IOException
	{
	    if (this.allowedThreadIDs.contains(Thread.currentThread().getId())) {
		// we do not need to synchronize here because the PrintStream already does that
		super.write(b);	
	    }
	}
    }

    /**
     * Add the calling thread to those which may output to the console
     */   
    public void addCurrentThread() {
	final long threadID = Thread.currentThread().getId();
	addThread(threadID);	
    }
        
    /**
     * Add the given thread to those which may output to the console
     * 
     * @param threadID id of the thread which shall be allowed to write output
     */    
    public synchronized void addThread(final long threadID) {
	// flush the previous wrappers (apparently not really necessary, but hey...)
	if (this.system_err_old != null) this.system_err_old.flush();
	if (this.system_out_old != null) this.system_out_old.flush();
	
	// add the new threadID and create appropriate streams
	this.allowedthreadIDs.add(threadID);
	final PrintStream newErrStream = new PrintStream(new AllowedThreadOnlyOutputStream(this.system_err_Original, this.allowedthreadIDs));
	final PrintStream newOutStream = new PrintStream(new AllowedThreadOnlyOutputStream(this.system_out_Original, this.allowedthreadIDs));
	System.setErr(newErrStream);
	System.setOut(newOutStream);
	
	// save the previous wrappers
	this.system_err_old = newErrStream;
	this.system_out_old = newOutStream;
    }
}
