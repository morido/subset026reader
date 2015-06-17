package docreader.range.paragraph.characterRun;

import helper.annotations.DomainSpecific;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherProperty;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.OfficeDrawing;
import org.apache.poi.hwpf.usermodel.OfficeDrawings;

import static helper.Constants.Internal.MSWord.PLACEHOLER_OFFICEDRAWING;

/**
 * Takes care about OfficeDrawings.<p>
 * An OfficeDrawing is a truly floating box which may contain images, OLE-data, predefined shapes and a combination thereof.
 * This implementation currently only takes care about
 * <ol>
 * <li>detecting office drawings and</li>
 * <li> extracting simple arrows out of such a drawing</li>
 * </ol>
 * </p>
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public final class OfficeDrawingReader {
    private final transient OfficeDrawings officeDrawings;
    private static final Logger logger = Logger.getLogger(OfficeDrawingReader.class.getName()); // NOPMD - Reference rather than a static field
    
    /**
     * Create a new OfficeDrawingReader
     * 
     * @param document Document for which to create the reader
     */
    public OfficeDrawingReader (final HWPFDocument document) {
	this.officeDrawings = document.getOfficeDrawingsMain();
    }
    
    /**
     * Check if the given characterRun contains at least one OfficeDrawing
     * 
     * @param characterRun characterRun to consider
     * @return {@code true} if the given characterRun contains an anchor of (= a pointer to) an OfficeDrawing; {@code false} otherwise
     */
    public static boolean hasOfficeDrawing(final CharacterRun characterRun) {
	// Note: this is intentionally non-static to reduce package dependencies
	if (characterRun == null) throw new IllegalArgumentException("characterRun cannot be null.");
	if (characterRun.isSpecialCharacter()) {
	    final String text = characterRun.text();
	    for (final char currentChar : text.toCharArray()) {
		if (isOfficeDrawingChar(currentChar)) return true;
	    }
	}
	return false;	
    }
    
    
    /**
     * Check if the given character is possibly a placeholder for an office drawing
     * 
     * @param input char to check
     * @return {@code true} if this char is a possible identifier of an office drawing; {@code false} otherwise
     */
    public static boolean isOfficeDrawingChar(final char input) {	
	// [MS-DOC], v20140721, 2.6.1, sprmCFSpec
	return input == PLACEHOLER_OFFICEDRAWING;
    }
    
    
    /**
     * Extracts standardized ArrowData from an OfficeDrawing at a given (global) character offset
     * 
     * @param characterOffset global character offset where to look for the office drawing char (this character must be {@code \u0008})
     * @return ArrowData representing the arrow or {@code null} of no suitable arrow was found
     * @throws IllegalStateException if there is something wrong with the input file
     */
    @DomainSpecific
    public ArrowData extractArrow(final int characterOffset) {
	ArrowData output = null;

	arrowDetection: {
	    final OfficeDrawing arrowCandidate = this.officeDrawings.getOfficeDrawingAt(characterOffset);
	    if (arrowCandidate == null) {
		throw new IllegalStateException("Could not extract any office drawing at the given character offset.");
	    }	
	    if (arrowCandidate.getPictureData() != null) {
		logger.log(Level.WARNING, "OfficeDrawing contains picture data. Cannot handle that. Will skip this drawing. Current offset: {0}", Integer.toString(characterOffset));
		break arrowDetection;	    
	    }	

	    final EscherRecordManager escherRecordManager = new EscherRecordManager(arrowCandidate.getOfficeArtSpContainer());
	    final EscherSpRecord escherSpRecord = escherRecordManager.getSpRecord();
	    final EscherOptRecord escherOptRecord = escherRecordManager.getOptRecord();

	    if (escherSpRecord == null || escherOptRecord == null) {
		logger.log(Level.WARNING, "Not enough data for this drawing available. Will skip.");
		break arrowDetection;
	    }

	    if (escherSpRecord.getInstance() != 0x14) { // [MS-ODRAW], v20140721, 2.4.24; msosptLine
		logger.log(Level.WARNING, "OfficeDrawing contains a shape which is not a simple line. Cannot handle that. Will skip this drawing. Current offset: {0}", Integer.toString(characterOffset));
		break arrowDetection;	    
	    }

	    final ArrowData.Direction direction = ArrowDirectionExtractor.getArrowDirection(escherSpRecord.getFlags(), escherOptRecord.getEscherProperties());
	    final ArrowDimensionsExtractor arrowDimensionsExtractor = new ArrowDimensionsExtractor(arrowCandidate);
	    final Integer left =  arrowDimensionsExtractor.getLeft();
	    final Integer right = arrowDimensionsExtractor.getRight();
	    final Integer top = arrowDimensionsExtractor.getTop();

	    if (left == null || right == null || top == null) {
		logger.log(Level.WARNING, "Detected an arrow but its dimensions are strange. Cannot handle that. Will skip this drawing. Current offset: {0}", Integer.toString(characterOffset));
		break arrowDetection;
	    }

	    output = new ArrowData(direction, left, right, top); // autoboxing is safe 
	}
	
	return output;
    }

    /**
     * Storage for arrow data; immutable
     */
    public final static class ArrowData {
	/**
	 * possible directions of an arrow	 
	 */
	public enum Direction {
	    /**
	     * arrowhead is at the right end of the arrow 
	     */
	    LeftToRight,
	    
	    /**
	     * arrowhead is at the left end of the arrow
	     */
	    RightToLeft }
	/**
	 * direction of the arrow
	 */
	private final Direction direction;
	/**
	 * left start value
	 */
	private final int left;	
	/**
	 * right start value 
	 */
	private final int right;
	
	/**
	 * top start value
	 */
	private final int top;
	
	/**
	 * Create a new arrow store
	 * 
	 * @param direction direction in which the arrow is pointing
	 * @param left left starting offset of the arrow in twips
	 * @param right right starting offset of the arrow in twips
	 * @param top top starting offset of the arrow in twips
	 */
	public ArrowData(final Direction direction, final int left, final int right, final int top) {
	    if ((right - left) <= 0) throw new IllegalArgumentException("Arrowlength is illegal.");
	    this.direction = direction;
	    this.left = left;
	    this.right = right;
	    this.top = top;
	}

	/**
	 * @return direction of this arrow as it appears in print
	 */
	public Direction getDirection() {
	    return this.direction;
	}

	/**
	 * @return left start value of this arrow in twips
	 */
	public int getLeft() {
	    return this.left;
	}

	/**
	 * @return right start value of this arrow in twips
	 */
	public int getRight() {
	    return this.right;
	}
	
	/**
	 * @return top start value of this arrow in twips
	 */
	public int getTop() {
	    return this.top;
	}
    }
    
    private final class ArrowDimensionsExtractor {
	private final transient OfficeDrawing officeDrawing;
	private final transient boolean dimensionsAreValid;
	
	public ArrowDimensionsExtractor(final OfficeDrawing officeDrawing) {
	    assert officeDrawing != null;	    
	    this.officeDrawing = officeDrawing;	    
	    this.dimensionsAreValid = checkArrowBoxPreconditions();
	}
	
	/**
	 * @return left value of this arrow or {@code null} if there is no valid left value
	 */
	public Integer getLeft() {	    
	    return this.dimensionsAreValid ? this.officeDrawing.getRectangleLeft() : null; // NOPMD - intentional null assignment
	}

	/**
	 * @return right value of this arrow or {@code null} if there is no valid right value
	 */
	public Integer getRight() {
	    return this.dimensionsAreValid ? this.officeDrawing.getRectangleRight() : null; // NOPMD - intentional null assignment
	}
	
	
	/**
	 * @return top value of this arrow or {@code null} if there is no value top value
	 */
	public Integer getTop() {
	    return this.dimensionsAreValid ? this.officeDrawing.getRectangleTop() : null; // NOPMD - intentional null assignment
	}
	

	/**
	 * Checks if the box in which an arrow is contained matches given criteria
	 * @return {@code true} if this box matches (and thus is likely to contain an arrow); {@code false} otherwise
	 */
	@DomainSpecific
	private boolean checkArrowBoxPreconditions() {
	    // so we are basically looking for a box (that surrounds the drawing) which is anchored in the text (i.e. the table cell) and has no height
	    // the width of that box will then be equal to the width of our arrow
	    // this only works for really simple arrows, though (e.g. multi-segment paths would not pass this test)

	    final OfficeDrawing.HorizontalPositioning expectedHPos = OfficeDrawing.HorizontalPositioning.ABSOLUTE;
	    final OfficeDrawing.HorizontalRelativeElement expectedHPosRelative = OfficeDrawing.HorizontalRelativeElement.TEXT;
	    final OfficeDrawing.VerticalPositioning expectedVPos = OfficeDrawing.VerticalPositioning.ABSOLUTE;
	    final OfficeDrawing.VerticalRelativeElement expectedVPosRelative = OfficeDrawing.VerticalRelativeElement.TEXT;
	    final int expectedHeight = 0;
	    final boolean hPosMatches = (expectedHPos == this.officeDrawing.getHorizontalPositioning());
	    final boolean hPosRelativeMatches = (expectedHPosRelative == this.officeDrawing.getHorizontalRelative());
	    final boolean vPosMatches = (expectedVPos == this.officeDrawing.getVerticalPositioning());
	    final boolean vPosRelativeMatches = (expectedVPosRelative == this.officeDrawing.getVerticalRelativeElement());
	    final boolean heightMatches = ((this.officeDrawing.getRectangleBottom() - this.officeDrawing.getRectangleTop()) == expectedHeight);

	    return (hPosMatches && hPosRelativeMatches && vPosMatches && vPosRelativeMatches && heightMatches);	
	}
    }
    
    /**
     * Utility class to extract the true direction of an arrow     
     */
    private final static class ArrowDirectionExtractor {
	
	private ArrowDirectionExtractor() { }	
	
	/**
	 * Determine arrow direction as it appears in print
	 * 
	 * @param spFlags flags from the shape properties
	 * @param optProperties list of primary option properties
	 * @return final arrow direction
	 */
	public static ArrowData.Direction getArrowDirection(final int spFlags, final List<EscherProperty> optProperties) {
	    assert optProperties != null;
	    final ArrowData.Direction output;
	    
	    final boolean isHFlipped = ((spFlags & 0x40) > 0);	    
	    final ArrowData.Direction directionUnflipped = getArrowDirectionUnflipped(optProperties);
	    
	    // check if we need to take any horizontal flipping into account
	    if (isHFlipped) {
		switch(directionUnflipped) {
		case LeftToRight:
		    output = ArrowData.Direction.RightToLeft;
		    break;
		case RightToLeft:
		    output = ArrowData.Direction.LeftToRight;
		    break;
		default: // cannot happen
		    output = null;
		    break;
		}			
	    }
	    else output = directionUnflipped;	   
	    
	    return output;
	}
	
	/**
	 * Determine the raw arrow direction (i.e. whether the user added the arrow as the "start" or "end" decoration of a line)
	 * irrespective of any rotation or flipping of the drawing
	 * 
	 * @param properties properties from an Escher opt record
	 * @return raw arrow direction
	 */
	private static ArrowData.Direction getArrowDirectionUnflipped(final List<EscherProperty> properties) {
	    assert properties != null;
	    ArrowData.Direction directionCandidate = null;	    
	    
	    endValueDeterminer: {
		final int maxAllowedArrows = 1;
		int arrowEndCounter = 0;		
		for (final EscherProperty currentProperty : properties) {
		    final int arrowEndValue;
		    switch (currentProperty.getPropertyNumber()) {
		    case 0x01D0:
			// lineStartArrowHead, [MS-ODRAW], v20140721, 2.3.8.20
			arrowEndValue = ((EscherSimpleProperty) currentProperty).getPropertyValue();
			if (isSignificantEndValue(arrowEndValue)) {
			    arrowEndCounter++;
			    directionCandidate = ArrowData.Direction.RightToLeft;
			}
			break;
		    case 0x01D1:
			// lineEndArrowHead, [MS-ODRAW], v20140721, 2.3.8.21
			arrowEndValue = ((EscherSimpleProperty) currentProperty).getPropertyValue();
			if (isSignificantEndValue(arrowEndValue)) {
			    arrowEndCounter++;
			    directionCandidate = ArrowData.Direction.LeftToRight;
			}			
			break;
		    default:
			// do not care about any other properties
			break;
		    }
		    if (arrowEndCounter > maxAllowedArrows) {
			directionCandidate = null; // NOPMD - intentional reset
			break endValueDeterminer;	    
		    }
		}
	    }
	    
	    return directionCandidate;	    
	}
	
	/**
	 * Checks if the given end value can be mapped to a real arrow (and is not a diamond, a dot or similar)
	 * 
	 * @param arrowEndValue identifier of an arrow end as obtained from MS Word
	 * @return {@code true} if the given parameter refers to some form of an arrow; {@code false} otherwise
	 */
	@DomainSpecific
	private static boolean isSignificantEndValue(final int arrowEndValue) {	    
	    final int[] significantArrowEndValues = { 0x00000001, 0x00000002, 0x00000005 }; // [MS-ODRAW], v20140721, 2.4.16
	    for (final int currentSignificantArrowEndValue : significantArrowEndValues) {
		if (currentSignificantArrowEndValue == arrowEndValue) {
		    return true;
		}
	    }
	    return false;
	}
    }
    
    
    /**
     * Manages the individual records of an Escher Container Record
     */
    private final static class EscherRecordManager {
	private final EscherSpRecord escherSpRecord;
	private final EscherOptRecord escherOptRecord;
	
	/**
	 * Create a new record manager and extract the relevant records from a container record
	 * 
	 * @param escherContainerRecord container record to use as the data source
	 */
	public EscherRecordManager(final EscherContainerRecord escherContainerRecord) {
	    @SuppressWarnings("hiding")
	    EscherSpRecord escherSpRecord = null;
	    @SuppressWarnings("hiding")
	    EscherOptRecord escherOptRecord = null;
	    
	    if (escherContainerRecord != null) {
		final Iterator<EscherRecord> childIterator = escherContainerRecord.getChildIterator();		
		{
		    int valueSetCounter = 0; // avoids unnecessary loop iterations
		    final int numValuesToSet = 2;
		    while (childIterator.hasNext() && valueSetCounter < numValuesToSet) {
			final EscherRecord currentRecord = childIterator.next();				
			if ("Sp".equals(currentRecord.getRecordName())) { // [MS-ODRAW], v20140721, 2.2.40
			    escherSpRecord = (EscherSpRecord) currentRecord;
			    valueSetCounter++;
			}
			else if ("Opt".equals(currentRecord.getRecordName())) { // [MS-ODRAW], v20140721, 2.2.9
			    escherOptRecord = (EscherOptRecord) currentRecord;
			    valueSetCounter++;
			}
		    }
		}		
	    }
	    
	    this.escherSpRecord = checkSpRecord(escherSpRecord);
	    this.escherOptRecord = checkOptRecord(escherOptRecord);
	}

	/**
	 * Checks if the properties of the shape record conform to certain expectations
	 * 
	 * @param escherSpRecord shape record to check
	 * @return the input if all expectations match or {@code null} if there were any anomalies
	 */
	@DomainSpecific
	private static EscherSpRecord checkSpRecord(final EscherSpRecord escherSpRecord) {
	    EscherSpRecord output = null;

	    checkProperties: {
		if (escherSpRecord != null) {

		    // [MS-ODRAW], v20140721, 2.2.40
		    final int flags = escherSpRecord.getFlags();		
		    final boolean isGroupShape = ((flags & 0x1) > 0);
		    final boolean isChildShape = ((flags & 0x2) > 0);
		    final boolean isPatriarchShape = ((flags & 0x4) > 0);
		    final boolean isDeleted = ((flags & 0x8) > 0);
		    final boolean isOLE = ((flags & 0x10) > 0);
		    final boolean hasMasterShape = ((flags & 0x20) > 0);
		    final boolean isConnectorShape = ((flags & 0x100) > 0);
		    final boolean hasAnchor = ((flags & 0x200) > 0);
		    final boolean isBackground = ((flags & 0x400) > 0);
		    final boolean hasShapeType = ((flags & 0x800) > 0);

		    final boolean[] propertiesMustBeUnset = {
			    isGroupShape, isChildShape, isPatriarchShape, isDeleted, isOLE, hasMasterShape, isConnectorShape, isBackground
		    };
		    final boolean[] propertiesMustBeSet = {
			    hasAnchor, hasShapeType
		    };

		    for (final boolean currentProperty : propertiesMustBeUnset) {
			if (currentProperty) break checkProperties; 
		    }
		    for (final boolean currentProperty : propertiesMustBeSet) {
			if (!currentProperty) break checkProperties;
		    }

		    // still alive, all good
		    output = escherSpRecord;
		}
	    }

	    return output;
	}	
	
	/**
	 * Checks if the primary options record contains illegal properties
	 * 
	 * @param escherOptRecord primary options to be checked
	 * @return the input if there are no illegal properties or {@code null} if there are illegal properties
	 */
	@DomainSpecific
	private static EscherOptRecord checkOptRecord(final EscherOptRecord escherOptRecord) {
	    EscherOptRecord output = null;

	    checkProperties: {
		if (escherOptRecord != null) {			
		    for (final EscherProperty currentProp : escherOptRecord.getEscherProperties()) {
			switch (currentProp.getPropertyNumber()) {
			case 0x0004:
			    // rotation, [MS-ODRAW], v20140721, 2.3.18.5
			    break checkProperties;
			case 0x0145:
			    // vertices, [MS-ODRAW], v20140721, 2.3.6.6
			    break checkProperties;
			case 0x0146:
			    // segment info, [MS-ODRAW], v20140721, 2.3.6.8
			    break checkProperties;
			case 0x01CB:
			    // line width, [MS-ODRAW], v20140721, 2.3.8.14
			    if (((EscherSimpleProperty) currentProp).getPropertyValue() > 0) break;
			    break checkProperties;
			default:
			    break; // do not care about other properties
			}
		    }
		    // still alive, all good
		    output = escherOptRecord;
		}
	    }

	    return output;
	}

	/**
	 * @return may be {@code null} if there is no SpRecord
	 */
	public EscherSpRecord getSpRecord() {
	    return this.escherSpRecord;
	}
	
	/**
	 * @return may be {@code null} if there is no OptRecord
	 */
	public EscherOptRecord getOptRecord() {
	    return this.escherOptRecord;
	}
    }
}