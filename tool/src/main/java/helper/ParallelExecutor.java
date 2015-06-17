package helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fire-and-forget parallel executor; uses runnables instead of callables to allow for exception propagation
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public final class ParallelExecutor {
    private static final Logger logger = Logger.getLogger(ParallelExecutor.class.getName()); // NOPMD - intentionally lower-case

    /**
     * wrapper class to ensure only our own thread pools are used
     */
    public final static class ThreadPool {
	final ExecutorService executorService;

	ThreadPool(final ExecutorService executorService) {
	    assert executorService != null;
	    this.executorService = executorService;

	}

	/**
	 * @return the wrapped executor service; never {@code null}
	 */
	public ExecutorService getExecutorService() {
	    return this.executorService;
	}
    }

    private ParallelExecutor() {
	// utility class; avoid instantiation
    }

    /**
     * Execute parallel tasks which do not return anything
     * 
     * @param namePrefix prefix of the names of the created threads
     * @param runnables a variable number of runnables to be executed in parallel
     * @throws IllegalArgumentException if one of the arguments is malformed
     */
    public static void execute(final String namePrefix, final Runnable... runnables) {
	if (runnables.length == 0) throw new IllegalArgumentException("Need at least one runnable");
	if (namePrefix == null) throw new IllegalArgumentException("namePrefix cannot be null.");

	final int numThreads = runnables.length;	
	final ExecutorService threadPool = createThreadPool(namePrefix, numThreads).getExecutorService();	
	final CompletionService<Boolean> ecs = new ExecutorCompletionService<>(threadPool);

	final Collection<Future<?>> futures = new ArrayList<>(numThreads);
	try {
	    for (final Runnable currentRunnable : runnables) futures.add(ecs.submit(currentRunnable, Boolean.TRUE));	
	    threadPool.shutdown();
	    for (int i = 0; i < futures.size(); i++) {
		try {
		    // dont care about the output; will be Boolean.TRUE for successful completion;
		    // but do care about the exceptions it might throw
		    ecs.take().get();
		} catch (InterruptedException | ExecutionException e) {
		    logger.log(Level.SEVERE, "Encountered problems during parallel execution", e);			
		}
	    }
	}
	finally {
	    for (final Future<?> currentFuture : futures) currentFuture.cancel(true);
	}
    }

    /**    
     * @param threadPool
     * @param runnables
     */
    public static void execute(final ThreadPool threadPool, final Runnable... runnables) {
	if (runnables.length == 0) throw new IllegalArgumentException("Need at least one runnable");
	if (threadPool == null) throw new IllegalArgumentException("threadPool cannot be null.");

	final CompletionService<Boolean> ecs = new ExecutorCompletionService<>(threadPool.getExecutorService());
	final Collection<Future<?>> futures = new ArrayList<>(runnables.length);
	try {
	    for (final Runnable currentRunnable : runnables) futures.add(ecs.submit(currentRunnable, Boolean.TRUE));
	    // no shutdown here since we are reusing the threadPool
	    for (int i = 0; i < futures.size(); i++) {
		try {
		    // dont care about the output; will be Boolean.TRUE for successful completion;
		    // but do care about the exceptions it might throw
		    ecs.take().get();
		} catch (InterruptedException | ExecutionException e) {
		    logger.log(Level.SEVERE, "Encountered problems during parallel execution", e);
		}
	    }
	}
	finally {
	    for (final Future<?> currentFuture : futures) currentFuture.cancel(true);
	}
    }  

    /**
     * Create a new fixed thread pool
     * 
     * @param namePrefix name prefix to use for the new thread pool
     * @param numThreads number of threads to create
     * @return newly created thread pool; never {@code null}
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public static ThreadPool createThreadPool(final String namePrefix, final int numThreads) {
	if (namePrefix == null) throw new IllegalArgumentException("namePrefix cannot be null.");
	return new ThreadPool(Executors.newFixedThreadPool(numThreads, createThreadFactory(namePrefix)));
    }      

    private static ThreadFactory createThreadFactory(final String namePrefix) {
	assert namePrefix != null;
	final AtomicLong count = new AtomicLong(0);		
	return new ThreadFactory() {	    
	    @Override
	    public Thread newThread(final Runnable runnable) {
		final Thread thread = new Thread(runnable);
		thread.setName(namePrefix + "-" + count.getAndIncrement());		
		return thread;
	    }
	};	
    }
}