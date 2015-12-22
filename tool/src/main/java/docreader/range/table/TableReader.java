package docreader.range.table;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import helper.CSSManager;
import helper.ParallelExecutor;
import helper.TableHelper;
import helper.TraceabilityManagerHumanReadable;
import helper.XmlStringWriter;
import helper.annotations.DomainSpecific;
import helper.subset26.tables.TableMatcher;
import helper.word.BorderManager;

import org.apache.poi.hwpf.usermodel.BorderCode;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.ShadingDescriptor;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;

import docreader.GenericReader;
import docreader.ReaderData;
import docreader.list.NestingType;
import docreader.range.RangeReader;
import docreader.range.paragraph.CaptionReader;
import requirement.RequirementOrdinary;
import requirement.RequirementTemporary;
import requirement.RequirementWParent;
import requirement.data.RequirementText;
import requirement.metadata.Kind;

/**
 * Handles paragraphs which are a table
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public final class TableReader extends RangeReader {
    private RequirementWParent tableRequirement;
    private final transient Paragraph paragraph;
    private final static int TABLE_HEAD_SET_FOR_ROW_INITIAL = -1;
    private transient int tableHeadSetForRow = TABLE_HEAD_SET_FOR_ROW_INITIAL;
    private static final Logger logger = Logger.getLogger(TableReader.class.getName()); // NOPMD - Reference rather than a static field

    /**
     * @param readerData global readerData
     * @param parentRequirement surrounding requirement of this table
     * @param initialParagraphIndex absolute start offset of the first paragraph of this table
     * @throws IllegalArgumentException paragraph is of wrong type or not part of a table
     */
    public TableReader(final ReaderData readerData, final RequirementTemporary parentRequirement, final int initialParagraphIndex) {
	// TODO check this constructor
	super(readerData, parentRequirement, initialParagraphIndex);		

	if (!(this.range instanceof Paragraph)) throw new IllegalArgumentException("We can only work on Paragraphs. However, we got " + this.range.getClass().toString() + ".");
	this.paragraph = (Paragraph) this.range; // explicit downcasting

	if (!this.paragraph.isInTable()) throw new IllegalArgumentException("The given paragraph is not part of a table.");
	//this.documentRange = readerData.getRange();
    }	

    /**
     * Read a table
     * 
     * @see docreader.GenericReader#read()
     * @return number of paragraphs to skip (that is: 0-based number of paragraphs read)
     */
    @Override
    public Integer read() {		
	final XmlStringWriter xmlwriter = new XmlStringWriter();

	final Table table = this.readerData.getRange().getTable(this.paragraph);
	final ShadingDescriptor tableShading = this.paragraph.getShading();
	final CSSManager cssmanager = new CSSManager();			
		
	xmlwriter.writeStartElement("table"); 
	cssmanager.putProperty("border-collapse", "collapse");
	cssmanager.putProperty("border-spacing", "0");
	// write table shading
	if (!tableShading.isShdAuto() && !tableShading.getCvFore().toHex().equals("FFFFFF")) {
	    // ShdNil is ignored here; do not write white backgrounds
	    cssmanager.putProperty("background-color", "#" + tableShading.getCvFore().toHex());
	}
	xmlwriter.writeAttribute("style", cssmanager.toString());
		
	final int additionalParagraphsToSkip = detectTableCaption(table.numParagraphs());
	final ProcessedTableData processedTableData = readTable(table);
	if (processedTableData.name != null) xmlwriter.writeAttribute("class", processedTableData.name);
	xmlwriter.writeRaw(processedTableData.xmlwriter.toString());
	xmlwriter.writeEndElement("table");

	// write output	
	this.tableRequirement.getMetadata().setKind(Kind.TABLE);
	this.tableRequirement.setText(new RequirementText(null, xmlwriter.toString()));
	
	this.readerData.getListToRequirementProcessor().setLastRequirement(this.requirement); // make sure any nesting hierarchy gets properly closed
	return (table.numParagraphs()-1 + additionalParagraphsToSkip);
    }


    /**
     * Reads the actual rows and columns of a table
     * 
     * @param table given table data
     * @return formatted XML data of the entire table
     * @throws IllegalStateException if certain internal data is not available
     */
    /**
     * @param table table to read
     * @return
     */
    private ProcessedTableData readTable(final Table table) {	
	assert table != null;
	
	final class ParallelHeaddataDetermination {	    	    	    
	    TableMatcher tableMatcher = null;
	    TableDimensionsManager tableDimensionsManager = null;

	    /**
	     * Runs independent preprocessing tasks for the current table in parallel
	     */
	    public ParallelHeaddataDetermination() {
		final ExecutorService pool = ParallelExecutor.createThreadPool("TableHeaddataDetermination", 2).getExecutorService();

		// run our expensive calculations in parallel
		final Future<TableDimensionsManager> tableDimensionsManagerFuture = pool.submit(new Callable<TableDimensionsManager>(){
		    @Override
		    public TableDimensionsManager call() {
			return new TableDimensionsManager(table);
		    }
		});
		final Future<TableMatcher> tableMatcherFuture = pool.submit(new Callable<TableMatcher>(){
		    @Override
		    public TableMatcher call() {
			return new TableMatcher(table);
		    }
		});
		pool.shutdown();

		// collect the results
		try {
		    this.tableMatcher = tableMatcherFuture.get();
		} catch (InterruptedException | ExecutionException e) {		    
		    logger.log(Level.SEVERE, "Error while determining tableMatcher data.", e);
		    // make sure to propagate some exception so we eventually halt the program
		    throw new IllegalStateException(e);
		}

		try {
		    this.tableDimensionsManager = tableDimensionsManagerFuture.get();
		} catch (InterruptedException | ExecutionException e) {
		    logger.log(Level.SEVERE, "Error while determining tableDimensions data.", e);
		    // make sure to propagate some exception so we eventually halt the program
		    throw new IllegalStateException(e);
		}
	    }

	    public TableDimensionsManager getTableDimensionsManager() {
		assert this.tableDimensionsManager != null;
		return this.tableDimensionsManager;
	    }

	    public TableMatcher getTableMatcher() {
		assert this.tableMatcher != null;
		return this.tableMatcher;
	    }
	}
	
	// 
	// actual start of this method
	//
	final XmlStringWriter xmlwriter = new XmlStringWriter();	
	
	final ParallelHeaddataDetermination headdata = new ParallelHeaddataDetermination();
	// extract the cell dimensions of this table
	final TableDimensionsManager tableDimensionsManager = headdata.getTableDimensionsManager();
	// check if this table matches with any predefined abstract table definitions
	final TableMatcher tableMatcher = headdata.getTableMatcher();
	// check if there are any overrides for the cell contents
	final TableContentOverrideManager tableOverrideManager = new TableContentOverrideManager(this.readerData, table, tableDimensionsManager, tableMatcher);
	
	for(int rn=0; rn<table.numRows(); rn++) {			
	    final TableRow row = table.getRow(rn);
	    
	    // circumvent a bug in WORD when it spits out ghost rows; triggers for subset026, 3.3.0, 7.5.1.78 but (sadly) is not testable in isolation
	    if (row.numCells() == 0) continue;	    
	    
	    RequirementOrdinary rowRequirement = null; // conservatively assume this row does not contain traceworthy cells
	    xmlwriter.writeStartElement("tr");
	    int mergeOffset = 0;
	    for(int cn=0; cn<row.numCells(); cn++) {
		final TableCell cell = row.getCell(cn);             

		// do not process non-first merged cells as they are empty by definition
		if (TableHelper.isMerged(cell)) continue;

		xmlwriter.writeStartElement("td");		
		final TableDimensionsManager.CellData cellData = tableDimensionsManager.getCellData(rn, cn);
		if (cellData == null) throw new IllegalStateException("Lacking cell data. This should never happen.");
		(new TableCellPropertiesDeterminerCSS(cell, cellData, xmlwriter)).read();		
		final RequirementText overrideCellContent = tableOverrideManager.getOverrideText(rn, cn); // may be null
		mergeOffset = mergeOffset + cellData.getColspan()-1;
		final int columnNumber = cn + mergeOffset;
		final RequirementTemporary cellRequirement;		

		
		final String cellContent;
		cellIdDetermination: {					
		    if (tableMatcher.matchFound()) {
			// Case 1: structure of table *is* known; use special ids

			final TraceabilityManagerHumanReadable hrManagerCell = tableMatcher.getTraceabiltyManagerHumanReadable(rn, cn, columnNumber);			
			final boolean traceQualifierOverrideData = !tableMatcher.onlyTraceIfContentIsOverridden(rn, cn) ? true : (overrideCellContent != null);  
			if (hrManagerCell != null && traceQualifierOverrideData) {
			    assert hrManagerCell.getCurrentTagType() == TraceabilityManagerHumanReadable.TagType.COLUMN;
			    rowRequirement = createRowRequirement(row, hrManagerCell, rowRequirement);			    			    

			    cellRequirement = new RequirementOrdinary(this.readerData, cell, hrManagerCell, rowRequirement, this.tableRequirement);
			    this.readerData.getListToRequirementProcessor().getListReader().addNestingLevel(cellRequirement, cell, NestingType.TABLE_CELL);
			    new RequirementReaderCell(this.readerData, cellRequirement, overrideCellContent, tableMatcher.isCellSplitAllowed(rn, cn)).read(hrManagerCell);
			    cellContent = cellRequirement.getText().getRichWithTraceTags();							
			    break cellIdDetermination;
			}
		    }
		    else {
			// Case 2: structure of table *is not* known; use ordinary row/column-traceids

			if (!isTableHeader(row, rn)){
			    rowRequirement = createRowRequirement(row, rn, rowRequirement);
			    final TraceabilityManagerHumanReadable hrManagerCell = new TraceabilityManagerHumanReadable();
			    hrManagerCell.addColumn(columnNumber);
			    cellRequirement = new RequirementOrdinary(this.readerData, cell, hrManagerCell, rowRequirement);
			    this.readerData.getListToRequirementProcessor().getListReader().addNestingLevel(cellRequirement, cell, NestingType.TABLE_CELL);
			    new RequirementReaderCell(this.readerData, cellRequirement, overrideCellContent, true).read(rn, columnNumber);
			    cellContent = cellRequirement.getText().getRichWithTraceTags();
			    break cellIdDetermination;
			}
		    }

		    // Case 3: generic fallback (can be triggered from case 1 and 2)
		    // do not trace anything but do process for richtext table output
		    cellRequirement = new RequirementTemporary(cell);
		    this.readerData.getListToRequirementProcessor().getListReader().addNestingLevel(cellRequirement, cell, NestingType.TABLE_CELL);
		    new RequirementReaderCell(this.readerData, cellRequirement, overrideCellContent, true).read();
		    assert cellRequirement.getText().getRichWithTraceTags() == null : "We should not have traceTags here.";
		    cellContent = cellRequirement.getText().getRich();
		}

		xmlwriter.writeRaw(cellContent);
		this.readerData.getListToRequirementProcessor().getListReader().removeNestingLevel();
		xmlwriter.writeEndElement("td");
	    }
	    xmlwriter.writeEndElement("tr");
	}	

	return new ProcessedTableData(xmlwriter, tableMatcher.getCSSName());
    }


    /**
     * Creates a row requirement if it does not exist yet
     * <p>This avoids having empty row requirements when there are no traceworthy cells in the respective row.</p>
     * 
     * @param row Range the new row requirement will be associated to 
     * @param rowNumber number of the row for which to create the requirement
     * @param rowRequirement reference to the existing row Requirement
     * @return the existing row Requirement if it already exists or a shiny new one
     */
    private RequirementOrdinary createRowRequirement(final TableRow row, final int rowNumber, final RequirementOrdinary rowRequirement) {
	if (rowRequirement == null) {
	    assert row != null;
	    assert rowNumber >= 0;
	    final TraceabilityManagerHumanReadable hrManagerRow = new TraceabilityManagerHumanReadable();
	    hrManagerRow.addRow(rowNumber);
	    final RequirementOrdinary rowRequirementFresh = new RequirementOrdinary(this.readerData, row, hrManagerRow, this.tableRequirement);
	    rowRequirementFresh.getMetadata().setKind(Kind.TABLE);
	    return rowRequirementFresh;
	}
	return rowRequirement;
    }

    private RequirementOrdinary createRowRequirement(final TableRow row, final TraceabilityManagerHumanReadable hrManagerSource, final RequirementOrdinary rowRequirement) {
	if (rowRequirement == null) {
	    assert row != null;
	    final TraceabilityManagerHumanReadable hrManagerRow = new TraceabilityManagerHumanReadable(hrManagerSource, TraceabilityManagerHumanReadable.TagType.ROW);
	    final RequirementOrdinary rowRequirementFresh = new RequirementOrdinary(this.readerData, row, hrManagerRow, this.tableRequirement);
	    rowRequirementFresh.getMetadata().setKind(Kind.TABLE);
	    return rowRequirementFresh;
	}
	return rowRequirement;
    }


    /**
     * Check for valid tableHeader property according to [MS-DOC], v20140721, 2.6.3, sprmTTableHeader
     * 
     * @param row row to be checked
     * @param rowNum 0-based index of the given row in the table
     * @return {@code true} if this row belongs to the table header; {@code false} otherwise
     */
    private boolean isTableHeader(final TableRow row, final int rowNum) {
	assert rowNum > TABLE_HEAD_SET_FOR_ROW_INITIAL : "rowNum is not greater than the initial value of our internal row counter. This is bad.";
	if (row.isTableHeader()) {
	    if (rowNum == this.tableHeadSetForRow) {
		return true;
	    }
	    else if (rowNum == this.tableHeadSetForRow +1) {
		this.tableHeadSetForRow = rowNum;
		return true;
	    }			
	}
	return false;
    }

    /**
     * extract a table caption from the word input stream
     * 
     * @param tableOffset paragraph offset where the caption is expected to be
     * @return number of paragraphs to skip because of the caption; side effect: setup {@code tableRequirement}
     */
    private int detectTableCaption(final int tableOffset) {
	final int absoluteStartOffset = this.initialParagraphIndex + tableOffset;
	final CaptionReader captionReader = new CaptionReader(this.readerData, absoluteStartOffset, this.requirement);		
	final int paragraphsToSkip = captionReader.detectTableCaption();
	this.tableRequirement = captionReader.getResultingRequirement();
	assert this.tableRequirement != null;
	return paragraphsToSkip;		
    }
    
    /**
     * Wrapper for extracted table data
     */
    private final static class ProcessedTableData {
	/**
	 * xhtml content of the table
	 */
	public final XmlStringWriter xmlwriter;
	/**
	 * name of the extracted table
	 */
	public final String name;
	
	public ProcessedTableData(final XmlStringWriter xmlwriter, final String name) {
	    assert xmlwriter != null; // name may be null
	    this.xmlwriter = xmlwriter;
	    this.name = name;
	}
    }
    

    /**
     * Determine the properties of a table cell and emit them as CSS
     */
    private final static class TableCellPropertiesDeterminerCSS implements GenericReader<Void> {
	private final transient XmlStringWriter xmlwriter;
	private final transient TableCell cell;
	private final transient TableDimensionsManager.CellData cellData;
	private final transient CSSManager cssmanager = new CSSManager();

	/**
	 * @param cell cell under consideration
	 * @param cellData metadata of the cell under consideration
	 * @param xmlwriter writer to use for the output
	 */
	public TableCellPropertiesDeterminerCSS(final TableCell cell, final TableDimensionsManager.CellData cellData, final XmlStringWriter xmlwriter) {
	    assert cell != null;
	    assert cellData != null;
	    assert xmlwriter != null;
	    this.cell = cell;
	    this.xmlwriter = xmlwriter;
	    this.cellData = cellData;
	}

	/* (non-Javadoc)
	 * @see docreader.GenericReader#read()
	 */
	@Override
	public Void read() {
	    //call all internal handlers
	    this.setColSpan();
	    this.setRowSpan();
	    this.setBgColor();
	    this.setBorder();
	    this.setWidth();
	    this.setVAlign();
	    this.setTextRotation();			

	    //write output
	    this.xmlwriter.writeAttribute("style", this.cssmanager.toString());

	    return null;
	}

	/**
	 * Feeds {@link BorderManager} with correct input data
	 */
	private void setBorder() {
	    //order matches that of the resulting css
	    final BorderCode[] bordercodes = {this.cell.getBrcTop(), this.cell.getBrcRight(), this.cell.getBrcBottom(), this.cell.getBrcLeft()};			
	    // TODO next line is not correct if we have multiple paragraphs per cell
	    final int[] paddingSupplement = {this.cell.getParagraph(0).getSpacingBefore(), this.cell.getParagraph(0).getIndentFromRight(), this.cell.getParagraph(this.cell.numParagraphs()-1).getSpacingAfter(), this.cell.getParagraph(0).getIndentFromLeft()};

	    (new BorderManager(this.cssmanager, bordercodes, paddingSupplement)).writeBorderProperties();
	}

	/**
	 * Calculate the width of a table cell
	 */
	private void setWidth() {
	    // POI's this.cell.getWidth() seems to *always* return twips
	    // hence, it remains unclear when getFtsWidth() should actually be used
	    // confer with docreader.range.TableDataManager.TableDataManager(Table)

	    int width = this.cell.getWidth();
	    final byte widthQualifier = this.cell.getFtsWidth(); 

	    // [MS-DOC], v20140721, 2.9.101
	    if (width == 0x00 || widthQualifier == 0x01) {
		return; // undefined value
	    }	    

	    if (widthQualifier == 0x02) {
		logger.log(Level.FINE, "Width of a table cell is given in percent. Cannot handle that. Will not write any width data.");
		return;
	    }
	    else if (widthQualifier == 0x03 || widthQualifier == 0x13) {
		width = (int) (width / 20.0); // measured in twips
		this.cssmanager.putProperty("width", width + "pt");
	    }
	}

	/**
	 * Determine the Text rotation
	 * <p>according to [MS-DOC], v20140721, 2.9.95</p>
	 * 
	 * <p>As this emits a pure CSS3-property it is currently unlikely to become rendered.<br />
	 * See also the comment in {@link #setVAlign()}.</p>
	 */
	private void setTextRotation() {
	    if (this.cell.isVertical()) {
		if (this.cell.isBackward()) {
		    // bottom to top
		    this.cssmanager.putProperty("transform", "rotate(-90deg)");
		    return;
		}		
		// top to bottom
		this.cssmanager.putProperty("transform", "rotate(90deg)");
	    }							
	}

	/**
	 * Determine horizontally merged cells
	 */
	private void setColSpan() {	    
	    final int colspan = this.cellData.getColspan();
	    if (colspan > 1) {
		this.xmlwriter.writeAttribute("colspan", Integer.toString(colspan)); // attribute for td
	    }
	}

	/**
	 * Determine vertically merged cells
	 */
	private void setRowSpan() {	    
	    final int rowspan = this.cellData.getRowspan();
	    if (rowspan > 1) {
		this.xmlwriter.writeAttribute("rowspan", Integer.toString(rowspan)); // attribute for td
	    }
	}

	/**
	 * Determine the vertical alignment of the contents of a table cell
	 * <p>according to [MS-DOC], v20140721, 2.9.341</p>
	 * 
	 * <p>If the text is also rotated (i.e. {@link #setTextRotation()} also applies) the alignment
	 * sometimes leads to strange results. But thats purely a rendering issue and since text-rotation is
	 * <ul>
	 * <li>only used sparingly</li>
	 * <li>a cutting edge CSS-feature, which barely becomes rendered at the moment, anyways</li>
	 * </ul>
	 * we do not investigate further here.</p>  
	 */
	private void setVAlign() {
	    final byte valign = this.cell.getVertAlign();
	    final String output;

	    switch(valign) {
	    case 0x00: output = "top"; break;
	    case 0x01: output = "middle"; break;
	    case 0x02: output = "bottom"; break;
	    default: logger.log(Level.WARNING, "Illegal vAlign value encountered. Word document malformed. Will skip this styling."); return;
	    }
	    this.cssmanager.putProperty("vertical-align", output);
	}

	/**
	 * Determine the background color of a table-cell
	 */
	private void setBgColor() {
	    if (this.cell.getShd().isShdAuto()) return;

	    String foreground = this.cell.getShd().getCvFore().toHex();
	    final String background = this.cell.getShd().getCvBack().toHex();
	    final int ipat = this.cell.getShd().ipatToAlpha();					

	    if (ipat == 0) {
		// just take the background
		this.cssmanager.putProperty("background-color", "#"+background);
	    }
	    else {
		// this is somewhat an abuse of ipat again (determines the tilting of the gradient). But we just want to do something with this nasty value...
		// ipat is in the range 0..100, degrees are 0..360 so we do some scaling here
		if (foreground.equals(background)) {
		    foreground = "FFFFFF"; // our gradient does not make sense if the colors are equal. So hard-set it to white.
		}		
		
		this.cssmanager.putProperty("background-color", mixColors(foreground, background, ipat));	        	
	    }	                
	}
	
	/**
	 * Mixes two colors into one.
	 * <p><em>Note:</em> This does try to do something useful with the {@code ipat},
	 * but it does <em>not</em> resemble the original formatting that Word would apply.
	 * It only makes different combinations of (color#1, color#2, ipat) distinguishable.</p>
	 * 
	 * @param foreground color in hexadecimal notation
	 * @param background color in hexadecimal notation
	 * @param ipat normalized ipat value
	 * @return an rgb representation of the new color for use as a css style argument
	 */
	@DomainSpecific
	private static String mixColors(final String foreground, final String background, final int ipat) {
	    assert foreground.length() == 6 && background.length() == 6;
	    
	    final String[] foregroundComponents = {foreground.substring(0, 2), foreground.substring(2, 4), foreground.substring(4, 6) };
	    final int[] foregroundComponentsDecimal = new int[3];
	    final String[] backgroundComponents = {background.substring(0, 2), background.substring(2, 4), background.substring(4, 6) };
	    final int[] backgroundComponentsDecimal = new int[3];
	    final StringBuilder output = new StringBuilder();
	    output.append("rgb(");
	    String delimiter = "";
	    
	    for (int i = 0; i<backgroundComponents.length; i++) {
		output.append(delimiter);
		delimiter = ", ";
		// Step 1: RGB is an additive color model; so we need to subtract the values from 255 first
		backgroundComponentsDecimal[i] = 255 - Integer.parseInt(backgroundComponents[i], 16);
		backgroundComponentsDecimal[i] = (int) Math.round(backgroundComponentsDecimal[i] * ipat / 100.0);
		foregroundComponentsDecimal[i] = 255 - Integer.parseInt(foregroundComponents[i], 16);
		int currentValue = backgroundComponentsDecimal[i] - foregroundComponentsDecimal[i];
		if (currentValue < 0) currentValue = 0;
		// Step 2: and then subtract again
		output.append(255 - currentValue);
	    }
	    
	    output.append(')');	    
	    return output.toString();
	}
    }
}