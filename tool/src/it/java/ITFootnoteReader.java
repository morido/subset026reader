import static org.junit.Assert.*;

import java.util.Iterator;

import helper.TraceabilityManagerHumanReadable;

import org.apache.poi.hwpf.usermodel.Paragraph;
import org.junit.Before;
import org.junit.Test;

import requirement.RequirementRoot;
import requirement.RequirementTemporary;
import requirement.RequirementWParent;
import test.helper.ITGenericReader;
import docreader.list.ListReader;
import docreader.range.RequirementReader;
import docreader.range.paragraph.characterRun.FootnoteReader;

/**
 * Test with a contrived example if the footnote reader works 
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class ITFootnoteReader extends ITGenericReader {

    private static String filename = getResourcesDir() + "footnote_test.doc";
    private ListReader listReader;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
	setupTest("Footnote Test", filename);
	this.listReader = this.readerData.getListToRequirementProcessor().getListReader();
    }	

    /**
     * Test method for {@link FootnoteReader#read()}
     */
    @Test
    public void testFootnotes() {
	final String[] comparisonHTML = {
		"<span style=\"font-family:Times New Roman; font-size:12pt;\">This is a test requirement. Here comes a footnote</span><span style=\"font-family:Times New Roman; font-size:12pt; vertical-align:super;\"><sup class=\"note\" style=\"background-color:lime; font-family:courier; font-weight:bolder;\">[N]1</sup></span><span style=\"font-family:Times New Roman; font-size:12pt;\"> in the middle of a sentence. Here is another footnote.</span><span style=\"font-family:Times New Roman; font-size:12pt; vertical-align:super;\"><sup class=\"note\" style=\"background-color:lime; font-family:courier; font-weight:bolder;\">[N]2</sup></span>",
		"<span style=\"font-family:Times New Roman; font-size:12pt;\">This is another text requirement. With another footnote</span><span style=\"font-family:Times New Roman; font-size:12pt; vertical-align:super;\"><sup class=\"note\" style=\"background-color:lime; font-family:courier; font-weight:bolder;\">[N]3</sup></span><span style=\"font-family:Times New Roman; font-size:12pt;\">. Here are two adjacent footnotes</span><span style=\"font-family:Times New Roman; font-size:12pt; vertical-align:super;\"><sup class=\"note\" style=\"background-color:lime; font-family:courier; font-weight:bolder;\">[N]4</sup><sup class=\"note\" style=\"background-color:lime; font-family:courier; font-weight:bolder;\">[N]5</sup></span><span style=\"font-family:Times New Roman; font-size:12pt;\">.</span>",
		"<span style=\"font-family:Times New Roman; font-size:12pt;\">Here comes an endnote</span><span style=\"font-family:Times New Roman; font-size:12pt; vertical-align:super;\"><sup class=\"note\" style=\"background-color:lime; font-family:courier; font-weight:bolder;\">[n]1</sup></span>"
	};
	
	final String[] noteTexts = {
		"<span style=\"font-family:Times New Roman; font-size:10pt;\">Text for footnote 1</span>",
		"<span style=\"font-family:Times New Roman; font-size:10pt;\">Text for footnote 2</span>",
		"<span style=\"font-family:Times New Roman; font-size:10pt;\">Text for footnote 3</span>",
		"<span style=\"font-family:Times New Roman; font-size:10pt;\">Text for footnote 4</span>",
		"<b><span style=\"font-family:Times New Roman; font-size:10pt;\">Bold Text</span></b><span style=\"font-family:Times New Roman; font-size:10pt;\"> for footnote 5</span>",
		"<span style=\"font-family:Times New Roman; font-size:10pt;\">Endnote Text.</span>"
	};
	
	final String expectedTree = 
		"\n" +
			" 1\n" +
			"  1.[N]1\n" +
			"  1.[N]2\n"  + 
			" 2\n" +
			"  2.[N]3\n" +
			"  2.[N]4\n" +
			"  2.[N]5\n" +
			" 3\n" +
			"  3.[n]1";
		
	// actual test
	int testCounter = 0;
	final RequirementRoot requirementRoot = new RequirementRoot();
	RequirementWParent currentRequirement;	
	for (int i = 0; i<this.readerData.getRange().numParagraphs(); i++) {
	    final Paragraph paragraph = this.readerData.getRange().getParagraph(i);			

	    this.listReader.processParagraph(paragraph);
	    final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();
	    hrManager.addList(this.listReader.getFullyQualified());
	    currentRequirement = new RequirementWParent(this.readerData, paragraph, requirementRoot);
	    currentRequirement.setHumanReadableManager(hrManager);
	    final RequirementReader requirementReader = new  RequirementReader(this.readerData, currentRequirement, i);
	    i += requirementReader.read();
	    assertEquals(comparisonHTML[i], currentRequirement.getText().getRich());
	    switch (i) {
	    case 0:
		final Iterator<RequirementWParent> iterator = currentRequirement.getChildIterator();
		assertEquals(noteTexts[0], iterator.next().getText().getRich());
		assertEquals(noteTexts[1], iterator.next().getText().getRich());		
		testCounter++;
		break;
	    case 1:
		final Iterator<RequirementWParent> iterator2 = currentRequirement.getChildIterator();
		assertEquals(noteTexts[2], iterator2.next().getText().getRich());
		assertEquals(noteTexts[3], iterator2.next().getText().getRich());
		assertEquals(noteTexts[4], iterator2.next().getText().getRich());
		testCounter++;
		break;
	    case 2:
		final RequirementTemporary child = currentRequirement.getChildIterator().next();
		assertEquals(noteTexts[5], child.getText().getRich());
		testCounter++;
		break;
	    default:
		break;
	    }	    
	}
	assertEquals(3, testCounter);
		
	assertEquals(expectedTree, getTree(requirementRoot, 0));
    }
}
