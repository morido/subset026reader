package docreader.list;

import static helper.Constants.MSWord.DELIMITER_LISTLEVEL;
import static helper.Constants.MSWord.IDENTIFIER_APPENDIX;
import static helper.Constants.MSWord.PATTERN_APPENDIX;
import static helper.Constants.Traceability.IDENTIFIER_BULLETLIST;
import static helper.Constants.Traceability.LEVELPLACEHOLDERS;
import helper.HashMapWeakValue;
import helper.RegexHelper;
import helper.annotations.DomainSpecific;
import helper.word.DataConverter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hwpf.model.ListLevel;
import org.apache.poi.hwpf.sprm.ParagraphSprmUncompressor;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.ParagraphProperties;
import org.apache.poi.hwpf.usermodel.Range;

import requirement.RequirementRoot;
import docreader.ReaderData;
import docreader.list.ListReader.FakeLsidManager;
import docreader.range.paragraph.ParagraphListAware;

/**
 * Determines the fully qualified identifier for a list-item and takes care about correct parent/child-relationships
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
final class SublistManager {
    private final transient HashMapWeakValue<Long, LevelStore> sublistFinder = new HashMapWeakValue<>(3);
    private transient LevelStore baseList;
    private transient int oldLevelNumber = -1; // we basically start with an imaginary root element at the position -1	
    private transient int currentLevelNumber = 0;
    private transient Integer levelNumberOfFirstPlaceholder = null;
    private transient LevelStore.LevelTuple lastListParagraph = null;
    private transient LevelStore.LevelTuple predecessorNonListParagraph = null;
    private final transient FakeListManager fakeListManager;    
    private final NestingType nestingType;
    private final RequirementRoot hrParent;
    private final Range range;
    private static final Logger logger = Logger.getLogger(SublistManager.class.getName()); // NOPMD - Reference rather than a static field   
    
    
    /**
     * Ordinary constructor
     * 
     * @param fakeLsidManager manager for crafted {@code lsid} values
     * @param hrParent tracestring parent of this manager
     * @param range Range which covers the entire text represented by this manager
     * @param isNested {@code true} if this represents a nested manager which manages lists inside tables; {@code false otherwise}
     */
    SublistManager(final FakeLsidManager fakeLsidManager, final RequirementRoot hrParent, final Range range, final NestingType nestingType) {
	assert fakeLsidManager != null && hrParent != null && range != null && nestingType != null;
	this.fakeListManager = new FakeListManager(fakeLsidManager);	
	
	this.nestingType = nestingType;	
	this.hrParent = hrParent;
	this.range = range;
    }

    /**
     * Determine the sublist level of a new paragraph which is part of a list
     * 
     * @param readerData global readerData
     * @param paragraph paragraph to process
     * @param lvl ListLevel associated with this paragraph
     * @param numberText raw NumberText as obtained from MS Word
     * @return LevelTuple representing this new paragraph 
     */
    LevelStore.LevelTupleWSkippedLevels processParagraph(final ReaderData readerData, final ParagraphListAware paragraph, final ListLevel lvl, final String numberText) {
	assert paragraph != null;
	assert paragraph.isInList();
	assert numberText != null;

	final int lsid = paragraph.getList().getLsid();
	final int ilvl = paragraph.getIlvl();	    

	final ParagraphPropertiesDeterminer pProperties = new ParagraphPropertiesDeterminer(readerData, paragraph, lvl);
	return processParagraph(lsid, ilvl, pProperties, numberText);
    }


    /**
     * Determine the sublist level of a pseudo-list paragraph
     * 
     * @param paragraph Paragraph to process
     * @return wrapper class which contains a {@code LevelTuple} representing this new paragraph and a {@code numberText}, never {@code null} 
     * @throws IllegalStateException if this paragraph cannot be legally inserted into the requirement hierarchy
     */
    LevelTupleWFakeNumberText processParagraph(final ParagraphListAware paragraph) {
	assert paragraph != null && !paragraph.isInList();	    
	assert !DataConverter.isEmptyParagraph(paragraph.getParagraph());
	final String numberText;

	final LevelStore.LevelTupleWSkippedLevels outputTupleWSkippedLevels;	   
	final ParagraphPropertiesDeterminer pProperties = new ParagraphPropertiesDeterminer(paragraph);
	if (this.fakeListManager.processParagraph(paragraph)) {
	    // this is a fake list paragraph (i.e. the paragraph has its numberText embedded in the actual paragraph text)
	    if (this.fakeListManager.isNewBaseList()) this.baseList = null; // NOPMD - has intended side effects on next call
	    pProperties.setFakeListStatus(true);
	    numberText = this.fakeListManager.getNumberText();
	    outputTupleWSkippedLevels = processParagraph(this.fakeListManager.getLsid(), this.fakeListManager.getIlvl(), pProperties, numberText);		    
	}
	else {
	    // this is a pseudo list paragraph
	    final LevelStore.LevelTuple[] skippedLevels = LevelStore.NOSKIPPEDLEVELS;
	    final LevelStore.LevelTuple outputTuple;

	    if (isNestedModeActive() && this.baseList == null) {
		// first paragraph in a table cell. Is not in any list
		pProperties.setFakeListStatus(true); // ...'but has to behave like one
		final LevelStore.LevelTupleWSkippedLevels levelStoreWSkippedLevels = processParagraph(this.fakeListManager.computeNewLsid(), 0, pProperties, "");
		// discard the skipped level stuff
		assert levelStoreWSkippedLevels.skippedLevels.length == 0;
		outputTuple = levelStoreWSkippedLevels.levelTuple;
	    }
	    else if ((isNestedModeActive() || !paragraph.getParagraph().isInTable()) && pProperties.isLeftIndentationAvailable() && pProperties.getOverriddenLeftIndentationFirstLine() > 0 && this.predecessorNonListParagraph == null) {
		// try to find the correct insertion point
		outputTuple = this.baseList.findCorrectInsertionPointBaseList(pProperties, "");
	    }
	    else if (isNestedModeActive() && this.baseList.levels.size() == 1) {
		// for nested paragraphs which dont have no indentation fallback to most significant level (different from above which would fallback to least significant)
		outputTuple = this.baseList.levels.firstEntry().getValue();
		this.baseList.levels.firstEntry().getValue().removeAllChildren();
	    }
	    else {
		// assume this list is at the same level as the directly preceding paragraph which is also not in a list 
		if (this.predecessorNonListParagraph != null) outputTuple = this.predecessorNonListParagraph;
		// assume this is a child of the last list paragraph
		else if (this.lastListParagraph != null) {
		    if (this.lastListParagraph.getBreadcrumbData().isUnnumbered()) { // with the current implementation this check is actually superfluous
			this.lastListParagraph.setPredecessor(true);
		    }
		    outputTuple = this.lastListParagraph;
		}
		else throw new IllegalStateException("This is a non-indented, non-list paragraph which does not have any list predecessors. No idea where this belongs hierarchically.");
	    }
	    this.predecessorNonListParagraph = outputTuple;
	    numberText = "";
	    outputTupleWSkippedLevels = new LevelStore.LevelTupleWSkippedLevels(outputTuple, skippedLevels);
	}
	return new LevelTupleWFakeNumberText(outputTupleWSkippedLevels, numberText); 
    }


    /**
     * Process some text (usually a heading) which does not fit to any list pattern but we need to squeeze it in there, nevertheless.
     * <p>
     * This is similar to a fakeList paragraph (see {@link #fakeListManager}) but we do not have any usable {@code olvl} and indent information available.
     * </p>
     * 
     * @param lsid lsid of the paragraph to be inserted
     * @param ilvl level at which this paragraph should be inserted
     * @param numberText (fake) numberText of the paragraph
     * @return wrapper class which contains a {@code LevelTuple} representing this new paragraph and a {@code numberText}; none of which may be {@code null}
     */
    LevelTupleWFakeNumberText processParagraph(final long lsid, final int ilvl, final String numberText) {
	assert numberText != null;
	final ParagraphPropertiesDeterminer pProperties = new ParagraphPropertiesDeterminer();
	pProperties.setFakeListStatus(true);
	final LevelStore.LevelTupleWSkippedLevels outputTuple = processParagraph(lsid, ilvl, pProperties, numberText);
	return new LevelTupleWFakeNumberText(outputTuple, numberText);
    }
    
    
    /**
     * Internal handler for a paragraph which is either really in a list or a paragraph which pretends to be in one
     * 
     * @param lsid real or faked {@code lsid}
     * @param ilvl real or faked {@code ilvl}
     * @param pProperties properties of the paragraph to be inserted
     * @param numberText numberText of the paragraph to be inserted
     * @return a LevelTuple representing this new list item
     */
    private LevelStore.LevelTupleWSkippedLevels processParagraph(final long lsid, final int ilvl, final ParagraphPropertiesDeterminer pProperties, final String numberText) {	    
	final LevelStore currentList;

	if (this.baseList == null) {
	    // create baseList
	    currentList = new LevelStore(this.sublistFinder, lsid, isNestedModeActive());			
	    this.baseList = currentList; 
	}
	else {
	    assert this.baseList != null;
	    final LevelStore storedListMatch = this.sublistFinder.get(lsid);
	    if (storedListMatch == null || storedListMatch.isDeleted()) {
		// new list; never seen before or intentionally forgotten about
		final LevelStore.LevelTuple insertionPoint = this.baseList.findCorrectInsertionPointBaseList(pProperties, numberText);
		if (insertionPoint.isPredecessor()) {
		    // new list at same position as other list; "predecessor"-case
		    if (insertionPoint.isPredecessorBulletList()) {
			currentList = insertionPoint.getEnclosingList();
			this.sublistFinder.put(lsid, currentList);
		    }
		    else {			    
			currentList = new LevelStore(this.sublistFinder, lsid, insertionPoint.getEnclosingList());
		    }
		}
		else {
		    // really a new list
		    currentList = new LevelStore(this.sublistFinder, lsid, insertionPoint);
		}		      
	    }
	    else currentList = storedListMatch;
	}

	final LevelStore.LevelTupleWSkippedLevels outputTuple = currentList.getLevelTuple(ilvl, pProperties, numberText);	    
	this.lastListParagraph = outputTuple.levelTuple;
	this.predecessorNonListParagraph = null; // NOPMD - intentional null-assignment
	return outputTuple;
    }

    /**
     * Prepends a given number with the known identifiers from the upper levels; works through all physical lists in the hierarchy of this LevelTuple
     * 
     * @param levelTuple LevelTuple whose numberText is to be prepended; may be {@code null}
     * @return fully qualified numberText for this LevelTuple; never {@code null}
     */
    String prependFullyQualified(final LevelStore.LevelTuple levelTuple) {
	final StringBuilder output = new StringBuilder();
	if (levelTuple != null) {
	    // Piece together the trace string from the individual levels across different lists
	    // Note: we do this from the least significant level up to the top; hence the double reverse 
	    output.append(reverseString(levelTuple.getBreadcrumbData().getNumberTextWithRunningAnnex(true)));

	    LevelStore.LevelTuple currentTuple = levelTuple;
	    @SuppressWarnings("hiding")
	    int currentLevelNumber = 0;
	    while((currentTuple = currentTuple.getMoreSignificantLevel()) != null) { // NOPMD - intentional assignment in operand
		currentLevelNumber++;
		output.append(DELIMITER_LISTLEVEL); // single char; so we do not need to reverse it
		output.append(reverseString(currentTuple.getBreadcrumbData().getNumberTextWithRunningAnnex(false)));		
	    }
	    this.currentLevelNumber = currentLevelNumber;
	}
	return output.reverse().toString();
    }

    /**
     * Same as {@link #prependFullyQualified(docreader.list.SublistManager.LevelStore.LevelTuple)} but can process an array of tuples, used for placeholder levels
     * 
     * @param levelTuples levelTuples whose numberTexts are to be prepended
     * @return an array with the fully qualified numberTexts in the same order as the input array
     */
    String[] prependFullyQualified(final LevelStore.LevelTuple[] levelTuples) {
	assert levelTuples != null;
	final String[] output = new String[levelTuples.length];	    

	for (int i = 0; i < output.length; i++) {
	    output[i] = prependFullyQualified(levelTuples[i]);
	    if (i == 0) this.levelNumberOfFirstPlaceholder = this.currentLevelNumber; // level number of the first placeholder element to be inserted		
	}	    

	return output;
    }	

    /**
     * Compute the hierarchical difference between the current level and its predecessor;<br />
     * must be called after {@link #prependFullyQualified(docreader.list.SublistManager.LevelStore.LevelTuple)}
     * 
     * <p><em>Note:</em> If there are placeholder elements (skipped levels) "current level" refers to the level of the very first placeholder element.</p>
     * 
     * @return 
     * <dl>
     * <dt>{@code <0}</dt><dd>current is child of some more significant level than that of the predecessor</dd>
     * <dt>{@code 0}</dt><dd>both items are on the same level</dd>
     * <dt>{@code >0}</dt><dd>current is child of predecessor</dd>
     * </dl>
     */
    int getLevelDifference() {
	assert this.currentLevelNumber >= 0;
	final int comparisonBase;
	if (this.levelNumberOfFirstPlaceholder != null) {
	    // we had skipped levels
	    comparisonBase = this.levelNumberOfFirstPlaceholder;
	    // reset so the call will work properly the next time without skipped levels
	    this.levelNumberOfFirstPlaceholder = null;
	}
	else comparisonBase = this.currentLevelNumber;

	final int output = comparisonBase - this.oldLevelNumber;
	this.oldLevelNumber = this.currentLevelNumber;	    
	return output;
    }
    
    RequirementRoot getHRParent() {
	return this.hrParent;
    }

    Range getRange() {
	return this.range;
    }
    
    NestingType getNestingType() {
	return this.nestingType;
    }
    
    /**
     * Reverse a string
     * 
     * @param input the string to be reversed
     * @return a reversed copy of the original string; that is: character {@code n} will be at position {@code length-n} 
     */
    private static String reverseString(final String input) {
	return new StringBuilder(input).reverse().toString();
    }	

    
    private boolean isNestedModeActive() {
	return this.nestingType != NestingType.NOT_NESTED;
    }
    
    
    /**
     * Small helper class which stores a fake numberText along with a levelTuple;
     * necessary for pseudo / fake list paragraphs	 
     */
    final static class LevelTupleWFakeNumberText extends LevelStore.LevelTupleWSkippedLevels {	    
	public final String numberText;

	public LevelTupleWFakeNumberText(final LevelStore.LevelTupleWSkippedLevels inputTuple, final String numberText) {
	    super(inputTuple.levelTuple, inputTuple.skippedLevels);
	    this.numberText = numberText;
	}
    }

    /**
     * Handles paragraphs which are technically not in a list but visually look like a list member</br>
     * i.e. <tt>"1.2.3 I pretend to be a list item"</tt> as the paragraph text
     * 
     * <p><em>Note:</em> Limitations of this implementation:
     * <ol>
     * <li> Paragraphs which do not conform to the above pattern but need to be considered as a list paragraph, nevertheless, need to be coded in here explicitly</li>
     * <li> We assume all numberTexts of the paragraphs start at the root level (i.e. each time we come across a new numberText pattern we start a new base list)</li>
     * </ol>
     * </p>
     */
    private final static class FakeListManager {
	
	private final transient Map<String, Long> mapMatcher = new HashMap<>(1);
	private long lsid; // NOPMD - intentionally non-transient, accessor below
	private int ilvl; // NOPMD - intentionally non-transient, accessor below
	private String numberText = null; // NOPMD - intentionally non-transient, accessor below
	private boolean newBaseList; // NOPMD - intentionally non-transient, accessor below
	private final FakeLsidManager fakeLsidManager;
	
	
	public FakeListManager(final FakeLsidManager fakeLsidManager) {
	    assert fakeLsidManager != null;
	    this.fakeLsidManager = fakeLsidManager;
	}

	/**
	 * @param paragraph paragraph to process
	 * @return {@code true} if this paragraph may be considered a part of a (technically non-existent) list; {@code false} otherwise
	 */
	public boolean processParagraph(final ParagraphListAware paragraph) {
	    final boolean output;
	    listItemDeterminer: {		
		if (paragraph.getLvl() < 9 || paragraph.getIndentFromLeft() == 0) { // sometimes POI erroneously returns 9 for the outline level; hence there is a fallback after the or						
		    final String appendixNumberText = getAppendixNumberText(paragraph.getParagraph());
		    if (appendixNumberText != null) {
			// Appendix case
			this.numberText = appendixNumberText;
			this.lsid = guessLsid(this.numberText);
			this.ilvl = 1; // appendix is de facto on ilvl 1 and not 0
			output = true;
			break listItemDeterminer;
		    }
		    
		    // Ordinary case
		    assert appendixNumberText == null;
		    @SuppressWarnings("hiding")
		    final String numberText = DataConverter.separateFakeListParagraph(paragraph.getParagraph(), true);

		    if (numberText != null) {				
			final String lsid_relevantPart = getNumberTextRelevantPart(numberText);				
			this.lsid = guessLsid(lsid_relevantPart);		
			this.ilvl = guessIlvl(numberText);
			this.numberText = numberText;
			output = true;
			break listItemDeterminer;
		    }
		}
		output = false;
	    }
	    return output;
	}

	public long getLsid() {
	    return this.lsid;
	}

	public int getIlvl() {
	    return this.ilvl;
	}

	public String getNumberText() {
	    return this.numberText;
	}

	/**
	 * @return {@code true} if this list constitutes a new base list; {@code false} otherwise
	 */
	public boolean isNewBaseList() {
	    return this.newBaseList;
	}

	/**
	 * @param paragraphText text of the paragraph
	 * @return number of the appendix if this is an "appendix start paragraph" or {@code null} if this is not an "appendix start paragraph"
	 */
	@DomainSpecific
	private static String getAppendixNumberText(final Paragraph paragraph) {
	    assert paragraph != null;
	    final String paragraphText = paragraph.text();
	    final String output;
	    appendixNumberDeterminer: {
		if (paragraph.getCharacterRun(0).isBold()) {
		    final String chapNum = RegexHelper.extractRegex(paragraphText, PATTERN_APPENDIX);
		    if (chapNum != null) {
			output = Character.toString(IDENTIFIER_APPENDIX) + Character.toString(DELIMITER_LISTLEVEL) + chapNum;
			break appendixNumberDeterminer;
		    }		    
		}
		// fallback
		output = null;
	    }
	    return output;
	}

	/**
	 * Extract the very first level of the numberText
	 * 
	 * @param numberText numberText to process
	 * @return String representation of the first (most-significant) level
	 */
	private static String getNumberTextRelevantPart(final String numberText) {
	    assert numberText != null;
	    final String quotedListLevelDelimiter = RegexHelper.quoteRegex(DELIMITER_LISTLEVEL);		
	    final String[] numberParts = numberText.split(quotedListLevelDelimiter, 2);
	    final String relevantPart;

	    relevantPartDeterminer : {
		if (Character.toString(IDENTIFIER_APPENDIX).equals(numberParts[0])) {
		    final String quotedAppendixIdentifier = RegexHelper.quoteRegex(IDENTIFIER_APPENDIX);
		    final String pattern = "^" + quotedAppendixIdentifier + quotedListLevelDelimiter + "([0-9]+)" + ".*$"; 
		    final String appendixNumber = RegexHelper.extractRegex(numberText, pattern);
		    if (appendixNumber != null) {
			relevantPart = numberParts[0] + appendixNumber; // we do not insert a dot here -- this is in fact only one level and not two
			break relevantPartDeterminer;
		    }
		}
		// fallback
		relevantPart = numberParts[0];
	    }
	    return relevantPart;
	}

	/**
	 * Guess the level of a list based on the numberText string
	 * 
	 * @param numberText A string which holds the text used as the list item descriptor (i.e. "1.1.1.")
	 * @return a 0-based ilvl value
	 */
	private static int guessIlvl(final String numberText) {
	    assert numberText != null;
	    String numberTextRaw = numberText;
	    final String listLevelDelimiterAsString = Character.toString(DELIMITER_LISTLEVEL);
	    while (numberTextRaw.endsWith(listLevelDelimiterAsString)) numberTextRaw = numberTextRaw.substring(0, numberTextRaw.length()-1);

	    int levelCounter = 0;
	    for (final char currentChar : numberTextRaw.toCharArray()) {
		if (currentChar == DELIMITER_LISTLEVEL) levelCounter++; 
	    }

	    return levelCounter;		   
	}

	private long guessLsid(final String mostSiginificantListNumberElement) {
	    final long output;
	    if (this.mapMatcher.containsKey(mostSiginificantListNumberElement)) {
		output = this.mapMatcher.get(mostSiginificantListNumberElement);
		this.newBaseList = false;
	    }
	    else {
		output = computeNewLsid();
		this.mapMatcher.put(mostSiginificantListNumberElement, output);
		final String appendixPrepender = Character.toString(IDENTIFIER_APPENDIX);
		if (mostSiginificantListNumberElement.startsWith(appendixPrepender)) {		    
		    // this is a new baseList
		    this.newBaseList = true;
		}
		else this.newBaseList = false;
	    }
	    return output;
	}

	private long computeNewLsid() {
	    return this.fakeLsidManager.computeNewLsid();	    
	}
    }


    /**
     * Determines formatting properties of a paragraph necessary for correct inclusion into a list hierarchy
     * with respect to any possible list formatting overrides
     * 
     * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>	     
     */
    private final static class ParagraphPropertiesDeterminer implements Comparable<ParagraphPropertiesDeterminer> {	    
	private final transient ListLevel lvl;
	private final transient int leftIndentationFirstLine;
	private final transient int leftIndentationParagraph;
	private boolean leftIndentationAvailable;
	private final transient int outlineLevel;
	private transient boolean fakeList = false;

	private enum ListType {
	    REAL_LIST, FAKE_LIST, NO_LIST;
	}

	/**
	 * Constructor for paragraphs belonging to a list
	 * 
	 * @param readerData global readerData
	 * @param paragraph Paragraph for which to determine the properties
	 * @param lvl ListLevel data of this paragraph
	 */
	public ParagraphPropertiesDeterminer(final ReaderData readerData, final ParagraphListAware paragraph, final ListLevel lvl) {
	    assert paragraph != null && lvl != null;
	    this.lvl = lvl;

	    // compute left indentation
	    // 0 is the value that the JVM assigns by default; thus we check at several levels if it has been overridden
	    @SuppressWarnings("hiding")
	    int leftIndentationParagraph = 0;
	    // the first line indent is relative to the absolute paragraph indent
	    // see [MS-DOC] v20140721, 2.6.2, sprmPDxaLeft + sprmPDxaLeft1. Unfortunately, POI does not seem to handle sprmPNest.
	    int leftIndentationFirstLineRelative = 0;
	    @SuppressWarnings("hiding")
	    int outlineLevel = 0;

	    // Override Level 1: from list
	    final int listStyle = paragraph.getList().getListData().getLevelStyle(paragraph.getIlvl());
	    // [MS-DOC], v20140721, 2.4.6.3, Part 3, Step 1
	    if (listStyle != 0x0FFF && listStyle < readerData.getDocument().getStyleSheet().numStyles()) {
		final byte[] papx = readerData.getDocument().getStyleSheet().getStyleDescription(listStyle).getPAPX();
		final ParagraphProperties rgistdPara = ParagraphSprmUncompressor.uncompressPAP(new ParagraphProperties(), papx, 2);
		leftIndentationParagraph = rgistdPara.getDxaLeft();
		leftIndentationFirstLineRelative = rgistdPara.getDxaLeft1();
		outlineLevel = rgistdPara.getLvl();
	    }

	    // Override Level 2: from paragraph style
	    // [MS-DOC], v20140721, 2.4.6.3, Part 3, Step 4
	    // borrowed from org.apache.poi.hwpf.usermodel.Paragraph.newParagraph(Range, PAPX)
	    final ParagraphProperties paragraphPropertiesfromLvl = ParagraphSprmUncompressor.uncompressPAP(new ParagraphProperties(), this.lvl.getGrpprlPapx(), 2);
	    if (leftIndentationParagraph == 0) leftIndentationParagraph = paragraphPropertiesfromLvl.getDxaLeft();
	    if (leftIndentationFirstLineRelative == 0) leftIndentationFirstLineRelative = paragraphPropertiesfromLvl.getDxaLeft1();	        
	    if (outlineLevel == 0) outlineLevel = paragraphPropertiesfromLvl.getLvl();

	    // Override Level 3: from direct paragraph formatting
	    if (leftIndentationParagraph == 0) leftIndentationParagraph = paragraph.getIndentFromLeft();
	    if (leftIndentationFirstLineRelative == 0) leftIndentationFirstLineRelative = paragraph.getFirstLineIndent();
	    if (outlineLevel == 0) outlineLevel = paragraph.getLvl();

	    this.leftIndentationAvailable = getLeftIndentAvailability(leftIndentationParagraph, leftIndentationFirstLineRelative);	        
	    // Note: DxaLeft1() still sometimes yields wrong results. No idea why...
	    this.outlineLevel = outlineLevel;

	    // Handle the (apparently undocumented) case of a first line indent which starts somewhere left of the page edge
	    // the following is a guess but seems to work
	    @SuppressWarnings("hiding")
	    final int leftIndentationFirstLine = getAbsoluteFirstLineIndent(leftIndentationParagraph, leftIndentationFirstLineRelative);
	    if (leftIndentationFirstLine < 0) {
		this.leftIndentationParagraph = leftIndentationParagraph + (leftIndentationFirstLine * -1);
		this.leftIndentationFirstLine = 0;
	    }
	    else {
		this.leftIndentationParagraph = leftIndentationParagraph;
		this.leftIndentationFirstLine = leftIndentationFirstLine;
	    }	        	         
	}

	/**
	 * Constructor for non list-paragraphs
	 * 
	 * @param paragraph Paragraph for which to determine the properties
	 */
	public ParagraphPropertiesDeterminer(final ParagraphListAware paragraph) {
	    // TODO amend this to use a ParagraphListAware instead; lvl may be inaccurate otherwise
	    assert paragraph != null;

	    // we need to calculate the absolute indent of the first line; see #ParagraphPropertiesDeterminer(final Paragraph paragraph, final ListLevel lvl)
	    @SuppressWarnings("hiding")
	    final int leftIndentationParagraph = paragraph.getIndentFromLeft();
	    final int leftIndentationFirstLineRelative = paragraph.getFirstLineIndent();
	    this.leftIndentationAvailable = getLeftIndentAvailability(leftIndentationParagraph, leftIndentationFirstLineRelative);
	    this.leftIndentationFirstLine = getAbsoluteFirstLineIndent(leftIndentationParagraph, leftIndentationFirstLineRelative);
	    this.leftIndentationParagraph = leftIndentationParagraph;

	    this.outlineLevel = paragraph.getLvl();
	    this.lvl = null; // NOPMD - there really is no lvl in this case
	}

	/**
	 * Constructor if no information is available -- will result in a very significant paragraph
	 */
	public ParagraphPropertiesDeterminer() {
	    this.leftIndentationAvailable = true;
	    this.leftIndentationFirstLine = 0;
	    this.leftIndentationParagraph = 0;
	    this.outlineLevel = 1;
	    this.lvl = null; // NOPMD - there really is no lvl in this case
	}

	/**
	 * Get the left indentation of the first line of this paragraph.
	 * <p>For list paragraphs (i.e. those where {@link #getListType()} is set) this describes the left indent of the numberText.</p>
	 * 
	 * @return true left indentation of the first line with respect to any possible overrides.
	 */
	public int getOverriddenLeftIndentationFirstLine() {
	    return this.leftIndentationFirstLine;
	}

	/**
	 * Get the left indent of lines {@code n>1} of this paragraph.
	 * <p>For list paragraphs (i.e. those where {@link #getListType()} is set) this describes the left indent of the text of the current item.
	 * For ordinary paragraphs it is just the indent of the second line and all lines thereafter.</p>
	 * 
	 * @return true left indentation of the actual paragraph with respect to any possible overrides. 
	 */
	public int getOverriddenLeftIndentationParagraph() {
	    return this.leftIndentationParagraph;
	}

	/**
	 * @return true outline level of a list paragraph with respect to any possible overrides
	 */
	public int getOverriddenOutlineLevel() {
	    return this.outlineLevel;
	}

	/**
	 * <em>Note:</em> This method mainly exists due to a bug in POI (or Word?; who knows...)
	 * which results in 0 as the {@code dxaleft}-value despite the paragraph being indented.<br />
	 * Hence, this is a slightly more thorough test to avoid such pitfalls.
	 * 
	 * @return {@code true} if a left indentation has been found; otherwise {@code false}
	 */
	public boolean isLeftIndentationAvailable() {
	    return this.leftIndentationAvailable;
	}

	/**
	 * @param status {@code true} if this paragraph is part of a fake list; {@code false} otherwise
	 */
	public void setFakeListStatus(final boolean status) {
	    this.fakeList = status;
	}

	/**
	 * Force the determiner to not have any indentation; used for messed up formatting; will result in a paragraph at the least possible significance level
	 */
	public void setNoIndentation() {
	    this.leftIndentationAvailable = false;
	}


	/**
	 * Determines if the list number of the last processed paragraph is not unique by itself (i.e. we have a bulleted or unnumbered list or no list number at all)
	 * 
	 * @return {@code true} if the number of the last processed paragraph needs a number appendix; {@code false} otherwise
	 */
	public boolean isUnnumbered() {
	    final boolean output;
	    final ListType listType = getListType();
	    switch (listType) {
	    case REAL_LIST:
		assert this.lvl != null;
		output = this.lvl.getNumberFormat() == 0x17 || this.lvl.getNumberFormat() == (byte) 0xFF;
		break;		
	    case FAKE_LIST:
		output = false;
		break;
	    case NO_LIST:
		output = true;
		break;
	    default:
		// cannot happen; necessary to trick the flow-analysis into believing that "output" is really set
		output = true;
		break;
	    }
	    return output;		 
	}


	/**
	 * @return {@code true} if the paragraph represented by this object is part of a list; {@code false} otherwise
	 */
	public ListType getListType() {
	    final ListType output;
	    if (this.lvl == null) {
		if (this.fakeList) output = ListType.FAKE_LIST;
		else output = ListType.NO_LIST;
	    }
	    else {
		output = ListType.REAL_LIST;
	    }
	    return output;
	}

	/**
	 * Determine the list level significance of this instance compared to another {@link ParagraphPropertiesDeterminer}.
	 * 
	 * @return One of the following values:
	 * <dl>
	 * <dt>{@code -1}</dt><dd>more significant</dd>
	 * <dt>{@code 0}</dt><dd>equally significant</dd>
	 * <dt>{@code 1}</dt><dd>less significant</dd>
	 * </dl>
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	@DomainSpecific
	public int compareTo(final ParagraphPropertiesDeterminer pProperties) {
	    assert pProperties != null;
	    final int output;

	    if (this.isLeftIndentationAvailable() && pProperties.isLeftIndentationAvailable()) {
		if (pProperties.getListType() == ListType.REAL_LIST) {
		    // compare the numberText indent (if this item is part of a list) or the paragraph indent (if this item is not part of a list) with the numberText indent of the target paragraph
		    final int comparisonBaseSelf = (this.getListType() == ListType.REAL_LIST) ? this.getOverriddenLeftIndentationFirstLine() : this.getOverriddenLeftIndentationParagraph();
		    if (comparisonBaseSelf > pProperties.getOverriddenLeftIndentationFirstLine()) output = 1;
		    else if (comparisonBaseSelf == pProperties.getOverriddenLeftIndentationFirstLine()) output = 0;
		    else output = -1;
		}
		else {
		    // compare the left indent of the paragraph text of this item (if in list) or whatever is the greater indent (if not in list) with the greatest indent of the target paragraph
		    final int comparisonBaseSelf = (this.getListType() == ListType.REAL_LIST) ? this.getOverriddenLeftIndentationParagraph() : Math.max(this.getOverriddenLeftIndentationParagraph(), this.getOverriddenLeftIndentationFirstLine());
		    final int comparisonBaseTarget = Math.max(pProperties.getOverriddenLeftIndentationFirstLine(), pProperties.getOverriddenLeftIndentationParagraph());

		    assert pProperties.getListType() != ListType.REAL_LIST;
		    if (comparisonBaseSelf > comparisonBaseTarget) output = 1;
		    else if (comparisonBaseSelf == comparisonBaseTarget) output = 0;
		    else output = -1;
		}
	    }
	    else if (this.getListType() == ListType.REAL_LIST && pProperties.getListType() != ListType.REAL_LIST) {
		// indentation does not help here
		// try to have them on the same level
		output = 0;
	    }
	    else {
		// no usable left-indentations available
		logger.log(Level.FINE, "Fallback to outline levels for properties determination. This is far less reliable.");
		if (this.getOverriddenOutlineLevel() > pProperties.getOverriddenOutlineLevel()) output = 1;
		else if (this.getOverriddenOutlineLevel() == pProperties.getOverriddenOutlineLevel()) output = 0;
		else output = -1;
	    }

	    return output;
	}

	/**
	 * @param leftIndentationParagraph left indentation of the entire paragraph
	 * @param leftIndentationFirstLineRelative relative left indentation of the first line of the paragraph
	 * @return {@code true} if a left indent is available; otherwise {@code false}
	 */
	private static boolean getLeftIndentAvailability(final int leftIndentationParagraph, final int leftIndentationFirstLineRelative) {
	    assert leftIndentationParagraph >= 0; // leftIndentationFirstLineRelative may also be negative
	    return (leftIndentationParagraph != 0 || leftIndentationFirstLineRelative != 0);
	}

	/**
	 * @param leftIndentationParagraph left indentation of the entire paragraph
	 * @param leftIndentationFirstLineRelative relative left indentation of the first line of the paragraph
	 * @return absolute left indent of the first line in this paragraph
	 */
	private static int getAbsoluteFirstLineIndent(final int leftIndentationParagraph, final int leftIndentationFirstLineRelative) {
	    assert leftIndentationParagraph >= 0; // leftIndentationFirstLineRelative may also be negative
	    final int output = leftIndentationParagraph + leftIndentationFirstLineRelative;
	    assert output >= 0;
	    return output;
	}
    }	

    /**
     * Stores a list in a document
     */
    final static class LevelStore {
	/**
	 * key is the {@code ilvl}
	 */
	private final transient NavigableMap<Integer, LevelTuple> levels;
	private final List<LevelStore> copies = new ArrayList<>();
	private final LevelStore copySource; // only necessary to prevent the GC from finalizing it
	private final transient LevelTuple parent;
	private boolean deleted;
	private final boolean nestedModeActive;

	/**
	 * characters which Word uses for numberTexts of bulleted list items; wrap in '['+']' for match() 
	 */
	private final static String BULLETLEVELMATCHPATTERN = "\\u00b7\\u2012-\\u2014\\-";

	/**
	 * static object so we safe the overhead from creating a new one every time when there are no skipped levels
	 */
	private final static LevelTuple[] NOSKIPPEDLEVELS = new LevelTuple[0];


	private static abstract class LevelTupleA {	    
	    public boolean force = false;
	    public abstract LevelTuple getLevelTuple();

	    /**
	     * Only for debugging purposes
	     * @see java.lang.Object#toString()
	     */
	    @Override
	    public String toString() {
		final StringBuilder output = new StringBuilder(); // NOPMD - size of StringBuilder does not matter
		output.append(getLevelTuple().getBreadcrumbData().getNumberTextPlain());
		if (getLevelTuple().getChild() != null) {
		    output.append(" | has a child");
		}
		if (getLevelTuple().getBreadcrumbData().isUnnumbered()) {
		    output.append(" | is Unnumbered");
		}
		if (getLevelTuple().isPredecessor()) {
		    output.append(" | is Predecessor");
		}
		if (this.force) {
		    output.append(" | force");
		}
		return output.toString();
	    }
	}

	private final static class LevelTupleWForce extends LevelTupleA {
	    private final LevelTuple levelTuple;

	    public LevelTupleWForce(final LevelTuple levelTuple) {
		assert levelTuple != null;		    
		this.levelTuple = levelTuple;
		this.force = true;		    
	    }

	    @Override
	    public LevelTuple getLevelTuple() {
		return this.levelTuple;
	    }
	}

	/**
	 * immutable wrapper class for a concrete LevelTuple and an array of its associated (preceding) skipped LevelTuples	     
	 */
	static class LevelTupleWSkippedLevels {
	    public final LevelTuple levelTuple;
	    public final LevelTuple[] skippedLevels;

	    public LevelTupleWSkippedLevels(final LevelTuple levelTuple, final LevelTuple[] skippedLevels) {
		this.levelTuple = levelTuple;
		this.skippedLevels = skippedLevels;
	    }
	}

	/**
	 * Stores levels of a list	     
	 */
	class LevelTuple extends LevelTupleA {
	    private Deque<LevelStore> children = new ArrayDeque<>();		
	    private final transient ParagraphPropertiesDeterminer pProperties;		
	    private final BreadcrumbData breadcrumbData;
	    private final LevelTuple moreSignificantLevel;
	    private final LevelStore enclosingList;
	    /**
	     * Qualifier, see {@link #setPredecessor(boolean)}
	     */
	    private boolean predecessor = false;
	    /**
	     * Qualifier, see {@link #setPredecessorBulletList(boolean)}
	     */
	    private transient boolean predecessorBulletList = false;

	    /**
	     * Store and manage the number texts of all the different sublist levels;<br />
	     * not immutable	 
	     */
	    private final class BreadcrumbData {
		private final String numberText; // NOPMD - intentionally non-transient; used by #getNumberTextPlain()
		private final boolean unnumbered;
		private final static int RUNNING_NUMBER_START = 1;
		private transient int runningNumberBullet = RUNNING_NUMBER_START-1;
		private transient int runningNumberParagraph = RUNNING_NUMBER_START-1;

		/**
		 * @param numberText raw numberText for this level as obtained from MS Word
		 * @param unnumbered {@code true} if this represents an unnumbered list item; {@code false} otherwise
		 */
		public BreadcrumbData(final String numberText, final boolean unnumbered) {
		    assert numberText != null;
		    this.numberText = numberTextExtractor(numberText);
		    this.unnumbered = unnumbered;
		}

		/**
		 * Copy constructor
		 * 
		 * @param copySource BreadcrumbData where to copy from 
		 */
		public BreadcrumbData(final BreadcrumbData copySource) {
		    assert copySource != null;
		    this.numberText = copySource.numberText;
		    this.unnumbered = copySource.unnumbered;
		    this.runningNumberBullet = copySource.runningNumberBullet;
		    this.runningNumberParagraph = copySource.runningNumberParagraph;
		}

		/**
		 * Gets the number text of the level and appends a running number in brackets to make it unique, if necessary
		 * 
		 * @param increment if {@code true} increment the running number; if {@code false} then dont  
		 * @return unique, fully qualified identifier of the level, never {@code null}
		 */
		public String getNumberTextWithRunningAnnex(final boolean increment) {			
		    final StringBuilder outputBuilder = new StringBuilder(this.numberText.length());
		    outputBuilder.append(this.numberText);

		    if (increment) {
			if (this.unnumbered) {
			    if (LevelTuple.this.predecessor) {
				if (LevelTuple.this.predecessorBulletList) {
				    this.runningNumberBullet++;
				    this.runningNumberParagraph = RUNNING_NUMBER_START;
				}
				else this.runningNumberParagraph++;
			    }
			    else {
				this.runningNumberBullet++;
				if (this.runningNumberParagraph < RUNNING_NUMBER_START) this.runningNumberParagraph = RUNNING_NUMBER_START;
			    }
			}
			else this.runningNumberParagraph++;
		    }

		    if (this.unnumbered) {
			outputBuilder.append('[');
			outputBuilder.append(this.runningNumberBullet);
			outputBuilder.append(']');
		    }
		    if (this.runningNumberParagraph > RUNNING_NUMBER_START
			    || (LevelStore.this.nestedModeActive && LevelTuple.this.moreSignificantLevel == null)) {
			// do not append for paragraph# == 1 unless we are the frontier list in a table cell
			outputBuilder.append('[');
			outputBuilder.append(this.runningNumberParagraph);
			outputBuilder.append(']');
		    }

		    return outputBuilder.toString();
		}

		/**
		 * @return The numberText for this level without any annex
		 */
		public String getNumberTextPlain() {
		    return this.numberText;
		}

		/**
		 * @return {@code true} if this level is unnumbered; {@code false} otherwise
		 */
		public boolean isUnnumbered() {
		    return this.unnumbered;
		}

	    }		

	    /**
	     * Create a new LevelTuple
	     * 
	     * @param enclosingList List which this LevelTuple belongs to
	     * @param moreSignificantLevel next more significant levelTuple
	     * @param pProperties formatting properties of the paragraph this LevelTuple represents
	     * @param numberText numberText associated with the paragraph
	     */
	    public LevelTuple(final LevelStore enclosingList, final LevelTuple moreSignificantLevel, final ParagraphPropertiesDeterminer pProperties, final String numberText) {
		assert enclosingList != null && pProperties != null && numberText != null;
		this.moreSignificantLevel = moreSignificantLevel; // can be null
		this.pProperties = pProperties;
		this.enclosingList = enclosingList;
		this.breadcrumbData = new BreadcrumbData(numberText, pProperties.isUnnumbered());		    
	    }

	    /**
	     * Copy constructor
	     * 
	     * @param enclosingList the (new) enclosing list where this LevelTuple belongs to
	     * @param moreSignificantLevel the (new) moreSignificantLevel of this Tuple (will usually be copied as well - hence we need to update the reference) 
	     * @param copySource the original LevelTuple where to copy from
	     */
	    public LevelTuple(final LevelStore enclosingList, final LevelTuple moreSignificantLevel,final LevelTuple copySource) {
		assert enclosingList != null && copySource != null;
		this.moreSignificantLevel = moreSignificantLevel;
		this.pProperties = copySource.pProperties;
		this.enclosingList = enclosingList;
		this.breadcrumbData = new BreadcrumbData(copySource.breadcrumbData);
	    }

	    /**
	     * @return List which this LevelTuple belongs to; never {@code null}
	     */
	    public LevelStore getEnclosingList() {
		return this.enclosingList;
	    }

	    /**
	     * Link this LevelTuple to a child list. That is: A list
	     * <ul>
	     * <li>which is independent from the list this LevelTuple belongs to</li>
	     * <li>whose levels are all hierarchically less significant than this LevelTuple</li>
	     * </ul> 
	     * 
	     * @param child Child list
	     */
	    public void setChild(final LevelStore child) {
		assert child != null;
		assert child != getEnclosingList();
		this.children.add(child);		    
	    }

	    /**
	     * @return the child list of this level or {@code null} if there is no such list
	     */
	    public LevelStore getChild() {
		// the last added child list is the currently active one
		return this.children.peekLast();
	    }

	    /**
	     * Removes all child lists from this and less significant levels. Does not remove less significant levels of any lists.
	     *
	     * <p><em>Note:</em> Since we cannot directly finalize the children and they are also (weak) referenced from {@link SublistManager#sublistFinder}
	     * we set a deleted-flag so they wont be reused until the GC kicks in.</p>
	     * 
	     * @see LevelStore#removeLevelsBelow(int)
	     */
	    public void removeAllChildren() {
		final Set<Entry<Integer, LevelTuple>> descendingSet = this.enclosingList.levels.descendingMap().entrySet();
		for (final Entry<Integer, LevelTuple> currentEntry : descendingSet) {
		    final LevelTuple currentTuple = currentEntry.getValue();			

		    final Iterator<LevelStore> iterator = currentTuple.children.iterator();
		    while (iterator.hasNext()) {
			final LevelStore currentChild = iterator.next();
			// recursively do this for all levels (thus we are starting at the most significant one) of the child list as well
			currentChild.levels.firstEntry().getValue().removeAllChildren();

			currentChild.setDeleted();
			iterator.remove(); // allows the GC to kick in			    
		    }

		    // exit condition
		    // for non-recursive mode: this will fire for ourself (so we are effectively deleting everything which is less significant)
		    // for recursive mode: this will always point to the most significant level of the current LevelStore
		    if (currentTuple == this) break;
		}
	    }

	    public void removeAllCopies() {
		@SuppressWarnings("hiding")
		final List<LevelStore> copies = this.enclosingList.getCopies();
		while (copies.size() > 0) {
		    final LevelStore currentCopy = copies.remove(0); // eat up everything in this list
		    if (currentCopy.parent != null) {
			currentCopy.parent.setChild(currentCopy.getCopySource()); // restore original child
		    }
		    // recursively clean up
		    currentCopy.levels.firstEntry().getValue().removeAllCopies(); // TODO can we refactor this? it is unnecessarily complicated
		    currentCopy.levels.firstEntry().getValue().removeAllChildren();			
		    currentCopy.setDeleted();			
		}
	    }


	    /**
	     * @return the BreadcrumbData for this level
	     */
	    public BreadcrumbData getBreadcrumbData() {
		return this.breadcrumbData;
	    }

	    /**
	     * @return The next more significant level; may be {@code null} if there is no such level
	     */
	    public LevelTuple getMoreSignificantLevel() {
		return this.moreSignificantLevel;
	    }

	    /**
	     * <em>Note:</em> Not a true implementation of the {@code Comparable}-interface because the this is one-way
	     * 
	     * @param pPropertiesOther other properties to compare against
	     * @return One of the following values:
	     * <dl>
	     * <dt>{@code -1}</dt><dd>smaller than (more significant)</dd>
	     * <dt>{@code 0}</dt><dd>equal</dd>
	     * <dt>{@code 1}</dt><dd>greater than (less significant)</dd>
	     * </dl>
	     */
	    public int compareTo(final ParagraphPropertiesDeterminer pPropertiesOther) {
		return this.pProperties.compareTo(pPropertiesOther);
	    }

	    /**
	     * Set the predecessor field
	     * 
	     * @param input {@code true} if this level must be regarded as being on the same significance level as the paragraph to be inserted; {@code false} otherwise
	     */
	    public void setPredecessor(final boolean input) {
		this.predecessor = input;
	    }


	    /**
	     * Set the predecessor for bullets field
	     * 
	     * @param input {@code true} if this level must be regarded as being on the same significance level as the paragraph to be inserted and both the paragraph and the existing level are members of a bulleted list; {@code false} otherwise
	     * @see #setPredecessor(boolean)
	     */
	    public void setPredecessorBulletList(final boolean input) {
		this.predecessorBulletList = input;
	    }


	    /**
	     * Reset internal values for the a reuse of this object; only applicable if this represents a bulleted list
	     */
	    public void prepareForReuse() {
		this.setPredecessor(false);
		this.setPredecessorBulletList(false);
		this.breadcrumbData.runningNumberParagraph = 0;
	    }


	    /**
	     * Check the predecessor field
	     * 
	     * @return {@code true} if this level must be regarded as being on the same significance level as the paragraph to be inserted; {@code false} otherwise
	     */
	    public boolean isPredecessor() {
		return this.predecessor;
	    }	    
	    
	    /**
	     * Check the predecessor for bullet field
	     * 
	     * @return {@code true} if both this level and the paragraph to be inserted are bulleted/unnumbered; {@code false} otherwise
	     */
	    public boolean isPredecessorBulletList() {
		return this.predecessorBulletList;
	    }

	    @Override
	    public LevelTuple getLevelTuple() {
		return this;
	    }
	}	   

	/**
	 * Create a new LevelStore
	 * 
	 * @param sublistFinder context for this LevelStore; stores a weak reference to this object for fast retrieval
	 * @param lsid lsid of the list this LevelStore represents
	 * @param parent parent LevelTuple or {@code null} if there is no parent
	 */
	public LevelStore(final HashMapWeakValue<Long, LevelStore> sublistFinder, final long lsid, final LevelTuple parent) {
	    assert sublistFinder != null;
	    assert parent != null;
	    sublistFinder.put(lsid, this);		
	    parent.setChild(this);
	    this.parent = parent;
	    this.levels = new TreeMap<>();
	    this.copySource = null;
	    this.nestedModeActive = parent.enclosingList.nestedModeActive;
	}

	/**
	 * Create a new LevelStore for the baseList
	 * 
	 * @param sublistFinder context for this LevelStore; stores a weak reference to this object for fast retrieval
	 * @param lsid lsid of the list this LevelStore represents
	 * @param nestedModeActive {@code true} if we are nesting inside a table; {@code false} otherwise
	 */
	public LevelStore(final HashMapWeakValue<Long, LevelStore> sublistFinder, final long lsid, final boolean nestedModeActive) {
	    assert sublistFinder != null;
	    sublistFinder.put(lsid, this);		
	    this.parent = null; // NOPMD - intentional null assignment
	    this.levels = new TreeMap<>();
	    this.copySource = null;
	    this.nestedModeActive = nestedModeActive;
	}

	/**
	 * Copy constructor
	 * <p>Necessary for lists (fake/real) which are technically independent from the {@code copySource} but share the same hierarchy.</p> 
	 * 
	 * @param sublistFinder context for this LevelStore; stores a weak reference to this object for fast retrieval
	 * @param lsid lsid of the list this LevelStore represents
	 * @param copySource LevelStore from where to copy
	 * @throws IllegalArgumentException if an argument is {@code null}
	 * @throws IllegalStateException if copy cannot be performed
	 */
	public LevelStore(final HashMapWeakValue<Long, LevelStore> sublistFinder, final long lsid, final LevelStore copySource) {
	    assert sublistFinder != null;
	    if (copySource == null) throw new IllegalArgumentException("copySource cannot be null.");
	    if (this.deleted) throw new IllegalStateException("Cannot copy a deleted LevelStore");

	    sublistFinder.put(lsid, this);
	    if (copySource.parent != null) copySource.parent.setChild(this); // overwrite any existing child
	    this.parent = copySource.parent;

	    // we need a deep copy here because the individual levels will have a new enclosing list
	    this.levels = new TreeMap<>();
	    // do not change the moreSignificantLevel of the very first tuple (as this points to a different list anyways)
	    LevelTuple lastTuple = copySource.levels.firstEntry().getValue().moreSignificantLevel;
	    for (final Entry<Integer, LevelTuple> currentEntry : copySource.levels.entrySet()) {
		final Integer key = currentEntry.getKey();
		final LevelTuple value = new LevelTuple(this, lastTuple, currentEntry.getValue());
		lastTuple = value;
		this.levels.put(key, value);
	    }

	    copySource.addCopy(this);
	    this.copySource = copySource;
	    this.nestedModeActive = copySource.nestedModeActive;
	}


	/**
	 * Get a LevelTuple at a certain {@code ilvl} or create it if necessary
	 * 
	 * @see LevelTupleA#getLevelTuple()
	 * 
	 * @param ilvl ilvl of the level
	 * @param pProperties Paragraph properties which this LevelTuple shall be associated with
	 * @param numberText numberText associated with the paragraph
	 * @return LevelTuple representing the given values TODO
	 */
	public LevelTupleWSkippedLevels getLevelTuple(final int ilvl, final ParagraphPropertiesDeterminer pProperties, final String numberText) {
	    assert pProperties != null && numberText != null;

	    LevelTuple levelTuple = this.levels.get(ilvl);
	    final LevelTuple[] skippedLevels;
	    if (levelTuple != null) {
		// make sure any existing level tuple at this ilvl has its child removed
		// we need to do this irrespectively of whether this level is reused or not
		levelTuple.removeAllChildren();
		levelTuple.removeAllCopies();
		skippedLevels = new LevelTuple[0];
	    }
	    else {
		// never seen this level before; check if we need to prepend it with skipped levels
		skippedLevels = addSkippedLevels(numberText, ilvl, pProperties); // use our own pProperties for any skipped levels		    
	    }

	    // check if we can safely replace this tuple (true for ordinary numbered lists) or not (bulleted lists)
	    if (levelTuple == null || !levelTuple.getBreadcrumbData().isUnnumbered() || (!isBulletedNumberText(numberText) && !"".equals(numberText))) {
		levelTuple = new LevelTuple(this, findMoreSignificantLevel(ilvl), pProperties, numberText);
		this.levels.put(ilvl, levelTuple);
	    }
	    else {
		// bulleted level; reset paragraph counters
		levelTuple.prepareForReuse();		    
	    }

	    removeLevelsBelow(ilvl);		
	    return new LevelTupleWSkippedLevels(levelTuple, skippedLevels);
	}

	/**
	 * Set the deleted flag for this list.
	 * <p>This is a workaround for lists which should not be used anymore but are still existent because the GC has not yet finalized them.</p> 
	 */
	public void setDeleted() {
	    this.deleted = true;
	}

	/**
	 * @return {@true} if this list is effectively deleted and should not be used anymore; {@code false} otherwise
	 */
	public boolean isDeleted() {
	    return this.deleted;
	}

	public void addCopy(final LevelStore copy) {
	    assert copy != null;
	    this.copies.add(copy);
	}

	public List<LevelStore> getCopies() {
	    return this.copies;
	}

	public LevelStore getCopySource() {
	    return this.copySource;
	}

	/**	     
	 * Essentially does the same as {@link #findCorrectInsertionPoint(ParagraphPropertiesDeterminer, String)} but falls back to the first member of the baseList if no output was found.
	 * <p><em>Only call this for the base list!</em></p>
	 * 
	 * @param pProperties properties of the paragraph to be inserted
	 * @param numberText numberText numberText of the paragraph to be inserted
	 * @return the LevelTuple which is to be used as the insertion point, never {@code null}
	 * @throws IllegalStateException if a logical error occurred
	 */
	public LevelTuple findCorrectInsertionPointBaseList(final ParagraphPropertiesDeterminer pProperties, final String numberText) {		
	    LevelTupleA output = findCorrectInsertionPoint(pProperties, numberText);				

	    // getting the baseList as the proper insertion point is almost certainly wrong; so reset
	    if (output == this.levels.firstEntry().getValue()) output = null;

	    int strategy = 1;
	    while (output == null) {
		// apparently our paragraph does not fit anywhere
		switch (strategy) {
		case 1:
		    // try again with removed indentation
		    pProperties.setNoIndentation();
		    output = findCorrectInsertionPoint(pProperties, numberText);			
		    break;
		case 2:
		    // fallback to most significant level of the base list
		    assert this.levels.firstEntry() != null; // the baseList cannot legally exist without containing at least one level
		    output = this.levels.firstEntry().getValue();
		    assert output != null;
		    break;
		default:
		    // cannot happen
		    throw new IllegalStateException("Encountered a logical error while trying to find insertion position. Giving up.");			
		}
		strategy++;
	    }

	    assert output.getLevelTuple() != null;
	    return output.getLevelTuple();
	}

	/**
	 * Find the correct insertion point for a list item recursively in this list and all its descendants
	 * 
	 * @see #findCorrectInsertionPointBaseList(ParagraphPropertiesDeterminer, String)
	 * @param pProperties properties of the paragraph to be inserted
	 * @param numberText numberText of the paragraph to be inserted
	 * @return the LevelTuple which is to be used as the insertion point or {@code null} if no such Tuple exists
	 */
	private LevelTupleA findCorrectInsertionPoint(final ParagraphPropertiesDeterminer pProperties, final String numberText) {
	    assert pProperties != null && numberText != null;
	    final LevelTupleA outputTuple;
	    detection: {
		LevelTupleA previousTuple = null;
		for (final LevelTupleA currentTuple : this.levels.values()) {
		    final int hierarchyComparison = currentTuple.getLevelTuple().compareTo(pProperties);
		    if (hierarchyComparison > 0) {
			// handle the force-case: actually the currentTuple is already too far right, but still we need to handle it
			final LevelTupleA candidate = forceOnThisLevel(numberText, currentTuple);
			if (candidate.force) {
			    // break the current run and propagate this result up the stack
			    outputTuple = candidate;
			    break detection;				
			}

			// we have gone too far; continue with previousTuple from the last iteration			    
			break;
		    }
		    previousTuple = currentTuple;
		}

		if (previousTuple != null) {
		    // ordinary "parent" case; may propagate a "predecessor" from recursion, though
		    assert previousTuple.getLevelTuple().compareTo(pProperties) <= 0;
		    if (previousTuple.getLevelTuple().getChild() != null) {
			final LevelTupleA childTuple = previousTuple.getLevelTuple().getChild().findCorrectInsertionPoint(pProperties, numberText);
			if (childTuple != null) {
			    // finds the least significant list level which has the same pProperties significance (i.e. same left indentation)				
			    final int hierarchyComparison = childTuple.getLevelTuple().compareTo(pProperties);
			    if (hierarchyComparison == 0) {
				// we found a candidate for a "predecessor" at the same level				    
				final LevelTupleA currentLevelCandidate = isOnSameListLevel(numberText, childTuple.getLevelTuple());
				if (currentLevelCandidate.getLevelTuple().isPredecessor()) {
				    if (childTuple.force) {
					// override a forced child from a lower level if this level also matches
					outputTuple = new LevelTupleWForce(currentLevelCandidate.getLevelTuple());
				    }
				    else outputTuple = currentLevelCandidate; 
				    break detection;
				}
				else if (!childTuple.force) {					
				    // child will become parent of the new paragraph
				    // not so ordinary parent/child case (effectively we have a child list which is missing proper indentation)					
				    assert !currentLevelCandidate.getLevelTuple().isPredecessor();
				    previousTuple = currentLevelCandidate;
				}
			    }
			    else if (hierarchyComparison < 0) {
				// child will become parent of the new paragraph
				// ordinary parent/child-case
				childTuple.getLevelTuple().setPredecessor(false);
				previousTuple = childTuple;
			    }
			    if (childTuple.force) {
				outputTuple = childTuple;
				break detection;
			    }
			}
			else previousTuple.getLevelTuple().removeAllChildren(); // TODO check if this makes sense
		    }
		    else {
			// check for a fake list paragraph which should hierarchically become a child of previousTuple
			// but whose numberText wants it to be on the same level instead 
			assert previousTuple.getLevelTuple().compareTo(pProperties) <= 0;
			final LevelTupleA currentLevelCandidate = isOnSameListLevel(numberText, previousTuple.getLevelTuple());
			if (currentLevelCandidate.getLevelTuple().isPredecessor()) {
			    previousTuple = currentLevelCandidate;				
			}
		    }
		    outputTuple = previousTuple;
		}
		else {
		    // no valid insertion point found
		    outputTuple = null;
		}
	    }

	    return outputTuple;
	}

	/**
	 * Sets a {@code force}-flag if the paragraph to be inserted has to be on this current level, no matter what
	 * <p>
	 * This happens with bulleted lists which cannot be more significant than an immediately preceding bulleted list, otherwise they would break the hierarchy.
	 * </p>
	 * 
	 * @param numberText numberText of the paragraph to be inserted
	 * @param inputTuple currentTuple to check against
	 * @return the inputTuple, possibly in a wrapper class
	 * @see #isOnSameListLevel(String, LevelTuple, boolean)
	 */
	@DomainSpecific
	private static LevelTupleA forceOnThisLevel(final String numberText, final LevelTupleA inputTuple) {
	    assert numberText != null && inputTuple != null;
	    final String extractedNumber = numberTextExtractor(numberText);	
	    if (inputTuple.getLevelTuple().getBreadcrumbData().isUnnumbered()) {
		// base is a bulleted List
		if ((Character.toString(IDENTIFIER_BULLETLIST)).equals(extractedNumber)) {
		    // follower is a bulleted list
		    inputTuple.getLevelTuple().setPredecessor(true);
		    inputTuple.getLevelTuple().setPredecessorBulletList(true);
		    return new LevelTupleWForce(inputTuple.getLevelTuple());
		}
	    }
	    return inputTuple;
	}


	/**
	 * Check for "pseudo consecutive list behavior". That is: the paragraph to be inserted has to be on the same level as {@code inputTuple}.
	 * 
	 * @param numberText numberText of the paragraph to be inserted
	 * @param inputTuple current tuple to check against
	 * @return the inputTuple, possibly mutated
	 */
	private static LevelTupleA isOnSameListLevel(final String numberText, final LevelTuple inputTuple) {	
	    assert numberText != null && inputTuple != null;
	    final String[] extractedNumbers = numberTextExtractor(numberText, false);
	    final String extractedNumberLast = extractedNumbers[extractedNumbers.length-1];		
	    final boolean isSameLevel;

	    if (inputTuple.getBreadcrumbData().isUnnumbered()) {
		// base is a bulleted list
		if ("".equals(extractedNumberLast)) {
		    // follower is not a list
		    isSameLevel = true;
		    inputTuple.setPredecessorBulletList(false);
		}
		else if ((Character.toString(IDENTIFIER_BULLETLIST)).equals(extractedNumberLast)) {
		    // follower is a bulleted list
		    isSameLevel = true;
		    inputTuple.setPredecessorBulletList(true);
		}
		else isSameLevel = false;
	    }
	    else {
		assert !inputTuple.getBreadcrumbData().isUnnumbered();
		// base is a numbered list
		if ("".equals(extractedNumberLast)) {
		    // follower is not a list
		    isSameLevel = true;
		}
		else if (isConsecutiveNumbers(inputTuple.getBreadcrumbData().getNumberTextPlain(), extractedNumberLast) && isEqualLevels(extractedNumbers, inputTuple, false)) {
		    // follower is a numbered list
		    isSameLevel = true;		    
		}
		else if (extractedNumbers.length > 1 && isEqualLevels(extractedNumbers, inputTuple, true)) {
		    // multilevel list; first item of a less significant level 
		    // all more significant levels match
		    isSameLevel = true;
		}
		else isSameLevel = false;
	    }

	    inputTuple.setPredecessor(isSameLevel);
	    return inputTuple;
	}

	/**
	 * Checks if two numbers (given as strings) are consecutive
	 * 
	 * <p><em>Note:</em> This only handles the cases of simple Arabic numbers and single digit letter numbering</p>
	 * 
	 * @param first first string which must contain the smaller number
	 * @param second second string which must contain the bigger number
	 * @return {@code true} if the numbers are consecutive; {@code false} otherwise
	 */
	private static boolean isConsecutiveNumbers(final String first, final String second) {		
	    final boolean output;

	    relationDeterminer: {
		if (first.matches("^[A-Za-z]$") && second.matches("^[A-Za-z]$")) {
		    final char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();		    
		    for (int i = 0; i<alphabet.length-1; i++) {
			if (Character.toLowerCase(first.charAt(0)) == alphabet[i] && Character.toLowerCase(second.charAt(0)) == alphabet[i+1]) {			    
			    output = true;
			    break relationDeterminer;
			}			
		    }			
		}
		else if (first.matches("^[0-9]+$") && (second.matches("^[0-9]+$"))){
		    final Integer firstInt = Integer.parseInt(first);
		    final Integer secondInt = Integer.parseInt(second);
		    output = (firstInt+1 == secondInt);
		    break relationDeterminer;
		}
		// fallback: numberFormat unknown; default to false
		output = false;
	    }

	    return output;
	}


	/**
	 * Checks if the numberTexts of a given level hierarchy and an arbitrary array are (element-wise) equal
	 * 
	 * @param levelString array with the last element representing the numberText of the least significant level
	 * @param startTuple least significant tuple to base the comparison on
	 * @param startAtGivenLevel if {@code true} start the comparison at {@code startTuple}; otherwise start at the parent of {@code startTuple}
	 * @return {@code true} if all elements of the levelString match the numberTexts of the startTuple and all its more significant predecessors; otherwise {@code false}
	 */
	private static boolean isEqualLevels(final String[] levelString, final LevelTuple startTuple, final boolean startAtGivenLevel) {
	    assert levelString != null && startTuple != null;
	    final boolean output;	

	    sameLevelDetection: {
		// if there are more significant levels, check them for equality
		if (levelString.length > 1) {
		    LevelTuple currentComparisonTuple = startTuple;
		    // start with the least-significant, second last element (which is the first one that must be equal)
		    boolean skipHierarchyAdvance = startAtGivenLevel;
		    boolean firstRun = true;
		    for (int i = levelString.length-2; i>=0; i--) {
			if (!skipHierarchyAdvance) currentComparisonTuple = currentComparisonTuple.getMoreSignificantLevel();
			else skipHierarchyAdvance = false;

			do {
			    if (currentComparisonTuple == null) {
				output = false;
				break sameLevelDetection;
			    }
			    if (i<0) {
				// we ran out of numbers during skipped level detection
				output = false;
				break sameLevelDetection;
			    }
			    else if (!currentComparisonTuple.getBreadcrumbData().getNumberTextPlain().equals(levelString[i])) {
				// take care of possible skipped levels during the first run
				if (firstRun) i--;
				else {
				    // ordinary exit condition; we simply do not fit together
				    output = false;
				    break sameLevelDetection;
				}
			    }
			    else {
				// found a matching level
				break;
			    }
			} while (firstRun);
			firstRun = false;			    			    
		    }
		}
		output = true;
	    }
	    return output;
	}

	/**
	 * Finds the next more significant level with respect to skipped levels and parent lists
	 * 
	 * @param ilvl ilvl property of the current level
	 * @return the next more significant level or {@code null} if no such level exists
	 */
	private LevelTuple findMoreSignificantLevel(final int ilvl) {
	    assert ilvl >= 0;
	    int currentLevel = ilvl-1;

	    // we need a loop here to take care of skipped levels
	    while (currentLevel >= 0 && !this.levels.containsKey(currentLevel)) currentLevel--;
	    return (currentLevel == -1) ? this.parent : this.levels.get(currentLevel);	// if == 1 then return the pseudo-parent	
	}

	@DomainSpecific
	private LevelTuple[] addSkippedLevels(final String numberText, final int ownIlvl, final ParagraphPropertiesDeterminer pProperties) {
	    if (!LEVELPLACEHOLDERS) {
		// only run this if the user requested to add skipped levels
		return new LevelTuple[0];
	    }
	    assert numberText != null;

	    final String[] levelTexts = numberTextExtractor(numberText, false);
	    final int visibleLevelsOfList = levelTexts.length;
	    // Assumption: levelText of targetIlvl is always visible


	    int currentIlvl = ownIlvl - (visibleLevelsOfList-1); // start at the first visible level of the list
	    final int ilvlVsVisibleOffset = currentIlvl -0; // will be >0 if the first visible level is not at ilvl==0
	    // process all more significant levels of this list
	    final Collection<LevelTuple> skippedLevels = new ArrayList<>();
	    while (currentIlvl < ownIlvl) {
		if (this.levels.get(currentIlvl) == null) {
		    final LevelTuple placeholderTuple = new LevelTuple(this, findMoreSignificantLevel(currentIlvl), pProperties, levelTexts[currentIlvl-ilvlVsVisibleOffset]);
		    this.levels.put(currentIlvl, placeholderTuple);
		    skippedLevels.add(placeholderTuple);
		}
		currentIlvl++;
	    }
	    return skippedLevels.toArray(NOSKIPPEDLEVELS);
	}

	/**
	 * Removes all less significant levels after a given {@code ilvl}
	 * 
	 * @param ilvl ilvl of last level to be preserved
	 */
	private void removeLevelsBelow(final int ilvl) {
	    assert ilvl >= 0;
	    // TODO check if this implementation is correct -- it should be; but there is currently no test coverage in ListReaderTest
	    // should be faster than the following line (no copying)
	    // this.levels = new TreeMap<>(this.levels.headMap(ilvl, true));	
	    final Iterator<Integer> iterator = this.levels.descendingKeySet().iterator();		
	    while (iterator.hasNext() && iterator.next() > ilvl) iterator.remove();    		
	}

	/**
	 * Checks if the input string represents a character which MS Word commonly uses as the numberText of a bulleted list
	 * 
	 * @param input string to check
	 * @return {@code true} if the input string represents a bullet; {@code false} otherwise
	 */
	private static boolean isBulletedNumberText(final String input) {
	    assert input != null;
	    return input.matches('[' + BULLETLEVELMATCHPATTERN + ']');
	}

	/**
	 * Extracts the very last part of a numberText which represents the current level
	 * 
	 * @param numberText raw numberText with all levels present
	 * @return extracted part representing the current level; never {@code null}
	 */
	private static String numberTextExtractor(final String numberText) {
	    assert numberText != null;
	    return numberTextExtractor(numberText, true)[0];
	}


	/**
	 * Extract the individual levels of a numberText into an array
	 * 
	 * @param numberText numberText to process
	 * @param onlyLeastSignificantLevel if {@code true} only process the level after the last {@code LISTLEVELDELIMITER} (= dot); otherwise process all levels
	 * @return an array containing all requested levelTexts
	 */
	private static String[] numberTextExtractor(final String numberText, boolean onlyLeastSignificantLevel) {
	    assert numberText != null;

	    // Step 1: trim numberText string		
	    final String delimiterAsString = Character.toString(DELIMITER_LISTLEVEL);
	    // remove leading and trailing LISTLEVELDELIMITER if present so split() below only finds actual delimiters between levels
	    String numberTextTrimmed = numberText;
	    while (numberTextTrimmed.startsWith(delimiterAsString)) numberTextTrimmed = numberTextTrimmed.substring(1);
	    while (numberTextTrimmed.endsWith(delimiterAsString)) numberTextTrimmed = numberTextTrimmed.substring(0, numberTextTrimmed.length()-1);

	    // Step 2: extract levels from numberText
	    final String quotedListLevelDelimiter = RegexHelper.quoteRegex(delimiterAsString);
	    final String[] levelTexts = numberTextTrimmed.split(quotedListLevelDelimiter);

	    if (levelTexts.length < 1) throw new IllegalStateException("Could not extract a numberText from \"" + numberText + "\". Giving up.");

	    // Step 3: process each levelText
	    final String[] output;		
	    final int levelIteratorStart;
	    if (onlyLeastSignificantLevel) {
		// process only the very last level (i.e. everything behind the last occurrence of LISTLEVELDELIMITER
		levelIteratorStart = levelTexts.length -1;
		output = new String[1];
	    }
	    else {
		// process all levels
		levelIteratorStart = 0;
		output = new String[levelTexts.length];
	    }

	    for (int levelIterator = levelIteratorStart; levelIterator < levelTexts.length; levelIterator++) {
		String outputCurrentLevel = levelTexts[levelIterator];
		// Step 3.1: beautify each level
		// remove any kinds of spaces
		outputCurrentLevel = outputCurrentLevel.replaceAll("\\s", "");
		// regex to match decorating characters like '(' or ')'
		final String decoratingCharacterRegex = "[^A-Za-z0-9" + BULLETLEVELMATCHPATTERN + "]";
		// remove any leading decorating characters
		while (outputCurrentLevel.matches('^' + decoratingCharacterRegex + ".*")) outputCurrentLevel = outputCurrentLevel.substring(1);		    
		// remove any trailing decorating characters
		while (outputCurrentLevel.matches(".*" + decoratingCharacterRegex + '$')) outputCurrentLevel = outputCurrentLevel.substring(0, outputCurrentLevel.length()-1);		    
		// replace bulleted list items
		if (isBulletedNumberText(outputCurrentLevel)) outputCurrentLevel = Character.toString(IDENTIFIER_BULLETLIST);
		// Java (unlike Ada) does not support arrays with arbitrary indexing, hence we need to normalize if only the last level is of interest
		output[onlyLeastSignificantLevel ? 0 : levelIterator] = outputCurrentLevel;
	    }

	    return output;
	}
    }	
}
