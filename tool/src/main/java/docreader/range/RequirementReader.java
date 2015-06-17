package docreader.range;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.TableCell;

import static helper.Constants.MSWord.DELIMITER_CAPTION;
import static helper.Constants.Internal.MSWord.PLACEHOLDER_IMAGE;
import helper.RegexHelper;
import helper.annotations.DomainSpecific;
import helper.word.FieldStore;
import helper.word.FieldStore.FieldIdentifier;
import helper.word.FieldStore.ShapeTuple;
import helper.word.DataConverter;
import docreader.ReaderData;
import docreader.range.paragraph.characterRun.FieldReader;
import docreader.range.paragraph.characterRun.OfficeDrawingReader;
import docreader.range.table.TableReader;
import requirement.RequirementTemporary;

/**
 * Reads an a single or "complex" (i.e. TableCell) paragraph and combines it into a single requirement
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 */
public class RequirementReader extends RangeReader implements RequirementReaderI {	
    private FieldStore<Integer> firstField = null;        
    private static final Logger logger = Logger.getLogger(RequirementReader.class.getName()); // NOPMD - Reference rather than a static field

    /**
     * Ordinary constructor
     * 
     * @param readerData global readerData
     * @param requirement Requirement where results will be written
     * @param initialParagraphIndex index of the paragraph within the overall document
     */
    public RequirementReader(final ReaderData readerData, final RequirementTemporary requirement, final Integer initialParagraphIndex) {
	super(readerData, requirement, initialParagraphIndex);	
    }

    /**
     * Constructor from within a table or other nested structure
     * 
     * @param readerData global readerData
     * @param requirement Requirement where results will be written
     */
    public RequirementReader(final ReaderData readerData, final RequirementTemporary requirement) {
	this(readerData, requirement, null);
    }
        
    /**
     * Reads
     * <ul>
     * <li>a single paragraph or</li>
     * <li>a single table cell (which may be composed of several paragraphs)</li>
     * </ul>
     * 
     * @return the number paragraphs to skip (that is: 0-based number of paragraphs read)
     */
    @Override
    public Integer read() {		
	// Case 1: Check for "floating" entities
	// Case 1.a: Is this a table?
	if (isTableCandidate()) {
	    assert this.initialParagraphIndex != null; // is guaranteed by isSingleParagraph()
	    return new TableReader(this.readerData, this.requirement, this.initialParagraphIndex).read();
	}
	// Case 1.b: Is this a figure?
	final FigureCandidateDeterminer.FigureCandidateData figureCandidateData = isFigureCandidate();
	if (figureCandidateData != null) {
	    assert this.initialParagraphIndex != null; // is guaranteed by isSingleParagraph()
	    final int paragraphsToSkip = new FigureReader(this.readerData, this.requirement, this.initialParagraphIndex, figureCandidateData.getPictureOffsets(), figureCandidateData.getCaptionParagraph()).read(); 
	    if (paragraphsToSkip >= 0) return paragraphsToSkip;
	    // else: fallthrough; this is not really a figure but rather an inline image
	}
	// Case 1.c Is this an officeDrawing?
	if (isOfficeDrawingCandidate()) {
	    logger.log(Level.WARNING, "Current paragraph contains an OfficeDrawing. Cannot formalize. Will skip the drawing but process the rest of the paragraph.");
	    // intentional fallthrough
	}
	
	// Case 2: No floating data. This is ordinary text.
	final RequirementReaderI ordinaryTextReader = new RequirementReaderTextual(this.readerData, this.requirement);
	final Integer output = ordinaryTextReader.read();
	this.firstField = ordinaryTextReader.getFirstField();
	return output;
    }        

    /* (non-Javadoc)
     * @see docreader.range.RequirementReaderI#getFirstField()
     */
    @Override
    public FieldStore<Integer> getFirstField() {
	return this.firstField;
    }	


    /**
     * Checks if this range is a candidate for a table 
     */
    private boolean isTableCandidate() {
	final boolean output;
	if (isSingleParagraph()) {
	    final Paragraph paragraph = (Paragraph) this.range;	    
	    output = DataConverter.isInTable(this.readerData, paragraph);
	}
	else output = false;
	
	return output;		
    }

