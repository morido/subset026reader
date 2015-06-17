package helper.nlp;

import static helper.Constants.Specification.SpecialConstructs.USE_NLP;
import helper.ConsoleOutputFilter;
import helper.ParallelExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Asynchronously manages NLP work
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class NLPManager {
    private final CompletionService<Boolean> nlpECS;
    private final Queue<NLPJob> nlpJobs;
    private final Collection<Future<?>> nlpWorkerThreads;
    private final NLPJob poisonPill;
    private boolean acceptsJobs = true;
    private NLPStatusWriter nlpStatusWriter;


    /**
     * Ordinary constructor
     * @param parsersToCreate number of parses to use
     * @throws IllegalArgumentException if the number of given parsers is {@code < 1}
     */
    public NLPManager(final int parsersToCreate) {
	if (parsersToCreate < 1) throw new IllegalArgumentException("The number of requested parsers is too low.");
	if (USE_NLP) {	    
	    final ExecutorService threadPool = ParallelExecutor.createThreadPool("NLPParser", parsersToCreate).getExecutorService();
	    this.nlpECS = new ExecutorCompletionService<>(threadPool);
	    this.nlpWorkerThreads = new ArrayList<>(parsersToCreate);
	    this.nlpJobs = new ConcurrentLinkedQueue<>(); // we are not using a BlockingQueue here as this would require more synchronization
	    this.poisonPill = new NLPJob();
	    for (int i = 0; i < parsersToCreate; i++) {
		final Future<Boolean> currentThread = this.nlpECS.submit(new NLPWorkerThread(this.nlpJobs), Boolean.TRUE);
		this.nlpWorkerThreads.add(currentThread);			
	    }
	    threadPool.shutdown();
	} else {
	    this.nlpECS = null;
	    this.nlpJobs = null;
	    this.nlpWorkerThreads = null;
	    this.poisonPill = null;	   
	}	
    }

    /**
     * @return {@true} if NLP is to be used; {@code false} otherwise
     */
    @SuppressWarnings("static-method")
    public boolean isActive() {
	return USE_NLP;
    }

    /**
     * Submit a new NLP job for later execution
     * 
     * @param job NLP job to perform
     * @throws IllegalArgumentException if the given job is {@code null}
     * @throws IllegalStateException if the job cannot be queued
     */
    public void submitNLPJob(final NLPJob job) {
	if (job == null) throw new IllegalArgumentException("job cannot be null.");
	if (!this.acceptsJobs) throw new IllegalStateException("Processing has been stopped. No new jobs can be accepted.");
	if (this.nlpJobs != null) {
	    this.nlpJobs.add(job);
	}
    }

    /**
     * Start to output status information about the ongoing NLP jobs
     * <p>may be stopped via a call to {@link #waitForNLPJobsToFinish()}</p>
     * 
     * @param consoleFilter a handle to the console output filter
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public void writeStatusOutput(final ConsoleOutputFilter consoleFilter) {
	if (consoleFilter == null) throw new IllegalArgumentException("consoleFilter cannot be null.");
	if (this.nlpECS != null) {
	    this.nlpStatusWriter = new NLPStatusWriter(this.nlpJobs, consoleFilter);
	}
    }

    /**
     * Blocks until all NLP jobs have finished execution;
     * <p>after this has been called no new jobs will be processed</p>
     * 
     * @throws IllegalStateException if there were problems while shutting down the worker threads
     */
    public void waitForNLPJobsToFinish() {	
	if (this.nlpECS != null) {
	    // Step 1: place poison pill; doing this once for all consumer threads is enough as they will reuse it
	    try {
		submitNLPJob(this.poisonPill);		
	    } catch (IllegalStateException e) {
		throw new IllegalStateException("Cannot queue NLP termination job. This should not happen.", e);
	    }
	    // Step 2: do not accept any new jobs
	    this.acceptsJobs = false;
	    
	    // Step 3: wait until all worker threads have terminated
	    // this is a little clumsy but allows to catch exceptions in the worker threads (and is thus preferable to an awaitTermination()-approach)
	    try {	
		for (int i = 0; i < this.nlpWorkerThreads.size(); i++) {
		    // dont care about the output; will be Boolean.TRUE for successful completion;
		    // but do care about the exceptions it might throw
		    this.nlpECS.take().get();
		}	    		   
	    } catch (InterruptedException e) {
		throw new IllegalStateException("Interrupted while waiting for NLP to finish. This should not happen.", e);
	    } catch (ExecutionException e) {
		throw new IllegalStateException("NLP-Thread threw an exception. This should not happen.", e);
	    }
	    finally {
		for (final Future<?> currentThread : this.nlpWorkerThreads) currentThread.cancel(true);
	    }
	    
	    // Step 4: clean up; remove the poison pill and then check if the job queue is empty
	    if (!(this.nlpJobs.poll() != null && this.nlpJobs.isEmpty())) {
		throw new IllegalStateException("Could not properly clean up. This should not happen. Remaining jobs: " + this.nlpJobs.size());
	    }

	    if (this.nlpStatusWriter != null) this.nlpStatusWriter.shutdown();
	}  
    }
}
