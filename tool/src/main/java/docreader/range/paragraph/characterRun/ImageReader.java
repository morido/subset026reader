package docreader.range.paragraph.characterRun;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import helper.TraceabilityManagerHumanReadable;
import helper.XmlStringWriter;
import helper.word.DataConverter;

import org.apache.poi.hwpf.usermodel.Picture;

import requirement.RequirementWParent;
import docreader.ReaderData;

/**
 * Extract image and equation data from internal word store; determine correct filename and write to disk
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class ImageReader {

    private final transient ReaderData readerData;
    private final transient Picture picture;
    private enum PictureType {IMAGE, EQUATION}
    private final transient boolean isInlined;
    private final transient Integer shapeStartOffset;
    private final XmlStringWriter xmlwriter;

    private static final Logger logger = Logger.getLogger(ImageReader.class.getName()); // NOPMD - Reference rather than a static field


    /**
     * Constructor for shapes
     * 
     * @param readerData global readerData
     * @param xmlwriter output writer
     * @param pictureOffset offset of the character which contains the link to the picture
     * @param isInlined {@code true} if the image is inlined (i.e. is not a figure), {@code false} otherwise
     * @param shapeStartOffset startOffset of the parent range of the shape, may be {@code null}
     * @throws IllegalArgumentException if some of the parameters are {@code null}
     */
    public ImageReader(final ReaderData readerData, final XmlStringWriter xmlwriter, final int pictureOffset, final boolean isInlined, Integer shapeStartOffset) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	if (xmlwriter == null) throw new IllegalArgumentException("xmlwriter cannot be null.");

	this.readerData = readerData;		
	this.xmlwriter = xmlwriter;	
	this.isInlined = isInlined;
	this.shapeStartOffset = shapeStartOffset; // can be null

	// extract actual picture
	this.picture = readerData.getPictureStore().getPicture(pictureOffset);		
    }   

    /**
     * Constructor for ordinary images (no shapes)
     * 
     * @param readerData global readerData
     * @param xmlwriter output writer
     * @param pictureOffset offset of the character which contains the link to the picture
     * @param isInlined {@code true} if the image is inlined (i.e. is not a figure), {@code false} otherwise
     */
    public ImageReader(final ReaderData readerData, final XmlStringWriter xmlwriter, final int pictureOffset, final boolean isInlined) {
	this(readerData, xmlwriter, pictureOffset, isInlined, null);
    }

    /**
     * Writes image or shape data to disk
     * 
     * @param hrManagerParent hrManager on which to base the image name 
     * @throws IllegalStateException If no picture data is available
     * @throws IllegalArgumentException if hrManagerParent is {@code null}
     */
    public void writeImage(final TraceabilityManagerHumanReadable hrManagerParent) {	
	if (hrManagerParent == null) throw new IllegalArgumentException("hrManagerParent cannot be null.");

	final FilenameDeterminer filenameDeterminer = new FilenameDeterminer(hrManagerParent, PictureType.IMAGE);
	if (pictureAvailable()) writeObject(filenameDeterminer);
	else {
	    logger.log(Level.WARNING, "Expected an image here but did not find one. Please extract manually. Will write a placeholder.");
	    writeObjectNotFound(filenameDeterminer);
	}	
    }

    /**
     * Writes equation data to disk
     * 
     * @param hrManagerParent hrManager on which to base the name of the equation
     * @throws IllegalStateException if no picture data is available
     * @throws IllegalArgumentException if hrManagerParent is {@code null}
     */
    public void writeEquation(final TraceabilityManagerHumanReadable hrManagerParent) {	
	if (hrManagerParent == null) throw new IllegalArgumentException("hrManagerParent cannot be null.");		
	if (!this.isInlined) logger.log(Level.INFO, "We have an equation which is not inlined. I did not expect that. Will process anyways.");

	final FilenameDeterminer filenameDeterminer = new FilenameDeterminer(hrManagerParent, PictureType.EQUATION);
	if (pictureAvailable()) writeObject(filenameDeterminer);
	else {
	    logger.log(Level.WARNING, "Expected an equation here but did not find one. Please extract manually. Will write a placeholder.");
	    writeObjectNotFound(filenameDeterminer);
	}	
    }

    /**
     * @return {@code true} if the requested picture has been found in Word's internal picture store; {@code false} otherwise
     */
    private boolean pictureAvailable() {
	// Note: This does not mean there is actual picture content available. All we know is: We somehow need to process an image.
	return (this.picture != null);
    }

    /**
     * Abstracts the actual writing process which is the same for both images and equations
     * 
     * @param filenameDeterminer FilenameDeterminer which holds the resulting filenames
     */
    private void writeObject(final FilenameDeterminer filenameDeterminer) {
	assert filenameDeterminer != null;	

	final double scalingFactorX = this.picture.getHorizontalScalingFactor() / 1000.0;
	final double scalingFactorY = this.picture.getVerticalScalingFactor() / 1000.0;

	final String altText = this.picture.getDescription();
	final int width = DataConverter.twipsToPixels((int) (this.picture.getDxaGoal() * scalingFactorX));
	final int height = DataConverter.twipsToPixels((int) (this.picture.getDyaGoal() * scalingFactorY));        

	// POI unfortunately does not expose border data as defined by [MS-DOC], v20140721, 2.6.5
	// internal functions are in org.apache.poi.hwpf.model.types.PICFAbstractType
	// Since there are no picture borders in the subset26 we wont invest any time on this.

	writeToFile(filenameDeterminer, width, height);	

	this.xmlwriter.writeStartElement("object");
	this.xmlwriter.writeAttribute("data", filenameDeterminer.getFilenameDisplay());
	this.xmlwriter.writeAttribute("type", "image/png");
	this.xmlwriter.writeAttribute("width", Integer.toString(width));
	this.xmlwriter.writeAttribute("height", Integer.toString(height));
	this.xmlwriter.writeCharacters(altText != null ? altText : "Picture missing. No alternative text available.");
	this.xmlwriter.writeEndElement("object");
    }

    /**
     * Write a placeholder if the referenced entity could not be found
     * 
     * @param filenameDeterminer FilenameDeterminer which holds the resulting filenames
     */
    private void writeObjectNotFound(final FilenameDeterminer filenameDeterminer) {
	assert filenameDeterminer != null;	

	this.xmlwriter.writeStartElement("object");
	String filename = filenameDeterminer.getFilenameDisplay();
	if (filename.length() >= 1 && filename.charAt(filename.length()-1) == '.') filename = filename.substring(0, filename.length()-1); // remove the final dot to make it look prettier 
	this.xmlwriter.writeAttribute("data", filename);
	this.xmlwriter.writeAttribute("type", "image/png");
	this.xmlwriter.writeCharacters("Picture missing. No alternative text available.");
	this.xmlwriter.writeEndElement("object");
    }

    /**
     * Write data to a physical file (if possible) or save information about the file for later retrieval
     * 
     * @param filenameDeterminer handle to the filenameDeterminer attached to the current image
     * @param width width of the resulting image
     * @param height height of the resulting image
     */
    private void writeToFile(final FilenameDeterminer filenameDeterminer, final int width, final int height) {
	assert filenameDeterminer != null;
	final String destination = filenameDeterminer.getFilenameStore();

	if (this.picture.getSize() > 0) {
	    // we have picture data; write it to a file
	    try (final FileOutputStream fos = new FileOutputStream(ImageReader.this.readerData.getAbsoluteFilePathPrefix() + File.separator + destination)) {
		this.picture.writeImageContent(fos);	    
		filenameDeterminer.addToPictureConversionStore(width, height);
	    } catch (FileNotFoundException e) {
		logger.log(Level.WARNING, "Could not write data to disk because of issues in the underlying file-system", e);			
	    } catch (IOException e) {
		logger.log(Level.WARNING, "Could not write data to disk because of issues inside POI.", e);
	    }
	}
	else {	  
	    // no picture data available
	    if (this.shapeStartOffset != null) {
		// picture is a shape
		filenameDeterminer.addToShapeConversionStore(this.shapeStartOffset);
	    }
	    else {
		// picture is not a shape; fail safely
		String filename = destination;
		if (filename.length() >= 1 && filename.charAt(filename.length()-1) == '.') filename = filename.substring(0, filename.length()-1); // remove the final dot to make it look prettier
		logger.log(Level.WARNING, "Picture \"{0}\" seems to be empty. Will not write to disk. Please extract manually.", filename);
	    }	    
	}
    }

    /**
     * Determine fully-qualified filename and maintain list of unconvertible files (i.e. files which need postprocessing by some external helper tool)
     */
    private class FilenameDeterminer {	
	private final String fullFilenameStore;
	private final String baseNameStore;
	private final String baseNameDisplay;
	private final String fullFilenameDisplay;	
	private final TraceabilityManagerHumanReadable hrManagerParent;
	private final PictureType pictureType;
	private final boolean pictureNeedsConversion;
	private final String mediaStorePrefix = ImageReader.this.readerData.getMediaStoreDirRelative();

	/**
	 * @param hrManagerParent base for the resulting filename of the image data
	 * @param pictureType Qualifies if this is an image or an equation; effects the trace string	
	 */
	public FilenameDeterminer(final TraceabilityManagerHumanReadable hrManagerParent, final PictureType pictureType) {
	    assert hrManagerParent != null && pictureType != null;	   
	    this.hrManagerParent = hrManagerParent;
	    this.pictureType = pictureType;

	    // Step 1: Determine the filename for storage on disk
	    final StringBuilder baseFilenameStore = new StringBuilder();
	    final String suggestedExtension;
	    if (ImageReader.this.picture == null) {
		// we expected a picture but did not find one; run in alibi mode
		suggestedExtension = "png";
	    }
	    else {
		// ordinary mode
		suggestedExtension = ImageReader.this.picture.suggestFileExtension();
	    }	    
	    final String uniqueFilename = getUniqueFilename(suggestedExtension);
	    baseFilenameStore.append(uniqueFilename);
	    baseFilenameStore.append('.');
	    final StringBuilder baseFilenameDisplay = new StringBuilder(baseFilenameStore); // copy away

	    baseFilenameStore.append(suggestedExtension);
	    this.baseNameStore = baseFilenameStore.toString();
	    this.fullFilenameStore = this.mediaStorePrefix + File.separator + this.baseNameStore;

	    // Step 2: Determine filename which can be rendered by javafx's webview and conforms to the reqif specification
	    // [reqif], v1.1, 10.8.20, point 2
	    if ("png".equalsIgnoreCase(suggestedExtension)) {
		// Picture can be rendered by a browser. Wonderful.				
		baseFilenameDisplay.append(suggestedExtension);		
		this.pictureNeedsConversion = false;
	    }
	    else {
		// Picture cannot be rendered. Not quite so wonderful.
		// expect it to be convertible to a png by some external helper tool
		baseFilenameDisplay.append("png");		
		this.pictureNeedsConversion = true;
	    }
	    this.baseNameDisplay = baseFilenameDisplay.toString();
	    this.fullFilenameDisplay = this.mediaStorePrefix + '/' + this.baseNameDisplay; // separator is always forward-slash irrespectively of OS
	}
	
	/**
	 * Determine a unique filename for the resulting file.
	 * <p>One paragraph may possibly contain more than one inlined picture. So we need this function to make them distinguishable.</p>
	 * 
	 * @param suggestedExtension the filename extension as it is suggested by POI
	 * @return the filename without a trailing dot and extension
	 */
	private String getUniqueFilename(final String suggestedExtension) {			
	    assert suggestedExtension != null && !"".equals(suggestedExtension);

	    // Step 1: Determine unique filename	    
	    final String absolutePrefix = ImageReader.this.readerData.getAbsoluteFilePathPrefix() + File.separator + this.mediaStorePrefix + File.separator;

	    assert this.hrManagerParent.getLinkedRequirement() != null;
	    final RequirementWParent.InlineElementCounter inlineElementCounter = this.hrManagerParent.getLinkedRequirement().getInlineElementCounter();
	    final TraceabilityManagerHumanReadable hrManagerCurrent = new TraceabilityManagerHumanReadable();
	    switch (this.pictureType) {
	    case EQUATION:
		hrManagerCurrent.addEquation(inlineElementCounter.getNextEquationNumber()); break;
	    case IMAGE:
		hrManagerCurrent.addImage(inlineElementCounter.getNextImageNumber()); break;
	    default:
		throw new IllegalStateException();
	    }
	    final TraceabilityManagerHumanReadable hrManagerFinal = new TraceabilityManagerHumanReadable(this.hrManagerParent, hrManagerCurrent);
	    final String outputFilenameBase = ImageReader.this.readerData.getDocumentPrefix() + hrManagerFinal.getTagForFilename();
	    assert !new File(absolutePrefix + outputFilenameBase + "." + suggestedExtension).isFile();

	    // this is the basename of the resulting unique filename
	    assert outputFilenameBase != null;	    
	    return outputFilenameBase;
	}

	/**
	 * Adds this picture including its dimensions to the conversion store if necessary
	 * 
	 * @param width displayed width of the image
	 * @param height displayed height of the image
	 */
	public void addToPictureConversionStore(final int width, final int height) {
	    if (this.pictureNeedsConversion) {		
		ImageReader.this.readerData.getPictureStore().putPictureToConvert(this.baseNameStore, this.baseNameDisplay, width, height);
	    }
	}	

	/**
	 * Add this shape to the conversion store
	 * 
	 * @param rangeStartOffset start offset of the range containing the shape
	 */
	public void addToShapeConversionStore(final int rangeStartOffset) {	    
	    assert this.pictureNeedsConversion == true; // POI will always return true here
	    ImageReader.this.readerData.getPictureStore().putShapeToConvert(rangeStartOffset, this.baseNameDisplay);
	}

	/**
	 * @return filename which can be displayed by a browser (i.e. ProR / javafx); must be relative to the reqif file
	 */
	public String getFilenameDisplay() {
	    return this.fullFilenameDisplay;
	}

	/**
	 * @return filename for (preliminary) storage on disk
	 */
	public String getFilenameStore() {
	    return this.fullFilenameStore;
	}
    }
}