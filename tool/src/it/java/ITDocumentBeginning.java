import static org.junit.Assert.*;
import helper.TraceabilityManagerHumanReadable;

import org.apache.poi.hwpf.usermodel.Paragraph;
import org.junit.Before;
import org.junit.Test;

import docreader.list.ListReader;
import docreader.range.RequirementReader;
import docreader.range.SkipReader;
import docreader.range.TitleReader;
import requirement.RequirementOrdinary;
import requirement.RequirementRoot;
import test.helper.ITGenericReader;

/**
 * Tests the beginning of a requirements document. Specifically:
 * <ol>
 * <li> the ability to read out the title</li>
 * <li> skip the TOC</li>
 * <li> find the first list paragraph</li>
 * </ol>
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class ITDocumentBeginning extends ITGenericReader {

    private static String filename = getResourcesDir() + "3_beginning.doc"; 
    private ListReader listReader;

    /**
     * Generic setup routine
     */
    @Before
    public void setUp() {
	setupTest("List Reader Test Beginning Chapter 3", filename);
	this.listReader = this.readerData.getListToRequirementProcessor().getListReader();
    }

    /**
     * Test method for
     * <ul>
     * <li>{@link docreader.range.TitleReader}</li>
     * <li>{@link docreader.range.SkipReader}</li>
     * <li>{@link docreader.list.ListReader}</li>
     * </ul>
     */
    @Test
    public void test() {
	final String[] expectedDocumentTitle = {
		"System Requirements Specification Chapter 3 Principles",
		"<p style=\"display:block; text-align:center;\"><b><span style=\"font-family:Arial; font-size:18pt;\">System Requirements Specification<br/>Chapter 3<br/>Principles</span></b></p>"
	};

	RequirementRoot lastRequirement = new RequirementRoot();
	final RequirementRoot root = lastRequirement;

	// Step 1: Handle the document title		
	final TitleReader titleReader = new TitleReader(this.readerData, this.listReader, lastRequirement, 0);
	final int startOffset = titleReader.read();
	lastRequirement = titleReader.getTitleRequirement();
	assertEquals(expectedDocumentTitle[0], titleReader.getTitleRequirement().getText().getRaw());
	assertEquals(expectedDocumentTitle[1], titleReader.getTitleRequirement().getText().getRich());

	// Step 2: Handle the main document part
	for(int currentRangeNum=startOffset; currentRangeNum<this.readerData.getRange().numParagraphs(); currentRangeNum++) {
	    // each iteration corresponds to a single requirement
	    final Paragraph paragraph = this.readerData.getRange().getParagraph(currentRangeNum);    		        		       

	    final int paragraphsToSkip = new SkipReader(this.readerData, currentRangeNum).read();
	    if (paragraphsToSkip > 0) {
		currentRangeNum += paragraphsToSkip -1; // -1 because we will again do +1 in the next iteration of the for loop
	    }
	    else {
		// do the listhandling here
		this.listReader.processParagraph(paragraph);
		final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();
		hrManager.addList(this.listReader.getFullyQualified());
		final RequirementOrdinary currentRequirement = new RequirementOrdinary(this.readerData, paragraph, hrManager, this.listReader.getParent(lastRequirement), root);
		lastRequirement = currentRequirement;	   

		currentRangeNum += new RequirementReader(this.readerData, currentRequirement, currentRangeNum).read();
	    }
	}

	final String tree = getTree(root, 0);
	final String[] actualTree = tree.split("\n");

	// check for the tree structure
	final String[] expectedTree = {
		"",
		" 3",
		"  3.1",
		"  3.1[2]",
		"   3.1[2].[t]*",
		"  3.2",
		"  3.3",
		"   3.3.1",
		"    3.3.1.1",
	};
	assertArrayEquals(expectedTree, actualTree);

    }

}