    /**
     * Checks if this range is a candidate for a figure
     * @return offset of the special character that marks the picture or {@code null} if no picture was found
     */
    private FigureCandidateDeterminer.FigureCandidateData isFigureCandidate() {
	// this is slightly more complex than isTableCandidate() above since Word does not offer any standardized markers we could search for
	// Moreover, we have to deal with the lazy authors of the requirements who did not format the figures uniformly
	final FigureCandidateDeterminer.FigureCandidateData output;
	
	if (isSingleParagraph()) {
	    final Paragraph paragraph = (Paragraph) this.range; // isSingleParagraph() assures this casting is safe
	    final FigureCandidateDeterminer figureCandidateDeterminer = new FigureCandidateDeterminer(this.readerData, paragraph);
	    output = figureCandidateDeterminer.getFigureCandidateData();
	}
	else output = null;
	
	return output;
    }    
    
    /**
     * Checks if this range is a candidate for an office drawing
     * @return {@code true} if this paragraph contains an office drawing; {@code false} otherwise
     */
    private boolean isOfficeDrawingCandidate() {
	if (isSingleParagraph()) {
	    final Paragraph paragaph = (Paragraph) this.range;  // isSingleParagraph() assures this casting is safe
	    for (int i = 0; i < paragaph.numCharacterRuns(); i++) {
		final CharacterRun currentRun = paragaph.getCharacterRun(i);
		if (OfficeDrawingReader.hasOfficeDrawing(currentRun)) {
		    return true;
		}		
	    }
	}
	return false;
    }
    
    /**
     * @return {@code true} if the range is actually a single paragraph; {@code false} otherwise
     */
    private boolean isSingleParagraph() {
	if (this.range instanceof Paragraph && this.range.numParagraphs() == 1) {
	    if (this.initialParagraphIndex != null) return true;
	    // else: we probably have a caption; that's ok, return false
	}
	else if (this.range instanceof TableCell) { /* NOPMD - that's ok, but we will return false */ }
	else assert false : "We got a " + this.range.getClass().toString() + " as a range. We did not expect that.";
	return false;
    }

    /**
     * Simple immutable data store for a tuple indicating the positions if the image placeholder char and the associated caption     
     */
    private final static class FigureCandidateDeterminer {
	private final FigureCandidateData figureCandidateData;

	private final static class FigureCandidateData {
	    private final Map<Integer, Integer> pictureOffsets;
	    private final Paragraph captionParagraph;

	    /**
	     * Setup new FigureCandidateData
	     * 
	     * @param pictureOffsets see {@link #getPictureOffsets()}
	     * @param captionParagraph see {@link #getCaptionParagraph()}
	     */
	    public FigureCandidateData(final Map<Integer, Integer> pictureOffsets, final Paragraph captionParagraph) {
		assert pictureOffsets != null;
		// caption paragraph may be null
		this.pictureOffsets = pictureOffsets;		
		this.captionParagraph = captionParagraph;
	    }

	    /**
	     * @return a map whose item is
	     * <ul> 
	     * <li> if the value is {@code false}: an offset to the picture placeholder char ({@link #PLACEHOLDER_IMAGE}) as obtained from {@link org.apache.poi.hwpf.usermodel.CharacterRun#getPicOffset()}</lI>
	     * <li> if the value is {@code true}: the start offset the the range containing the shape
	     */
	    public Map<Integer, Integer> getPictureOffsets() {
		return this.pictureOffsets;
	    }

	    /**
	     * @return <dl>
	     * <dt>{@code null}</dt><dd> we have two paragraphs (first one contains image, second one the caption)</dd>
	     * <dt>{@code !null}</dt><dd> one paragraph for both image and caption; actual return value is an artificial paragraph containing nothing but the caption</dd>
	     * </dl>
	     */
	    public Paragraph getCaptionParagraph() {
		return this.captionParagraph;
	    }
	}

