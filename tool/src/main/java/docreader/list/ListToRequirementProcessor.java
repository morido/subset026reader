package docreader.list;

import helper.RequirementHelper;
import helper.TraceabilityManagerHumanReadable;

import org.apache.poi.hwpf.usermodel.Paragraph;

import docreader.ReaderData;
import docreader.range.SkipReader;
import requirement.RequirementOrdinary;
import requirement.RequirementPlaceholder;
import requirement.RequirementRoot;
import requirement.RequirementWParent;

/**
 * Abstraction layer for the document list reading process
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class ListToRequirementProcessor {    
    private final transient ReaderData readerData;    
    private final transient ListReader listReader;
    private transient RequirementRoot lastRequirement = new RequirementRoot();
    private final RequirementRoot rootRequirement = this.lastRequirement;    
    private RequirementOrdinary currentRequirement = null; // NOPMD - intentionally non-transient; getter below
    
    /**
     * Ordinary constructor
     * 
     * @param readerData global readerData
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public ListToRequirementProcessor(final ReaderData readerData) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	// statusData may be null
	this.readerData = readerData;	
	this.listReader = new ListReader(readerData, this.rootRequirement);
    }
    
    /**
     * Convert a paragraph to a requirement
     * 
     * @param paragraphNum the 0-based running number of the paragraph to read in the entire document 
     * @return the paragraph number of the next paragraph to read -1
     */
    public int processParagraph(final int paragraphNum) {	
	final Paragraph paragraph = this.readerData.getRange().getParagraph(paragraphNum);	
	
	final int paragraphsToSkip = new SkipReader(this.readerData, paragraphNum).read();
	int newParagraphNum = paragraphNum;
	if (paragraphsToSkip > 0) {
	    // no requirement here
	    newParagraphNum += paragraphsToSkip -1; // -1 because we will again do +1 in the next iteration of the for loop
	    this.currentRequirement = null; // NOPMD - intentional null assignment
	}
	else {
	    // do the listhandling here
	    this.listReader.processParagraph(paragraph);
	    
	    // get the insertion point for the first element to be inserted (which may be either a real list item or some skipped stuff)
	    this.lastRequirement = this.listReader.getParent(this.lastRequirement);
	    
	    // process skipped levels
	    for (final String skippedLevelString : this.listReader.getFullyQualifiedSkippedLevels()) {
		final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();
		hrManager.addList(skippedLevelString);

		final RequirementWParent possibleDuplicate = this.readerData.getTraceabilityLinker().getRequirement(skippedLevelString);		
		if (possibleDuplicate != null
			&& (this.lastRequirement == this.rootRequirement || RequirementHelper.isParentalRelation(this.lastRequirement, possibleDuplicate))) {
		    // this requirement was already created earlier (i.e. some other list item had missing levels and one of them referred to this requirement)
		    // do not recreate it, but set our lastRequirement to this so we can properly create children
		    this.lastRequirement = possibleDuplicate;
		}
		else {
		    // we do not use this.listReader.getParent() here because all inserted levels *must* be a hierarchical child of their respective predecessors
		    this.lastRequirement = new RequirementPlaceholder(this.readerData, paragraph, hrManager, this.lastRequirement, this.rootRequirement);
		}		
	    }
	    
	    final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();
	    hrManager.addList(this.listReader.getFullyQualified());
	    this.currentRequirement = new RequirementOrdinary(this.readerData, paragraph, hrManager, this.lastRequirement, this.listReader.getHRParent());
	    this.currentRequirement.setNumberText(this.listReader.getAsPrinted());	    
	    this.lastRequirement = this.currentRequirement;
	}
	return newParagraphNum;
    }
           
    /**
     * Get the requirement from the last run of {@link #processParagraph(int)}
     * 
     * @return the last requirement, or {@code null} if there was no requirement at the given offset
     */
    public RequirementOrdinary getCurrentRequirement() {
	return this.currentRequirement;	
    }
    
    /**
     * @return a handle to the root of the resulting requirement tree
     */
    public RequirementRoot getRootRequirement() {
	return this.rootRequirement;
    }
        
    /**
     * @return a handle to the underlying listReader
     */
    public ListReader getListReader() {
	return this.listReader;
    }
    
    /**
     * Set the last processed requirement to an arbitrary value
     * 
     * @param lastRequirement the last processed requirement to inject
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public void setLastRequirement(final RequirementRoot lastRequirement) {
	if (lastRequirement == null) throw new IllegalArgumentException("lastRequirement cannot be null.");
	this.lastRequirement = lastRequirement;
    }
}
