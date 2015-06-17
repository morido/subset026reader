/**
 * 
 */
package helper.subset26.tables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hwpf.usermodel.Table;

import helper.CSSManager;
import helper.ParallelExecutor;
import helper.TraceabilityManagerHumanReadable;


/**
 * Frontend to match a given (concrete) table against a set of abstract table definitions
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public final class TableMatcher {		
    private final GenericTable matchingTable;    
    private static final Logger logger = Logger.getLogger(TableMatcher.class.getName()); // NOPMD - Reference rather than a static field

    /**
     * Create a new matcher for a given table
     * 
     * @param table concrete table to match against
     * @throws IllegalArgumentException if the given table is {@code null}
     */
    public TableMatcher(final Table table) {
	if (table == null) throw new IllegalArgumentException("Table cannot be null.");	
	
	final Collection<GenericTable> handlers = (new TableServiceLocator(table)).getHandlers();		
	this.matchingTable = findMatchingTable(handlers);	
    }

    /**
     * Obtain a traceability manager containing a fully processed trace-tag
     * 
     * @param row row for which to obtain the trace tag
     * @param column column for which to obtain the trace tag
     * @param columnTrace column number to use for tracing (may be different from {@code column} if merged cells occur)
     * @return a fully processed traceTag or {@code null} if none exists
     * @throws IllegalStateException if there is no matching table
     */
    public TraceabilityManagerHumanReadable getTraceabiltyManagerHumanReadable(final int row, final int column, final int columnTrace) {
	if (!matchFound()) throw new IllegalStateException("We can only compute a tracestring if a matching table was found.");
	return this.matchingTable.getTraceabilityManagerHumanReadable(row, column, columnTrace);
    }

    /**
     * Get the qualifier if this cell's traceworthyness depends on the presence of override data
     * 
     * @param row row for which to obtain the qualifier
     * @param column column for which to obtain the qualifier
     * @return {@code true} if this cell should only be traced if there is also override data for it; {@code false} if it should always be traced
     */
    public boolean onlyTraceIfContentIsOverridden(final int row, final int column) {
	if (!matchFound()) throw new IllegalStateException("We can only obtain a qualifier if a matching table was found.");
	return this.matchingTable.onlyTraceIfContentIsOverridden(row, column);
    }

    /**
     * @return {@code true} If a matching table exists; {@code false} otherwise
     */
    public boolean matchFound() {
	return (this.matchingTable != null);
    }

    /**
     * @return name of the matching table; suitable for a CSS class attribute or {@code null} if there is no matching table
     */
    public String getCSSName() {
	return matchFound() ? CSSManager.getIdentifier(this.matchingTable.getName()) : null;	
    }
    
    
    /**
     * @param row row number of the current cell (0-based)
     * @param column column number of the current cell (0-based)
     * @return {@code true} if this cell may be split up; {@code false} otherwise
     */
    public boolean isCellSplitAllowed(final int row, final int column) {
	return matchFound() ? this.matchingTable.isCellSplitAllowed(row, column) : true;
    }

    /**
     * Match the given table against a collection of predefined abstract table definitions
     * 
     * @param matchers collection of abstract table defintions
     * @return the matching abstract table definition or {@code null} if no definition matches
     */
    private static GenericTable findMatchingTable(final Collection<GenericTable> matchers) {
	assert matchers != null;
	// execute this in parallel; one thread for each abstract table definition
	// inspired by http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorCompletionService.html
	final int n = matchers.size();
	final ExecutorService pool = ParallelExecutor.createThreadPool("TableMatcher", n).getExecutorService();
	final CompletionService<GenericTable> ecs = new ExecutorCompletionService<>(pool);
	final List<Future<GenericTable>> futures = new ArrayList<>(n);

	GenericTable output = null;
	try {
	    // Step 1: populate the futures
	    for (final GenericTable currentMatcher : matchers) {
		final CallableTableMatcher currentMatcherCallable = new CallableTableMatcher(currentMatcher);
		futures.add(ecs.submit(currentMatcherCallable));
	    }
	    pool.shutdown();

	    // Step 2: Query all our threads if one has a non-null result (i.e. did match the concrete table)
	    for (int i = 0; i < n; i++) {
		try {
		    final GenericTable matcherCandidate = ecs.take().get();
		    if (matcherCandidate != null) {
			output = matcherCandidate;
			break;
		    }
		} catch (InterruptedException | ExecutionException e) {
		    logger.log(Level.SEVERE, "Error while comparing concrete table against abstract table definitions.", e);		    
		}
	    }
	}
	finally {
	    for (final Future<GenericTable> currentFuture : futures) currentFuture.cancel(true);
	}
	
	// may be null if no matching table was found
	return output;
    }

    /**
     * Wrapper class which defines when an abstract table definition matches
     */
    private final static class CallableTableMatcher implements Callable<GenericTable> {	
	private final GenericTable matcher;

	public CallableTableMatcher(final GenericTable matcher) {	    
	    this.matcher = matcher;
	}

	@Override
	public GenericTable call() throws Exception {
	    return this.matcher.isTableMatch() ? this.matcher : null;
	}	
    }
}
