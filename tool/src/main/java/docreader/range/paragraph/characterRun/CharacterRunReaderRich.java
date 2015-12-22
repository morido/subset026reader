package docreader.range.paragraph.characterRun;

import helper.CSSManager;
import helper.Destructible;
import helper.HTMLHelper;
import helper.XmlStringWriter;
import helper.word.DataConverter;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hwpf.usermodel.CharacterRun;

import requirement.RequirementTemporary;
import docreader.ReaderData;

/**
 * Reads in a single character run including the rich text formatting
 */
public class CharacterRunReaderRich implements CharacterRunReaderGeneric {
    private final transient CharacterRunCharacteristics crCharacteristics = new CharacterRunCharacteristics();
    private final transient XmlStringWriter xmlwriter;
    private final ReaderData readerData;
    private final RequirementTemporary requirement;
    private static final Logger logger = Logger.getLogger(CharacterRunReaderRich.class.getName()); // NOPMD - Reference rather than a static field

    /**
     * Ordinary constructor
     * 
     * @param readerData global readerData
     * @param xmlwriter writer for the rich output
     * @param requirement requirement which is being read
     * @throws IllegalArgumentException if any of the arguments is {@code null}
     */
    public CharacterRunReaderRich(final ReaderData readerData, final XmlStringWriter xmlwriter, final RequirementTemporary requirement) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	if (xmlwriter == null) throw new IllegalArgumentException("xmlwriter cannot be null.");
	if (requirement == null) throw new IllegalArgumentException("requirement cannot be null.");
	this.readerData = readerData;
	this.xmlwriter = xmlwriter;
	this.requirement = requirement;
    }	    

    /**
     * Read out the properties from POI and forward them to {@link CharacterRunCharacteristics}
     * <p>this is a customized version of @link {@link org.apache.poi.hwpf.converter.WordToHtmlUtils#addCharactersProperties(CharacterRun, StringBuilder)}</p>
     * 
     * @param characterRun character run currently being processed
     */
    @SuppressWarnings("javadoc")
    @Override
    public void read(final CharacterRun characterRun) {	
	assert characterRun != null;

	// Skip trailing newlines
	//TODO check if this should rather be a cr.text().startsWith("\\r") or a cr.text().equals("\r\n");
	if(characterRun.text().equals("\r")) return;

	// Step 1: Determine actual styling
	if (characterRun.isBold()) {
	    this.crCharacteristics.addToCurrentRunPlain(HTMLHelper.getBold());
	}
	if (characterRun.isItalic()) {
	    this.crCharacteristics.addToCurrentRunPlain(HTMLHelper.getItalic());
	}
	if (characterRun.isStrikeThrough()) {
	    this.crCharacteristics.addToCurrentRunPlain("del");
	}
	if (characterRun.isDoubleStrikeThrough()) {
	    // cannot be represented better in HTML easily
	    this.crCharacteristics.addToCurrentRunPlain("del");
	}
	if (characterRun.isVanished()) {
	    logger.log(Level.INFO, "We have vanished text here. Will mark it as strike through.");
	    this.crCharacteristics.addToCurrentRunPlain("del");
	}
	if (characterRun.isMarkedDeleted()) {
	    logger.log(Level.INFO, "Text contains characters which were deleted while revision marking was on. Will apply strikethrough formatting to them.");
	    this.crCharacteristics.addToCurrentRunPlain("del");
	}
	if (characterRun.isMarkedInserted()) {
	    logger.log(Level.INFO, "Text contains characters which were inserted while revision marking was on. Will treat them as ordinary characters.");
	}
	if (characterRun.isCapitalized()) {
	    this.crCharacteristics.addToCurrentRunCSS("text-transform", "uppercase");
	}
	if (characterRun.isSmallCaps()) {
	    this.crCharacteristics.addToCurrentRunCSS("font-variant", "small-caps");
	}
	if (characterRun.getUnderlineCode() != 0x00) {
	    // We do not distinguish between different underline types here; [MS-DOC], v20140721, 2.9.127
	    this.crCharacteristics.addToCurrentRunCSS("text-decoration", "underline");		    		   
	}
	if (!characterRun.getCV().isEmpty() && !characterRun.getCV().toHex().equals("000000")) {
	    this.crCharacteristics.addToCurrentRunCSS("color", "#" + characterRun.getCV().toHex());
	}
	if (characterRun.isSymbol() && characterRun.getSymbolFont() != null) {
	    this.crCharacteristics.addToCurrentRunCSS("font-family", characterRun.getSymbolFont().getMainFontName());	    			
	}
	else if (!characterRun.getFontName().isEmpty()) {
	    this.crCharacteristics.addToCurrentRunCSS("font-family", characterRun.getFontName());
	}	    	

	this.crCharacteristics.addToCurrentRunCSS("font-size", Math.round(characterRun.getFontSize() / 2.0) + "pt");

	// check for sub- / superscript-marker in word; we do not read out font-size changes here, though
	// [MS-DOC], v20140721, 2.6.1, sprmCIss
	switch (characterRun.getSubSuperScriptIndex()) {
	case 0x00: break; //normal text
	case 0x01: this.crCharacteristics.addToCurrentRunCSS("vertical-align", "super"); break;
	case 0x02: this.crCharacteristics.addToCurrentRunCSS("vertical-align", "sub"); break;
	default: logger.log(Level.WARNING, "Encountered an illegal sub/superscript qualifier. Your document is corrupted. Will skip this styling."); break;
	}

	// Whatever this is good for. Haven't seen a document in the wild that triggers this. 
	if (characterRun.isHighlighted()) {
	    this.crCharacteristics.addToCurrentRunCSS("background-color", DataConverter.getColorName(characterRun.getHighlightedColor()));
	}		    	   

	// Write formatting output		    	   
	this.crCharacteristics.writeNextRun();


	// Step 2: output routine
	final Integer pictureOffset;
	if(characterRun.isSymbol()) {
	    // Symbol output routine  	
	    this.xmlwriter.writeCharacters(Character.toString(DataConverter.convertToUnicodeChar(characterRun)));
	}
	else if ((pictureOffset = DataConverter.getPicOffset(characterRun)) != null) {
	    // this fires for images which are not embedded in a field
	    final ImageReader imageReader = new ImageReader(this.readerData, this.xmlwriter, pictureOffset, true);
	    imageReader.writeImage(this.requirement.getHumanReadableManager());
	}
	else if (FootnoteReader.noteAvailable(characterRun)) {
	    new FootnoteReader(this.readerData, this.xmlwriter, this.requirement, characterRun).read();
	}
	else if (DataConverter.isSpace(characterRun)) {
	    this.xmlwriter.writeCharacters(" ");
	}	    
	else {
	    // Generic output routine		
	    final String[] outputLines = characterRun.text().split("[" + UNICODECHAR_VERTICALTAB + "\r]"); // split for either a vertical tab or a general cr+lf
	    boolean firstRun = true;
	    for (final String currentLine : outputLines) {
		if (!firstRun) {
		    this.xmlwriter.writeCombinedStartEndElement("br"); // replace all splits from above by a "<br />"						 
		}
		// remove all remaining control characters and write if not empty
		final String charactersToWrite = DataConverter.cleanupTextWSpaces(currentLine);
		if (!"".equals(charactersToWrite)) {
		    this.xmlwriter.writeCharacters(charactersToWrite);
		    firstRun = false;
		}
	    }
	}
    }

    /**
     * Fake destructor to make sure all opened elements get closed properly
     */
    @Override
    public void close() {
	this.xmlwriter.writeCharacters(""); // make sure to close a trailing <br/> from above
	this.crCharacteristics.close(); // write closing elements in case there are any leftovers
    }

    /**
     * Determine the applicable styles for a single character run.
     * 
     * <p>A <em>character run</em> is a set of characters with similar attributes. But, apparently,
     * Word defines "similar" in a strange way. Hence, we do some cleanup here and try to match
     * subsequent runs which share the same subset of the properties which we are actually interested in.</p>
     */
    private final class CharacterRunCharacteristics implements Destructible {			
	private final transient Set<String> currentPlain = new LinkedHashSet<>();
	private transient Set<String> oldPlain = new LinkedHashSet<>();			
	private transient CSSManager currentCSSManager = new CSSManager();
	private transient CSSManager oldCSSManager = new CSSManager();
	private transient boolean oldCSS = false;

	/**			 
	 * Differences two sets of style-tags. Called between each character run.
	 */
	public void writeNextRun() {
	    // Step 1: Determine differences for plain HTML tags

	    final LinkedHashSet<String> oldPlainTmp = new LinkedHashSet<>(this.oldPlain);
	    oldPlainTmp.removeAll(this.currentPlain); // all tags which are no more applicable

	    final LinkedHashSet<String> currentPlainTmp = new LinkedHashSet<>(this.currentPlain);
	    currentPlainTmp.removeAll(this.oldPlain); // all tags which are now applicable

	    this.oldPlain.retainAll(this.currentPlain); // tags which remain applicable
	    
	    // Step 2: Determine differences for CSS tags
	    // effectively we do not do this. Instead one character run always maps to one span tag
	    // unless no CSS styling was applicable in which case there is no span at all

	    // check if anything has changed between this and the previous run; if so write the changes
	    if (!(oldPlainTmp.isEmpty() && currentPlainTmp.isEmpty() && this.currentCSSManager.equals(this.oldCSSManager))) {
		writeClosingElementsCSS();
		writeClosingElementsPlain(oldPlainTmp);
		writeStartingElementsPlain(currentPlainTmp);
		writeStartingElementsCSS();

		this.oldPlain = new LinkedHashSet<>(this.currentPlain);		
		
		this.currentPlain.clear();
		this.oldCSSManager = this.currentCSSManager;
		this.currentCSSManager = new CSSManager();
	    }
	}

	public void addToCurrentRunPlain(final String element) {				
	    this.currentPlain.add(element);
	}

	public void addToCurrentRunCSS(final String name, final String argument) {
	    // the CSS version of the above method				
	    this.currentCSSManager.putProperty(name, argument);
	}

	/**
	 * Fake destructor
	 */
	@Override
	public void close() {
	    this.currentPlain.clear();
	    this.currentCSSManager = new CSSManager();
	    this.writeNextRun(); // causes all left-over open tags to be closed				
	}

	/**
	 * Writes plain HTML-tags
	 * 
	 * @param input A set of all the elements to write
	 */
	private void writeStartingElementsPlain(final Set<String> input) {
	    assert input != null;
	    final LinkedList<String> inputlist = new LinkedList<>(input);
	    final Iterator<String> iterator = inputlist.iterator();

	    while(iterator.hasNext()) CharacterRunReaderRich.this.xmlwriter.writeStartElement(iterator.next());				
	}						

	/**
	 * Closes a list of plain HTML-tags
	 * @see #writeStartingElementsPlain(Set)
	 * 
	 * @param input A set of all the elements to close
	 */
	private void writeClosingElementsPlain(final Set<String> input) {
	    assert input != null;
	    // unfortunately we cannot guarantee "input" contains the tags in their correct order...	    
	    CharacterRunReaderRich.this.xmlwriter.writeClosingElements(input);
	}

	/**
	 * Writes a span tag with a list of CSS-styles obtained from cssmanager
	 */
	private void writeStartingElementsCSS() {
	    if (!this.currentCSSManager.propertiesAvailable()) {
		this.oldCSS = false;					
	    }
	    else {
		CharacterRunReaderRich.this.xmlwriter.writeStartElement("span");
		CharacterRunReaderRich.this.xmlwriter.writeAttribute("style", this.currentCSSManager.toString());
		this.oldCSS = true;
	    }
	}

	/**
	 * Closes an open span-tag
	 * @see #writeStartingElementsCSS()
	 */
	private void writeClosingElementsCSS() {
	    if (this.oldCSS) {
		CharacterRunReaderRich.this.xmlwriter.writeEndElement("span");
	    }
	}
    }
}
