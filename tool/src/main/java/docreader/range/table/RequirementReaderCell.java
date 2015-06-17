package docreader.range.table;

import helper.TraceabilityManagerHumanReadable;
import helper.XmlStringWriter;
import helper.formatting.CellIdFormatter;
import docreader.ReaderData;
import docreader.range.RequirementReaderRange;
import requirement.RequirementTemporary;
import requirement.data.RequirementText;
import requirement.metadata.Kind;

/**
 * Reader for the contents of a table cell; supports nested structures and replacing the content with injected data (used for arrows in the subset026)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public final class RequirementReaderCell extends RequirementReaderRange {
    private final transient RequirementText overrideContent;
    @SuppressWarnings("hiding") // we are intentionally hiding here
    protected final static String SPLITTEXT = "CELL CONTENTS HAVE BEEN SPLIT UP - SEE CHILDREN";
    

    /**
     * Ordinary constructor
     * 
     * @param readerData global readerData
     * @param requirement requirement where the output will be written
     * @param overrideContent if not {@code null} this will be used as the content of the resulting requirement
     * @param splitAllowed {@code true} if this cell may be split up into individual requirements
     */
    public RequirementReaderCell(final ReaderData readerData, final RequirementTemporary requirement, final RequirementText overrideContent, final boolean splitAllowed) {
	super(readerData, requirement, splitAllowed);
	// overrideContent may be null
	this.overrideContent = overrideContent;	
    }


    /** 
     * Reader for cells without tracing information
     * 
     * @see docreader.GenericReader#read()
     * @return always {@code null}; do not use
     */
    @Override
    public Integer read() {
	if (this.overrideContent == null) {
	    // ordinary case
	    readAbstract();
	    // split cell is not taken into account here because we only write preview data anyways
	    this.requirement.setText(this.readerData, new RequirementText(this.requirementContentRaw.toString(), this.requirementContentRich.toString()));
	}
	else {
	    // cell content will be overridden by arbitrary other data
	    assert this.overrideContent != null;
	    this.requirement.setText(this.readerData, this.overrideContent);	    	   
	}
	this.requirement.getMetadata().setKind(Kind.ORDINARY);	
	return null;
    }    
    
    /**
     * Special reader which adds row- and cell- numbers to a table
     * 
     * @param rowId row number of this cell
     * @param columnId column number of this cell
     * @throws IllegalArgumentException If one of the arguments was out of range
     */
    public void read(final int rowId, final int columnId) {
	if (rowId < 0 || columnId < 0) throw new IllegalArgumentException("Given ids were malformed.");
	final CellHRTraceDataManager cellHRTraceDataManager = new CellHRTraceDataManager(rowId, columnId);
	read(cellHRTraceDataManager.writeMetadata());
    }

    /**
     * Special reader which adds a given raw identifier as a tracetag to a table
     * 
     * @param traceId given tag which is to be used for this cell
     * @throws IllegalArgumentException if the given parameter is {@code null}
     */
    public void read(final TraceabilityManagerHumanReadable traceId) {
	if (traceId == null) throw new IllegalArgumentException("traceId cannot be null"); 
	final CellHRTraceDataManager cellHRTraceDataManager = new CellHRTraceDataManager(traceId);
	read(cellHRTraceDataManager.writeMetadata());
    }
    
    /**
     * Reader for table cells
     * 
     * <p><em>Note: </em> this does not return the number of paragraphs which have been read,
     * because Word has a marker for the next paragraph in tables, anyways</p>
     * 
     * @see RequirementReaderRange#read()
     * @param cellIdTracePrepender
     */
    private void read(final String cellIdTracePrepender) {
	if (cellIdTracePrepender == null) throw new IllegalArgumentException("cellIdTracePrepender cannot be null.");	
	if (this.overrideContent == null) {
	    // ordinary case
	    readAbstract(); // intentionally discard the output
	    if (this.splitRange) {
		setupSplitRequirement(cellIdTracePrepender);
	    }
	    else {
		this.requirement.setText(this.readerData, new RequirementText(this.requirementContentRaw.toString(), this.requirementContentRich.toString(), cellIdTracePrepender + this.requirementContentRich.toString()));
	    }
	}
	else {	    
	    // cell content will be overridden by arbitrary other data
	    assert this.overrideContent != null;	    
	    this.requirement.setText(this.readerData, new RequirementText(this.overrideContent.getRaw(), this.overrideContent.getRich(), cellIdTracePrepender + this.overrideContent.getRich()));	    
	}
	this.requirement.getMetadata().setKind(Kind.ORDINARY);	
    }
    
    
    /**
     * Holds data for human-readable trace strings
     */
    private final static class CellHRTraceDataManager {
	private final Integer rowNumber;
	private final Integer columnNumber;
	private final TraceabilityManagerHumanReadable rawIdentifier;
	private final XmlStringWriter xmlwriter = new XmlStringWriter();

	public CellHRTraceDataManager(final int rowNumber, final int columnNumber) {
	    this.rowNumber = rowNumber;
	    this.columnNumber = columnNumber;						
	    this.rawIdentifier = null;
	}

	public CellHRTraceDataManager(final TraceabilityManagerHumanReadable rawManager) {
	    this.rowNumber = null;
	    this.columnNumber = null;			
	    this.rawIdentifier = rawManager; // rawInput can be null
	}

	public String writeMetadata() {
	    if (this.rawIdentifier != null && "".equals(this.rawIdentifier.getTag())) {
		return ""; // rawIdentifier is set but empty -> do not write a trace tag at all since this cell is not important
	    }

	    final CellIdFormatter cellIdFormatter = new CellIdFormatter(this.xmlwriter);
	    cellIdFormatter.writeStartElement();
	    final TraceabilityManagerHumanReadable hrManager;
	    if (this.rawIdentifier == null) {
		hrManager = new TraceabilityManagerHumanReadable();
		hrManager.addRow(this.rowNumber);
		hrManager.addColumn(this.columnNumber);
	    }
	    else hrManager = this.rawIdentifier;
	    
	    this.xmlwriter.writeCharacters(hrManager.getTag());			
	    cellIdFormatter.writeEndElement();

	    return this.xmlwriter.toString();
	}		
    }
}
