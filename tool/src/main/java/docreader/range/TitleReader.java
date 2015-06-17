package docreader.range;

import static helper.Constants.MSWord.STYLENAME_TITLE;
import static helper.Constants.MSWord.PATTERN_TITLE;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hwpf.usermodel.Range;

import helper.RegexHelper;
import helper.TraceabilityManagerHumanReadable;
import helper.annotations.DomainSpecific;
import helper.poi.PoiHelpers;
import helper.word.DataConverter;
import requirement.RequirementOrdinary;
import requirement.RequirementRoot;
import requirement.RequirementTemporary;
import requirement.metadata.Kind;
import docreader.GenericReader;
import docreader.ReaderData;
import docreader.list.ListReader;
import docreader.range.paragraph.ParagraphListAware;

/**
 * Reads the very beginning of any requirement document, specifically its title
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public final class TitleReader implements GenericReader<Integer> {
    private final transient ReaderData readerData;
    private final transient RequirementRoot parent;
    private final transient int initialParagraphIndex;
    private final transient Range documentRange;    
    private final transient ListReader listReader;
    private RequirementOrdinary titleRequirement = null; // NOPMD -- getter is provided below
    
    private static final Logger logger = Logger.getLogger(TitleReader.class.getName()); // NOPMD - Reference rather than a static field

    /**
     * Ordinary constructor
     * 
     * @param readerData global readerData
     * @param listReader used to inject a fake list item for the document title
     * @param parent title requirement will become child of this
     * @param initialParagraphIndex number of the first paragraph (0-based, unique for the entire document) where to search for title
     * @throws IllegalArgumentException if any of the given parameters is {@code null} or out of range
     */
    public TitleReader(final ReaderData readerData, final ListReader listReader, final RequirementRoot parent, final int initialParagraphIndex) {	
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	if (listReader == null) throw new IllegalArgumentException("listReader cannot be null.");
	if (parent == null) throw new IllegalArgumentException("parent cannot be null.");
	if (initialParagraphIndex < 0) throw new IllegalArgumentException("initialParagraphIndex out of range.");
	
	this.readerData = readerData;
	this.parent = parent;
	this.initialParagraphIndex = initialParagraphIndex;
	
	this.documentRange = readerData.getRange();	
	this.listReader = listReader;
    }

    /**
     * Read out the title of this document and return the offset of the first paragraph which is in a list
     * 
     * @see docreader.GenericReader#read()
     */
    @Override
    @DomainSpecific
    public Integer read() {
	int currentIndex = this.initialParagraphIndex;
	
	// Step 1: Skip all leading empty paragraphs
	while (DataConverter.isEmptyParagraph(this.readerData, new ParagraphListAware(this.readerData, this.documentRange.getParagraph(currentIndex)))) currentIndex++;
	
	// Step 2a: Find the first title paragraph based on the name of its attached style
	{
	    final int forcedUpperBound = (int)(this.documentRange.numParagraphs() * 0.1);
	    while (!isTitleParagraph(currentIndex)) {
		currentIndex++;
		if (currentIndex == forcedUpperBound) {
		    throw new IllegalStateException("Document title was not found within the first 10 percent of the document text. Please check your parameters.");
		}
	    }
	}
	
	// Step 2b: Find all paragraphs belonging to the title
	final int characterOffsetStart = this.documentRange.getParagraph(currentIndex).getStartOffset(); // NOPMD - intentional premature declaration
	{
	    final int forcedUpperBound = currentIndex + 50;
	    // we can safely loop in do/while manner here since step 2a has left us with the index of the first title paragraph
	    do {
		currentIndex++;
		if (currentIndex == forcedUpperBound) {
		    throw new IllegalStateException("Your document title seems unusually long. Please check the document formatting.");
		}
	    } while (isTitleParagraph(currentIndex));
	    // currentIndex is now off by one
	    currentIndex--;
	}
	final int characterOffsetEnd = this.documentRange.getParagraph(currentIndex).getEndOffset();
	
	// Step 3: Read out the actual title
	final Range titleRange = new Range(characterOffsetStart, characterOffsetEnd, this.readerData.getDocument());
	final RequirementTemporary titleRequirementTmp = new RequirementTemporary(titleRange);
	new RequirementReaderTextual(this.readerData, titleRequirementTmp).read();	
	
	// Step 4: Skip all subsequent non-list paragraphs
	{
	    final int forcedUpperBound = currentIndex + 100;
	    while (!(new ParagraphListAware(this.readerData, this.documentRange.getParagraph(currentIndex)).isInList())){
		currentIndex++;
		if (currentIndex == forcedUpperBound) {
		    throw new IllegalStateException("Your document does not seem to contain any (real) lists close to the document title. Please check the formatting.");
		}
	    }
	}
	
	// Step 5a: Get the lsid of the very first list paragraph which is assumed to be part of the baseList
	// docreader.ListReader.SublistManager.processParagraph(long, int, ParagraphPropertiesDeterminer, String) also relies on this assumption
	final int lsid = new ParagraphListAware(this.readerData, this.documentRange.getParagraph(currentIndex)).getList().getLsid();
	// Step 5b: Extract chapter number of the title and store it as a fake list item
	extractChapterNum(titleRequirementTmp.getText().getRaw(), lsid);
	
	// Step 6: Create a new requirement from all this data
	final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();
	hrManager.addList(this.listReader.getFullyQualified());
	this.titleRequirement = new RequirementOrdinary(this.readerData, titleRequirementTmp.getAssociatedRange(), hrManager, this.listReader.getParent(this.parent));
	this.titleRequirement.setText(titleRequirementTmp.getText());
	this.titleRequirement.getMetadata().setKind(Kind.HEADING);

	return currentIndex;
    }

    /**
     * @return the resulting title requirement ready for insertion into the requirement tree
     * @throws IllegalStateException if {@link #read()} failed or has not been called yet
     */
    public RequirementOrdinary getTitleRequirement() {
	if (this.titleRequirement == null) throw new IllegalStateException("The title must be read out first.");
	return this.titleRequirement;
    }
    
    
    /**
     * Subset-026 specific function to extract the chapter number from the document title
     * 
     * @param titleString Raw text of the entire title of the document
     * @param lsid lsid of the list of the first real list item in this document
     */
    @DomainSpecific
    private void extractChapterNum(final String titleString, final int lsid) {
	assert titleString != null;
	final String pattern = PATTERN_TITLE;
	final String chapterNumber = RegexHelper.extractRegex(titleString, pattern);
	if (chapterNumber != null) {
	    this.listReader.injectListItem(lsid, 0, chapterNumber);
	}
	else {
	    logger.log(Level.WARNING, "Could not extract a chapter number from the input document. Will use \"R\".");	    
	    this.listReader.injectListItem(null, 0, "R");
	}
    }
    
    
    /**
     * Checks if a certain paragraph is part of the document title based on the
     * paragraph style it is linked to
     * 
     * @param paragraphIndex number of the paragraph to check
     * @return {@code true} if the paragraph with {@code paragraphIndex} is part of the document title; {@code false} otherwise
     * @throws IllegalArgumentException if the paragraph to check is out of range
     */
    @DomainSpecific
    private boolean isTitleParagraph(final int paragraphIndex) {
	// TODO why is paragraphIndex == 0 not allowed?
	if (paragraphIndex == 0 || paragraphIndex >= this.documentRange.numParagraphs()) throw new IllegalArgumentException("Title paragraph candidate is out of range.");
	return STYLENAME_TITLE.equals(PoiHelpers.getStyleName(this.readerData, this.documentRange.getParagraph(paragraphIndex).getStyleIndex()));	
    }
}
