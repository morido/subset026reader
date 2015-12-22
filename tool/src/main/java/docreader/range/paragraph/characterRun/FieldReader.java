package docreader.range.paragraph.characterRun;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hwpf.model.FieldsDocumentPart;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Field;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;

import static helper.Constants.Internal.MSWord.PLACEHOLER_OFFICEDRAWING;
import docreader.GenericReader;
import docreader.ReaderData;
import helper.RegexHelper;
import helper.word.FieldStore;
import helper.word.DataConverter;
import helper.word.FieldStore.FieldIdentifier;
import helper.word.FieldStore.LinkTuple;
import helper.word.FieldStore.ShapeTuple;
import helper.word.FieldStore.CharacterRunTuple;


/**
 * Finds field data in a given character run
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class FieldReader implements GenericReader<FieldStore<?>> {    
    private final InternalFieldData internalFieldData;    
    private FieldStore<?> fieldStore = null;	
    private static final Logger logger = Logger.getLogger(FieldReader.class.getName()); // NOPMD - Reference rather than a static field


    /**
     * @param readerData Global readerData
     * @param inputRun CharacterRun containing the field
     * @param surroundingParagraph paragraph which contains inputRun
     * @throws IllegalArgumentException if one of the arguments is {@code null}
     */
    public FieldReader(final ReaderData readerData, final CharacterRun inputRun, final Paragraph surroundingParagraph) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	if (inputRun == null) throw new IllegalArgumentException("inputRun cannot be null.");
	if (surroundingParagraph == null) throw new IllegalArgumentException("surroundingParagraph cannot be null.");
	
	final Field field = readerData.getDocument().getFields().getFieldByStartOffset(FieldsDocumentPart.MAIN, inputRun.getStartOffset());
	if (field != null && !field.isPrivateResult()) {
	    // field available
	    this.internalFieldData = new InternalFieldData(inputRun, field, surroundingParagraph);			
	    processField();
	}
	else {
	    // no field; nothing to do
	    this.internalFieldData = null;
	}
    }

    /** 
     * Getter for the extracted FieldStore
     * 
     * @see docreader.GenericReader#read()
     * @return extracted FieldStore or {@code null} if no FieldStore was found 
     */
    @Override
    public FieldStore<?> read() {
	return this.fieldStore;
    }

    /**
     * Get the first characterRun which starts behind this field
     * <p><em>Note:</em> characterRuns are not guaranteed to end at field boundaries.</p>
     * 
     * @param startRun Last character run before the field (only used to optimize the search)
     * @return number of the first character run after the field
     * @throws IllegalArgumentException if the argument is malformed
     * @throws IllegalStateException if no field data is available
     */
    public int getCharacterRunAfterField(final int startRun) {
	if (startRun < 0) throw new IllegalArgumentException("startRun cannot be negative.");
	if (this.fieldStore == null) throw new IllegalStateException("No field available. Method cannot be called.");

	final int fieldEndOffset = this.internalFieldData.field.getMarkEndOffset();
	int iterator = startRun;
	while(iterator < this.internalFieldData.surroundingParagraph.numCharacterRuns()
		&& this.internalFieldData.surroundingParagraph.getCharacterRun(iterator).getStartOffset() < fieldEndOffset) {
	    iterator++;
	}
	
	assert iterator > 0;
	if (this.internalFieldData.surroundingParagraph.getCharacterRun(iterator-1).getEndOffset() != fieldEndOffset) {
	    logger.log(Level.WARNING, "Field of Type \"{0}\" does not end at characterRun boundaries. Some characters after the field may get lost.", this.fieldStore.getIdentifier().name());
	}
	
	return iterator;	
    }

    private void processField() {
	for (final FieldHandler currentHandler : FieldHandler.values()) {
	    if (this.internalFieldData.field.getType() == currentHandler.getTypeId()) {
		this.fieldStore = currentHandler.process(this.internalFieldData);		
		return;
	    }
	}
	logger.log(Level.INFO, "Encountered an unsupported field. Will skip it. Raw data is: {0}", this.internalFieldData);	
    }

    /**
     * Processes different types of fields. Refer to [MS-DOC], v20080215, pp. 136ff.     
     */
    private enum FieldHandler {
	/**
	 * Matches in-document links (a.k.a. "bookmarks")
	 */
	BOOKMARK(3) {		
	    @Override
	    public FieldStore<?> process(final InternalFieldData internalFieldData) {
		assert internalFieldData != null;
		if (internalFieldData.getFieldFirstSubrangeText().matches(".*REF.*")) {
		    final FieldIdentifier identifier;
		    typeDeterminer: {
			String reference;
			if ((reference = RegexHelper.extractRegex(internalFieldData.getFieldFirstSubrangeText(), "(?i)^.*\\s(_Ref[0-9]+)\\s.*")) != null) {
			    // in-text link to some other part of the document
			    identifier = FieldIdentifier.CROSSREFERENCE;
			}
			else if ((reference = RegexHelper.extractRegex(internalFieldData.getFieldFirstSubrangeText(), "(?i)^.*REF\\s(\\S+)\\s.*")) != null) {
			    // named in-text link to some other part of the document
			    identifier = FieldIdentifier.CROSSREFERENCE;
			}
			else if (RegexHelper.extractRegex(internalFieldData.getFieldFirstSubrangeText(), "(?i)^.*\\s(_Hlt[0-9]+)\\s.*") != null) {
			    // God knows what these are good for...			    
			    logger.log(Level.INFO, "Got an \"HLT\" bookmark field. No idea what to do with it. Will skip. Raw input text: {0}", internalFieldData.getFieldFirstSubrangeText());
			    break typeDeterminer;
			}
			else {
			    logger.log(Level.INFO, "Unknown bookmark field encountered. Will skip it. Raw input text: {0}", internalFieldData.getFieldFirstSubrangeText());						
			    break typeDeterminer;
			}
			final String linkText = internalFieldData.getFieldSecondSubrangeText();
			if ("".equals(linkText)) {
			    logger.log(Level.INFO, "Found an in-text link without any linked text (i.e. there is nothing which can be highlighted). Link will be added to the requirement anyways.");
			}
			return new FieldStoreInternal<>(identifier, new LinkTuple(reference, linkText));
		    }
		}
		return null;
	    }
	},
	/**
	 * Matches sequence fields (e.g. subtitles for "floating" images/tables)
	 */
	SEQUENCE(12) {
	    @Override
	    public FieldStore<?> process(final InternalFieldData internalFieldData) {
		assert internalFieldData != null;
		if (internalFieldData.getFieldFirstSubrangeText().matches(".*SEQ.*")) {
		    final FieldIdentifier identifier;
		    typeDeterminer: {
			if (internalFieldData.getFieldFirstSubrangeText().matches("(?i).*Table.*")) identifier = FieldIdentifier.TABLENUMBER;
			else if (internalFieldData.getFieldFirstSubrangeText().matches("(?i).*Figure.*")) identifier = FieldIdentifier.FIGURENUMBER;			
			else {
			    logger.log(Level.INFO, "Unknown sequence field encountered. Will skip it. Raw input text: {0}", internalFieldData.getFieldFirstSubrangeText());						
			    break typeDeterminer;
			}
			final Integer reference = RegexHelper.extractNumber(internalFieldData.getFieldSecondSubrangeText());
			if (reference == null) {
			    logger.log(Level.INFO, "No associated number found for field. Probably the input document is malformed. Will skip this field. Payload was: {0}", internalFieldData.getFieldSecondSubrangeText());
			    break typeDeterminer;
			}
			return new FieldStoreInternal<>(identifier, reference);
		    }
		}
		return null;
	    }			
	},

	/**
	 * Matches references to pages (i.e. clickable links to other pages in the document)	 
	 */
	PAGEREF(37) {
	    @Override
	    public FieldStore<?> process(final InternalFieldData internalFieldData) {
		assert internalFieldData != null;
		if (internalFieldData.getFieldFirstSubrangeText().matches(".*PAGEREF.*")) {
		    final FieldIdentifier identifier = FieldIdentifier.PAGEREFERENCE;
		    typeDeterminer: {
			final Integer reference = RegexHelper.extractNumber(internalFieldData.getFieldSecondSubrangeText());
			if (reference == null) {
			    logger.log(Level.INFO, "No associated number found for field. Probably the input document is malformed. Will skip this field. Raw input text: {0}", internalFieldData.getFieldSecondSubrangeText());
			    break typeDeterminer;
			}
			return new FieldStoreInternal<>(identifier, reference);
		    }
		}
		return null;
	    }	   
	},
	
	/**
	 * Matches a single symbol character embedded in a field	 
	 */
	SYMBOL(57) {
	    @Override
	    public FieldStore<?> process(final InternalFieldData internalFieldData) {
		assert internalFieldData != null;
		if (internalFieldData.getFieldFirstSubrangeText().matches(".*SYMBOL.*")) {
		    final FieldIdentifier identifier = FieldIdentifier.SYMBOL;
		    typeDeterminer: {
			final Range payloadRaw = internalFieldData.getFieldSecondSubRangeRaw();
			final CharacterRun payload;
			if (payloadRaw.numCharacterRuns() == 1) {
			    payload = payloadRaw.getCharacterRun(0);
			}
			else {
			    logger.log(Level.INFO, "Got a SYMBOL-field with strange payload. Will skip. Payload was: {0}", internalFieldData.getFieldSecondSubrangeText());
			    break typeDeterminer;
			}
			if (payload == null) {
			    logger.log(Level.INFO, "No associated characters found for field. Probably the input document is malformed. Will skip this field. Payload was: {0}", internalFieldData.getFieldSecondSubrangeText());
			    break typeDeterminer;
			}
			
			final String[] extractedGroups = RegexHelper.extractRegex(internalFieldData.getFieldFirstSubrangeText(), "^SYMBOL.*?([0-9]+).*\\\\f \"(.*)\"\\s?\\\\s.*$", 2);
			final Integer characterCode;
			final String fontName;

			if (extractedGroups == null) {
			    logger.log(Level.INFO, "No associated font and/or characterCode found for field. Probably the input document is malformed. Will use fall back. Payload was: {0}", internalFieldData.getFieldSecondSubrangeText());
			    characterCode = null;
			    fontName = null;
			}
			else {
			    assert extractedGroups.length == 2;
			    characterCode = Integer.parseInt(extractedGroups[0]);
			    fontName = extractedGroups[1];
			}
			return new FieldStoreInternal<>(identifier, new CharacterRunTuple(payload, fontName, characterCode));
		    }
		}
		return null;
	    }	   
	},
	
	/**
	 * Matches embedded data such as images, equations and other OLE-objects
	 */
	EMBEDDED(58) {
	    @Override
	    public FieldStore<?> process(final InternalFieldData internalFieldData) {
		assert internalFieldData != null;
		if (internalFieldData.getFieldFirstSubrangeText().matches(".*EMBED.*")) {
		    final FieldIdentifier identifier;			
		    typeDeterminer: {
			final String typeText = internalFieldData.getFieldFirstSubrangeText();
			// TODO rather make FieldIdentifier.IMAGE the fallback after EQUATION; but first check if this conforms with all the tests (i.e. do we ever reach the fallback else?)
			if (typeText.matches("(?i).*Word\\.Picture.*")
				|| typeText.matches("(?i).*Visio\\.Drawing.*") 
				|| typeText.matches("(?i).*Designer\\.Drawing.*") // this matches Corel Designer; needed for subset-026, chapter 3, Baseline 2.3.0.d 
				|| typeText.matches("(?i).*FlowCharter7\\.Document.*") // this matches iGrafx Flowcharter; needed for subset-026, chapter 5, Baseline 2.3.0.d 
				|| typeText.matches("(?i).*Word\\.Document.*")
				|| typeText.matches(".*Unknown")) identifier = FieldIdentifier.IMAGE;
			else if (typeText.matches("(?i).*Equation.*")) identifier = FieldIdentifier.EQUATION;
			else {
			    logger.log(Level.INFO, "Unknown embedded field encountered. Will skip it. Raw input text: {0}", internalFieldData.getFieldFirstSubrangeText());						
			    break typeDeterminer;
			}
			final Integer reference = internalFieldData.getFieldSecondSubrangePicOffset();
			if (reference == null) {
			    logger.log(Level.INFO, "No associated picture offset found for field. Probably the input document is malformed. Will skip this field. Payload was: {0}", internalFieldData.getFieldSecondSubrangeText());
			    break typeDeterminer;
			}
			return new FieldStoreInternal<>(identifier, reference);
		    }					
		}
		return null;
	    }
	},
	
	/**
	 * Matches external hyperlinks
	 */
	HYPERLINK(88) {
	    @Override
	    public FieldStore<?> process(final InternalFieldData internalFieldData) {
		assert internalFieldData != null;
		if (internalFieldData.getFieldFirstSubrangeText().matches(".*HYPERLINK.*")) {
		    final FieldIdentifier identifier;
		    typeDeterminer: {
			String reference;
			if ((reference = RegexHelper.extractRegex(internalFieldData.getFieldFirstSubrangeText(), "(?i)HYPERLINK\\s*\\\\l\\s*\"(\\S+)\"\\s.*")) != null) {
			    // named in-text link to some other part of the document (link a bookmark, see above)
			    identifier = FieldIdentifier.CROSSREFERENCE;
			}			
			else {
			    logger.log(Level.INFO, "Unknown hyperlink field encountered. Will skip it. Raw input text: {0}", internalFieldData.getFieldFirstSubrangeText());						
			    break typeDeterminer;
			}
			final String linkText = internalFieldData.getFieldSecondSubrangeText();
			if ("".equals(linkText)) {
			    logger.log(Level.INFO, "Found an in-text link without any linked text (i.e. there is nothing which can be highlighted). Link will be added to the requirement anyways.");
			}
			return new FieldStoreInternal<>(identifier, new LinkTuple(reference, linkText));
		    }
		}
		return null;
	    }    
	},
	
	/**	 
	 * Matches OfficeDrawings hidden in a SHAPE field
	 */
	SHAPE(95) {
	    @Override
	    public FieldStore<?> process(final InternalFieldData internalFieldData) {
		assert internalFieldData != null;
		if (internalFieldData.getFieldFirstSubrangeText().matches(".*SHAPE.*")) {
		    final FieldIdentifier identifier;
		    typeDeterminer: {
			final Integer referencePicOffset;
			final Integer referenceRangeOffset;
			final Range secondSubRange = internalFieldData.getFieldSecondSubRangeRaw();			
			if (secondSubRange.numCharacterRuns() == 2) {
			    final String text = secondSubRange.getCharacterRun(0).text();		   
			    if (text.length() == 1 && text.charAt(0) == PLACEHOLER_OFFICEDRAWING) {
				// is an officeDrawing
				// Note: we are not making use of the OfficeDrawingReader here because we cannot extract any meaningful data anyways				
				logger.log(Level.INFO, "Got a shape with an OfficeDrawing here. Will try to obtain an image representation.");				
				// there is in fact picture data here; so we could do DataConverter.getPicOffset(secondSubRange.getCharacterRun(1)); but that would only get us to the actual OfficeDrawing which we cannot render
				referenceRangeOffset = internalFieldData.inputRun.getStartOffset();
				referencePicOffset = DataConverter.getPicOffset(secondSubRange.getCharacterRun(1));
				if (referencePicOffset == null) {
				    logger.log(Level.INFO, "No associated picture offset found for field. Probably the input document is malformed. Will skip this field. Payload was: {0}", internalFieldData.getFieldSecondSubrangeText());
				    break typeDeterminer;
				}
				identifier = FieldIdentifier.SHAPE;
			    }
			    else {
				logger.log(Level.INFO, "Got a shape field without any drawing. This is confusing. Will skip. Payload was: {0}", internalFieldData.getFieldSecondSubrangeText());
				break typeDeterminer;
			    }
			}
			else {
			    logger.log(Level.INFO, "Got a SHAPE-field with a strange payload. Will skip. Payload was: {0}", internalFieldData.getFieldSecondSubrangeText());
			    break typeDeterminer;
			}
			return new FieldStoreInternal<>(identifier, new ShapeTuple(referencePicOffset, referenceRangeOffset));
		    }
		}
		return null;
	    }	    
	};

	/**
	 * Numerical id according to the table in [MS-DOC], v20080215, pp. 136ff. 
	 */
	private final int typeId;		

	private FieldHandler(final int typeId) {
	    this.typeId = typeId;
	}

	public int getTypeId() {
	    return this.typeId;
	}

	/**
	 * Processes POI data into a standardized FieldStore
	 * 
	 * @param internalFieldData Field as extracted by POI
	 * @return standardized FieldStore
	 */
	public abstract FieldStore<?> process(final InternalFieldData internalFieldData);

	private class FieldStoreInternal<T> implements FieldStore<T> {
	    private final FieldIdentifier identifier;
	    private final T data;

	    private FieldStoreInternal(final FieldIdentifier identifier, final T data) {
		assert identifier != null && data != null;
		this.identifier = identifier;
		this.data = data;
	    }

	    @Override
	    public T getData() {
		return this.data;
	    }

	    @Override
	    public FieldIdentifier getIdentifier() {
		return this.identifier;
	    }
	}		
    }

    /**
     * Holds the raw field as extracted by POI and the surrounding paragraph
     */
    private static class InternalFieldData {
	public final CharacterRun inputRun;
	public final Field field;
	public final Paragraph surroundingParagraph;	

	public InternalFieldData(CharacterRun inputRun, final Field field, final Paragraph surroundingParagraph) {
	    this.inputRun = inputRun;
	    this.field = field;
	    this.surroundingParagraph = surroundingParagraph;
	}

	/**
	 * Helper function to avoid NPEs
	 * 
	 * @return the text of the first subrange or an empty string if no such text exists
	 */
	public String getFieldFirstSubrangeText() {
	    final String output;
	    if (this.field.firstSubrange(this.surroundingParagraph) != null && !this.field.firstSubrange(this.surroundingParagraph).getCharacterRun(0).isFldVanished()) {
		output = this.field.firstSubrange(this.surroundingParagraph).text();
	    }
	    else output = "";
	    return output;
	}

	/**
	 * Helper function to avoid NPEs
	 * 
	 * @return the text of the second subrange or an empty string if no such text exists
	 */
	public String getFieldSecondSubrangeText() {
	    final String output;
	    if (this.field.secondSubrange(this.surroundingParagraph) != null && !this.field.secondSubrange(this.surroundingParagraph).getCharacterRun(0).isFldVanished()) {
		output = this.field.secondSubrange(this.surroundingParagraph).text();
	    }
	    else output = "";
	    return output;
	}
	
	/**
	 * @return the raw characterRun containing the second subrange or {@code null} if no such subrange exists
	 */
	public Range getFieldSecondSubRangeRaw() {
	    final Range output;
	    if (this.field.secondSubrange(this.surroundingParagraph) != null && !this.field.secondSubrange(this.surroundingParagraph).getCharacterRun(0).isFldVanished()) {
		output = this.field.secondSubrange(this.surroundingParagraph);
	    }
	    else output = null;
	    return output;
	}

	/**
	 * Helper function to avoid NPEs
	 * 
	 * @return the offset (basically a pointer) into the picture store for embedded objects
	 */
	public Integer getFieldSecondSubrangePicOffset() {
	    return DataConverter.getPicOffset(this.field.secondSubrange(this.surroundingParagraph));	    
	}

	@Override
	public String toString() {
	    final StringBuilder debugOutput = new StringBuilder(36);
	    debugOutput.append("Type: ")
	    .append(this.field.getType())			
	    .append("\nSubrange 1: ")
	    .append(this.field.firstSubrange(this.surroundingParagraph).toString())
	    .append(": ")
	    .append(this.field.firstSubrange(this.surroundingParagraph).text())
	    .append("Subrange 2: ")
	    .append(this.field.secondSubrange(this.surroundingParagraph).toString())
	    .append(": ")
	    .append(this.field.secondSubrange(this.surroundingParagraph).text());
	    return debugOutput.toString();			
	}
    }
}
