package docreader.list;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hwpf.model.LFO;
import org.apache.poi.hwpf.model.LFOData;
import org.apache.poi.hwpf.model.ListData;
import org.apache.poi.hwpf.model.ListFormatOverrideLevel;
import org.apache.poi.hwpf.model.ListLevel;
import org.apache.poi.hwpf.usermodel.HWPFList;

import docreader.ReaderData;
import docreader.range.paragraph.ParagraphListAware;


/**
 * Computes the number text which prepends each list paragraph
 * 
 * <p><em>Note:</em> This class only handles the raw number text and does not apply any further formatting as described in [MS-DOC], v20140721, 2.4.6.3, Part 3 to it.<p>
 * <p><em>Note 2:</em> The {@code tplc}, a visual override for the appearance of list levels, as defined in [MS-DOC], v20140721, 2.9.328 is not taken care of in this class.</p>
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
final class ListReaderPlain {
    private transient final ReaderData readerData;
    private final ListStore listStore = new ListStore();
    private final static int WORD_NUM_LEVELS_MIN = 1;
    private final static int WORD_NUM_LEVELS_MAX = 9;
    private final static int NO_NUMBER_INDICATOR = -1;
    private static final Logger logger = Logger.getLogger(ListReaderPlain.class.getName()); // NOPMD - Reference rather than a static field
    private ListLevel lvlOfLastProcessedParagraph;    
    private Boolean paragraphIndentationMustBePreserved = null;

    /**
     * Ordinary constructor for a new list reader
     * 
     * @param readerData global readerData
     */
    ListReaderPlain(final ReaderData readerData) {
	this.readerData = readerData;
    }

    /**
     * Get the formatted number for a given paragraph
     * 
     * <p><em>Note:</em> This only works correctly if called subsequently for <em>all</em> paragraphs in a valid selection (main document, text field, ...) which are part of a list.</p>
     * 
     * @param paragraph list paragraph to process 
     * @return String which represents the numbering of this list paragraph; never {@code null}
     * @throws IllegalArgumentException If the given paragraph is {@code null} or is not part of a list
     * @throws IllegalStateException If problems with the document are encountered
     */
    public String getFormattedNumber(final ParagraphListAware paragraph) {
	if (paragraph == null) throw new IllegalArgumentException("Given paragraph cannot be null.");
	if (!paragraph.isInList()) throw new IllegalArgumentException("Can only process list paragraphs.");

	final HWPFList list = paragraph.getList();

	// [MS-DOC], v20140721, 2.4.6.3, Part 1, Step 2	
	final int iLvlCur = paragraph.getIlvl();
	if (iLvlCur > list.getListData().getLevels().length-1) {
	    logger.log(Level.SEVERE, "List levels are presumably not zero-based. This can happen if the file was written by 3rd party tools like OpenOffice. Cannot handle this. Giving up.");
	    throw new IllegalStateException();
	}
	final int iLfoCur = getTrueIlfo(paragraph.getIlfo());
	final ListLevelDataDeterminer listLevelDataDeterminer = new ListLevelDataDeterminer(list, iLvlCur);
	final ListFormatOverrideLevel lfolvl = listLevelDataDeterminer.getLfoLvl();
	final ListLevel lvl = listLevelDataDeterminer.getLvl();
	final ListLevel lvlNotOverridden = listLevelDataDeterminer.getLvlNotOverridden();

	this.lvlOfLastProcessedParagraph = lvl; // store for later retrieval by getLvlOfLastProcessedParagraph()
	
	// [MS-DOC], v20140721, 2.4.6.3, Part 2, Step 1
	final String xstNumberText = lvl.getNumberText();
	final PlaceholderReplacer placeholderReplacer = new PlaceholderReplacer(xstNumberText);

	determineNumberText: {
	    // [MS-DOC], v20140721, 2.4.6.3, Part 2, Step 2
	    if (lvl.getNumberFormat() == 0x17) { // NOPMD - literal comes from [MS-DOC]		
		// [MS-DOC], v20140721, 2.4.6.3, Part 2, Step 3
		final char xchBullet = xstNumberText.charAt(0);
		if ((xchBullet & 0xF000) != 0) {
		    placeholderReplacer.putReplacement(0, Character.toString((char) (xchBullet & 0x0FFF)));								
		    break determineNumberText;
		}
	    }						
	    // [MS-DOC], v20140721, 2.4.6.3, Part 2, Step 4
	    final boolean isLegalNumbering = lvl.isLegalNumbering();
	    for (final byte j : lvl.getLevelNumberingPlaceholderOffsets()) {
		if (j != 0) {
		    assert j > 0;
		    final int position = j - 1;
		    final char iLvlTemp = lvl.getNumberText().charAt(position);
		    final LevelTupleReadOnly levelTuple;
		    if (iLvlTemp == iLvlCur) levelTuple = getLevelTuple(iLfoCur, iLvlCur, lfolvl, lvl, lvlNotOverridden);					
		    else if (iLvlTemp < iLvlCur) {
			// if there is no closest previous tuple (e.g. first item of list had iLvl > 0) then initialize this level
			final ListLevelDataDeterminer listLevelDataDeterminerTemp = new ListLevelDataDeterminer(list, iLvlTemp); // NOPMD - instantiation inside loop cannot be easily avoided
			final ListFormatOverrideLevel lfolvlTemp = listLevelDataDeterminerTemp.getLfoLvl();
			final ListLevel lvlTemp = listLevelDataDeterminerTemp.getLvl();
			final ListLevel lvlNotOverriddenTemp = listLevelDataDeterminerTemp.getLvlNotOverridden();
			levelTuple = (this.listStore.getList(iLfoCur).getClosestPreviousTuple(iLvlTemp) != null && this.listStore.getList(iLfoCur).hasPreviousTupleBeenPrinted(iLvlTemp)) ? this.listStore.getList(iLfoCur).getClosestPreviousTuple(iLvlTemp) : getLevelTuple(iLfoCur, iLvlTemp, lfolvlTemp, lvlTemp, lvlNotOverriddenTemp);
		    }
		    else {
			assert iLvlTemp > iLvlCur;
			logger.log(Level.SEVERE, "Problem while finding correct replacement character in list formatting. Document malformed.");
			throw new IllegalStateException();
		    }
		    placeholderReplacer.putReplacement(position, levelTuple, isLegalNumbering);
		}
	    }
	}
	return placeholderReplacer.getReplacedString();
    }

    /**
     * @return The true Lvl, with respect to any possible overrides, of the last paragraph processed by {@link #getFormattedNumber(ParagraphListAware)}
     * @throws IllegalStateException if no lvl is available, yet
     */
    ListLevel getLvlOfLastProcessedParagraph() {
	if (this.lvlOfLastProcessedParagraph == null) throw new IllegalStateException("getFormattedNumber must be called first");
	return this.lvlOfLastProcessedParagraph;
    }
    
    
    /**
     * @return {@code true} if the left indentation of the last paragraph paragraph processed by {@link #getFormattedNumber(ParagraphListAware)} must be preserved despite any list formatting; {@code false} otherwise
     * @throws IllegalStateException if no flag is available, yet 
     */
    boolean isIndentationMustBePreserved() {
	if (this.paragraphIndentationMustBePreserved == null) throw new IllegalStateException("getFormattedNumber must be called first");
	return this.paragraphIndentationMustBePreserved;
    }
    
    /**
     * Finds the correct level tuple (=the internal store) for a given level in a given list and increments it for the next call
     * 
     * @param iLfoCur current level format overwrite index
     * @param iLvlCur current level index
     * @param lfolvl ListFormatOverrideLevel data
     * @param lvl ListLevel data, may come from an override
     * @param lvlNotOverridden ListLevel data, not from any override
     * @return a LevelTuple which may only be used for reading purposes
     */
    private LevelTupleReadOnly getLevelTuple(final int iLfoCur, final int iLvlCur, final ListFormatOverrideLevel lfolvl, final ListLevel lvl, final ListLevel lvlNotOverridden) {
	assert lvl != null; // lfolvl can be null

	// [MS-DOC], v20140721, 2.4.6.4, Step 2
	final byte nfcCur = (byte) lvl.getNumberFormat();
	if (nfcCur == (byte) 0xFF || nfcCur == 0x17) return new LevelTupleReadOnly(lvl); // NOPMD - intentional early return according to [MS-DOC]			

	// [MS-DOC], v20140721, 2.4.6.4, Step 4	
	// Note: It is not clear from [MS-DOC] whether iLvlRestartLim should depend on lvl or lvlNotOverridden. This is just a good guess.
	final int iLvlRestartLim = (lvlNotOverridden.getRestart() == -1) ? iLvlCur : lvlNotOverridden.getRestart();
	// [MS-DOC], v20140721, 2.9.150, ilvlRestartLim
	if (iLvlRestartLim > iLvlCur) {
	    logger.log(Level.SEVERE, "Level restart value is invalid. Document malformed.");
	    throw new IllegalStateException();
	}

	// Note: If we have an lfolvl available then iStartAt is only used when the respective level in the override restarts.
	// However, for the initial run Word always uses the non-overridden iStartAt
	final int iStartAtNotOverridden = checkStartAtRange(lvlNotOverridden.getStartAt());		

	// [MS-DOC], v20140721, 2.4.6.4, Step 5
	final int numCur = iStartAtNotOverridden;
	
	// [MS-DOC], v20140721, 2.4.6.4, Step 3
	// Note: this is different from paragraph.getList().getStartAt((char) iLvlCur)
	int iStartAt = checkStartAtRange((lfolvl != null && lfolvl.isStartAt() && !lfolvl.isFormatting()) ? lfolvl.getIStartAt() : lvl.getStartAt());
	
	// [MS-DOC], v20140721, 2.4.6.4, Step 6
	// yes, we do need iLfoCur twice here... Word has so many beautiful levels of indirection
	final LevelTuple levelTuple;
	if (lfolvl != null) {
	    // TODO this logic could be optimized; lfolvl.isStartAt() has been used before
	    levelTuple = this.listStore.getList(iLfoCur).getTupleOverridden(iLvlCur, iLfoCur, lvl, numCur, iLvlRestartLim, iStartAt, lfolvl.isStartAt());
	}
	else {
	    assert lvl == lvlNotOverridden;
	    levelTuple = this.listStore.getList(iLfoCur).getTupleOrdinary(iLvlCur, iLfoCur, lvl, numCur, iLvlRestartLim, iStartAt);
	}
	
	assert levelTuple != null; // assured by getTuple() above
	final LevelTupleReadOnly levelTupleOutput = levelTuple.getReadOnly(iLfoCur);
	
	this.listStore.getList(iLfoCur).putClosestPreviousTuple(iLvlCur, levelTupleOutput, true);
	this.listStore.getList(iLfoCur).resetLevels(iLvlCur);
	levelTuple.incNumCur();
	
	return levelTupleOutput;
    }

    /**
     * Checks if a given {@code startAt} value, used for the number of the first item in a particular list level, is in a valid range
     * 
     * @param iStartAt {@code startAt} value to be checked
     * @return the same {@code startAt} value, unless it is invalid
     * @throws IllegalStateException If the document is malformed.
     */
    private static int checkStartAtRange(final int iStartAt) {
	if (iStartAt < 0 || iStartAt > 0x7FFF) {
	    // [MS-DOC], v20140721, 2.9.150 / 2.9.133, iStartAt
	    logger.log(Level.SEVERE, "Level start number is invalid. Document malformed.");
	    throw new IllegalStateException();
	}
	return iStartAt;
    }

    /**
     * Obtains the true ilfo for a given paragraph.
     * <p>Sometimes Word stores the inversion of the actual ilfo in order to convey
     * information related to paragraph indentation. This method takes care of this.</p>     
     * 
     * @param ilfo Ilfo as obtained from POI
     * @return ilfo which can be used as a pointer into {@code rgLfo}
     */
    private int getTrueIlfo(final int ilfo) {	
	final int output;
	// [MS-DOC], v20140721, 2.6.2, sprmPIlfo
	if (ilfo >= 0xF802 && ilfo <= 0xFFFF) {
	    output = ilfo * -1;
	    this.paragraphIndentationMustBePreserved = Boolean.TRUE;
	}
	else {
	    assert ilfo != 0x000 && ilfo != 0xF801; // assured by paragraph.isInList()
	    output = ilfo;
	    this.paragraphIndentationMustBePreserved = Boolean.FALSE;
	}
	return output;
    }
    
    
    /**
     * Determines and stores metadata for a level in a list     
     */
    private final static class ListLevelDataDeterminer {
	private final ListFormatOverrideLevel lfolvlResult;
	private final ListLevel lvlResult;
	private final ListLevel lvlNotOverriddenResult;	
	
	/**
	 * Process a given level in a given list and determine its metadata
	 * 
	 * @param list List to process
	 * @param ilvl Level of the list to process
	 * @throws IllegalStateException If the document is malformed
	 */
	public ListLevelDataDeterminer(final HWPFList list, final int ilvl) {	    
	    // [MS-DOC], v20140721, 2.4.6.3, Part 1, Step 4
	    final ListData lstf = list.getListData();
	    if (lstf == null) {
		logger.log(Level.SEVERE, "No lstf for list formatting found. Document malformed.");
		throw new IllegalStateException();
	    }
	    // [MS-DOC], v20140721, 2.4.6.3, Part 1, Step 3	
	    final LFO lfo = list.getLFO();
	    if(lstf.getLsid() != lfo.getLsid()) {
		logger.log(Level.SEVERE, "Problem in list formatting. Lfo and Lstf data do not match. Document malformed.");
		throw new IllegalStateException();
	    }

	    ListFormatOverrideLevel lfolvl = null;
	    final ListLevel lvl;
	    determineOverride: {
		// [MS-DOC], v20140721, 2.4.6.3, Part 1, Step 5
		final LFOData lfodata = list.getLFOData();
		// [MS-DOC], v20140721, 2.4.6.3, Part 1, Step 6				
		for (final ListFormatOverrideLevel lfolvlCur : lfodata.getRgLfoLvl()) {
		    if (lfolvlCur.getLevelNum() == ilvl) {						
			lfolvl = lfolvlCur;									
			// [MS-DOC], v20140721, 2.4.6.3, Part 1, Step 7
			if (lfolvl.isFormatting()) {
			    lvl = lfolvl.getLevel();
			    break determineOverride;
			}
		    }					
		}
		// did not find an override; fallback
		// [MS-DOC], v20140721, 2.4.6.3, Part 1, Steps 8-10
		lvl = list.getListData().getLevel(ilvl+1);
	    }
	    final ListLevel lvlNotOverridden = list.getListData().getLevel(ilvl+1);

	    // write output
	    this.lvlResult = lvl;
	    this.lvlNotOverriddenResult = lvlNotOverridden;
	    this.lfolvlResult = lfolvl;	    
	}

	/**
	 * @return ListFormatOverrideLevel for a specific list at a specific level; can be {@code null}
	 */
	public ListFormatOverrideLevel getLfoLvl() {	   
	    return this.lfolvlResult;
	}

	/**
	 * @return ListLevel for a specific list at a specific level; never {@code null}
	 */
	public ListLevel getLvl() {	 
	    return this.lvlResult;
	}

	/**
	 * @return ListLevel from {@code ListData}, independent from any applicable {@code ListFormatOverrideLevel}; never {@code null}
	 */
	public ListLevel getLvlNotOverridden() {	 
	    return this.lvlNotOverriddenResult; 
	}
    }

    /**
     * Manages the character replacement and formatting for a single {@code xsLevelNumber}   	
     */
    private static final class PlaceholderReplacer {
	private final String placeholderString;
	private final Map<Integer, String> replaceStrings = new HashMap<>(WORD_NUM_LEVELS_MAX);
	private final static int[] ROMAN_VALUES_NUMBERS = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
	private final static String[] ROMAN_VALUES_CHARS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
	private final static char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz".toCharArray();

	/**
	 * Setup a new replacer for a given replacement string
	 * 
	 * @param placeholderString String containing placeholder identifiers; [MS-DOC] calls this {@code xstNumberText}
	 */
	public PlaceholderReplacer(final String placeholderString) {
	    assert placeholderString != null;
	    this.placeholderString = placeholderString;
	}

	/**
	 * Add a new replacement for a given position
	 * 
	 * @param position Offset in the placeholderString where to add the replacement
	 * @param levelTuple Source data for the replacement
	 * @param isLegalNumbering LegalNumbering qualifier from the level for which the replacement string is computed
	 */
	public void putReplacement(final int position, final LevelTupleReadOnly levelTuple, final boolean isLegalNumbering) {			
	    assert levelTuple != null;			
	    final String replacement = formatPlaceholder(levelTuple, isLegalNumbering);			
	    putReplacement(position, replacement);
	}

	/**
	 * Add a new replacement for a given position
	 * 
	 * @param position Offset in the placeholderString where to add the replacement
	 * @param replacement Raw string containing the replacement characters; <em>Note:</em> This string may have an arbitrary length
	 */
	public void putReplacement(final int position, final String replacement) {
	    assert position >= 0 && position < this.placeholderString.length();
	    assert replacement != null;
	    this.replaceStrings.put(position, replacement);
	}

	/**
	 * Compute a human-readable string with all characters replaced; [MS-DOC] calls this {@code xsLevelNumber}
	 * 
	 * @return replaced string; never {@code null}
	 */
	public String getReplacedString() {
	    // Note: capacity of the StringBuilder is just a good guess. May also be longer or shorter.
	    final StringBuilder output = new StringBuilder(this.placeholderString.length());
	    for (int i = 0; i < this.placeholderString.length(); i++) {
		final char currentChar = this.placeholderString.charAt(i);
		if (this.replaceStrings.containsKey(i)) output.append(this.replaceStrings.get(i));
		else output.append(currentChar);
	    }
	    return output.toString();
	}

	/**
	 * Formats a single level (thus a single replacement char in the {@code placeholderString})
	 * 
	 * @param levelTuple data of the current level (whose iLvl may be >= than that of the target level)
	 * @param isLegalNumbering LegalNumbering qualifier of the target level (i.e. the level where the formatting shall eventually be applied for)
	 * @return a formatted representation of the number in the given {@code levelTuple}; never {@code null}
	 */
	private static String formatPlaceholder(final LevelTupleReadOnly levelTuple, final boolean isLegalNumbering) {
	    assert levelTuple != null;

	    // check for "legal overwrite"; [MS-DOC], v20140721, 2.9.150, fLegal
	    // Java unfortunately does not support unsigned bytes (the original type of numberFormat/nfc in Word) so we need some casts in the following code
	    final byte numberFormat = (isLegalNumbering && (byte) levelTuple.getLvl().getNumberFormat() != 0x16) ? 0x00 : (byte) levelTuple.getLvl().getNumberFormat();
	    final int numCur = levelTuple.getNumCur();
	    assert ((numberFormat != 0x17 && numberFormat != (byte) 0xFF) ? numCur > NO_NUMBER_INDICATOR : numCur == NO_NUMBER_INDICATOR);

	    final String placeholder;	    
	    switch (numberFormat) { // NOPMD - contains intentional fallthrough
	    case 0x00: // arabic number
		placeholder = Integer.toString(numCur);
		break;

	    case 0x01: // uppercase roman number
		placeholder = intToRoman(numCur);
		break;

	    case 0x02: // lowercase roman number
		placeholder = intToRoman(numCur).toLowerCase();
		break;

	    case 0x03: // uppercase letter
		placeholder = intToLetter(numCur).toUpperCase();
		break;

	    case 0x04: // lowercase letter
		placeholder = intToLetter(numCur);
		break;

	    case 0x05: // ordinal number (1st, 2nd, 3rd, ...).
		placeholder = intToOrdinal(numCur);
		break;

	    case 0x12: // circle numbering
		// placeholder = "(" + Integer.toString(numCur) + ")";
		// do ordinary numbering instead
		placeholder = Integer.toString(numCur);
		break;

	    case 0x16: // arabic with leading zero
		placeholder = String.format("%02d", numCur);
		//placeholder = (numCur < 10) ? "0" + Integer.toString(numCur) : Integer.toString(numCur);
		break;

	    case 0x17: // bulleted list
		// this can in fact happen if we have levels below a bullet, otherwise this case is handled by #getNumberText()
		placeholder = ""; // This is intentional. Word inserts an empty string here.
		break;

	    case (byte) 0xFF: // no numbering at all		
		placeholder = "";
		break;

	    case 0x08:
	    case 0x09:
	    case 0x0F:
	    case 0x13:
		// these are not legal values according to [MS-DOC], v20140721, 2.9.150, nfc
		logger.log(Level.WARNING, "Illegal list number identifier encountered. Your document is malformed.");		
		// intentional fallthrough

	    default: // any other unimplemented case		
		logger.log(Level.INFO, "Unsupported list number format. Will assume this is a simple arabic number.");
		placeholder = Integer.toString(numCur);
	    }

	    assert placeholder != null;
	    return placeholder;
	}

	/**
	 * Convert the arabic number representation of a list item to its roman version
	 * <p>based on <a href="http://developerhints.blog.com/2010/08/28/finding-out-list-numbers-in-word-document-using-poi-hwpf/">Finding out list numbers in Word document using POI HWPF</a></p>
	 * 
	 * @param input The number to be converted
	 * @return A string with the roman representation of the input; never {@code null}
	 */
	private static String intToRoman(final int input) {	    
	    if (input < 1 || input > 4999) {
		logger.log(Level.INFO, "Encountered a roman value which is out of range for the conversion algorithm. Will return an arabic number, instead.");			
		return Integer.toString(input); // NOPMD - this is a fallback return 
	    }
	    
	    int currentNumber = input;
	    final StringBuilder output = new StringBuilder();	    
	    for (int i = 0; i < ROMAN_VALUES_NUMBERS.length; i++) {
		while (currentNumber >= ROMAN_VALUES_NUMBERS[i]) {
		    currentNumber -= ROMAN_VALUES_NUMBERS[i];
		    output.append(ROMAN_VALUES_CHARS[i]);        		
		}
	    }
	    return output.toString();
	}

	/**
	 * Convert the arabic number representation of a list item to a letter-based one
	 * 
	 * <p>Word uses this sequence: {@code a..z, aa..zz, aaa..}<br />
	 * Hence we can wrap around with modulo and avoid a loop.</p>
	 * <p>based on <a href="http://developerhints.blog.com/2010/08/28/finding-out-list-numbers-in-word-document-using-poi-hwpf/">Finding out list numbers in Word document using POI HWPF</a></p>
	 *  
	 * @param input The number to be converted to a letter
	 * @return A string containing the letter representation of the input
	 */
	private static String intToLetter(final int input) {      
	    final char[] output = new char[(input-1) / ALPHABET.length + 1];
	    Arrays.fill(output, ALPHABET[(input-1) % ALPHABET.length]);
	    return new String(output);
	}
	
	/**
	 * Convert the arabic number representation of a list item to a ordinal one 
	 * 
	 * @param input THe number to be converted to ordinal
	 * @return A string containing the original number with English ordinal suffixes
	 */
	private static String intToOrdinal(final int input) {
	    assert input >= 0;
	    final String output;
	    switch (input % 10) {
	    case 1: output = Integer.toString(input) + "st"; break;
	    case 2: output = Integer.toString(input) + "nd"; break;
	    case 3: output = Integer.toString(input) + "rd"; break;
	    default: output = Integer.toString(input) + "th"; break;
	    }
	    return output;
	}
    }


    /**
     * Storage for all lists in the document 
     */
    private final class ListStore {
	/**
	 * all the lists of this document, indexed by their {@code lsid}
	 */
	private final Map<Integer, LevelStore> documentLists = new HashMap<>();
	
	/**
	 * Storage for a particular list in the document (i.e. there is one instance of this class for each {@code lsid})
	 */	
	private final class LevelStore {			
	    /**
	     * array of levelTuples, indexed by their ilvl value (which is ascending from 0 to 8 for complex lists or has just one level for non-complex lists)
	     */
	    private final LevelTuple[] levelTuples;
	    private final LevelTupleReadOnly[] closestPreviousTuples;
	    private final boolean[] closestPreviousTupleReadQualifier;

	    /**
	     * Set up storage for levels of a single list
	     * 
	     * @param listLevels number of levels in this list
	     */
	    public LevelStore(final int listLevels) {
		assert listLevels == WORD_NUM_LEVELS_MIN || listLevels == WORD_NUM_LEVELS_MAX;
		this.levelTuples = new LevelTuple[listLevels];
		this.closestPreviousTuples = new LevelTupleReadOnly[listLevels];
		this.closestPreviousTupleReadQualifier = new boolean[listLevels];
	    }

	    /**
	     * Obtain the level tuple for a given level; override data available
	     * 
	     * @param ilvl level number for which to obtain the data
	     * @param ilfo level format override index at this point
	     * @param lvl ListLevel data associated with the {@code ilfo}
	     * @param startAtRestart startAt value for restarts
	     * @param restartLim limiter index for the restart of this level number
	     * @param startAtFirst effective (overridden) startAt value for the initial run (no restart)
	     * @param resetsNumbering if {@true} then this override resets the running number of this level
	     * @return LevelTuple for the requested level; never {@code null}
	     */
	    public LevelTuple getTupleOverridden(final int ilvl, final int ilfo, final ListLevel lvl, final int startAtRestart, final int restartLim, final int startAtFirst, final boolean resetsNumbering) {
		assert ilvl < this.levelTuples.length;
		if (this.levelTuples[ilvl] == null) this.levelTuples[ilvl] = new LevelTuple();
		final LevelTuple levelTuple = this.levelTuples[ilvl];
				
		levelTuple.putBaseData(restartLim, startAtRestart, startAtFirst, levelTuple.overrideRestartApplicable(resetsNumbering, ilfo, lvl));
		levelTuple.putOverrideData(ilfo, lvl, restartLim, startAtRestart);
		return levelTuple;
	    }

	    
	    /**
	     * Obtain the level tuple for a given level; no override data available
	     * 
	     * @param ilvl level number for which to obtain the data
	     * @param ilfo level format override index at this point
	     * @param lvl ListLevel data associated with the {@code ilfo}
	     * @param startAtRestart startAt value for restarts
	     * @param restartLim limiter index for the restart of this level number
	     * @param startAtFirst effective startAt value for the initial run (no restart)
	     * @return LevelTuple for the requested level; never {@code null}
	     */
	    public LevelTuple getTupleOrdinary(final int ilvl, final int ilfo, final ListLevel lvl, final int startAtRestart, final int restartLim, final int startAtFirst) {
		assert ilvl < this.levelTuples.length;
		if (this.levelTuples[ilvl] == null) this.levelTuples[ilvl] = new LevelTuple();
		final LevelTuple levelTuple = this.levelTuples[ilvl];
		
		levelTuple.putBaseData(restartLim, startAtRestart, startAtFirst, false);
		levelTuple.putLvlData(ilfo, lvl);		
		return levelTuple;
	    }	    
	    
	    /**
	     * Find the last encountered LevelTuple at a given {@code level}
	     * 
	     * @param ilvl Level which is represented by the tuple
	     * @return LevelTuple if one exists; otherwise {@code null}
	     */
	    public LevelTupleReadOnly getClosestPreviousTuple(final int ilvl) {
		assert ilvl < this.closestPreviousTuples.length;			
		return this.closestPreviousTuples[ilvl];
	    }

	    /**
	     * Store a new tuple for the previous-tuple-lookup
	     * <p><em>Note:</em> This is independent of the {@code ilfo} of this level.<p>
	     * 
	     * @param ilvl Level which is represented by the tuple 
	     * @param levelTuple LevelTuple to store
	     * @param hasBeenPrinted Qualifier whether this level has already been printed out to the user
	     */
	    public void putClosestPreviousTuple(final int ilvl, final LevelTupleReadOnly levelTuple, final boolean hasBeenPrinted) {				
		assert ilvl < this.closestPreviousTuples.length;
		assert levelTuple != null;
		this.closestPreviousTuples[ilvl] = levelTuple;
		this.closestPreviousTupleReadQualifier[ilvl] = hasBeenPrinted;
	    }


	    /**
	     * Get the qualifier whether this previous level has already been printed out to the user;
	     * will be {@code false} if we are stepping over a certain level e.g. {@code 1.1.1} directly follows {@code 1} and {@code 1.1} does not exist.
	     * 
	     * @param ilvl level of the tuple of interest
	     * @return {@code true} if this level has been printed out to the user; {@code false} otherwise
	     */
	    public boolean hasPreviousTupleBeenPrinted(final int ilvl) {
		assert ilvl < this.closestPreviousTupleReadQualifier.length;
		return this.closestPreviousTupleReadQualifier[ilvl];
	    }

	    /**
	     * Reset less-significant levels to their initial {@code startAt} values
	     * 
	     * @param ilvl level number which is to be checked against stored {@code restartLims}
	     */
	    public void resetLevels(final int ilvl) {		
		for (int i=ilvl+1; i<this.levelTuples.length; i++) {
		    final LevelTuple currentTuple = this.levelTuples[i];
	    
		    // [MS-DOC], v20140721, 2.4.6.4, Step 6, 2nd part
		    if (currentTuple != null) {
			final Integer ilfo = currentTuple.reset(ilvl);
			if (ilfo != null) putClosestPreviousTuple(i, currentTuple.getReadOnly(ilfo), false);			
			// ilfo == null means we have custom a restartLim set, which is rare
		    }		    
		}
	    }
	    	    
	}	

	/**
	 * Obtain the storage data for a particular list
	 * 
	 * @param ilfo list format overwrite index
	 * @return LevelStore for the list represented by {@code iLfo}; never {@code null}
	 */
	public LevelStore getList(final int ilfo) {
	    // Note: We need to address the individual lists via their unique lsid
	    // if there is a ListFormatOverrideLevel then possibly several different ilfos
	    // can point to the same lsid and thus the same list
	    final int lsid = ListReaderPlain.this.readerData.getDocument().getListTables().getLfo(ilfo).getLsid();
	    if (!this.documentLists.containsKey(lsid)) {
		final int numLevels = ListReaderPlain.this.readerData.getDocument().getListTables().getListData(lsid).numLevels();
		this.documentLists.put(lsid, new LevelStore(numLevels));	    			
	    }
	    return this.documentLists.get(lsid);
	}
	
    }

    /**
     * Storage for a particular level of a particular list in the document, immutable version with ListFormatOverrideLevels resolved
     * @see LevelTuple   
     */
    private final static class LevelTupleReadOnly {
	private final int numCur;
	private final ListLevel lvl;

	/**
	 * Constructor for list elements which do not have counts associated
	 * i.e. bulleted lists and lists which have no list numbers at all
	 * 
	 * @param lvl resolved ListLevel data to store
	 */
	public LevelTupleReadOnly(final ListLevel lvl) {
	    this.numCur = NO_NUMBER_INDICATOR;
	    this.lvl = lvl;
	}

	/**
	 * Ordinary constructor
	 * 
	 * @param numCur current value of the counter for this level
	 * @param lvl resolved ListLevel data to store
	 */
	public LevelTupleReadOnly(final int numCur, final ListLevel lvl) {
	    this.numCur = numCur;
	    this.lvl = lvl;
	}

	/**
	 * @return level data for this level
	 */
	public ListLevel getLvl() {
	    return this.lvl;
	}		

	/**
	 * @return current count of this level
	 */
	public int getNumCur() {
	    return this.numCur;
	}
    }

    /**
     * Storage for a particular level of a particular list in the document
     */
    private final class LevelTuple {	
	/**
	 * maintains the different overrides for each level; indexed by {@code ilfo}; access-ordered
	 */
	private final Map<Integer, LvlData> lvlMatcher = new LinkedHashMap<>(1, 0.75f, true); // this will only grow larger if a ListFormatOverrideLevel is applicable, which (according to Microsoft) is "rare"
	private BaseData baseData = null;
	
	
	private final class BaseData {
	    public final int restartLim;
	    public final int startAt;
	    private int numCur;	    
	    
	    /**
	     * Ordinary constructor
	     * 
	     * @param restartLim number of the level after which this level restarts
	     * @param startAt restart value
	     * @param startAtReal running number of the first item of this list
	     */
	    public BaseData(final int restartLim, final int startAt, final int startAtReal) {
		this.restartLim = restartLim;
		this.startAt = startAt;
		this.numCur = startAtReal;
	    }
	    
	    public void reset() {
		this.numCur = this.startAt;		
	    }
	    
	    public void incNumCur() {
		this.numCur++;
	    }
	    
	    public int getNumCur() {
		return this.numCur;
	    }
	}
	
	
	private class LvlData {
	    private final ListLevel lvl;
	    	    
	    /**
	     * Ordinary constructor
	     * 
	     * @param lvl resolved ListLevel data applicable for this {@code ilfo}	    
	     */
	    public LvlData(final ListLevel lvl) {
		assert lvl != null;
		this.lvl = lvl;		
	    }
	    
	    /**
	     * @return formatting data
	     */
	    public ListLevel getLvl() {
		return this.lvl;
	    }
	    
	    /**
	     * @return number of the level after which this level restarts
	     */
	    public int getRestartLim() {
		return LevelTuple.this.baseData.restartLim;
	    }	   
	    
	    public void reset() {
		LevelTuple.this.baseData.reset();
	    }	    
	}
	
	/**
	 * Immutable storage class for data associated with a particular {@code ilfo}
	 * <p>
	 * implements [MS-DOC], v20140721, 2.9.133
	 * </p>		 
	 */
	private final class OverrideData extends LvlData {	    
	    private final int restartLim;
	    private final int startAt;

	    /**
	     * Ordinary constructor
	     * 
	     * @param lvl resolved ListLevel data applicable for this {@code ilfo}
	     * @param restartLim number of the level after which this level restarts
	     * @param startAt restart value
	     */
	    public OverrideData(final ListLevel lvl, final int restartLim, final int startAt) {
		super(lvl);
		this.restartLim = restartLim;
		this.startAt = startAt;
	    }	    

	    /**
	     * @return number of the level after which this level restarts
	     */
	    @Override
	    public int getRestartLim() {
		// TODO is this actually applicable for an LFOLVL?
		return this.restartLim;
	    }
	    
	    @Override
	    public void reset() {
		LevelTuple.this.baseData.numCur = this.startAt;
	    }
	}

	/**
	 * Store a new association between this {@code ilvl} and an {@code ilfo} if no ListLevelOverride is applicable
	 * 
	 * @param ilfo {@code ilfo} for which this override shall be applicable
	 * @param lvl formatting data
	 */
	private void putLvlData(final int ilfo, final ListLevel lvl) {
	    assert lvl != null;
	    final LvlData previousData = this.lvlMatcher.get(ilfo);
	    if (previousData == null || previousData instanceof OverrideData) {
		// either we have nothing yet or there is some non LvlData (i.e. OverrideData) stored at the current position
		this.lvlMatcher.put(ilfo, new LvlData(lvl));
	    }	    
	}
	
	/**
	 * Store a new association between this {@code ilvl} and an {@code ilfo} if a ListLevelOverride is applicable
	 * 
	 * @param ilfo {@code ilfo} for which this override shall be applicable
	 * @param lvl formatting data
	 * @param restartLim number of the level after which this level restarts
	 * @param startAtOverridden restart value
	 */
	private void putOverrideData(final int ilfo, final ListLevel lvl, final int restartLim, final int startAtOverridden) {
	    assert lvl != null;
	    // this always overrides, no matter what
	    // small penalty for creating a new object every time, but overrides are rare anyways
	    this.lvlMatcher.put(ilfo, new OverrideData(lvl, restartLim, startAtOverridden));
	}
	
	
	/**
	 * Checks if we need to reset the numbering due to a LevelOverride
	 * 
	 * @param isRestart qualifier from Word; {@code true} if the first occurrence of this override instance causes a numbering reset
	 * @param ilfo {@code ilfo} for which the override is applicable
	 * @param lvl ListLevel of the override
	 * @return {@code true} if this particular occurrence of a LevelOverride causes a numbering restart; {@code false} otherwise
	 */
	private boolean overrideRestartApplicable(final boolean isRestart, final int ilfo, final ListLevel lvl) {
	    assert lvl != null;
	    final boolean output;

	    if (isRestart) {
		// Word says this LevelOverride restarts the numbering

		final LvlData previousData = this.lvlMatcher.get(ilfo);	    
		if (previousData instanceof OverrideData) {
		    if (previousData.getLvl() == lvl) {
			// TODO rather compare the actual LFOLVLs
			// new and old override are equal; no numbering restart applicable
			output = false;
		    }		
		    else {
			// new and old override are different, i.e. we have a new Override
			output = true;
		    }
		}
		else {
		    // we had no OverrideData here before
		    output = true;
		}
	    }
	    else {
		// we are not supposed to restart anyways
		output = false;
	    }

	    return output;
	}
	
	/**
	 * Store a new running number counter for this {@code ilvl}; each {@code ilvl} must have this set
	 * 
	 * @param restartLim number of the level after which this level restarts
	 * @param startAtOverridden restart value
	 * @param startAtReal number of the first item of this new list (may be different from {@code startAtOverridden} e.g. with zero-based lists)
	 * @param force if set then always override any (possibly) existing data
	 */
	private void putBaseData(final int restartLim, final int startAtOverridden, final int startAtReal, final boolean force) {
	    if (this.baseData == null || force) this.baseData = new BaseData(restartLim, startAtOverridden, startAtReal);
	}
	
	/**
	 * Checks if an override for a particular {@code ilfo} is available
	 * 
	 * @param ilfo {@code ilfo} for which to obtain the override
	 * @return {@code true} if an override is available; {@code false} otherwise
	 */
	private boolean isDataAvailable(final int ilfo) {
	    return this.lvlMatcher.containsKey(ilfo);
	}

	/**
	 * Obtain a "flattened" read-only version of this object; that is a particular list level for a particular ilfo
	 * 
	 * @param ilfo {@code ilfo} for which to obtain the flattened version
	 * @return a copy of the current state of this object for a particular ilfo-combination
	 */
	private LevelTupleReadOnly getReadOnly(final int ilfo) {
	    assert isDataAvailable(ilfo);	    
	    return new LevelTupleReadOnly(this.baseData.getNumCur(), this.lvlMatcher.get(ilfo).getLvl());
	}	

	/**
	 * Resets the current number of this level to its initial value
	 * 
	 * @param ilvl {@code ilvl} of the (more-significant) level which causes the reset
	 * @return the {@code ilfo} value of the reseted level or {@code null} if no reset was applicable
	 */
	private Integer reset(final int ilvl) {	    
	    Integer lastInsertedIlfo = null;
	    
	    // TODO should not be necessary to loop here anymore
	    for (final Entry<Integer, LvlData> entry : this.lvlMatcher.entrySet()) {
		final LvlData dataStore = entry.getValue();
		// [MS-DOC], v20140721, 2.9.150, ilvlRestartLim
		if (ilvl < dataStore.getRestartLim()) {
		    dataStore.reset();
		    // the underlying map is access-ordered; hence this gives us the ilfo which was last accessed
		    // this should be correct when dealing with n>1 overrides
		    lastInsertedIlfo = entry.getKey();
		}
	    }	    
	    return lastInsertedIlfo;	   
	}

	/**
	 * Increment the count of this current level
	 */
	private void incNumCur() {	    
	    this.baseData.incNumCur();	    
	}	
    }
}
