package helper.nlp;

import helper.ConsoleOutputFilter;

import java.util.Queue;

/**
 * Regularly write status output about the ongoing NLP operations to Standard Error
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class NLPStatusWriter {
    final Thread writerThread;       
    
    private final class Writer implements Runnable {
	private final Queue<?> queue;
	
	public Writer(final Queue<?> queue) {
	    assert queue != null;	   
	    this.queue = queue;
	}

	@Override
	public void run() {	    
	    System.err.println("NLP is active. Processing may take a while...");
	    
	    while(!(Thread.currentThread().isInterrupted())) {		
		try {
		    Thread.sleep(5000);
		} catch (InterruptedException e) {
		    break;
		}		
			
		final String output = String.format("%5d", this.queue.size()) + " NLP-jobs remaining.";
		System.err.println(output);
	    }	    
	}	
    }
    
    /**
     * Ordinary constructor
     * 
     * @param queue queue where to obtain status information
     * @param consoleFilter a handle to the console output filter
     */
    public NLPStatusWriter(final Queue<?> queue, final ConsoleOutputFilter consoleFilter) {
	assert queue != null && consoleFilter != null;
	this.writerThread = new Thread(new Writer(queue));
	this.writerThread.setName("NLPStatusWriter");
	this.writerThread.setPriority(Thread.MIN_PRIORITY);
	this.writerThread.setDaemon(true); // not really necessary; but show off how little we care about you	
	consoleFilter.addThread(this.writerThread.getId());
	this.writerThread.start();
    }
    
    /**
     * Stop output of status information
     */
    void shutdown() {
	this.writerThread.interrupt();
    }    
}