	@DomainSpecific
	public FigureCandidateDeterminer(final ReaderData readerData, final Paragraph paragraph) {
	    assert readerData != null;
	    assert paragraph != null;

	    final String text = paragraph.text();
	    if (!text.contains("\u0001")) {
		// nothing valuable in here
		this.figureCandidateData = null;
		return;
	    }
	    
	    // Step 1: Slice the paragraph into meaningful chunks
	    final List<Range> ranges = new ArrayList<>(1);
	    final int absoluteOffset = paragraph.getStartOffset();
	    int textIterator;
	    sliceLoop: for (textIterator = 0; textIterator < text.length(); textIterator++) {
		final char currentChar = text.charAt(textIterator);
		final int startOffset = textIterator;
		switch (currentChar) {
		case 0x13:
		    // Field begin
		    textIterator++;
		    while (textIterator < text.length() && text.charAt(textIterator) != 0x14) textIterator++; // Field separator mark
		    while (textIterator < text.length() && text.charAt(textIterator) != 0x15) textIterator++; // Field end mark
		    final Range fieldRange = new Range(absoluteOffset + startOffset, absoluteOffset + textIterator +1, readerData.getDocument());
		    ranges.add(fieldRange);
		    break;
		case 0x01:
		    // lonely image placeholder
		    final Range imageRange = new Range(absoluteOffset + startOffset, absoluteOffset + startOffset +1, readerData.getDocument());
		    ranges.add(imageRange);
		    break;		
		case '\u000B' :
		    // visual placeholder
		    break;
		case '\r' :
		    // final line break
		    break sliceLoop;
		default:
		    // other data
		    if (textIterator > 0) {
			// we are behind some image data and thus read one char too far
			// this may leave trailing \u000B's in (from above), but that does not matter
			textIterator--;
		    }
		    break sliceLoop;
		}
	    }
	    
	    // Step 2: Analyze the chunks
	    assert ranges.size() > 0;
	    final Map<Integer, Integer> pictureOffsets = new LinkedHashMap<>(1); // must be insertion ordered
	    for (final Range currentRange : ranges) {
		final FieldReader fieldReader = new FieldReader(readerData, currentRange.getCharacterRun(0), paragraph);
		final FieldStore<?> fieldStore = fieldReader.read();
		final Integer currentPictureOffset;
		final Integer shapeStartOffset;
		if (fieldStore != null) {
		    if (fieldStore.getIdentifier() == FieldIdentifier.IMAGE) {
			currentPictureOffset = (Integer) fieldStore.getData();
			shapeStartOffset = null;
		    }
		    else if (fieldStore.getIdentifier() == FieldIdentifier.SHAPE) {
			final ShapeTuple data = (ShapeTuple) fieldStore.getData();
			currentPictureOffset = data.getPictureOffset();
			shapeStartOffset = data.getEmbeddingCharacterRunStartOffset();
		    }
		    else {
			// got some other field
			this.figureCandidateData = null;
			return;
		    }
		}
		else {
		    assert currentRange.numCharacterRuns() == 1;
		    currentPictureOffset = DataConverter.getPicOffset(currentRange);
		    shapeStartOffset = null;
		    if (currentPictureOffset == null) throw new IllegalStateException("Logical error during chunking. This should not happen.");
		}
		pictureOffsets.put(currentPictureOffset, shapeStartOffset);
	    }
	   
	    // Step 3: See if chunks may be followed by a caption
	    if (textIterator == text.length()-1) {
		// paragraph only consists of images
		this.figureCandidateData = new FigureCandidateData(pictureOffsets, null);
	    }
	    else if (isImageAndCaptionParagraph(paragraph, textIterator)) {
		// paragraph consists of both image and associated caption
		final Range captionRange = new Range(absoluteOffset + textIterator, absoluteOffset + text.length(), readerData.getDocument());
		assert captionRange.numParagraphs() == 1;
		this.figureCandidateData = new FigureCandidateData(pictureOffsets, captionRange.getParagraph(0));
	    }
	    else {
		// not a figure
		this.figureCandidateData = null;
	    }
	}

	public FigureCandidateData getFigureCandidateData() {
	    return this.figureCandidateData;
	}

	/**
	 * Checks if this is a paragraphs which both contains an image and its caption.
	 * <p><em>Note:</em> This technically cannot happen to tables and their respective captions. See [MS-DOC], v20140721, 2.4.3</p>
	 * 
	 * @param paragraph Paragraph of interest
	 * @param startOffset character start offset where to start looking for the caption
	 * @return {@code true} if this is a combined Image-and-Caption paragraph (i.e. Image and Caption are a single paragraph instead of two consecutive ones); {@code false} otherwise
	 */
	@DomainSpecific
	private static boolean isImageAndCaptionParagraph(final Paragraph paragraph, final int startOffset) {
	    assert paragraph != null;
	    assert startOffset >= 0 && startOffset < paragraph.text().length();
	    final String captionText = paragraph.text().substring(startOffset);
	       
	    final String quotedDelimiter = RegexHelper.quoteRegex(DELIMITER_CAPTION);
	    return DataConverter.cleanupText(captionText.toString()).matches("(?U)^Figure\\s.+" + quotedDelimiter + "\\s.+$");	
	}
    }    
}
