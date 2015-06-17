package docreader.range;

import static helper.Constants.Traceability.NESTING;
import static helper.Constants.Traceability.RANGE_SPLIT_CHARACTER_COUNT_THRESHOLD;
import helper.CSSManager;
import helper.XmlStringWriter;
import helper.formatting.SplitContentFormatter;
import helper.word.DataConverter;
import requirement.RequirementOrdinary;
import requirement.RequirementTemporary;
import requirement.data.RequirementText;
import requirement.metadata.Kind;
import docreader.ReaderData;
import docreader.list.ListToRequirementProcessor;

/**
 * Reader for arbitrary ranges which may need to be split into several paragraphs; supports nested structures
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class RequirementReaderRange extends RequirementReaderTextual {
    protected final boolean splitRange;
    private String delimiter = ""; // intentionally hides a field from RequirementReaderTextual; this is a richText delimiter only
    private int listIndent = 0;
    private final static String NESTEDTEXT = "NESTED STRUCTURE - SEE CHILDREN";
    protected final static String SPLITTEXT = "CONTENTS HAVE BEEN SPLIT UP - SEE CHILDREN";

    /**
     * Ordinary constructor
     * 
     * @param readerData global readerData
     * @param requirement requirement where to store the results
     */
    public RequirementReaderRange(final ReaderData readerData, final RequirementTemporary requirement) {
	this(readerData, requirement, true);
    }
    
    /**
     * Constructor for table cells
     * 
     * @param readerData global readerData 
     * @param requirement requirement where to store the results
     */
    protected RequirementReaderRange(final ReaderData readerData, final RequirementTemporary requirement, final boolean splitAllowed) {
	super(readerData, requirement);
	
	this.splitRange = splitRangeChecker(splitAllowed);
    }
    
    
    /**
     * Reader for ranges
     * 
     * @see docreader.GenericReader#read()
     * @return always {@code null}; do not use
     */
    @Override
    public Integer read() {
	// ordinary case
	readAbstract(); // intentionally discard the output
	if (this.splitRange) {
	    setupSplitRequirement(null);
	}
	else {
	    this.requirement.setText(this.readerData, new RequirementText(this.requirementContentRaw.toString(), this.requirementContentRich.toString(), this.requirementContentRich.toString()));
	}

	this.requirement.getMetadata().setKind(Kind.ORDINARY);	
	return null;
    }
    
    /**
     * Reads a requirement under the assumption it only contains a single paragraph; from this paragraph only interesting parts may be specified
     * 
     * @param paragraphOffset offset of the paragraph within the enclosing range
     * @param characterRunStartOffset startOffset of the first characterRun to read
     * @param characterRunEndOffset end offset of the last characterRun to read (inclusive)
     * @return the number of paragraphs to skip (that is: 0-based number of paragraphs read)
     */
    @Override
    protected int readAbstractSingleParagraph(final int paragraphOffset) {
	final int output;
	
	if (this.splitRange) {
	    // Note: we add children to the original cell requirement here. That cell requirement will only receive rich text data for the table rendering.
	    
	    // start a new paragraph
	    this.requirementContentRich.append(this.delimiter);
	    
	    // process each paragraph into a separate requirement
	    final ListToRequirementProcessor listToRequirementProcessor = this.readerData.getListToRequirementProcessor();
	    	    	 	    
	    int currentRangeNum = listToRequirementProcessor.processParagraph(paragraphOffset);
	    final RequirementOrdinary currentRequirement = listToRequirementProcessor.getCurrentRequirement();
	    if (currentRequirement != null) {		
		output = new RequirementReader(this.readerData, currentRequirement, currentRangeNum).read(); // allows all sorts of nested structures
		assert output >= 0; // will be 0 if we read an ordinary paragraph; or greater if this is a complex structure (e.g. nested table)
		
		// write output for cell requirement (i.e. the preview in the RichText table)
		final String previewText;
		if ("".equals(currentRequirement.getMetadata().getNumberText())) {
		    // paragraph not actually in a list
		    previewText = getPreviewText(currentRequirement);		   
		}
		else {
		    // paragraph in a list
		    final XmlStringWriter xmlwriter = new XmlStringWriter();
		    {
			// numberText
			xmlwriter.writeStartElement("div");
			final CSSManager cssmanager = new CSSManager();
			cssmanager.putProperty("float", "left");
			this.listIndent += listToRequirementProcessor.getListReader().getLevelDifference();
			final String indentAsString = Float.toString(this.listIndent * 0.2f) + "em";
			cssmanager.putProperty("margin-left", indentAsString);
			cssmanager.putProperty("margin-right", "0.2em");
			xmlwriter.writeAttribute("style", cssmanager.toString());
			xmlwriter.writeCharacters(currentRequirement.getMetadata().getNumberText());
			xmlwriter.writeEndElement("div");
		    }
		    {
			// paragraph content
			xmlwriter.writeStartElement("div");
			final CSSManager cssmanager = new CSSManager();
			cssmanager.putProperty("display", "table");
			xmlwriter.writeAttribute("style", cssmanager.toString());
			xmlwriter.writeRaw(getPreviewText(currentRequirement));
			xmlwriter.writeEndElement("div");
		    }
		    previewText = xmlwriter.toString();
		}
		this.requirementContentRich.append(previewText);		
	    }    
	    else {
		output = 0; // paragraph is not important; skip			
	    }
	}
	else {
	    // merge all content of this cell into one big requirement
	    output = super.readAbstractSingleParagraph(paragraphOffset);
	}
	
	this.delimiter = "<br />";
	
	return output;
    }
    
    /**
     * @return {@code true} if this cell must be split into several requirements, {@code false} otherwise
     */
    protected boolean splitRangeChecker(final boolean splitAllowed) {
	// check preconditions: NESTING must be allowed *globally* and split *locally* (for this cell)
	if (NESTING && splitAllowed) {
	    // Note: This does not trigger if we only have numberTexts but the actual paragraph texts are empty; that case is handled by TableOverrideManager (if this is a table cell)
	    int numberOfNonEmptyParagraphs = 0;	
	    for (int i = 0; i < this.range.numParagraphs(); i++) {
		if (!DataConverter.isEmptyParagraph(this.range.getParagraph(i))) {
		    numberOfNonEmptyParagraphs++;
		    if (numberOfNonEmptyParagraphs > 1) {
			if (this.range.text().length() > RANGE_SPLIT_CHARACTER_COUNT_THRESHOLD) {
			    this.readerData.getListToRequirementProcessor().setLastRequirement(this.requirement);
			    return true;
			}
		    }
		}	    
	    }
	}
	return false;
    }
    
    protected void setupSplitRequirement(final String cellIdTracePrepender) {
	assert this.splitRange;
	final RequirementText requirementText;
	if (cellIdTracePrepender == null) {
	    requirementText = new RequirementText(new SplitContentFormatter().writeString(SPLITTEXT));
	}
	else {
	    // since we are writing computed data here let rawText be null
	    requirementText = new RequirementText(null, new SplitContentFormatter().writeString(SPLITTEXT), cellIdTracePrepender + this.requirementContentRich.toString());
	}	
	this.requirement.setText(this.readerData, requirementText);
	this.requirement.getMetadata().setImplementationFlag(false);
    }
    
    private static String getPreviewText(final RequirementOrdinary currentRequirement) {
	final String output;
	if (currentRequirement.getText() != null) {
	    output = currentRequirement.getText().getRich();
	}
	else {
	    // this is a nested structure
	    output = new SplitContentFormatter().writeString(NESTEDTEXT);
	    currentRequirement.getMetadata().setImplementationFlag(false);
	}
	return output;
    }
}
