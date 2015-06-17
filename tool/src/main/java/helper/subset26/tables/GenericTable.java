package helper.subset26.tables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import helper.HashMap2D;
import helper.RegexHelper;
import helper.TableHelper;
import helper.TraceabilityManagerHumanReadable;
import helper.word.DataConverter;

import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;

/**
 * Match known patterns of tables (i.e. abstract descriptions of how they look like) with those found in the subset-026
 * and use this data to add proper traceability links
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
abstract class GenericTable {
    protected final static int UNCONSTRAINED = -1;
    protected NumberPair rows = null;
    protected NumberPair columns = null;
    protected Table concreteTable = null;
    protected boolean forceFailingMatch = false;
    private boolean rectangular = false;
    private final CellData cellData = new CellData();
    private static final Logger logger = Logger.getLogger(GenericTable.class.getName()); // NOPMD - Reference rather than a static field

    /**
     * @param concreteTable concrete table to match against
     */
    public final void setContext(final Table concreteTable) {
	assert concreteTable != null;
	this.columns = new NumberPair(TableHelper.getMaxColumns(concreteTable));
	this.rows = new NumberPair(concreteTable.numRows());
	this.concreteTable = concreteTable;
	
	// setup matching data
	this.setTableData();

	this.cellData.fillUpUnspecifiedRows();
    }

    /**
     * Check if a given (concrete) table matches the pattern of this abstract table 
     * 
     * @return {@code true} if a match has been found; {@code false} otherwise
     * @throws IllegalStateException If no context has been provided to this matcher
     */
    public final boolean isTableMatch() {
	if (this.concreteTable == null) throw new IllegalStateException("No context has been provided, yet.");

	// single point of exit to make this more debug-friendly
	// TO DEBUG:
	// insert int rn = -1, cn = -1; here; undeclare in loops and set a breakpoint on the exit condition
	boolean output = false;	
	matchDeterminer: {

	    // Step 1 - check if this match is doomed to fail
	    if (this.forceFailingMatch) break matchDeterminer;
	    
	    // Step 2 - check if dimensions match
	    if (!this.rows.valuesMatch() || !this.columns.valuesMatch()) break matchDeterminer;
	    if (this.rectangular && this.rows.getActual() != this.columns.getActual()) break matchDeterminer;	

	    // Step 3 - check if actual data matches
	    MatchingData currentMatchingData = null;
	    for(int rn=0; rn<this.concreteTable.numRows(); rn++) {
		final TableRow row = this.concreteTable.getRow(rn);
		for(int cn=0; cn<row.numCells(); cn++) {
		    final TableCell cell = row.getCell(cn);
		    if (TableHelper.isMerged(cell)) {
			continue; // do not process non-first merged cells as they are empty by definition            	  
		    }

		    currentMatchingData = this.getMatchingData(rn, cn);						
		    if (currentMatchingData == null) {
			continue; // no comparison data available
		    }

		    if (!DataConverter.cleanupText(cell.text()).matches(currentMatchingData.cellContentRegex)) {
			// cell contents do not match
			if (currentMatchingData.conditional) {
			    continue;
			}
			break matchDeterminer;
		    }

		    // we only take the very first paragraph of a table cell into consideration here
		    final Paragraph cellParagraph = cell.getParagraph(0);

		    if (currentMatchingData.contentFormatting != ContentFormatting.INCONSISTENT) {
			if (cellParagraph.getCharacterRun(0).isBold()) {
			    if (currentMatchingData.contentFormatting != ContentFormatting.BOLD) {
				break matchDeterminer;
			    }
			}
			else {
			    if (currentMatchingData.contentFormatting != ContentFormatting.NORMAL) {
				break matchDeterminer;
			    }
			}
		    }

		    if (currentMatchingData.contentAlignment != ContentAlignment.INCONSISTENT) {
			switch (cellParagraph.getJustification()) {
			case 0x00: // left
			    if (currentMatchingData.contentAlignment != ContentAlignment.LEFT && currentMatchingData.contentAlignment != ContentAlignment.LEFTORJUSTIFY) {
				break matchDeterminer;
			    } break;
			case 0x01: // center
			    if (currentMatchingData.contentAlignment != ContentAlignment.CENTER) {
				break matchDeterminer;
			    } break;
			case 0x02: // right
			    if (currentMatchingData.contentAlignment != ContentAlignment.RIGHT) {
				break matchDeterminer;
			    } break;
			case 0x03: case 0x04: case 0x05: // justify
			    if (currentMatchingData.contentAlignment != ContentAlignment.JUSTIFY && currentMatchingData.contentAlignment != ContentAlignment.LEFTORJUSTIFY) {
				break matchDeterminer;
			    } break;
			default: // something which we do not know how to handle
			    break matchDeterminer;
			}		
		    }
		}
	    }
	    // yepee, we are still alive; i.e. the given table matches with the abstract one
	    output = true;	    
	}
	return output;
    }

    /**
     * Obtain the traceability manager for a given table cell in a given table
     * 
     * @param row row of interest (0-based)
     * @param column column of interest (0-based)
     * @param columnTrace column number to use for tracing (0-based)
     * @return fully processed trace tag or {@code null} if this cell has no associated tracing information
     */
    public final TraceabilityManagerHumanReadable getTraceabilityManagerHumanReadable(final int row, final int column, final int columnTrace) {
	if (this.cellData.tracingData.data.get(row, columnTrace) != null) {
	    return this.cellData.tracingData.data.get(row, columnTrace).getProcessedTracestringManager(this.concreteTable, row, column, columnTrace);
	}
	return null;
    }

    /**
     * Check if this cell's traceworthyness depends on the override data (i.e. only trace it if it contains an arrow)
     *  
     * @param row row of interest (0-based)
     * @param column column of interest (0-based)
     * @return {@code true} if this cell shall only be traced if it contains overridden content; {@code false} otherwise
     */
    public final boolean onlyTraceIfContentIsOverridden(final int row, final int column) {
	if (this.cellData.tracingData.data.get(row, column) != null) {
	    return this.cellData.tracingData.data.get(row, column).onlyTraceIfContentIsOverridden(this.concreteTable, row, column);
	}
	return false;
    }
    
    /**
     * Check if this cell's content may be split up into individual requirements or not
     *  
     * @param row row of interest (0-based)
     * @param column column of interest (0-based)
     * @return {@code true} if this cell's content may be split up; {@code false} otherwise
     */
    public final boolean isCellSplitAllowed(final int row, final int column) {
	if (this.cellData.splitUpData.data.get(row, column) != null) {
	    return this.cellData.splitUpData.data.get(row, column).splitUpAllowed;
	}
	return true;
    }
    
    
    /**
     * @return name of the matching table
     */
    public abstract String getName();
    
    /**
     * set up the properties of the individual table pattern to be matched against
     */
    protected abstract void setTableData();

    /**
     * Add new matching data (i.e. properties that are expected to hold true for the concrete table)
     * 
     * @param row row number where to add data (0-based)
     * @param column column number where to add data (0-based)
     * @param matchingData data to add
     * @throws IllegalArgumentException if the given data is {@code null}
     */
    protected final void addData(final int row, final int column, final MatchingData matchingData) {
	if (matchingData == null) throw new IllegalArgumentException("tracingData cannot be null.");
	this.cellData.matchingData.addData(row, column, matchingData);
    }

    /**
     * Add new tracing data (i.e. properties of cells which shall become tracable)
     * 
     * @param row row number where to add data (0-based)
     * @param column column number where to add data (0-based)
     * @param tracingData data to add
     * @throws IllegalArgumentException if the given data is {@code null}
     */
    protected final void addData(final int row, final int column, final TracingData tracingData) {
	if (tracingData == null) throw new IllegalArgumentException("tracingData cannot be null.");
	this.cellData.tracingData.addData(row, column, tracingData);
    }
    
    /**
     * Add new splitUp data (i.e. qualifier if a certain cell may be turned into several requirements)
     * 
     * @param row row number where to add data (0-based)
     * @param column column number where to add data (0-based)
     * @param splitupData data to add
     * @throws IllegalArgumentException if the given data is {@code null}
     */
    protected final void addData(final int row, final int column, final SplitupData splitupData) {
	if (splitupData == null) throw new IllegalArgumentException("splitupData cannot be null.");
	this.cellData.splitUpData.addData(row, column, splitupData);
    }

    /**
     * @param input number of the row to repeat
     * @throws IllegalArgumentException If the requested repeating row is out of range
     */
    protected final void setRepeatingRowMatchingData(final int input) {
	if (input < 0) throw new IllegalArgumentException("Requested repeating row number is invalid.");
	this.cellData.matchingData.setRepeatingRow(input);
    }

    /**
     * @param input number of the row to repeat
     * @throws IllegalArgumentException If the requested repeating row is out of range
     */
    protected final void setRepeatingRowTracingData(final int input) {
	if (input < 0) throw new IllegalArgumentException("Requested repeating row number is invalid.");
	this.cellData.tracingData.setRepeatingRow(input);
    }

    /**
     * @param input number of the row to repeat
     * @throws IllegalArgumentException If the requested repeating row is out of range
     */
    protected final void setRepeatingRowSplitupData(final int input) {
	if (input < 0) throw new IllegalArgumentException("Requested repeating row number is invalid.");
	this.cellData.splitUpData.setRepeatingRow(input);
    }
    
    /**
     * Set the rectangularity property of the table to be matched
     * 
     * @param input if {@code true} the number of columns must match the number of rows of this table; if {@code false} then not
     */
    protected final void setRectangular(final boolean input) {
	this.rectangular = input;
    }
    
    
    private final MatchingData getMatchingData(final int row, final int column) {
	return this.cellData.matchingData.data.get(row, column);
    }


    private final class CellData {
	private final List<DataStore<?>> availableData = new ArrayList<>();
	private final DataStore<MatchingData> matchingData = new DataStore<>();
	private final DataStore<TracingData> tracingData = new DataStore<>();
	private final DataStore<SplitupData> splitUpData = new DataStore<>();

	private void fillUpUnspecifiedRows() {
	    for (final DataStore<?> currentData : this.availableData) {
		currentData.fillUpUnspecifiedRows();				
	    }			
	}

	private final class DataStore<DataType extends GenericData> {
	    private Integer rowSimilarToAllSubsequentRows = UNCONSTRAINED;
	    private final HashMap2D<Integer, Integer, DataType> data = new HashMap2D<>();

	    public DataStore() {
		// make ourself known
		CellData.this.availableData.add(this);				
	    }

	    /**
	     * writes data to this {@code DataStore}, i.e. sets up the data for the matching algorithm
	     * 
	     * @param row row where to write the data (0-based) 
	     * @param column column where to write the data (0-based)
	     * @param properties expected properties of the cell
	     * @throws IllegalStateException if the user tried to override existing data
	     */
	    private void addData(final int row, final int column, final DataType properties) {
		assert properties != null;		
		if (this.data.get(row, column) != null) {
		    throw new IllegalStateException("You have already stored data of this type for cell (" + Integer.toString(row) + "," + Integer.toString(column) + "). Please correct your input.");
		}
		// columnNumbers in TracingData are colspan-aware; hence they may (intentionally) be out of bounds
		if (GenericTable.this.rows.isOutOfBounds(row) || (GenericTable.this.columns.isOutOfBounds(column) && !(properties instanceof TracingData))) {
		    return; // we are out of bounds
		}
		this.data.put(row, column, properties);
	    }

	    private void setRepeatingRow(final int row) {
		this.rowSimilarToAllSubsequentRows = row;
	    }

	    /**
	     * If the pattern of a certain row is repeated it has to be specified only once.
	     * This method fills up all the following rows with the data of that row.
	     * 
	     */	
	    private void fillUpUnspecifiedRows() {
		if (this.rowSimilarToAllSubsequentRows == UNCONSTRAINED) {
		    // no repeating row specified
		    return;
		}

		if (this.data.getRow(this.rowSimilarToAllSubsequentRows) == null) throw new IllegalArgumentException("Attempting to copy from a row which does not contain data. Please review your abstract table defintion.");
		final Map<Integer, DataType> rowToCopy = new HashMap<>(this.data.getRow(this.rowSimilarToAllSubsequentRows));				
		for (int rn = this.rowSimilarToAllSubsequentRows+1; rn<GenericTable.this.rows.getActual(); rn++) {
		    this.data.putRow(rn, rowToCopy);
		}
	    }
	}
    }

    /**
     * Class to hold a pair of row/column-data
     * 
     * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
     *
     */
    protected final static class NumberPair {
	private transient int expected;
	private transient final int actual;

	public NumberPair(final int actual) {
	    this.actual = actual;
	    this.setExpected(UNCONSTRAINED);
	}

	public boolean valuesMatch() {
	    return (this.isUnconstrained()) ? true : (this.actual == this.expected);
	}

	public boolean isOutOfBounds(final int value) {
	    return (!this.isUnconstrained() && value > this.actual);
	}

	private boolean isUnconstrained() {
	    return (this.expected == UNCONSTRAINED);
	}

	/**
	 * @param expected The maximum number of rows/columns to expect in this table
	 */
	public void setExpected(final int expected) {
	    this.expected = expected;
	}

	public int getActual() {
	    return this.actual;
	}
    }


    /**
     * Alignment of the contents of a table cell
     * 
     * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
     *
     */
    protected enum ContentAlignment {
	LEFT, CENTER, RIGHT, JUSTIFY,


	/**
	 * specifies that this content has either left or justified justification applied; stricter version of INCONSISTENT below 
	 */
	LEFTORJUSTIFY,

	/**
	 * specifies that this property should not be checked against (i.e. there is no consistent alignment in the subset-26)
	 */
	INCONSISTENT;
    }


    /**
     * Boldness of the (entire) contents of a table cell
     * 
     * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
     *
     */
    protected enum ContentFormatting {
	BOLD, NORMAL, 

	/**
	 * specifies that this property should not be checked against (i.e. there is no consistent formatting on the subset-26) 
	 */
	INCONSISTENT;
    }

    private interface GenericData { /* intentionally empty */ }
    
    
    /**
     * Stores if a certain cell's contents may be split up into individual requirements
     * 
     * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>     
     */
    protected final static class SplitupData implements GenericData {
	public final boolean splitUpAllowed;
	
	private SplitupData(final boolean qualifier) {
	    this.splitUpAllowed = qualifier;
	}
	
	/**
	 * Static constructor for the case "cell contents may be split up if necessary"
	 * 
	 * @return a new instance of this class
	 */
	public static SplitupData SplitupAllowed() {
	    return new SplitupData(true);
	}
	
	/**
	 * Static constructor for the case "cell contents may never be split up"
	 * 
	 * @return a new instance of this class
	 */
	public static SplitupData SplitupForbidden() {
	    return new SplitupData(false);
	}
    }
    

    /**
     * Stores data for matching information of an individual table cell
     * 
     * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
     */
    protected final static class MatchingData implements GenericData {
	public final String cellContentRegex;
	public final ContentFormatting contentFormatting;
	public final ContentAlignment contentAlignment;
	public final boolean conditional;

	/**
	 * Internal constructor
	 */
	private MatchingData(final ContentFormatting contentFormatting, final ContentAlignment contentAlignment, final String cellContentRegex, final boolean conditional) {
	    if (contentFormatting == null) throw new IllegalArgumentException("contentFormatting cannot be null.");
	    if (contentAlignment == null) throw new IllegalArgumentException("contentAlignment cannot be null.");
	    if (cellContentRegex == null) throw new IllegalArgumentException("cellContentRegex cannot be null.");
	    
	    this.cellContentRegex = cellContentRegex;
	    this.contentFormatting = contentFormatting;
	    this.contentAlignment = contentAlignment;
	    this.conditional = conditional;
	}
	
	/**
	 * Setup ordinary matching; all given properties have to be matched by the actual table cell
	 * 
	 * @param contentFormatting
	 * @param contentAlignment
	 * @param cellContentRegex
	 * @return a new instance of this class
	 */
	public static MatchingData newMatchingData(final ContentFormatting contentFormatting, final ContentAlignment contentAlignment, final String cellContentRegex) {
	    return new MatchingData(contentFormatting, contentAlignment, cellContentRegex, false);
	}
	
	/**
	 * Setup conditional matching
	 * 
	 * @param contentFormatting
	 * @param contentAlignment
	 * @param cellContentRegex
	 * @param conditional if
	 * <dl>
	 * <dt>{@code true}</dt><dd> then only match {@link #contentAlignment} and {@link #contentFormatting} against the cell if {@link #cellContentRegex} matches with the actual cell contents; this is necessary to determine non-emtpy cells</dd>
	 * <dt>{@code false}</dt><dd> also match {@link #cellContentRegex} against the cell (i.e. expect that all cells confirm to this pattern); this is the unconditional behavior from the ordinary constructor</dd>
	 * </dl>
	 * @return a new instance of this class
	 */
	public static MatchingData newMatchingDataConditional(final ContentFormatting contentFormatting, final ContentAlignment contentAlignment, final String cellContentRegex, final boolean conditional) {
	    return new MatchingData(contentFormatting, contentAlignment, cellContentRegex, conditional); 
	}
	
    }

    /**
     * Stores data for (human-readable) tracing information of an individual table cell
     * 
     * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
     */
    protected static abstract class TracingData implements GenericData {
	
	/**
	 * Static constructor for conditional tracing data (i.e. tracing information which will only be applied if the cell content matches a given pattern)
	 * 
	 * @param cellContentOfOwnCell pattern of own cell; must be matched in order to have tracing information applied
	 * @return a new instance of this class
	 * @throws IllegalArgumentException if the argument is {@code null}
	 */
	public static TracingData newTracingDataForThisCellConditional(final String cellContentOfOwnCell) {
	    if (cellContentOfOwnCell == null) throw new IllegalArgumentException("cellContentOfOwnCell cannot be null");
	    return new TracingDataForThisCellConditional(cellContentOfOwnCell);
	}

	/**
	 * Static constructor for tracing data which has the form {@code [A]n.B}<br />
	 * where
	 * 
	 * <dl>
	 * <dt>{@code n}</dt><dd> is a text from a column identified by {@code columnOffset} and {@code regexForRowId}</dd>
	 * <dt>{@code A} and {@code B}</dt> <dd> are user-definable, see below </dd>
	 * </dl>
	 * 
	 * @param columnOffset relative offset of the cell containing the rowId (cell must be on the same row)
	 * @param regexForRowId regex describing the actual rowId to be extracted from the cell referenced by columnOffset; regex must contain exactly one group
	 * @param prependerForRowId text to prepend the rowId with (will be inserted for {@code [A]})
	 * @param columnId raw columnId of this cell (will be inserted for {@code B})
	 * @param onlyTraceIfContentIsOverridden if {@code true} this cell will only have tracing data attached if there is also override data for it
	 * @return a new instance of this class
	 * @throws IllegalArgumentException if one of the arguments is malformed
	 */
	public static TracingData newTracingDataRowIdFromCell(final int columnOffset, final String regexForRowId, final TraceabilityManagerHumanReadable.RowLevelPrefix prependerForRowId, final String columnId, final boolean onlyTraceIfContentIsOverridden) {
	    if (regexForRowId == null) throw new IllegalArgumentException("regexForRowId cannot be null.");
	    if (prependerForRowId == null) throw new IllegalArgumentException("prependerForRowId cannot be null");
	    if (columnId == null) throw new IllegalArgumentException("columnId cannot be null.");
	    return new TracingDataRowIdFromCell(columnOffset, regexForRowId, prependerForRowId, columnId, onlyTraceIfContentIsOverridden);
	}	
		
	/**
	 * Static constructor for tracing data with a stock row-part and a user-definable column-appendix
	 * (i.e. {@code [r][3].myBeautifulColumnDescriptor})
	 * 
	 * @param columnId arbitrary string to be used for the column part of the tracestring
	 * @param onlyTraceIfContentIsOverridden if {@code true} this cell will only have tracing data attached if there is also override data for it
	 * @return a new instance of this class
	 * @throws IllegalArgumentException if the argument is malformed
	 */
	public static TracingData newTracingDataFixedColumnId(final String columnId, final boolean onlyTraceIfContentIsOverridden) {
	    if (columnId == null) throw new IllegalArgumentException("columnId cannot be null.");
	    return new TracingDataFixedColumnId(columnId, onlyTraceIfContentIsOverridden);
	}
	
	
	/**
	 * Static constructor for tracing data with a stock row-part and a column-appendix which is based on the contents of the respective column
	 * 
	 * @param columnIdRegex a regex with exactly one group. The contents of the group will become the column part of the tracestring.
	 * @return a new instance of this class
	 * @throws IllegalArgumentException of the argument is malformed
	 */
	public static TracingData newTracingDataColumnIdFromColumn(final String columnIdRegex) {
	    if (columnIdRegex == null) throw new IllegalArgumentException("columnIdRegex cannot be null.");
	    return new TracingDataColumnIdFromColumn(columnIdRegex);
	}
	
	
	/**
	 * Static constructor for tracing data with a stock row-part and a user-definable column-appendix
	 * (i.e. {@code [r][3].myBeautifulColumnDescriptor}) which will only be applied if the current cell matches a given pattern
	 * 
	 * @param columnId arbitrary string to be used for the column part of the tracestring
	 * @param cellContentOfOwnCell pattern of the cell to match
	 * @return a new instance of this class
	 * @throws IllegalArgumentException if one of the arguments is malformed
	 */
	public static TracingData newTracingDataFixedColumnIdConditional(final String columnId, final String cellContentOfOwnCell) {
	    if (columnId == null) throw new IllegalArgumentException("columnId cannot be null.");
	    if (cellContentOfOwnCell == null) throw new IllegalArgumentException("cellContentOfOwnCell cannot be null.");
	    return new TracingDataFixedColumnIdConditional(columnId, cellContentOfOwnCell, 0);
	}
	
	
	/**
	 * Static constructor for tracing data with a stock row-part and a user-definable column-appendix
	 * (i.e. {@code [r][3].myBeautifulColumnDescriptor}) which will only be applied if a cell in the same row (specified by {@code offset}) matches a given pattern
	 * 
	 * @see #newTracingDataFixedColumnIdConditional(String, String)
	 * @param columnId arbitrary string to be used for the column part of the tracestring
	 * @param cellContentOfTargetCell pattern of the cell to match
	 * @param targetCellOffset offset of {@code cellContentOfTargetCell} with respect to the current cell (positive values mean further right, negative values further left, 0 is the same cell)
	 * @return a new instance of this class
	 * @throws IllegalArgumentException if one of the arguments is malformed
	 */
	public static TracingData newTracingDataFixedColumnIdConditional(final String columnId, final String cellContentOfTargetCell, final int targetCellOffset) {
	    if (columnId == null) throw new IllegalArgumentException("columnId cannot be null.");
	    if (cellContentOfTargetCell == null) throw new IllegalArgumentException("cellContentOfTargetCell cannot be null.");
	    return new TracingDataFixedColumnIdConditional(columnId, cellContentOfTargetCell, targetCellOffset);
	}
	
	/**
	 * Compute a trace string; resulting string must be table-wide unique (i.e. usually contain both a row- and a column-part)
	 * 
	 * @param table table where the string shall be applied
	 * @param row row where the string shall be applied
	 * @param column column where the string shall be applied
	 * @param columnTrace column number to use for tracing
	 * @return fully-processed trace string or {@code null} if the current cell should not be traced at all
	 */
	public abstract TraceabilityManagerHumanReadable getProcessedTracestringManager(final Table table, final int row, final int column, final int columnTrace);
	
	
	/**
	 * Check if traceworthyness depends on the presence of override content
	 * 
	 * @param table table where the tracedata shall be applied
	 * @param row row where the tracedata shall be applied
	 * @param column column where the tracedata shall be applied
	 * @return {@code true} if this cell shall only be traced if it contains overridden content; {@code false} otherwise
	 */
	@SuppressWarnings("static-method")
	public boolean onlyTraceIfContentIsOverridden(final Table table, final int row, final int column) {
	    // subclasses are encouraged to override this
	    return false;
	}	
	
	/**
	 * This class is noninstantiable; use the static factory-methods instead
	 */
	private TracingData() {}
	
    }
    
    /**
     * Merely a placeholder; all subclasses which deal with tracing information obtained from the same row
     */
    private abstract static class TracingDataSameRow extends TracingData { /* intentionally empty */ }
    
    private final static class TracingDataFixedColumnId extends TracingDataSameRow {
	private final String columnId;
	private final boolean overriddenTraceQualifier;
	
	public TracingDataFixedColumnId(final String columnId, final boolean onlyTraceIfContentIsOverridden) {
	    this.columnId = columnId;
	    this.overriddenTraceQualifier = onlyTraceIfContentIsOverridden;
	}
	
	@Override
	public TraceabilityManagerHumanReadable getProcessedTracestringManager(final Table table, final int row, final int column, final int columnTrace) {
	    final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();
	    hrManager.addRow(row);
	    hrManager.addColumn(this.columnId);
	    return hrManager;
	}
	
	@Override
	public boolean onlyTraceIfContentIsOverridden(final Table table, final int row, final int column) {
	   return this.overriddenTraceQualifier; 
	}
    }
    
    
    private final static class TracingDataColumnIdFromColumn extends TracingDataSameRow {
	private final String columnIdRegex;
	
	public TracingDataColumnIdFromColumn(final String columnIdRegex) {
	    this.columnIdRegex = columnIdRegex;	     
	}

	@Override
	public TraceabilityManagerHumanReadable getProcessedTracestringManager(final Table table, final int row, final int column, final int columnTrace) {
	    final String contentsOfCurrentCell = table.getRow(row).getCell(column).text();
	    
	    final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();
	    hrManager.addRow(row);
	    final String columnPart = RegexHelper.extractRegex(contentsOfCurrentCell, this.columnIdRegex);
	    if (columnPart != null) {
		hrManager.addColumn(columnPart);
	    }
	    else {
		// fallback if regex did not match
		logger.log(Level.INFO, "Could not extract tracing component from current cell. Had to use a generic fallback. Please amend your MatchingData.");
		hrManager.addColumn(columnTrace);
	    }	    
	    return hrManager;	    
	}
	
    }
    
    
    private abstract static class TracingDataSameRowConditional extends TracingDataSameRow {
	private final String cellContentOfOwnCell;
	protected int columnOffset = 0;
	
	protected TracingDataSameRowConditional(final String cellContentOfOwnCell) {
	    this.cellContentOfOwnCell = cellContentOfOwnCell;
	}
	
	/**
	 * Check if the current cell must be traced
	 * 
	 * @param table table to process
	 * @param row row to process
	 * @param column column to process
	 * @return {@code true} if the current cell must be traced; {@code false} otherwise
	 * @throws IllegalArgumentException if the specified column offset refers to a cell which is out of range
	 */
	protected boolean isTraceworthyCell(final Table table, final int row, final int column) {
	    final boolean output;
	    assert this.cellContentOfOwnCell != null;
	    final int targetColumn = column + this.columnOffset;
	    
	    if (targetColumn < 0 || targetColumn >= table.getRow(row).numCells()) {
		throw new IllegalArgumentException("Given column offset for target cell refers to a nonexistent cell. Please check your table definitions.");
	    }
	    
	    if (!DataConverter.cleanupText(table.getRow(row).getCell(targetColumn).text()).matches(this.cellContentOfOwnCell)) {
		// conditional match did not succeed
		output = false;
	    }
	    else output = true;	    
	    return output;
	}
    }
    
    private final static class TracingDataForThisCellConditional extends TracingDataSameRowConditional {	
	public TracingDataForThisCellConditional(final String cellContentOfOwnCell) {
	    super(cellContentOfOwnCell);
	}
	
	@Override
	public TraceabilityManagerHumanReadable getProcessedTracestringManager(final Table table, final int row, final int column, final int columnTrace) {
	    if (!isTraceworthyCell(table, row, column)) return null;
	    final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();
	    hrManager.addRow(row);
	    hrManager.addColumn(columnTrace);
	    return hrManager;
	}	
    }        
    
    private final static class TracingDataFixedColumnIdConditional extends TracingDataSameRowConditional {
	protected final String columnId;
	
	private TracingDataFixedColumnIdConditional(final String columnId, final String cellContentOfTargetCell, final int columnOffset) {
	    super(cellContentOfTargetCell);
	    this.columnId = columnId;
	    this.columnOffset = columnOffset;
	}
	
	@Override
	public TraceabilityManagerHumanReadable getProcessedTracestringManager(final Table table, final int row, final int column, final int columnTrace) {
	    if (!isTraceworthyCell(table, row, column)) return null;
	    final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();
	    hrManager.addRow(row);
	    hrManager.addColumn(this.columnId);
	    return hrManager;
	}
    }
    
    private final static class TracingDataRowIdFromCell extends TracingDataSameRow {
	private final int columnOffset;
	private final String regexForRowId;
	private final TraceabilityManagerHumanReadable.RowLevelPrefix prependerForRowId;
	private final String columnId;
	private final boolean overriddenTraceQualifier;
	
	
	public TracingDataRowIdFromCell(final int columnOffset, final String regexForRowId, final TraceabilityManagerHumanReadable.RowLevelPrefix prependerForRowId, final String columnId, final boolean onlyTraceIfContentIsOverridden) {
	    this.columnOffset = columnOffset;
	    this.regexForRowId = regexForRowId;
	    this.prependerForRowId = prependerForRowId;
	    this.columnId = columnId;
	    this.overriddenTraceQualifier = onlyTraceIfContentIsOverridden;
	}
	
	@Override
	public TraceabilityManagerHumanReadable getProcessedTracestringManager(final Table table, final int row, final int column, final int columnTrace) {
	    final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();	    
	    final String referencedCell = DataConverter.cleanupText(table.getRow(row).getCell(columnTrace + this.columnOffset).text());
	    final String rowId = RegexHelper.extractRegex(referencedCell, this.regexForRowId);
	    if (rowId != null) hrManager.addRow(this.prependerForRowId, rowId);
	    else logger.log(Level.WARNING, "Matching of relative cell content failed. Please check your configuration.");
	    hrManager.addColumn(this.columnId);
	    return hrManager;
	}
	
	@Override
	public boolean onlyTraceIfContentIsOverridden(final Table table, final int row, final int column) {
	   return this.overriddenTraceQualifier; 
	}
    }
}