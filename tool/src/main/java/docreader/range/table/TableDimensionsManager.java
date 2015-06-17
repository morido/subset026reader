package docreader.range.table;

import helper.HashMap2D;
import helper.TableHelper;
import helper.word.DataConverter;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;

/**
 * Manages dimensions (left/right offset values, ...) of all visible cells in a table
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
final class TableDimensionsManager {
    /**
     * Hashmap to store cellData. First key is row, second is column. Not guaranteed to be rectangular or even continuous.
     */
    private final transient HashMap2D<Integer, Integer, CellData> tableData = new HashMap2D<>();
    private final transient Map<Integer, Integer> rowLastColumnIndex = new HashMap<>();
    private final int indexOfLastRow;
    private static final Logger logger = Logger.getLogger(TableDimensionsManager.class.getName()); // NOPMD - Reference rather than a static field
    /**
     * Immutable store for metadata of a table cell
     */
    public final static class CellData {
	private final int left;
	private final int right;
	private final int top;
	private final int bottom;
	private final int rowspan;
	private final int colspan;

	/**
	 * Create a new metadata store for a table cell
	 * 
	 * @param left computed left value of this cell in twips
	 * @param right computed right value of this cell in twips
	 * @param top computed top value of this cell in twips
	 * @param bottom computed bottom value of this cell in twips
	 * @param rowspan 1-based number of rows this cell spans
	 * @param colspan 1-based number of columns this cell spans
	 */
	public CellData(final int left, final int right, final int top, final int bottom, final int rowspan, final int colspan) {
	    this.left = left;
	    this.right = right;
	    this.top = top;
	    this.bottom = bottom;
	    this.rowspan = rowspan;
	    this.colspan = colspan;
	}

	/**
	 * @return left value of this table cell in twips
	 */
	public int getLeft() {
	    return this.left;
	}

	/**
	 * @return right value of this table cell in twips
	 */
	public int getRight() {
	    return this.right;
	}

	/**
	 * @return top value of this table cell in twips
	 */
	public int getTop() {
	    return this.top;
	}


	/**
	 * @return bottom value of this  table cell in twips
	 */
	public int getBottom() {
	    return this.bottom;
	}

	/**
	 * @return number of rows this table cell spans; always {@code >= 1}
	 */
	public int getRowspan() {
	    return this.rowspan;
	}

	/**
	 * @return number of columns this table cell spans; always {@code >= 1}
	 */
	public int getColspan() {
	    return this.colspan;
	}
    }

    /**
     * Create new TableDataManager and populate internal data store
     * 
     * @param table Table for which to manage data
     */
    public TableDimensionsManager(final Table table) {
	assert table != null;	

	final TableRowspanManager tableRowspanManager = new TableRowspanManager(table);
	final TableColspanManager tableColspanManager = new TableColspanManager(table);

	int top;
	int bottom = 0; // fake bottom value of "row #-1"
	int rowNum;
	for (rowNum = 0; rowNum < table.numRows(); rowNum++) {
	    final TableRow tableRow = table.getRow(rowNum);
	    int columnNum;
	    top = bottom;
	    bottom += getRowHeightInTwips(tableRow);
	    for (columnNum = 0; columnNum < tableRow.numCells(); columnNum++) {		
		final TableCell cell = tableRow.getCell(columnNum);
		if (TableHelper.isMerged(cell)) continue;

		final int left = cell.getLeftEdge();
		final int right = cell.getLeftEdge() + cell.getWidth();		
		final int rowspan = tableRowspanManager.getRowspan(rowNum, columnNum);
		final int colspan = tableColspanManager.getColspan(rowNum, columnNum);
		this.tableData.put(rowNum, columnNum, new CellData(left, right, top, bottom, rowspan, colspan)); // NOPMD - intentional instantiation inside loop		
	    }
	    this.rowLastColumnIndex.put(rowNum, columnNum);
	}
	this.indexOfLastRow = rowNum;
    }

    /**
     * @param rowNum 0-based row number for which to get data
     * @param columnNum 0-based column number for which to get data
     * @return cellData for the given cell or {@code null} if there is no such data
     */
    public CellData getCellData(final int rowNum, final int columnNum) {
	return this.tableData.get(rowNum, columnNum);
    }

    /**
     * @param rowNum index of the row of interest (0-based)
     * @return absolute index of the last column in this row (taking into account any possible merged cells) or {@code null} if the given {@code rowNum} was out of range
     */
    public Integer getLastColumnIndexForRow(final int rowNum) {
	return this.rowLastColumnIndex.get(rowNum);
    }

    /**
     * @return absolute index of the last row in this table
     */
    public int getIndexOfLastRow() {
	return this.indexOfLastRow;
    }


    /**
     * Calculate the row height
     * 
     * @param tableRow row for which to calculate the height
     * @return calculated row height in twips
     */
    private static int getRowHeightInTwips(final TableRow tableRow) {
	final int output;

	// [MS-DOC], v20140721, 2.6.3, sprmTDyaRowHeight
	final int heightRaw = tableRow.getRowHeight(); // may be 0

	if (heightRaw < 0) {
	    // height is an absolute value irrespective of the row's contents
	    output = heightRaw * -1;
	}
	else {
	    // try to find the row height on the basis of the cell contents (i.e. render the stuff as Word would do it)	    
	    // WARNING: This code is based on estimations and may fail miserably

	    // Step 1: find the cell with the longest content in this row
	    // will not work if the largest cell contains an image
	    // if the cell's contents are rotated then Word apparently sets an explicit (minimum) row height
	    int numCharsLongestCell = 0;
	    TableCell cellOfInterest = null;	    
	    for (int i = 0; i < tableRow.numCells(); i++) {
		final TableCell currentCell = tableRow.getCell(i);
		final String currentText = currentCell.text();
		final int numCharsCurrentCell = currentText.length();
		if (numCharsCurrentCell > numCharsLongestCell) {
		    numCharsLongestCell = numCharsCurrentCell;
		    cellOfInterest = currentCell;		    
		}
	    }

	    if (cellOfInterest != null) {
		// there is at least one cell with some text in this row		
		assert cellOfInterest.numParagraphs() > 0;
		final Paragraph firstParagraph = cellOfInterest.getParagraph(0);
		assert firstParagraph.numCharacterRuns() > 0;

		// Step 2: estimate (!) the height of the cell based on the contents
		// use AWT here; this is going to be rough and not anywhere close to Word's own rendering engine but still the best we can do
		final int cellWidthInPixels = DataConverter.twipsToPixels(cellOfInterest.getWidth());
		int height = 0;
		for (int paragraphIterator = 0; paragraphIterator < cellOfInterest.numParagraphs(); paragraphIterator++) {
		    final Paragraph currentParagraph = cellOfInterest.getParagraph(paragraphIterator);
		    if (currentParagraph.isAutoHyphenated()) logger.log(Level.FINE, "Encountered a paragraph inside a table cell which uses auto hyphenation. This *may* lead to strange results for the computed table dimensions."); 

		    final AttributedString attributedString = new AttributedString(currentParagraph.text());		    		    		   
		    for (int characterRunIterator = 0; characterRunIterator < currentParagraph.numCharacterRuns(); characterRunIterator++) {
			final CharacterRun currentRun = currentParagraph.getCharacterRun(characterRunIterator);
			// Note: we are not skipping non-printable characters here which makes the result a little inaccurate
			// Rationale: skipping would complicate the generation of attributedString

			final String fontName = currentRun.getFontName();
			final int fontStyle;
			if (currentRun.isBold() && currentRun.isItalic()) fontStyle = Font.BOLD|Font.ITALIC;	    
			else if (currentRun.isBold()) fontStyle = Font.BOLD;
			else if (currentRun.isItalic()) fontStyle = Font.ITALIC;  
			else fontStyle = Font.PLAIN;
			final int fontSize = (int) Math.round(currentRun.getFontSize() / 2.0);
			final Font currentFont = new Font(fontName, fontStyle, fontSize);
			final int beginIndex = currentRun.getStartOffset() - currentParagraph.getStartOffset();
			final int endIndex = currentRun.getEndOffset() - currentParagraph.getStartOffset();			
			attributedString.addAttribute(TextAttribute.FONT, currentFont, beginIndex, endIndex);

			// [MS-DOC], v20140721, 2.6.1, sprmCIss
			// see also: docreader.range.paragraph.ParagraphReader.CharacterRunReaderRich.read(CharacterRun)
			switch (currentRun.getSubSuperScriptIndex()) {			
			case 0x01: attributedString.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER); break;
			case 0x02: attributedString.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB); break;
			default: break;
			}			
			// [MS-DOC], v20140721, 2.6.1, sprmCHpsKern
			final int kerningThreshold = (int) Math.round(currentRun.getKerning() / 2.0);
			if (kerningThreshold != 0 && fontSize >= kerningThreshold) {
			    attributedString.addAttribute(TextAttribute.KERNING, TextAttribute.KERNING_ON, beginIndex, endIndex);
			}
		    }
		    
		    // [MS-DOC], v20140721, 2.6.2, sprmPFWordWrap
		    // Note: Terminologies of [MS-DOC] and java.text.BreakIterator differ
		    final BreakIterator breakIterator;
		    if (currentParagraph.isWordWrapped()) {
			// breaking at the character level
			breakIterator = BreakIterator.getWordInstance();
		    }
		    else {
			// breaking at the word level (default)
			breakIterator = BreakIterator.getLineInstance();
		    }

		    final int paragraphWidthInPixels = cellWidthInPixels - DataConverter.twipsToPixels(currentParagraph.getIndentFromLeft() + currentParagraph.getIndentFromRight());
		    final double dpiConversionConstant = 0.7246; // .7246 matches Word's original algorithm quite well; close to 72/96 but not quite (why?)
		    final int paragraphWidthDPIAware = (int) Math.round(paragraphWidthInPixels * dpiConversionConstant);  
		    final AttributedCharacterIterator attributedCharacterIterator = attributedString.getIterator();
		    final AffineTransform affineTransform = new AffineTransform();
		    affineTransform.setToScale(96/72, 96/72); // we are working on 96 DPI; Java always assumes 72 DPI, though
		    final FontRenderContext frc = new FontRenderContext(affineTransform, false, true);
		    final LineBreakMeasurer lineBreakMeasurer = new LineBreakMeasurer(attributedCharacterIterator, breakIterator, frc);
		    float heightOfCurrentParagraph = 0;
		    while (lineBreakMeasurer.getPosition() < attributedCharacterIterator.getEndIndex()) {			
			final TextLayout textLayout = lineBreakMeasurer.nextLayout(paragraphWidthDPIAware);
			// TODO why do the following two lines differ?
			// height += (int) Math.ceil(textLayout.getBounds().getHeight());
			heightOfCurrentParagraph += textLayout.getAscent() + textLayout.getDescent() + textLayout.getLeading();
		    }
		    height = (int) Math.ceil(heightOfCurrentParagraph / dpiConversionConstant);
		    height += DataConverter.twipsToPixels(currentParagraph.getSpacingBefore() + currentParagraph.getSpacingAfter());		    
		}

		// Step 3: convert height to twips
		height = DataConverter.pixelsToTwips(height);

		// Step 4: compare against Word's own value which serves as a lower limit
		if (height < heightRaw) {
		    height = heightRaw;
		}

		// save for output
		output = height;
	    }
	    else {
		// no cell with content in this row. Take what Word gives us. (i.e. unconditional Step 4 from above)
		output = heightRaw;
	    }
	}

	assert output >= 0;
	return output;
    }

    /**
     * Manage cells which span more than one row
     */
    private final static class TableRowspanManager {
	private final transient Table table;

	/**
	 * @param table Table for which to create the manager
	 */
	public TableRowspanManager(final Table table) {
	    assert table != null;
	    this.table = table;
	}

	/**
	 * Determine vertically merged cells
	 * 	 
	 * @param rowNum row number of the cell to process
	 * @param columnNum column number of the cell to process
	 * @return the number of rows this cell spans (1-based)
	 */
	public int getRowspan(final int rowNum, final int columnNum) {
	    assert rowNum < this.table.numRows();    
	    final TableRow rowToProcess = this.table.getRow(rowNum);
	    assert columnNum < rowToProcess.numCells();
	    final TableCell baseCell = rowToProcess.getCell(columnNum);
	    int numberOfCellsToMerge = 1;

	    if (baseCell.isFirstVerticallyMerged()) {
		int iterator = rowNum+1;
		while (iterator < this.table.numRows() && this.table.getRow(iterator).getCell(columnNum).isVerticallyMerged() 
			&& !this.table.getRow(iterator).getCell(columnNum).isFirstVerticallyMerged()) { // this triggers if we have two adjacent groups of merged cells
		    numberOfCellsToMerge++;
		    iterator++;
		}
	    }

	    assert numberOfCellsToMerge >= 1;
	    return numberOfCellsToMerge;
	}

    }


    /**
     * Manage cells which span more than one column
     */
    private final static class TableColspanManager {
	private final transient Table table;
	private final transient Set<Integer> cellEdges = new TreeSet<>();

	/**
	 * Writes an ordered set of cell edges (i.e. the absolute values of the left and right boundaries of a table cell in twips).
	 * 
	 * <p>based on {@link org.apache.poi.hwpf.converter.AbstractWordUtils#buildTableCellEdgesArray(Table table)}</p>
	 * @param table table to extract information from
	 */
	@SuppressWarnings("javadoc")
	public TableColspanManager(final Table table) {
	    assert table != null;
	    this.table = table;

	    for (int rn = 0; rn < table.numRows(); rn++) {
		final TableRow row = table.getRow(rn);
		for ( int cn = 0; cn < row.numCells(); cn++ ) {
		    final TableCell cell = row.getCell(cn);   
		    this.cellEdges.add(cell.getLeftEdge());                
		    this.cellEdges.add(cell.getLeftEdge() + cell.getWidth());
		}
	    }
	}

	/**
	 * Determine horizontally merged cells
	 * 
	 * @param rowNum row number of the cell
	 * @param columnNum column number of the cell
	 * @return number of columns this cell spans (1-based)
	 */
	public int getColspan(final int rowNum, final int columnNum) {
	    assert rowNum < this.table.numRows();
	    final TableRow rowToProcess = this.table.getRow(rowNum);
	    assert columnNum < rowToProcess.numCells();
	    final TableCell baseCell = rowToProcess.getCell(columnNum);
	    byte numberOfCellsToMerge = 1;

	    byte guessIterator = 1;
	    guessLoop : do {
		switch (guessIterator) {
		case 1:
		    // try the ordinary way via flags in the Word document
		    if (baseCell.isFirstMerged()) {
			// determine number of cells to merge								
			int cellIterator = columnNum+1;
			while(cellIterator < rowToProcess.numCells() && rowToProcess.getCell(cellIterator).isMerged() &&
				!rowToProcess.getCell(cellIterator).isFirstMerged()) { // this triggers if we have two adjacent groups of merged cells
			    cellIterator++;
			    numberOfCellsToMerge++;						
			}						
		    }
		    guessIterator++;
		    break;
		case 2:
		    // try via the width of the cell
		    numberOfCellsToMerge = getColspanOffsetBased(baseCell);
		    guessIterator++;
		    break;
		default:
		    // give up; no colspan found
		    break guessLoop;
		}
	    } while (numberOfCellsToMerge == 1);

	    assert numberOfCellsToMerge >= 1;
	    return numberOfCellsToMerge;
	}

	/**
	 * Calculates the colspan based on the right edge offset of the current cell. I.e. we assume these offsets are unique
	 * <p>based on {@link org.apache.poi.hwpf.converter.AbstractWordConverter#getNumberColumnsSpanned(int[] tableCellEdges, int currentEdgeIndex, TableCell tableCell)}</p>
	 * 
	 * @param cell Cell to process
	 * @return A colspan value or {@code 1} if no colspan is applicable
	 */
	private byte getColspanOffsetBased(final TableCell cell) {
	    assert cell != null;
	    final Iterator<Integer> iterator = this.cellEdges.iterator();
	    final int cellLeftEdge = cell.getLeftEdge();
	    final int cellRightEdge = cellLeftEdge + cell.getWidth();

	    //set the iterator to the leftEdge
	    while (iterator.hasNext() && iterator.next() < cellLeftEdge) { /* NOPMD - body intentionally empty */ }

	    byte colspan = 1;
	    while (iterator.hasNext() && iterator.next() < cellRightEdge) colspan++;
	    return colspan;
	}		
    }
}