package helper.word;

import static helper.Constants.Generic.IMAGE_CONVERSION_TOOL_PATTERN;
import static helper.Constants.Generic.IMAGE_REMOVAL_TOOL_PATTERN;
import static helper.Constants.Generic.SHAPE_CONVERSION_TOOL_PATTERN;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Picture;

/**
 * Stores pictures of a word document
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class PictureStore {	
    private final Map<Integer, Picture> pictures = new HashMap<>();    
    private final Collection<PictureToConvert> picturesToConvert = new HashSet<>();
    private final Collection<ShapeToConvert> shapesToConvert = new HashSet<>();
    private static final Logger logger = Logger.getLogger(PictureStore.class.getName()); // NOPMD - Reference rather than a static field

    private final class PictureToConvert {
	private final String inputFilename;
	private final String outputFilename;
	private final int outputWidth;
	private final int outputHeight;

	private PictureToConvert(final String inputFilename, final String outputFilename, final int outputWidth, final int outputHeight) {
	    assert inputFilename != null && outputFilename != null;
	    this.inputFilename = inputFilename;
	    this.outputFilename = outputFilename;
	    this.outputWidth = outputWidth;
	    this.outputHeight = outputHeight;
	}	
    }
    
    private final class ShapeToConvert {
	private final int rangeStartOffset;
	private final String outputFilename;
	
	private ShapeToConvert(final int rangeStartOffset, final String outputFilename) {
	    assert outputFilename != null;
	    this.rangeStartOffset = rangeStartOffset;
	    this.outputFilename = outputFilename;
	}
    }

    /**
     * A PrintWriter which always terminates lines with {@code CRLF}     
     */
    private class WindowsPrintWriter extends PrintWriter {
	public WindowsPrintWriter(final String fileName) throws FileNotFoundException {
	    super(fileName);	    
	}

	@Override
	public void println() {
	    // enforce Windows line endings even if we are on Unix
	    // non thread-safe version
	    write(new char[]{'\r', '\n'});
	}
    }

    /**
     * @param document Document for which to generate a picture store
     * @throws IllegalArgumentException if the given document is {@code null}
     */
    public PictureStore(final HWPFDocument document) {
	if (document == null) throw new IllegalArgumentException("document cannot be null.");
	assert document.getPicturesTable() != null && document.getPicturesTable().getAllPictures() != null;
	for (final Picture currentPicture : document.getPicturesTable().getAllPictures()) {
	    this.pictures.put(currentPicture.getStartOffset(), currentPicture);	
	}
    }

    /**
     * Get a picture at a certain offset and remove it from the store (so this method may only be called once for each picture)
     * 
     * @param pictureOffset an integer obtained from {@link CharacterRun#getPicOffset()}
     * @return The picture linked to the given CharacterRun or {@code null} if no such picture exists
     */
    public Picture getPicture(final int pictureOffset) {	
	return (pictureOffset != -1) ? this.pictures.remove(pictureOffset) : null; // NOPMD - intentional null assignment	
    }

    /**
     * Store mappings for files which could not be converted (e.g. EMF and friends)
     * 
     * @param currentFilename Filename as it is currently stored on disk
     * @param expectedFilename Filename as it is referred to in the reqif output
     * @param expectedWidth resulting width of the file as expected in the reqif output (relevant for vector data)
     * @param expectedHeight resulting height of the file as expected in the reqif output (relevant for vector data)
     * @throws IllegalArgumentException one of the parameters is {@code null}
     */
    public void putPictureToConvert(final String currentFilename, final String expectedFilename, final int expectedWidth, final int expectedHeight) {
	if (currentFilename == null) throw new IllegalArgumentException("currentFilename cannot be null.");
	if (expectedFilename == null) throw new IllegalArgumentException("expectedFilename cannot be null.");
	this.picturesToConvert.add(new PictureToConvert(currentFilename, expectedFilename, expectedWidth, expectedHeight));
    }

    /**
     * Store mappings for shapes which can not be extracted
     * 
     * @param rangeStartOffset start offset of the range which contains the shape
     * @param expectedFilename Filename as it is referred to in the reqif output
     * @throws IllegalArgumentException one of the parameters is {@code null}
     */
    public void putShapeToConvert(final int rangeStartOffset, final String expectedFilename) {
	if (expectedFilename == null) throw new IllegalArgumentException("expectedFilename cannot be null.");
	this.shapesToConvert.add(new ShapeToConvert(rangeStartOffset, expectedFilename));
    }

    /**
     * Write all pictures which were not referenced in the main document into a file
     * 
     * @param outputDir directory where the files shall be written
     * @return the number of files written
     * @throws IllegalArgumentException if the argument is {@code null} or malformed
     */
    public int writeUnwrittenPictures(final String outputDir) {
	if (outputDir == null) throw new IllegalArgumentException("outputDir cannot be null.");
	if (outputDir.length() == 0 || outputDir.charAt(outputDir.length()-1) != File.separatorChar) throw new IllegalArgumentException("outputDir must contain trailing separator.");

	final String prepender = "UNREFERENCED-";
	int i = 0;
	final Iterator<Picture> iterator = this.pictures.values().iterator();
	while (iterator.hasNext()) {	    
	    final Picture currentPicture = iterator.next();
	    if (currentPicture.getSize() == 0) continue; // do not process empty pictures

	    // same as docreader.range.paragraph.characterRun.ImageReader.writeToFile()
	    final String destination = outputDir + prepender + Integer.toString(++i) + '.' + currentPicture.suggestFileExtension();
	    try (final FileOutputStream fos = new FileOutputStream(destination)) {
		currentPicture.writeImageContent(fos);		
	    } catch (FileNotFoundException e) {
		logger.log(Level.WARNING, "Could not write data to disk because of issues in the underlying file-system", e);			
	    } catch (IOException e) {
		logger.log(Level.WARNING, "Could not write data to disk because of issues inside POI.", e);
	    }
	}
	return i;
    }

    /**
     * Create a CRLF-terminated file (batch-script, csv, ...) which can be used to convert all the image data
     * 
     * @param outputFilename filename where the file shall be written
     * @return the number of images to be converted; if {@code == 0} then no batch file will be written
     * @throws IllegalStateException if the output script could not be written
     */
    public int writeImageConversionFile(final String outputFilename) {
	if (outputFilename == null) throw new IllegalArgumentException("outputFilename cannot be null.");
	if (new File(outputFilename).isFile()) throw new IllegalArgumentException(outputFilename + " already exists.");
	final boolean batchScriptMode = (outputFilename.toLowerCase(Locale.ENGLISH).endsWith(".bat"));

	int imagesToBeConverted = 0;
	if (this.picturesToConvert.isEmpty()) return imagesToBeConverted;

	try (final PrintWriter printWriter = new WindowsPrintWriter(outputFilename)) {

	    if (batchScriptMode) {
		printWriter.println("@echo off");
		printWriter.println("echo Starting media conversion. Please stand by.");
		printWriter.println();
	    }
	    {
		if (batchScriptMode) printWriter.println("REM Step1: Convert files");
		final Iterator<PictureToConvert> iterator = this.picturesToConvert.iterator();
		while (iterator.hasNext()) {
		    final PictureToConvert currentPicture = iterator.next();
		    final String conversionString = IMAGE_CONVERSION_TOOL_PATTERN
			    .replace("{1}", currentPicture.inputFilename)
			    .replace("{2}", currentPicture.outputFilename)
			    .replace("{3}", Integer.toString(currentPicture.outputWidth))
			    .replace("{4}", Integer.toString(currentPicture.outputHeight));
		    printWriter.println(conversionString);
		    imagesToBeConverted++;
		}
	    }
	    if (batchScriptMode && IMAGE_REMOVAL_TOOL_PATTERN != null) {
		printWriter.println();
		printWriter.println("REM Step 2: Delete all input files");
		final Iterator<PictureToConvert> iterator = this.picturesToConvert.iterator();
		while (iterator.hasNext()) {
		    final PictureToConvert currentPicture = iterator.next();
		    final String removalString = IMAGE_REMOVAL_TOOL_PATTERN
			    .replace("{1}", currentPicture.inputFilename);
		    printWriter.println(removalString);
		}
	    }
	    if (batchScriptMode) printWriter.println("echo DONE!");		

	} catch (FileNotFoundException e) {
	    throw new IllegalStateException("Cannot write image conversion file.");
	}

	return imagesToBeConverted;
    }
    
    /**
     * Create a CRLF-terminated file (csv) which can be used to convert all the image data
     * 
     * @param outputFilename filename where the file shall be written
     * @return the number of images to be converted; if {@code == 0} then no batch file will be written
     * @throws IllegalStateException if the output script could not be written
     */
    public int writeShapeConversionFile(final String outputFilename) {
	if (outputFilename == null) throw new IllegalArgumentException("outputFilename cannot be null.");
	if (new File(outputFilename).isFile()) throw new IllegalArgumentException(outputFilename + " already exists.");	

	int shapesToBeConverted = 0;
	if (this.shapesToConvert.isEmpty()) return shapesToBeConverted;

	try (final PrintWriter printWriter = new WindowsPrintWriter(outputFilename)) {

	    final Iterator<ShapeToConvert> iterator = this.shapesToConvert.iterator();
	    while (iterator.hasNext()) {
		final ShapeToConvert currentShape = iterator.next();
		final String conversionString = SHAPE_CONVERSION_TOOL_PATTERN
			.replace("{1}", Integer.toString(currentShape.rangeStartOffset))
			.replace("{2}", currentShape.outputFilename);			
		printWriter.println(conversionString);
		shapesToBeConverted++;
	    }

	} catch (FileNotFoundException e) {
	    throw new IllegalStateException("Cannot write shape conversion file.");
	}

	return shapesToBeConverted;
    }
}