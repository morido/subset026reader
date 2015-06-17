package docreader;

import helper.ConsoleOutputFilter;
import helper.annotations.DomainSpecific;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.poi.hwpf.HWPFDocument;

import docreader.GenericReader;
import docreader.ReaderData;
import docreader.list.ListToRequirementProcessor;
import docreader.range.RequirementReader;
import docreader.range.TitleReader;
import reqifwriter.DocumentWriter;
import reqifwriter.DocumentWriter.ReferenceableReqifField;
import reqifwriter.ReqifField;
import reqifwriter.ReqifFieldBoolean;
import reqifwriter.ReqifFieldEnum;
import reqifwriter.ReqifField.RequirementCall;
import requirement.RequirementOrdinary;
import requirement.RequirementRoot;
import requirement.RequirementWParent;
import requirement.metadata.Kind;
import requirement.metadata.LegalObligation;

/**
 * Reader for an entire Microsoft Word document
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class DocumentReader implements GenericReader<Integer> {
    private final String inputFilename;
    private final String outputFilename;
    private final String globalPrepender;
    private int totalParagraphNumber;
    private final transient PrintWriter status = new PrintWriter(System.err, true);

    /**
     * Ordinary constructor
     * 
     * @param globalPrepender prepender for derived files (image filenames, ...)
     * @param inputFilename file to process
     * @param outputFilename file where the output shall be written; existing files will be overridden without warning
     * @throws IllegalArgumentException if any of the arguments is {@code null}
     */
    public DocumentReader(final String globalPrepender, final String inputFilename, final String outputFilename) {
	if (globalPrepender == null) throw new IllegalArgumentException("GlobalPrepender cannot be null.");
	if (inputFilename == null) throw new IllegalArgumentException("InputFilename cannot be null.");
	if (outputFilename == null) throw new IllegalArgumentException("outputFilename cannot be null.");
	
	this.globalPrepender = globalPrepender;
	this.inputFilename = inputFilename;
	this.outputFilename = outputFilename;
    }

    /**
     * Read a document
     * 
     * @see docreader.GenericReader#read()
     * @return {@code 0} upon successful completion
     */
    @Override
    @DomainSpecific
    public Integer read() {
	// Setup
	final ConsoleOutputFilter consoleFilter = new ConsoleOutputFilter();
	consoleFilter.addCurrentThread();	
	
	ReaderData readerData;
	try (final FileInputStream fileInputStream = new FileInputStream(this.inputFilename)) {
	    final HWPFDocument document = new HWPFDocument(fileInputStream);
	    readerData = new ReaderData(document, this.globalPrepender, this.outputFilename);
	} 
	catch (IOException e) {
	    throw new IllegalArgumentException("File " + this.inputFilename + " does not exist or is not a valid MS Word 97 file.");
	}
	new DocumentSummaryReader(readerData, this.inputFilename, this.outputFilename).read();
	this.totalParagraphNumber = readerData.getRange().numParagraphs();	
	final ListToRequirementProcessor listToRequirementProcessor = readerData.getListToRequirementProcessor();
	// Setup end
	
	RequirementRoot lastRequirement = listToRequirementProcessor.getRootRequirement();
	final RequirementRoot root = lastRequirement;
	final SecondPassReader secondPassReader = new SecondPassReader(root, consoleFilter); // setup the second pass reader (give it some time to warm up NLP)

	// Step 1: Handle the document title
	final TitleReader titleReader = new TitleReader(readerData, listToRequirementProcessor.getListReader(), lastRequirement, 0);
	final int startOffset = titleReader.read();
	listToRequirementProcessor.setLastRequirement(titleReader.getTitleRequirement());

	// Step 2: Handle the main document part	
	for(int currentRangeNum=startOffset; currentRangeNum<this.totalParagraphNumber; currentRangeNum++) {
    	    currentRangeNum = listToRequirementProcessor.processParagraph(currentRangeNum);
	    final RequirementOrdinary currentRequirement = listToRequirementProcessor.getCurrentRequirement();
	    if (currentRequirement != null) {
		final int rangeNumForStatus = currentRangeNum;		
		currentRangeNum += new RequirementReader(readerData, currentRequirement, currentRangeNum).read();
		final String tag = currentRequirement.getHumanReadableManager() != null ? currentRequirement.getHumanReadableManager().getTag() : "";
		final String text = currentRequirement.getText() != null && currentRequirement.getText().getRaw() != null ? currentRequirement.getText().getRaw() : "";
		this.status.println(statusString(rangeNumForStatus, tag, text, this.totalParagraphNumber));
	    }
	    System.err.flush(); // make sure all error messages from this iteration end up in the output
	}

	// Step 3: do a second pass to detect certain properties which rely on a complete hierarchy	
	System.err.println("Performing second pass of generated document hierarchy.");
	secondPassReader.read();
	
	// Step 4: Serialize to XML	
	System.err.println("Starting XML serialization");
	final DocumentWriter documentWriter = new DocumentWriter();
	final ReferenceableReqifField fieldRID = documentWriter.addField(
		new ReqifField<>("requirementID", String.class, new RequirementCall<RequirementWParent>() {
		    @Override
		    public String call(final RequirementWParent requirement) {
			assert requirement != null && requirement.getHumanReadableManager() != null;
			return requirement.getHumanReadableManager().getTag();
		    }
		})
	);
	documentWriter.addField(
		new ReqifField<>("PlainText", String.class, new RequirementCall<RequirementWParent>() {
		    @Override
		    public String call(final RequirementWParent requirement) {
			assert requirement != null;
			return requirement.getText() != null && requirement.getText().getRaw() != null ? requirement.getText().getRaw() : "";			
		    }
		})
	);
	final ReferenceableReqifField fieldRichText = documentWriter.addField(
		new ReqifField<>("RichText", String.class, "XHTML", new RequirementCall<RequirementWParent>() {
		    @Override
		    public String call(final RequirementWParent requirement) {
			assert requirement != null;
			return requirement.getText() != null && requirement.getText().getRich() != null ? requirement.getText().getRich() : "";			
		    }
		})
	);
	documentWriter.addField(
		new ReqifField<>("ListNumberText", String.class , new RequirementCall<RequirementWParent>() {
		    @Override
		    public String call(final RequirementWParent requirement) {
			assert requirement != null;
			String numberText = requirement.getMetadata().getNumberText();
			if (numberText == null) numberText = ""; // TODO does this ever trigger?
			return numberText;
		    }
		})
	);
	documentWriter.addField(
		new ReqifField<>("WordTraceId", Integer.class, new RequirementCall<RequirementWParent>() {
		    @Override
		    public String call(final RequirementWParent requirement) {
			assert requirement != null;
			return Integer.toString(requirement.getAssociatedRange().getStartOffset());
		    }
		})
	);
	documentWriter.addField(
		new ReqifFieldEnum<>("kind", Kind.class, new RequirementCall<RequirementWParent>() {
		    @Override
		    public String call(final RequirementWParent requirement) {
			assert requirement != null;
			return requirement.getMetadata().getKind().name();
		    }
		}).setMultivalued(false)
	);
	documentWriter.addField(
		new ReqifFieldBoolean<>("implement", Boolean.class, new RequirementCall<RequirementWParent>() {
		    @Override
		    public String call(final RequirementWParent requirement) {
			assert requirement != null;
			return (requirement.getImplementationStatus()) ? "true" : "false";
		    }
		}).setDefaultValue(true).setEditable(true)
	);
	documentWriter.addField(
		new ReqifFieldBoolean<>("atomic", Boolean.class, new RequirementCall<RequirementWParent>() {		    
		    @Override
		    public String call(final RequirementWParent requirement) {
			assert requirement != null;
			return requirement.getMetadata().isAtomic() ? "true" : "false";	
		    }
		}).setDefaultValue(false).setEditable(true)
	);
	documentWriter.addField(
		new ReqifField<>("implementerEnhanced", String.class, "XHTML", new RequirementCall<RequirementWParent>() {		    
		    @Override
		    public String call(final RequirementWParent requirement) {
			assert requirement != null;
			return requirement.getMetadata().getTextAnnotator() != null ? requirement.getMetadata().getTextAnnotator().getAnnotatedText() : "";
		    }
		}).setEditable(true)
	);
	documentWriter.addField(
		new ReqifFieldEnum<>("Legal Obligation", LegalObligation.class, new RequirementCall<RequirementWParent>() {
		    @Override
		    public String call(final RequirementWParent requirement) {
			assert requirement != null;
			return requirement.getMetadata().getLegalObligation().name();
		    }
		}).setMultivalued(false).setEditable(true)
	);
	documentWriter.setSortingField(fieldRID);
	documentWriter.setColumnField(fieldRID, 370);
	documentWriter.setColumnField(fieldRichText, 820);
	
	documentWriter.serializeTree(readerData, root, this.outputFilename);
	
	
	System.err.println("DONE");	
	System.err.flush();
	System.out.println("Processed " + Integer.toString(readerData.getTraceabilityLinker().getNumberOfRequirements()) + " traceable artifacts.");
	
	// Step 4: Handle images and shapes
	final int numUnwrittenImages = readerData.getPictureStore().writeUnwrittenPictures(readerData.getAbsoluteFilePathPrefix() + File.separator + readerData.getMediaStoreDirRelative() + File.separator);
	final String imageListLocation = readerData.getAbsoluteFilePathPrefix() + File.separator + readerData.getMediaStoreDirRelative() + File.separator + "images.csv";
	final int numImages = readerData.getPictureStore().writeImageConversionFile(imageListLocation);
	final String shapeListLocation = readerData.getAbsoluteFilePathPrefix() + File.separator + readerData.getMediaStoreDirRelative() + File.separator + "shapes.csv";
	final int numShapes = readerData.getPictureStore().writeShapeConversionFile(shapeListLocation);

	if (numUnwrittenImages > 0 || numImages > 0 || numShapes > 0) { System.out.println(); System.out.println("Media summary:"); }
	if (numUnwrittenImages > 0) System.out.println(Integer.toString(numUnwrittenImages) + " unreferenced images.");	    
	if (numImages > 0) System.out.println(Integer.toString(numImages) + " images. Please process " + imageListLocation);	    
	if (numShapes > 0) System.out.println(Integer.toString(numShapes) + " shapes. Please process " + shapeListLocation);	    
	
	return 0;
    }
    
    private static String statusString(final int currentParagraphNum, final String numberText, final String paragraphText, final int totalParagraphNumber) {
	final StringBuilder output = new StringBuilder();
	final int indexOfLastParagraph = totalParagraphNumber -1;
	
	final int lineLength = 80;
	final String formatParNum = "%" + Integer.toString(Integer.toString(indexOfLastParagraph).length()) + 's' + '/' + indexOfLastParagraph;
	output.append(String.format(formatParNum, Integer.toString(currentParagraphNum)));
	output.append(String.format("%30s", numberText));
	output.append(':').append(' ');	
	final int textLength;
	if (lineLength-output.length() > 0) {
	    if (lineLength-output.length() < paragraphText.length()) {
		 textLength = lineLength-output.length();
	    }
	    else {
		textLength = paragraphText.length();
	    }
	}
	else {
	    textLength = 0;
	}
	output.append(paragraphText.substring(0, textLength));
	return output.toString();
    }
}
