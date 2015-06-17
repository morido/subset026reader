package docreader.list;


import java.util.ArrayDeque;
import java.util.Iterator;

import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;

import docreader.ReaderData;
import docreader.range.paragraph.ParagraphListAware;
import requirement.RequirementRoot;


/**
 * Full featured ListReader which takes care of independent multilevel lists,
 * pseudo consecutive lists, pseudo / fake list paragraphs and other such niceties.<br />
 * In other words: This transforms a plain list number text as received from MS Word into a usable tracestring.
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public final class ListReader {
    private String listNumberAsPrinted = null; // NOPMD - intentionally non-transient, used by #getAsPrinted()
    private String listNumberFullyQualified = null; // NOPMD - intentionally non-transient, used by #getFullyQualified()
    private String[] listNumberFullyQualifiedSkippedLevels = null; // NOPMD - intentionally non-transient, used by #getFullyQualifiedSkippedLevels()
    private transient final ListReaderPlain listReaderPlain;   
    private final transient SublistStack sublistStack;
    private final transient FakeLsidManager fakeLsidManager = new FakeLsidManager();
    private final transient ReaderData readerData;

    /**
     * Ordinary constructor for a new ListReader
     * 
     * @param readerData global ReaderData
     * @param rootRequirement root requirement of the entire requirement tree
     * @throws IllegalArgumentException if the given argument is {@code null}
     */
    ListReader(final ReaderData readerData, final RequirementRoot rootRequirement) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	if (rootRequirement == null) throw new IllegalArgumentException("rootRequirement cannot be null.");
	this.readerData = readerData;
	this.sublistStack = new SublistStack(this.fakeLsidManager, rootRequirement, readerData.getDocument().getRange());
	this.listReaderPlain = new ListReaderPlain(readerData);
    }    

    /**
     * Process a single paragraph
     * 
     * <p><em>Note:</em> This must be called consecutively for every paragraph in the document
     * (with the exception of paragraphs in tables and possibly other nested structures)</p>
     * <p>Results may be obtained via {@link #getAsPrinted()}, {@link #getFullyQualified()} and {@link #getParent(RequirementRoot)} afterwards.</p>
     * 
     * @param paragraph Paragraph to process
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public void processParagraph(final Paragraph paragraph) {
	if (paragraph == null) throw new IllegalArgumentException("Given paragraph cannot be null.");

	@SuppressWarnings("hiding")
	final String listNumberAsPrinted;
	final SublistManager.LevelStore.LevelTuple levelTuple;
	final SublistManager.LevelStore.LevelTuple[] skippedLevels;
	final ParagraphListAware paragraphListAware = new ParagraphListAware(this.readerData, paragraph);
		
	if (paragraphListAware.isInList()) {
	    // Case 1: ordinary list paragraph
	    listNumberAsPrinted = this.listReaderPlain.getFormattedNumber(paragraphListAware);
	    final SublistManager.LevelStore.LevelTupleWSkippedLevels levelTupleWSkippedLevels = this.sublistStack.getActiveManager().processParagraph(this.readerData, paragraphListAware, this.listReaderPlain.getLvlOfLastProcessedParagraph(), listNumberAsPrinted);
	    levelTuple = levelTupleWSkippedLevels.levelTuple;
	    skippedLevels = levelTupleWSkippedLevels.skippedLevels;
	}
	else {
	    assert !paragraphListAware.isInList();
	    // Case 2: Check if this is a "pseudo list paragraph" or a "fake list paragraph"
	    // a "pseudo list paragraph" is a paragraph #n+m which comes after a list paragraph #n; all paragraphs #n+1..#n+m must have the same left indent as #n
	    // a "fake list paragraph" is a paragraph which looks like a real list paragraph (i.e. it has a numberText) but technically does not belong to a list
	    final SublistManager.LevelTupleWFakeNumberText levelTupleWFakeNumberText = this.sublistStack.getActiveManager().processParagraph(paragraphListAware); 
	    listNumberAsPrinted = levelTupleWFakeNumberText.numberText;
	    levelTuple = levelTupleWFakeNumberText.levelTuple;
	    skippedLevels = levelTupleWFakeNumberText.skippedLevels;
	}

	// write output
	this.listNumberAsPrinted = listNumberAsPrinted;
	this.listNumberFullyQualifiedSkippedLevels = this.sublistStack.getActiveManager().prependFullyQualified(skippedLevels);
	this.listNumberFullyQualified = this.sublistStack.getActiveManager().prependFullyQualified(levelTuple);			
    }

    
    /**
     * Process a single paragraph; special case when no only the resulting numberText is relevant and nothing else
     * 
     * @param paragraphListAware Paragraph to process
     * @return a String containing the numberText, never {@code null}
     */
    public String processParagraphPlain(final ParagraphListAware paragraphListAware) {
	if (paragraphListAware == null) throw new IllegalArgumentException("Given paragraphListAware cannot be null.");
	return this.listReaderPlain.getFormattedNumber(paragraphListAware);
    }

    /**
     * Inject an arbitrary item into any list
     * 
     * <p>
     * <em>Notes:</em>
     * <ol>
     * <li>This assumes the item has no left indentation and is at {@code olvl == 1}, see {@link SublistManager.ParagraphPropertiesDeterminer#ParagraphPropertiesDeterminer()}.</li>
     * <li>Use this with utmost care. Basically this is a dirty hack to somehow squeeze arbitrary stuff into this manager. Side effects might not be what you expected...</li>
     * </ol>
     * </p> 
     * 
     * @param lsid (fake) lsid of the injected item or {@code null} if this should become an independent list
     * @param ilvl (fake) ilvl of the injected item
     * @param numberText (fake) numberText of the injected item
     * @throws IllegalArgumentException if one of the arguments is {@code null}
     */
    @SuppressWarnings("javadoc")
    public void injectListItem(final Integer lsid, final int ilvl, final String numberText) {
	if (numberText == null) throw new IllegalArgumentException("numberText cannot be null.");

	final long lsidOutput;
	if (lsid != null) lsidOutput = lsid.longValue();
	else lsidOutput = this.fakeLsidManager.computeNewLsid();

	final SublistManager.LevelTupleWFakeNumberText levelTupleWFakeNumberText = this.sublistStack.getActiveManager().processParagraph(lsidOutput, ilvl, numberText);

	@SuppressWarnings("hiding")
	final String listNumberAsPrinted = levelTupleWFakeNumberText.numberText;
	final SublistManager.LevelStore.LevelTuple levelTuple = levelTupleWFakeNumberText.levelTuple;
	final SublistManager.LevelStore.LevelTuple[] skippedLevels = levelTupleWFakeNumberText.skippedLevels;

	// write output
	this.listNumberAsPrinted = listNumberAsPrinted;
	this.listNumberFullyQualifiedSkippedLevels = this.sublistStack.getActiveManager().prependFullyQualified(skippedLevels);
	this.listNumberFullyQualified = this.sublistStack.getActiveManager().prependFullyQualified(levelTuple);	
    }

    /**
     * Get the list number as it is displayed in the original input file (i.e. not qualified)
     * 
     * @return The number string for this list item as it is printed out by MS Word; never {@code null}
     */
    public String getAsPrinted() {
	// essentially a wrapper for {@link ListReaderPlain#getFormattedNumber(ParagraphListAware)}
	if (this.listNumberAsPrinted == null) throw new IllegalArgumentException("List number has not been read out, yet.");
	return this.listNumberAsPrinted;
    }

    /**
     * Obtain a list number text which is suitable for use as a trace string
     * 
     * @return The fully qualified number string for this list item; never {@code null}
     */
    public String getFullyQualified() {
	if (this.listNumberFullyQualified == null) throw new IllegalArgumentException("List number has not been read out, yet.");
	return this.listNumberFullyQualified;
    }


    /**
     * @return an ordered array of strings with all the skipped levels which must be inserted before the current item to generate a proper hierarchy; never {@code null}
     */
    public String[] getFullyQualifiedSkippedLevels() {
	if (this.listNumberFullyQualifiedSkippedLevels == null) throw new IllegalArgumentException("List number has not been read out, yet.");
	return this.listNumberFullyQualifiedSkippedLevels;
    }

    /**
     * Determine the correct parent of the current requirement to build a tree structure;
     * must be called after each call to {@link #processParagraph(Paragraph)} and {@link #injectListItem(Integer, int, String)}
     * 
     * <p><em>Note:</em> If there are placeholder requirements (i.e. skipped levels) this will return the parent of the very first placeholder.
     * All subsequent placeholders and the actual list item will be first-level children of that first placeholder.</p> 
     * 
     * @param input previous requirement
     * @return parent of the current requirement; never {@code null} 
     * @throws IllegalArgumentException if input is {@code null}
     */
    public RequirementRoot getParent(final RequirementRoot input) {
	if (input == null) throw new IllegalArgumentException("Given requirement cannot be null.");
	final int levelDifference = this.sublistStack.getActiveManager().getLevelDifference();		
	// 0 means we are staying on the same level; 1 we are going one level inwards, -1 we are going one level outwards
	if (levelDifference <= 1) { // NOPMD - intentional use of a literal
	    RequirementRoot output = input;
	    for (int i = levelDifference; i <= 0; i++) {
		assert ((i == levelDifference) ?  input != null : true); // i.e. our input cannot be null in the first iteration
		output = output.getParent();
		if (output == null) throw new IllegalStateException("Could not find a legitimate parent requirement. Have to quit. Sorry.");
	    }
	    assert output != null;
	    return output;
	}
	throw new IllegalArgumentException("LevelDifference was greater than 1. This should not happen.");
    }    

    /**
     * @see SublistManager#getLevelDifference()
     * @return a numerical value to describe the hierarchical relationship between the previous requirement and the current one 
     */
    @SuppressWarnings("javadoc")
    public int getLevelDifference() {
	return this.sublistStack.getActiveManager().getLevelDifference();
    }
    
    /**
     * @return the current sublist table nesting level (1-based)
     */
    public int getTableNestingLevel() {
        return this.sublistStack.getTableNestingLevel();
    }

    /**
     * Add a new nesting level
     * 
     * @param hrParent umbrella requirement of this nesting level (i.e. all requirements inside this nesting will be children of this)
     * @param range range which covers the entire nested part
     * @param nestingType the type of the new nested structure
     * @throws IllegalArgumentException if any of the parameters is {@code null} or invalid
     */
    public void addNestingLevel(final RequirementRoot hrParent, final Range range, final NestingType nestingType) {
	if (hrParent == null) throw new IllegalArgumentException("hrParent cannot be null.");
	if (range == null) throw new IllegalArgumentException("range cannot be null.");
	if (nestingType == null) throw new IllegalArgumentException("nestingType cannot be null");
	if (nestingType == NestingType.NOT_NESTED) throw new IllegalArgumentException("nestingType is not valid.");
	
	this.sublistStack.addNestedManager(hrParent, range, nestingType);
    }
    
    /**
     * Remove the last added nesting level 
     */
    public void removeNestingLevel() {
	this.sublistStack.removeNestedManager();
    }       
    
    /**
     * @return the currently active range with respect to any nesting
     */
    public Range getRange() {
	return this.sublistStack.getActiveManager().getRange();
    }
    
    /**
     * @return the currently active parent requirement for the tracestring-generation
     */
    RequirementRoot getHRParent() {
        return this.sublistStack.getActiveManager().getHRParent();
    }    
    
    /**
     * Manages crafted List-IDs necessary for paragraphs which are technically not in a list but need to behave as if they were
     * <p><em>Note:</em> There should be only one instance of this class for each document</em>
     * 
     * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>     
     */
    final static class FakeLsidManager {
	// the initializer is an illegal value according to [MS-DOC], v20140721, 2.9.147
	// assume all numbers above are unused by Word (since they wont fit into an unsigned integer)
	private final static long LSID_ILLEGAL = 0xFFFFFFFF; 
	private long currentLsid = LSID_ILLEGAL;
		
	/**
	 * @return a document-wide unique {@code lsid}
	 */
	public long computeNewLsid() {
	    return ++this.currentLsid;
	}
    }
    
    /**
     * Manages a stack of all our sublist managers; will grow larger for lists nested in tables
     */
    private final static class SublistStack {
	private final FakeLsidManager fakeLsidManager;
	private final transient ArrayDeque<SublistManager> sublistManagers = new ArrayDeque<>(1);
		
	public SublistStack(final FakeLsidManager fakeLsidManager, final RequirementRoot rootRequirement, final Range documentRange) {
	    assert fakeLsidManager != null && rootRequirement != null && documentRange != null;
	    this.fakeLsidManager = fakeLsidManager;
	    this.sublistManagers.add(new SublistManager(this.fakeLsidManager, rootRequirement, documentRange, NestingType.NOT_NESTED)); // base manager	    
	}
	
	/**
	 * @return the currently active manager; never {@code null}
	 */
	public SublistManager getActiveManager() {
	    assert this.sublistManagers.peek() != null;
	    return this.sublistManagers.peek();
	}
	
	public void addNestedManager(final RequirementRoot hrParent, final Range range, final NestingType nestingType) {	    
	    this.sublistManagers.addFirst(new SublistManager(this.fakeLsidManager, hrParent, range, nestingType));	    
	}
	
	public void removeNestedManager() {
	    // will never remove the base manager
	    if (this.sublistManagers.size() > 1) this.sublistManagers.removeFirst();
	}
	
	/**
	 * @return {@code 1} if no nesting is applicable or any greater number for each additional nesting level
	 */
	public int getTableNestingLevel() {
	    int output = 1;

	    final Iterator<SublistManager> iterator = this.sublistManagers.iterator();
	    nestingLoop: while (iterator.hasNext()) {
		final SublistManager currentManager = iterator.next();

		switch(currentManager.getNestingType()) {
		// NOTE and NOT_NESTED always form the lower bound of nested table levels
		// Word apparently does not support footnotes inside nested structures
		// i.e. there is no equivalent of a LaTeX minipage footnote in Word
		case NOTE:		    
		case NOT_NESTED:
		    break nestingLoop;
		case TABLE_CELL:
		    output++;
		    break;
		default:
		    throw new IllegalStateException();		    
		}
	    }	    	   

	    return output;
	}
    }
        
}