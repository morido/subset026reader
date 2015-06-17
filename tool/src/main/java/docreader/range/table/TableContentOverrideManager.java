package docreader.range.table;

import java.util.logging.Level;
import java.util.logging.Logger;

import helper.HashMap2D;
import helper.TableHelper;
import helper.TraceabilityManagerHumanReadable;
import helper.XmlStringWriter;
import helper.annotations.DomainSpecific;
import helper.formatting.ArrowTextFormatter;
import helper.subset26.tables.TableMatcher;
import helper.word.DataConverter;

import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;

import docreader.ReaderData;
import docreader.list.ListReader;
import docreader.range.table.TableDimensionsManager.CellData;
import docreader.range.paragraph.ParagraphListAware;
import docreader.range.paragraph.characterRun.OfficeDrawingReader;
import docreader.range.paragraph.characterRun.OfficeDrawingReader.ArrowData;
import requirement.data.RequirementText;

/**
 * Manages overwriting information for the contents of table cells
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
final class TableContentOverrideManager {
    private final transient TableDimensionsManager tableDimensionsManager;
    private final transient OfficeDrawingReader officeDrawingReader;
    private final transient TableMatcher tableMatcher;
    private final transient ListReader listReader;
    private static final Logger logger = Logger.getLogger(TableContentOverrideManager.class.getName()); // NOPMD - Reference rather than a static field    
    
    /**
     * Internal store for override data; first index is row, second is column; not guaranteed to be rectangular or consecutive 
     */
    private final transient HashMap2D<Integer, Integer, CellOverrideData> overrideData = new HashMap2D<>();

    /**
     * Create a new override manager
     * 
     * @param readerData global readerData
     * @param table Table for which to create the manager
     * @param tableDataManager cell dimension data
     * @param tableMatcher cell tracing data
     */
    public TableContentOverrideManager(final ReaderData readerData, final Table table, final TableDimensionsManager tableDataManager, final TableMatcher tableMatcher) {
	assert readerData != null && table != null && tableDataManager != null && tableMatcher != null;
	this.officeDrawingReader = readerData.getOfficeDrawingReader();
	this.tableDimensionsManager = tableDataManager;
	this.tableMatcher = tableMatcher;
	this.listReader = readerData.getListToRequirementProcessor().getListReader();

	// Note: do not change the running direction of the row/column loops since the listreader depends on this
	for (int rowNumber = 0; rowNumber < table.numRows(); rowNumber++) {
	    final TableRow row = table.getRow(rowNumber);
	    for (int columnNumber = 0; columnNumber < row.numCells(); columnNumber++) {
		final TableCell cell = row.getCell(columnNumber);
		if (TableHelper.isMerged(cell)) continue;
		final ArrowSourceTargetManager arrowSourceTargetManager = new ArrowSourceTargetManager(new CellID(rowNumber, columnNumber));		
		for (int paragraphNumber = 0; paragraphNumber < cell.numParagraphs(); paragraphNumber++) {
		    final Paragraph paragraph = cell.getParagraph(paragraphNumber);		    
		    computeOverrides(new ParagraphListAware(readerData, paragraph), new CellID(rowNumber, columnNumber));		    
		    for (int characterRunNumber = 0; characterRunNumber < paragraph.numCharacterRuns(); characterRunNumber++) {
			final CharacterRun characterRun = paragraph.getCharacterRun(characterRunNumber);
			if (OfficeDrawingReader.hasOfficeDrawing(characterRun)) {
			    computeOverrides(characterRun, arrowSourceTargetManager);
			}
		    }
		}
	    }
	}
    }

    /**
     * Get the override text for a certain cell
     * 
     * @param rowNum row number of the cell (0-based)
     * @param columnNum column number of the cell (0-based)
     * @return RequirementText without a traceTag-field or {@code null} if there is no override for the specified cell
     */
    public RequirementText getOverrideText(final int rowNum, final int columnNum) {
	final CellOverrideData cellOverrideData = this.overrideData.get(rowNum, columnNum);
	return (cellOverrideData != null) ? cellOverrideData.getRequirementText() : null; // NOPMD - intentional null assignment
    }

    /**
     * Flatten a list numbering into the table cell contents
     * 
     * @param paragraph paragraph inside a table cell
     * @param currentCell coordinates of the current cell
     */
    private void computeOverrides(final ParagraphListAware paragraph, final CellID currentCell) {
	assert paragraph != null;

	// check if this paragraph is part of a list and does not contain any text	
	if (paragraph.isInList() && "".equals(DataConverter.cleanupText(paragraph.getParagraph().text()))) {	    	  
	    final String numberText = this.listReader.processParagraphPlain(paragraph);
	    if (!"".equals(numberText)) {
		// TODO remove formatter
		final XmlStringWriter xmlwriter = new XmlStringWriter();		
		xmlwriter.writeCharacters(numberText);		
		getOverrideDataHandle(currentCell).append(numberText, xmlwriter);
	    }
	}
    }
    
    
    /**
     * Flatten an officeDrawing (=arrows) into the table cell contents
     * 
     * @param characterRun characterRun which may contain an office drawing
     * @param arrowSourceTargetManager manager for arrow dimensions relative to the table
     */
    @DomainSpecific
    private void computeOverrides(final CharacterRun characterRun, final ArrowSourceTargetManager arrowSourceTargetManager) {	
	assert characterRun != null && characterRun.isSpecialCharacter();
	assert arrowSourceTargetManager != null;
	final String text = characterRun.text();

	for (int i = 0; i < text.length(); i++) {
	    if (OfficeDrawingReader.isOfficeDrawingChar(text.charAt(i))) {
		final int characterOffset = characterRun.getStartOffset() + i;
		final ArrowData arrowData = this.officeDrawingReader.extractArrow(characterOffset);
		if (arrowData == null) {
		    logger.log(Level.WARNING, "Current cell contains an OfficeDrawing which is not an arrow. No idea how to formalize that. Will skip.");		    
		}
		else {
		    assert arrowData != null;
		    final CellID sourceCell = arrowSourceTargetManager.findArrowSourceCell(arrowData);
		    final CellID targetCell = arrowSourceTargetManager.findArrowTargetCell(arrowData);
		    if (sourceCell.equals(targetCell)) {
			logger.log(Level.WARNING, "Current cell contains an arrow which does not span several cells. No idea how to formalize that. Will skip.");			
		    }
		    else {			
			final String rawText = "ARROW TO: " + targetCell.getTargetTraceId();
			final XmlStringWriter xmlwriter = new XmlStringWriter();
			final ArrowTextFormatter arrowTextFormatter = new ArrowTextFormatter(xmlwriter);
			arrowTextFormatter.writeStartElement();
			xmlwriter.writeCharacters(rawText);
			arrowTextFormatter.writeEndElement();
			getOverrideDataHandle(sourceCell).append(rawText, xmlwriter);
			// make sure the targetCell also contains (at least empty) override data so all those beautifully hand-crafted (but meaningless) "arrow end bullets" in the table cells vanish reliably
			getOverrideDataHandle(targetCell);
		    }
		}
	    }
	}
    }


    /**
     * Get a handle (pointer) to override data for a given cell
     * 
     * @param cellID id of the cell for which to get a handle
     * @return existing override data or fresh data if there was no stored data yet; never {@code null}
     */
    private CellOverrideData getOverrideDataHandle(final CellID cellID) {
	assert cellID != null;
	final int rowNum = cellID.rowNum;
	final int columnNum = cellID.columnNum;
	CellOverrideData cellOverrideData = this.overrideData.get(rowNum, columnNum);

	if (cellOverrideData == null) {
	    final CellOverrideData freshOverrideData = new CellOverrideData();	    
	    this.overrideData.put(rowNum, columnNum, freshOverrideData);
	    cellOverrideData = freshOverrideData;
	}

	return cellOverrideData;
    }


    /**
     * Immutable class to store a reference to a specific cell     
     */
    private final class CellID {
	public final int rowNum; // NOPMD - intentionally non-transient
	public final int columnNum; // NOPMD - intentionally non-transient

	public CellID (final int rowNum, final int columnNum) {
	    assert rowNum >= 0 && columnNum >= 0;
	    this.rowNum = rowNum;
	    this.columnNum = columnNum;
	}

	/**
	 * @return human-readable tracestring of the cell which this object references
	 */
	public String getTargetTraceId() {
	    String output;
	    findTraceTag: {
		if (TableContentOverrideManager.this.tableMatcher.matchFound()) {
		    // use special ids
		    output = TableContentOverrideManager.this.tableMatcher.getTraceabiltyManagerHumanReadable(this.rowNum, this.columnNum, this.columnNum).getTag(); // Note: this means we cannot have merged cells in the underlying table
		    if (output != null) break findTraceTag;

		    assert output == null;
		    // output == null means the user made the target cell non-traceable; use fallback
		    logger.log(Level.INFO, // NOPMD - intentional unconditional logging here
			    "Arrow points to non-traced cell ({0}, {1}). Is this what you intended? Otherwise please check your abstract table definitions. Will use ordinary row/cell-Ids for now.",
			    new Object[]{Integer.toString(this.rowNum+1), Integer.toString(this.columnNum+1)}
			    );
		}

		// fallback; ordinary ids
		final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();
		hrManager.addRow(this.rowNum);
		hrManager.addColumn(this.columnNum);
		output = hrManager.getTag();		
	    }
	    assert output != null;
	    return output;
	}

	@Override
	public int hashCode() {
	    // autogenerated by Eclipse
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + getOuterType().hashCode();
	    result = prime * result + this.columnNum;
	    result = prime * result + this.rowNum;
	    return result;
	}

	@Override
	public boolean equals(final Object obj) {
	    // autogenerated by Eclipse
	    if (this == obj)
		return true;
	    if (obj == null)
		return false;
	    if (getClass() != obj.getClass())
		return false;
	    final CellID other = (CellID) obj;
	    if (!getOuterType().equals(other.getOuterType()))
		return false;
	    if (this.columnNum != other.columnNum)
		return false;
	    if (this.rowNum != other.rowNum)
		return false;
	    return true;
	}

	private TableContentOverrideManager getOuterType() {
	    // autogenerated by Eclipse
	    return TableContentOverrideManager.this;
	}	
    }

    private final static class CellOverrideData {
	private final transient StringBuilder rawText = new StringBuilder(); // NOPMD - intentional StringBuilder field
	private final transient XmlStringWriter richText = new XmlStringWriter();
	private transient boolean isEmpty = true;

	public void append(final String raw, final XmlStringWriter rich) {
	    assert raw != null && rich != null;	    

	    // prepend the next entry with line delimiters? true for all entries 2..n
	    if (!this.isEmpty) {
		this.rawText.append("\n");
		this.richText.writeCombinedStartEndElement("br");
	    }
	    else this.isEmpty = false;		

	    // write the next entry
	    this.rawText.append(raw);
	    this.richText.writeRaw(rich.toString());
	}

	public RequirementText getRequirementText() {
	    return new RequirementText(this.rawText.toString(), this.richText.toString());
	}
    }

    private final class ArrowSourceTargetManager {
	private final transient CellID currentCell;
	private final transient int currentCellLeft;
	private final transient int currentCellTop;

	public ArrowSourceTargetManager(final CellID currentCell) {
	    assert currentCell != null;
	    this.currentCell = currentCell;
	    final CellData cellData = TableContentOverrideManager.this.tableDimensionsManager.getCellData(currentCell.rowNum, currentCell.columnNum);
	    this.currentCellLeft = cellData.getLeft();
	    this.currentCellTop = cellData.getTop();
	}

	@DomainSpecific
	public CellID findArrowSourceCell(final ArrowData arrowData) {
	    assert arrowData != null;	    
	    final CellID output;
	    final int absoluteTop = this.currentCellTop + arrowData.getTop();
	    final int indexOfRowContainingArrow = findMatchingRow(absoluteTop);
	    
	    switch (arrowData.getDirection()) {
	    case LeftToRight:
		final int absoluteLeft = this.currentCellLeft + arrowData.getLeft();
		output = findMatchingCell(indexOfRowContainingArrow, absoluteLeft);
		break;
	    case RightToLeft:
		final int absoluteRight = this.currentCellLeft + arrowData.getRight();
		output = findMatchingCell(indexOfRowContainingArrow, absoluteRight);
		break;
	    default: // cannot happen; but case is necessary for static write check of final output variable
		throw new IllegalStateException();
	    }
	    return output;
	}

	@DomainSpecific
	public CellID findArrowTargetCell(final ArrowData arrowData) {
	    assert arrowData != null;
	    final CellID output;
	    final int absoluteTop = this.currentCellTop + arrowData.getTop();
	    final int indexOfRowContainingArrow = findMatchingRow(absoluteTop);

	    switch (arrowData.getDirection()) {
	    case LeftToRight:
		final int absoluteRight = this.currentCellLeft + arrowData.getRight();
		output = findMatchingCell(indexOfRowContainingArrow, absoluteRight);
		break;
	    case RightToLeft:
		final int absoluteLeft = this.currentCellLeft + arrowData.getLeft();
		output = findMatchingCell(indexOfRowContainingArrow, absoluteLeft);
		break;
	    default: // cannot happen; but case is necessary for static write check of final output variable
		throw new IllegalStateException();		
	    }
	    return output;
	}

	/**
	 * Find a table column which spans over (includes) the given offset
	 * 
	 * @param indexOfRowContainingArrow index of the row (0-based) which contains the visual representation of the arrow
	 * @param absoluteOffset offset in twips measured from the left edge of the leftmost column of the table
	 * @return ID of the cell which includes the given offset
	 */
	@DomainSpecific
	private CellID findMatchingCell(final int indexOfRowContainingArrow, final int absoluteOffset) {
	    // Assumption: part of the table containing the arrows is rectangular
	    final CellData currentCellData = TableContentOverrideManager.this.tableDimensionsManager.getCellData(indexOfRowContainingArrow, this.currentCell.columnNum);
	    assert currentCellData != null;

	    // Assumption: we are only moving horizontally
	    int columnNum = this.currentCell.columnNum;
	    final int firstColumnNum = 0;
	    final int lastColumnNum = TableContentOverrideManager.this.tableDimensionsManager.getLastColumnIndexForRow(indexOfRowContainingArrow); // autoboxing is safe; get() will never return null here
	    offsetDeterminer: {		
		if (absoluteOffset < currentCellData.getLeft()) {		
		    do {
			columnNum--;
			while (TableContentOverrideManager.this.tableDimensionsManager.getCellData(indexOfRowContainingArrow, columnNum) == null) {			    			    
			    if (columnNum < firstColumnNum) {
				// exit condition; out of range
				logger.log(Level.WARNING, "Arrow points out of the left edge of the table. This is strange. Will assume it points to the leftmost cell.");
				columnNum = firstColumnNum;
				break offsetDeterminer;
			    }
			    // skip merged cells
			    columnNum--;
			}
		    } while (absoluteOffset < TableContentOverrideManager.this.tableDimensionsManager.getCellData(indexOfRowContainingArrow, columnNum).getLeft());		

		}
		else if (absoluteOffset > currentCellData.getRight()) {
		    do {
			columnNum++;
			while (TableContentOverrideManager.this.tableDimensionsManager.getCellData(indexOfRowContainingArrow, columnNum) == null) {
			    if (columnNum > lastColumnNum) {
				// exit condition; out of range
				logger.log(Level.WARNING, "Arrow points out of the right edge of the table. This is strange. Will assume it points to the rightmost cell.");
				columnNum = lastColumnNum;
				break offsetDeterminer;
			    }
			    // skip merged cells
			    columnNum++;
			}
		    } while (absoluteOffset > TableContentOverrideManager.this.tableDimensionsManager.getCellData(indexOfRowContainingArrow, columnNum).getRight());
		}				
	    }

	    assert TableContentOverrideManager.this.tableDimensionsManager.getCellData(indexOfRowContainingArrow, columnNum) != null;
	    return new CellID(indexOfRowContainingArrow, columnNum);
	}
	
	
	/**
	 * Find a table row which spans over (includes) the given offset
	 * 
	 * @param absoluteOffset offset in twips measured from the top edge of the first row of the table
	 * @return index of the row which includes the given offset (0-based)
	 */
	@DomainSpecific
	private int findMatchingRow(final int absoluteOffset) {
	    final CellData currentCellData = TableContentOverrideManager.this.tableDimensionsManager.getCellData(this.currentCell.rowNum, this.currentCell.columnNum);
	    
	    final int columnNum = this.currentCell.columnNum;
	    int rowNum = this.currentCell.rowNum;
	    final int firstRowNum = 0;
	    final int lastRowNum = TableContentOverrideManager.this.tableDimensionsManager.getIndexOfLastRow();	    
	    
	    offsetDeterminer: {
		if (absoluteOffset < currentCellData.getTop()) {
		    do {
			rowNum--;
			while (TableContentOverrideManager.this.tableDimensionsManager.getCellData(rowNum, columnNum) == null) {
			    if (rowNum < firstRowNum) {
				// exit condition; out of range
				logger.log(Level.WARNING, "Arrow is above the table. This is strange. Will assume it belongs to the first row.");
				rowNum = firstRowNum;
				break offsetDeterminer;
			    }
			    // skip merged cells
			    rowNum--;
			}
		    } while (absoluteOffset < TableContentOverrideManager.this.tableDimensionsManager.getCellData(rowNum, columnNum).getTop());		    
		}
		else if (absoluteOffset > currentCellData.getBottom()) {		    
		    do {
			rowNum++;
			while (TableContentOverrideManager.this.tableDimensionsManager.getCellData(rowNum, columnNum) == null) {
			    if (rowNum > lastRowNum) {
				// exit condition; out of range
				logger.log(Level.WARNING, "Arrow is below the table. This is strange. Will assume it belongs to the last row.");
				rowNum = lastRowNum;
				break offsetDeterminer;
			    }
			    // skip merged cells
			    rowNum++;
			}
		    } while (absoluteOffset > TableContentOverrideManager.this.tableDimensionsManager.getCellData(rowNum, columnNum).getBottom());
		}
	    }
	    assert TableContentOverrideManager.this.tableDimensionsManager.getCellData(rowNum, columnNum) != null;
	    return rowNum;
	}	
    }
}
