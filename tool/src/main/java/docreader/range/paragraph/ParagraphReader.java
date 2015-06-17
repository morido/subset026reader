package docreader.range.paragraph;

import java.util.logging.Level;
import java.util.logging.Logger;

import static helper.Constants.Links.EXTRACT_LINKS;
import helper.AbstractRichTextReaderParagraph;
import helper.CSSManager;
import helper.XmlStringWriter;
import helper.formatting.FieldLinkFormatter;
import helper.formatting.FieldSequenceFormatter;
import helper.word.FieldStore;
import helper.word.FieldStore.LinkTuple;
import helper.word.FieldStore.CharacterRunTuple;
import helper.word.FieldStore.ShapeTuple;
import helper.word.DataConverter;

import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;

import docreader.GenericReader;
import docreader.ReaderData;
import docreader.range.RequirementReaderI;
import docreader.range.paragraph.characterRun.CharacterRunReaderRaw;
import docreader.range.paragraph.characterRun.CharacterRunReaderRich;
import docreader.range.paragraph.characterRun.FakeFieldHandler;
import docreader.range.paragraph.characterRun.FieldReader;
import docreader.range.paragraph.characterRun.ImageReader;
import requirement.RequirementTemporary;

/**
 * Reads a single paragraph and stores the resulting text as XHTML (RichText) and plain text
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class ParagraphReader implements RequirementReaderI {
    private final static char SPECIALCHARACTER_IDENTIFIER = '\u0013';
    private final transient XmlStringWriter xmlwriter = new XmlStringWriter();
    private final StringBuilder rawText = new StringBuilder();
    private transient boolean readPerformed = false;

    private final transient Paragraph paragraph;
    private final transient ReaderData readerData;
    private final transient RequirementTemporary requirement;
    private final transient String lastWordOfPreviousParagraph;
    private FieldStore<Integer> firstField = null;    
    private static final Logger logger = Logger.getLogger(ParagraphReader.class.getName()); // NOPMD - Reference rather than a static field    

    /**
     * Ordinary constructor
     * 
     * @param readerData global readerData
     * @param requirement Requirement where to store links; results of reading process will <em>not</em> be stored there; instead they are available via {@link #getRawText()} and {@link #getRichText()}
     * @param paragraph Paragraph to {@link Process}
     * @param lastWordOfPreviousParagraph used for links in tables which span several paragraphs
     * @throws IllegalArgumentException one of the arguments was {@code null} or out of range
     */
    public ParagraphReader(final ReaderData readerData, final RequirementTemporary requirement, final Paragraph paragraph, final String lastWordOfPreviousParagraph) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	if (requirement == null) throw new IllegalArgumentException("requirement cannot be null.");
	if (paragraph == null) throw new IllegalArgumentException("paragraph cannot be null.");
	if (lastWordOfPreviousParagraph == null) throw new IllegalArgumentException("lastWordOfPreviousParagraph may not be null - set to empty instead.");
	
	this.paragraph = paragraph;
	this.readerData = readerData;
	this.requirement = requirement;
	this.lastWordOfPreviousParagraph = lastWordOfPreviousParagraph;
    }


    /** 
     * Reader for paragraphs.
     * <p><em>Note:</em> This will also read empty paragraphs such as those resulting from
     * a user who continuously hit {@code return} while crafting a document.</p>
     * 
     * @return the number of paragraphs to skip (that is: 0-based number of paragraphs read)
     * @see docreader.GenericReader#read()
     */
    @Override
    public Integer read() {		
	// set Paragraph properties
	final ParagraphPropertiesReader pReader = new ParagraphPropertiesReader(this.xmlwriter, this.paragraph);
	pReader.read();
	final CharacterRunReaderRich crReaderRich = new CharacterRunReaderRich(this.readerData, this.xmlwriter, this.requirement);
	final int startOffset = absoluteStartOffsetForRawReader();
	final CharacterRunReaderRaw crReaderRaw = new CharacterRunReaderRaw(startOffset, this.rawText);
	final FakeFieldHandler fakeFieldHandler = new FakeFieldHandler(this.readerData.getTraceabilityLinker().getNonQualifiedManager(), startOffset, this.requirement, this.lastWordOfPreviousParagraph);
	for (int j = 0; j < this.paragraph.numCharacterRuns(); j++) {

	    final CharacterRun characterRun = this.paragraph.getCharacterRun(j);			
	    // raw output	    
	    crReaderRaw.read(characterRun);
	    
	    final FieldDataHandler fieldDataHandler = new FieldDataHandler(new FieldReader(this.readerData, characterRun, this.paragraph), j);
	    if (fieldDataHandler.processField()) {
		j = fieldDataHandler.getIterationCountAfter();
		final String fieldText = fieldDataHandler.getCurrentFieldText();
		crReaderRaw.appendRaw(fieldText);
		fakeFieldHandler.reset(fieldDataHandler.isBookmark(), fieldText);
		continue;
	    }
	    else if (characterRun.text().equals(Character.toString(SPECIALCHARACTER_IDENTIFIER))) {
		// TODO has to go away
		continue; // TODO HYPERLINK stuff
	    }
	    else if (DataConverter.cleanupText(characterRun.text()).equals("")) {
		continue; // nothing valuable in here
	    }
	    else {
		// check for any fake links
		fakeFieldHandler.read(characterRun);
		// rich output
		crReaderRich.read(characterRun);
	    }			
	}
	fakeFieldHandler.close();
	crReaderRaw.close();
	crReaderRich.close();
	pReader.close();

	// output
	this.readPerformed = true;		
	return 0;
    }

    /**
     * @return text of paragraph with formatting applied
     * @throws IllegalStateException paragraph has not been read, yet.
     */
    public String getRichText() {
	if (!this.readPerformed) throw new IllegalStateException("The paragraph must be read first.");
	return this.xmlwriter.toString();
    }

    /**
     * @return text of paragraph without any formatting applied
     * @throws IllegalStateException paragraph has not been read, yet.
     */
    public String getRawText() {
	if (!this.readPerformed) throw new IllegalStateException("The paragraph must be read first.");
	return this.rawText.toString();
    }

    /**
     * @return first printable field in this paragraph
     * @see docreader.range.RequirementReaderI#getFirstField()
     * @throws IllegalStateException paragraph has not been read, yet.
     */    
    @Override
    public FieldStore<Integer> getFirstField() {
	if (!this.readPerformed) throw new IllegalStateException("The paragraph must be read first.");
	return this.firstField;
    }

    /**
     * Determine the startOffset for the raw reader; necessary for fake list paragraphs
     * 
     * @return the first absolute (document-wide unique) startOffset when the raw reader should start operation
     */
    private int absoluteStartOffsetForRawReader() {
	final int output;
	final int initialOffset = this.paragraph.getCharacterRun(0).getStartOffset();

	final String paragraphTextAfterNumberText = DataConverter.separateFakeListParagraph(this.paragraph, false);
	if (paragraphTextAfterNumberText == null) {
	    // no fake list paragraph applicable
	    output = initialOffset;
	}
	else {
	    assert paragraphTextAfterNumberText != null;
	    // determine the number of the first interesting characterRun
	    // this is somewhat similar to docreader.range.paragraph.characterRun.FieldReader.getCharacterRunAfterField(int) but works differently		
	    final String paragraphTextRaw = this.paragraph.text();
	    final int lastOffset = this.paragraph.getCharacterRun(this.paragraph.numCharacterRuns()-1).getEndOffset();
	    assert this.paragraph.getStartOffset() == initialOffset;
	    assert paragraphTextRaw.length() == lastOffset - initialOffset + 1;
	    final int offsetOfFirstInterestingChar = paragraphTextRaw.indexOf(paragraphTextAfterNumberText);
	    output = initialOffset + offsetOfFirstInterestingChar;
	}

	return output;
    }
    
    /**
     * Format encountered field data and take care of intended side-effects (storing data away, creating new requirements etc.)
     */
    private class FieldDataHandler {
	private final transient FieldReader fieldReader;
	private final transient int iterationCountBefore;
	private int iterationCountAfter;	
	private String currentFieldText = null;
	private boolean bookmarkField = false;

	/**
	 * @param fieldReader reference to an external field reader which extracted the raw field data
	 * @param iterationCountBefore number of character runs before the current field (0-based)
	 */
	public FieldDataHandler(final FieldReader fieldReader, final int iterationCountBefore) {
	    assert fieldReader != null;
	    this.iterationCountBefore = iterationCountBefore;
	    this.iterationCountAfter = iterationCountBefore;
	    this.fieldReader = fieldReader;
	}

	/**
	 * @return {@code true} if a field is present; {@code false} otherwise
	 */
	public boolean processField() {
	    final FieldStore<?> fieldDataTmp = this.fieldReader.read();
	    final boolean output;
	    if (fieldDataTmp != null) {
		this.iterationCountAfter = this.fieldReader.getCharacterRunAfterField(this.iterationCountBefore);
		switch (fieldDataTmp.getIdentifier()) {
		case TABLENUMBER: case FIGURENUMBER: case PAGEREFERENCE:
		    processPrintableField(fieldDataTmp);
		    break;
		case SYMBOL:
		    processSymbolField(fieldDataTmp);
		    break;
		case SHAPE:
		    processShape(fieldDataTmp);
		    break;
		case IMAGE:
		    processImage(fieldDataTmp);
		    break;
		case EQUATION:
		    processEquation(fieldDataTmp);
		    break;		    
		case CROSSREFERENCE:
		    processBookmark(fieldDataTmp);
		    break;
		default:
		    assert false: "We got " + fieldDataTmp.getIdentifier().name() + " as a FieldIdentifier. This cannot be processed here.";
		break;
		}
		output = true;
	    }
	    else output = false;

	    return output;
	}

	/**
	 * @return number of the character run after the field (0-based)
	 */
	public int getIterationCountAfter() {
	    return this.iterationCountAfter;
	}

	public String getCurrentFieldText() {
	    return this.currentFieldText;
	}
	
	/**
	 * @return {@code true} if this field represents a bookmark; {@code false otherwise}
	 */
	public boolean isBookmark() {
	    return this.bookmarkField;
	}

	private void processShape(final FieldStore<?> inputField) {
	    // there is a shape in here
	    final ShapeTuple data = convertFieldToShapeTuple(inputField).getData();
	    final ImageReader imageReader = new ImageReader(ParagraphReader.this.readerData, ParagraphReader.this.xmlwriter, data.getPictureOffset(), true, data.getEmbeddingCharacterRunStartOffset());
	    imageReader.writeImage(ParagraphReader.this.requirement.getHumanReadableManager());
	}
	
	private void processImage(final FieldStore<?> inputField) {
	    // there is a picture in here
	    final ImageReader imageReader = new ImageReader(ParagraphReader.this.readerData, ParagraphReader.this.xmlwriter, convertFieldToInt(inputField).getData(), true);
	    imageReader.writeImage(ParagraphReader.this.requirement.getHumanReadableManager());
	}

	private void processEquation(final FieldStore<?> inputField) {
	    // there is an equation in here
	    final ImageReader imageReader = new ImageReader(ParagraphReader.this.readerData, ParagraphReader.this.xmlwriter, convertFieldToInt(inputField).getData(), true);
	    imageReader.writeEquation(ParagraphReader.this.requirement.getHumanReadableManager());
	}

	private void processBookmark(final FieldStore<?> inputField) {
	    final FieldStore<LinkTuple> inputFieldLocal = convertFieldToLinkTuple(inputField); // FieldStore is immutable; so copy is ok

	    assert inputFieldLocal.getData().getLinkTarget() != null; // LinkTarget-constructor will assure this
	    final Integer bookmarkTargetStartOffset = ParagraphReader.this.readerData.getBookmarkTargetStartOffset(inputFieldLocal.getData().getLinkTarget());
	    final FieldLinkFormatter fieldFormatter;
	    if (bookmarkTargetStartOffset != null) {
		// store away link reference
		if (EXTRACT_LINKS) ParagraphReader.this.requirement.getRequirementLinks().addLinkToExternalStartOffset(bookmarkTargetStartOffset);
		fieldFormatter = new FieldLinkFormatter(ParagraphReader.this.xmlwriter, false);
	    }
	    else {
		logger.log(Level.WARNING, "Got a crossreference mark in the text but no corresponding entry in Word's internal database. Will skip this link.");
		fieldFormatter = new FieldLinkFormatter(ParagraphReader.this.xmlwriter, true);
	    }			

	    // write output
	    this.currentFieldText = inputFieldLocal.getData().getLinkText();
	    fieldFormatter.writeStartElement();			
	    ParagraphReader.this.xmlwriter.writeCharacters(this.currentFieldText);			
	    fieldFormatter.writeEndElement();
	    this.bookmarkField = true;
	}

	private void processPrintableField(final FieldStore<?> inputField) {
	    final FieldStore<Integer> inputFieldLocal = convertFieldToInt(inputField); // FieldData is immutable; so copy is ok

	    // save if this is the first encountered field
	    if (ParagraphReader.this.firstField == null) ParagraphReader.this.firstField = inputFieldLocal;

	    // write output
	    // Note: intentionally we do not write the same output for the raw and rich text output
	    this.currentFieldText = Integer.toString(inputFieldLocal.getData()); // bare metal raw version
	    final FieldSequenceFormatter fieldFormatter = new FieldSequenceFormatter(ParagraphReader.this.xmlwriter);
	    fieldFormatter.writeStartElement();
	    ParagraphReader.this.xmlwriter.writeCharacters(inputFieldLocal.getIdentifier().toString() + " " + Integer.toString(inputFieldLocal.getData())); // "redundant" rich version
	    fieldFormatter.writeEndElement();
	}
	
	private void processSymbolField(final FieldStore<?> inputField) {
	    final FieldStore<CharacterRunTuple> inputFieldLocal = convertFieldToCharacterRunTupe(inputField); // FieldData is immutable; so copy is ok
	    final CharacterRun characterRun = inputFieldLocal.getData().getCharacterRun();
	    assert characterRun != null;
	    final String overriddenfontName = inputFieldLocal.getData().getOverriddenFont();
	    final Integer overriddenCharacterCode = inputFieldLocal.getData().getOverriddenCharacterCode();
	    
	    final String fontName = overriddenfontName != null ? overriddenfontName : characterRun.getFontName();
	    final int characterCode;
	    if (overriddenCharacterCode != null) {
		characterCode = overriddenCharacterCode; // autoboxing is safe
	    }
	    else if (characterRun.text().length() == 1) {
		characterCode = characterRun.text().charAt(0);
	    }
	    else {
		logger.log(Level.WARNING, "Could not extract a character from a symbol field. Will use a question mark instead.");
		characterCode = '?';
	    }
	    
	    final String text;
	    if ("Symbol".equals(fontName)) {
		text = Character.toString(DataConverter.convertToUnicodeChar(characterCode));
	    }
	    else {
		text = Character.toString((char) characterCode);
	    }
	    this.currentFieldText = text;
	    
	    // "stripped down" version of characterRunReaderRich
	    final CSSManager cssmanager = new CSSManager();
	    cssmanager.putProperty("font-family", fontName);
	    ParagraphReader.this.xmlwriter.writeStartElement("span");
	    ParagraphReader.this.xmlwriter.writeAttribute("style", cssmanager.toString());
	    ParagraphReader.this.xmlwriter.writeCharacters(text);
	    ParagraphReader.this.xmlwriter.writeEndElement("span");
	}


	@SuppressWarnings("unchecked")
	private FieldStore<LinkTuple> convertFieldToLinkTuple(final FieldStore<?> inputField) {
	    assert inputField != null;
	    assert inputField.getData() instanceof LinkTuple : "Data is of wrong type. Cannot cast safely.";
	    return (FieldStore<LinkTuple>) inputField;
	}

	@SuppressWarnings("unchecked")
	private FieldStore<Integer> convertFieldToInt(final FieldStore<?> inputField) {
	    assert inputField != null;
	    assert inputField.getData() instanceof Integer : "Data is of wrong type. Cannot cast safely.";
	    return (FieldStore<Integer>) inputField;			
	}
	
	@SuppressWarnings("unchecked")
	private FieldStore<CharacterRunTuple> convertFieldToCharacterRunTupe(final FieldStore<?> inputField) {
	    assert inputField != null;
	    assert inputField.getData() instanceof CharacterRunTuple : "Data is of wrong type. Cannot cast safely.";
	    return (FieldStore<CharacterRunTuple>) inputField;
	}
	
	@SuppressWarnings("unchecked")
	private FieldStore<ShapeTuple> convertFieldToShapeTuple(final FieldStore<?> inputField) {
	    assert inputField != null;
	    assert inputField.getData() instanceof ShapeTuple : "Data is of wrong type. Cannot cast safely.";
	    return (FieldStore<ShapeTuple>) inputField;
	}
    }

    /**
     * Wrap a single paragraph with applicable styles
     */
    private class ParagraphPropertiesReader extends AbstractRichTextReaderParagraph implements GenericReader<Void> {
	public ParagraphPropertiesReader(final XmlStringWriter xmlwriter, final Paragraph paragraph) {
	    super(xmlwriter, paragraph);		
	}

	@Override
	public Void read() {
	    final CSSManager cssmanager = new CSSManager();						

	    // 1. Text alignment
	    // [MS-DOC], v20140721, 2.6.2, sprmPJc80
	    if (this.paragraph.getJustification() != 0x00) {
		cssmanager.putProperty("display", "block");
		switch (this.paragraph.getJustification()) {
		case 0x01: cssmanager.putProperty("text-align", "center"); break;
		case 0x02: cssmanager.putProperty("text-align", "right"); break;
		case 0x03: case 0x04: case 0x05: cssmanager.putProperty("text-align", "justify"); break;
		default: logger.log(Level.WARNING, "Encountered an illegal text-justification value. Your input document is malformed. Will skip this styling."); break;
		}
	    }

	    // 2. Background shading
	    // [MS-DOC], v20140721, 2.6.3, sprmTDefTableShdRaw
	    if (!this.paragraph.getShading().isShdAuto() && (!this.paragraph.getShading().isShdNil())) {
		cssmanager.putProperty("background-color", "#" + this.paragraph.getShading().getCvFore().toHex());
	    }

	    if (cssmanager.propertiesAvailable()) {
		this.xmlwriter.writeStartElement("p");
		this.xmlwriter.writeAttribute("style", cssmanager.toString());
		this.startTag = "p";
	    }

	    return null;
	}		
    }    
}