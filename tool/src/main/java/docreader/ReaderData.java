package docreader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import helper.DeferredLoggingHandler;
import helper.word.PictureStore;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.FileInformationBlock;
import org.apache.poi.hwpf.usermodel.Bookmark;
import org.apache.poi.hwpf.usermodel.Fields;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Section;

import docreader.list.ListToRequirementProcessor;
import docreader.range.paragraph.characterRun.OfficeDrawingReader;
import requirement.TraceabilityLinker;
import requirement.metadata.KnownPhrasesLinker;
import static helper.Constants.Generic.MEDIA_STORE_DIR;

/**
 * Container for data (context) specific to each MS Word input file
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class ReaderData {
    private final String documentPrefix;
    private final String documentTitle;
    private final String mediaStoreDirRelative;
    private final HWPFDocument document;
    private final String outputDir;
    private final Range range;
    private final Fields fields;
    private final transient Map<String, Integer> bookmarks;
    private final PictureStore pictureStore;
    private final OfficeDrawingReader officeDrawingReader;
    private final TraceabilityLinker traceabilityLinker;
    private final KnownPhrasesLinker knownPhrasesLinker;
    private final ListToRequirementProcessor listToRequirementProcessor;
    private int footnoteRunningNumber = 1;
    private int endnoteRunningNumber = 1;
    private static final Logger logger = Logger.getLogger(ReaderData.class.getName()); // NOPMD - Reference rather than a static field    
    private static final DeferredLoggingHandler LOGGING_HANDLER = new DeferredLoggingHandler(new ByteArrayOutputStream());       
    
    static {
	// store away logging events for later retrieval
	logger.setUseParentHandlers(false);
	logger.addHandler(LOGGING_HANDLER);
    }
    
    /**
     * @param document Document which is being processed
     * @param documentTitle String which uniquely identifies this {@code document} and is used as a prepender for certain output
     * @param reqIFOutputFilename filename of the resulting reqIF
     * @throws IllegalArgumentException if one of the given arguments {@code null}
     * @throw IllegalStateException if there are problems with the image storage directory
     */
    public ReaderData(final HWPFDocument document, final String documentTitle,final String reqIFOutputFilename) {	
	if (document == null) throw new IllegalArgumentException("document cannot be null.");
	if (reqIFOutputFilename == null) throw new IllegalArgumentException("reqIFOutputFilename cannot be null.");

	this.document = document;		
	this.range = document.getRange();
	checkDocumentAssumptions();
	setupMainSection();
	this.listToRequirementProcessor = new ListToRequirementProcessor(this); // sets up the range
	
	this.fields = document.getFields();
	this.bookmarks = convertBookmarks(document.getBookmarks().getBookmarksStartedBetween(this.getRange().getStartOffset(), this.getRange().getEndOffset()));
	this.pictureStore = new PictureStore(document);
	this.officeDrawingReader = new OfficeDrawingReader(document);
	
	// determine the output directory for supplementary artifacts
	final Path outputPath = Paths.get(reqIFOutputFilename);	
	if (Files.isDirectory(outputPath)) throw new IllegalArgumentException("reqIFOutputFilename refers to a directory");
	if (Files.isRegularFile(outputPath)) throw new IllegalArgumentException("reqIFOutputFilename already exists. Please delete the file first.");
	this.outputDir = outputPath.toAbsolutePath().getParent().toString();
		
	if (documentTitle == null) throw new IllegalArgumentException("documentPrefix cannot be null.");
	@SuppressWarnings("hiding")
	final String documentPrefix = documentTitle.replace(" ", "_");
	this.documentPrefix = ("".equals(documentPrefix) || documentPrefix.endsWith("-")) ? documentPrefix : documentPrefix + "-";
	this.documentTitle = documentTitle;

	this.mediaStoreDirRelative = MEDIA_STORE_DIR;
	final String mediaStoreDirAbsolute = getAbsoluteFilePathPrefix() + File.separator + this.mediaStoreDirRelative;
	final File mediaStoreDirHandler = new File(mediaStoreDirAbsolute);
	if (mediaStoreDirHandler.exists()) throw new IllegalStateException("The mediaStoreDir already exists. Please delete it first. Path: " + mediaStoreDirAbsolute);
	else if (!mediaStoreDirHandler.mkdir()) throw new IllegalStateException("The mediaStoreDir cannot be created. Please check permissions. Path: " + mediaStoreDirAbsolute);

	this.traceabilityLinker = new TraceabilityLinker();
	this.knownPhrasesLinker = new KnownPhrasesLinker();
    }

    /**
     * @return tracestring prefix for the currently processed document; this is an arbitrary string which may be set by the user, never {@code null}
     */
    public String getDocumentPrefix() {
	return this.documentPrefix;
    }
    
    /**
     * @return title of this document; this is an arbitrary string which may be set by the user, never {@code null}
     */
    public String getDocumentTitle() {
	return this.documentTitle;
    }

    /**
     * @return the directory containing the ReqIF output file; used to prepend any relative filepaths where necessary; never {@code null}
     */    
    public String getAbsoluteFilePathPrefix() {
	return this.outputDir;
    }

    /**
     * @return subdirectory where embedded media shall be stored without a trailing separator-char; must be relative to {@link #getAbsoluteFilePathPrefix()}
     */
    public String getMediaStoreDirRelative() {
	return this.mediaStoreDirRelative;
    }

    /**
     * @return a handle to the currently processed document
     */
    public HWPFDocument getDocument() {
	return this.document;
    }

    /**
     * @return a handle to the currently processed range (either the document range or something more narrow if we are inside a nested structure)
     */
    public Range getRange() {
	return this.listToRequirementProcessor.getListReader().getRange();
    }

    /**
     * @return the current nesting level (1-based)
     */
    public int getTableNestingLevel() {
	return this.listToRequirementProcessor.getListReader().getTableNestingLevel();
    }
    
    /**
     * @return a handle to the fields of the currently processed document
     */
    public Fields getFields() {
	return this.fields;
    }

    /**
     * @param bookmarkName name of the bookmark to look up
     * @return the character startOffset of the target of a given bookmark; can be {@code null} if the argument does not refer to any known bookmark
     * @throws IllegalArgumentException if the parameter is {@code null}
     */
    public Integer getBookmarkTargetStartOffset(final String bookmarkName) {
	if (bookmarkName == null) throw new IllegalArgumentException("bookmarkName cannot be null.");
	return this.bookmarks.get(bookmarkName);
    }
    
    /**
     * @return a handle to the picture store (manages all the embedded media of the document)
     */
    public PictureStore getPictureStore() {
	return this.pictureStore;
    }
    
    /**
     * @return a handle to the office drawing reader (used to extract drawings from the document)
     */
    public OfficeDrawingReader getOfficeDrawingReader() {
	return this.officeDrawingReader;
    }

    /**
     * @return a handle to the traceabilityLinker which manages tracedata
     */
    public TraceabilityLinker getTraceabilityLinker() {
	return this.traceabilityLinker;
    }

    /**
     * @return a handle to the knownPhrasesLinker which manages links to phrases seen before
     */
    public KnownPhrasesLinker getKnownPhrasesLinker() {
	return this.knownPhrasesLinker;
    }
    
    /**
     * @return the next available footnote running number
     */
    public int getNextFootnoteRunningNumber() {
	return this.footnoteRunningNumber++;
    }
    
    
    /**
     * @return the next available endnote running number
     */
    public int getNextEndnoteRunningNumber() {
	return this.endnoteRunningNumber++;
    }
    
    /**
     * @return stateful handle to the global list manager; never {@code null}
     */
    public ListToRequirementProcessor getListToRequirementProcessor() {
	return this.listToRequirementProcessor;
    }

    /**
     * Flush any log messages which have been logged so far
     */
    @SuppressWarnings("static-method")
    public void flushLogMessages() {
	LOGGING_HANDLER.flushMessages();
    }
    
    
    /**
     * POI returns bookmarks sorted by startOffset. However, we need them to be accessible via their name. This method converts the view.
     */
    private static Map<String, Integer> convertBookmarks(final Map<Integer, List<Bookmark>> input) {
	final Map<String, Integer> outputMap = new HashMap<>();
	for (final List<Bookmark> currentList : input.values()) {
	    for (final Bookmark currentBookmark : currentList) {
		outputMap.put(currentBookmark.getName(), currentBookmark.getStart());
	    }
	}
	return outputMap;
    }
        
    private void checkDocumentAssumptions() {
	assert this.document != null;	
	final FileInformationBlock fib = this.document.getFileInformationBlock();
	if (fib.getFibBase().isFComplex()) {
	    logger.log(Level.INFO, "The last save operation of this document was an incremental save. Will try my best to handle it anyways.");
	}
	if (fib.getFibBase().isFDot()) {
	    logger.log(Level.INFO, "The specified file is a document template.");
	}	
	
	final Range textboxRange = this.document.getMainTextboxRange();
	// the checks in here are rather expensive
	// but a simple textboxRange.getStartOffset() != textboxRange.getEndOffset()-1 unfortunately does not work
	if (!"".equals(textboxRange.text().replaceAll("\\s", ""))) {
	    logger.log(Level.INFO, "This document contains textboxes. Will skip them.");	    
	}
	final Range commentsRange = this.document.getCommentsRange();
	if (!"".equals(commentsRange.text().replaceAll("\\s", ""))) {
	    logger.log(Level.INFO, "This document contains comments. Will skip them.");	    
	}	
    }
    
    /**
     * Initialize footnote- and endnote-counters
     */
    private void setupMainSection() {
	assert this.range != null;
	if (this.range.numSections() > 1) {
	    // see [MS-DOC], v20140721, 3.2., for an explanation what a section is
	    logger.log(Level.INFO, "This document has several sections. Did not expect that from a requirements document. Trying to process anyways.");
	}
	else {
	    final Section section = this.range.getSection(0);	    
	    
	    if (this.document.getFootnotes().getNotesCount() > 0) {
		if (section.getFootnoteNumberingOffset() != 0) this.footnoteRunningNumber += section.getFootnoteNumberingOffset() -1;
		if (section.getFootnoteNumberingFormat() != 0x00) {
		    logger.log(Level.INFO, "This document requests footnotes not to have arabic numbering. This is unsupported. Will fallback to arabic numbering.");		
		}
		if (section.getFootnoteRestartQualifier() == 0x02) {
		    logger.log(Level.INFO, "This document requests footnote numbering to restart on every page. This is unsupported. Assuming continuous numbering.");
		    // section.getEndnoteRestartQualifier() == 0x02 is illegal anyways - so we do not check for it
		}
	    }
	    if (this.document.getEndnotes().getNotesCount() > 0) {
		if (section.getEndnoteNumberingOffset() != 0) this.endnoteRunningNumber += section.getEndnoteNumberingOffset() -1;
		if (section.getEndnoteNumberingFormat() != 0x00 && this.document.getEndnotes().getNotesCount() > 0) {
		    logger.log(Level.INFO, "This document requests endnotes not to have arabic numbering. This is unsupported. Will fallback to arabic numbering.");
		}
	    }
	}
    }
}