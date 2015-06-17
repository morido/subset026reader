

import static org.junit.Assert.*;
import helper.TraceabilityManagerHumanReadable;

import org.apache.poi.hwpf.usermodel.Paragraph;
import org.junit.Before;
import org.junit.Test;

import requirement.RequirementRoot;
import requirement.RequirementWParent;
import test.helper.ITGenericReader;
import docreader.list.ListReader;
import docreader.range.RequirementReader;

/**
 * Test with a contrived example if we can properly read out arbitrarily nested structures
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class ITNestedStructures extends ITGenericReader {

    private static String filename = getResourcesDir() + "nested_structures.doc";
    private ListReader listReader;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
	setupTest("Nested Test", filename);
	this.listReader = this.readerData.getListToRequirementProcessor().getListReader();
    }
    
    /**
     * Test for nested tables with a figure and two footnotes
     */
    @Test
    public void test() {
	int testCounter = 0;
	final String expectedTree = 
		"\n" +
		" 1\n" +
		" 1[2]\n" +
		"  1[2].[t]*\n" +
		"   1[2].[t]*.[r][1]\n" +
		"    1[2].[t]*.[r][1].[c][1]\n" +
		"    1[2].[t]*.[r][1].[c][2]\n" +
		"    1[2].[t]*.[r][1].[c][3]\n" +
		"     1[2].[t]*.[r][1].[c][3].[1]\n" +
		"     1[2].[t]*.[r][1].[c][3].[2]\n" +
		"      1[2].[t]*.[r][1].[c][3].[2].[t]*\n" +
		"       1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][1]\n" +
		"        1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][1].[c][1]\n" +
		"        1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][1].[c][2]\n" +
		"         1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][1].[c][2].[1]\n" +
		"         1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][1].[c][2].[2]\n" +
		"          1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][1].[c][2].[2].[t]*\n" +
		"           1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][1].[c][2].[2].[t]*.[r][1]\n" +
		"            1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][1].[c][2].[2].[t]*.[r][1].[c][1]\n" +
		"           1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][1].[c][2].[2].[t]*.[r][2]\n" +
		"            1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][1].[c][2].[2].[t]*.[r][2].[c][1]\n" +
		"       1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2]\n" +
		"        1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2].[c][1]\n" +
		"        1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2].[c][2]\n" +
		"         1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2].[c][2].[N]1\n" +
		"          1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2].[c][2].[N]1.[1]\n" +
		"          1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2].[c][2].[N]1.[2]\n" +
		"           1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2].[c][2].[N]1.[2].[t]*\n" +
		"            1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2].[c][2].[N]1.[2].[t]*.[r][1]\n" +
		"             1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2].[c][2].[N]1.[2].[t]*.[r][1].[c][1]\n" +
		"            1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2].[c][2].[N]1.[2].[t]*.[r][2]\n" +
		"             1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2].[c][2].[N]1.[2].[t]*.[r][2].[c][1]\n" +
		"              1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2].[c][2].[N]1.[2].[t]*.[r][2].[c][1].[1]\n" +
		"               1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2].[c][2].[N]1.[2].[t]*.[r][2].[c][1].[1].[f]27\n" +
		"                1[2].[t]*.[r][1].[c][3].[2].[t]*.[r][2].[c][2].[N]1.[2].[t]*.[r][2].[c][1].[1].[f]27.C\n" +
		"    1[2].[t]*.[r][1].[c][4]\n" +
		"    1[2].[t]*.[r][1].[c][5]\n" +
		"   1[2].[t]*.[r][2]\n" +
		"    1[2].[t]*.[r][2].[c][1]\n" +
		"    1[2].[t]*.[r][2].[c][2]\n" +
		"    1[2].[t]*.[r][2].[c][3]\n" +
		"    1[2].[t]*.[r][2].[c][4]\n" +
		"    1[2].[t]*.[r][2].[c][5]\n" +
		" 1[3]\n" +
		"  1[3].[N]2";

	
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
	    switch (i) {	    
	    case 25:
		final String actualTableContent = currentRequirement.getChildIterator().next().getText().getRich();
		final String expectedTableContent = "<table style=\"border-collapse:collapse; border-spacing:0;\"><tr><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px; vertical-align:top; width:89pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][1].[c][1]</div><div style=\"clear:right;\"></div></div></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px; vertical-align:top; width:90pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][1].[c][2]</div><div style=\"clear:right;\"></div></div><span style=\"font-family:Times New Roman; font-size:12pt;\">Some</span></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px; vertical-align:top; width:106pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][1].[c][3]</div><div style=\"clear:right;\"></div></div><span style=\"font-family:Times New Roman; font-size:12pt;\">Text</span><br /><span style=\"background-color:#C0C0C0; color:black; font-family:monospace; font-size:smaller; padding:0.1em;\">NESTED STRUCTURE - SEE CHILDREN</span><br /></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px; vertical-align:top; width:89pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][1].[c][4]</div><div style=\"clear:right;\"></div></div></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px; vertical-align:top; width:89pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][1].[c][5]</div><div style=\"clear:right;\"></div></div></td></tr><tr><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px; vertical-align:top; width:89pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][2].[c][1]</div><div style=\"clear:right;\"></div></div></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px; vertical-align:top; width:90pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][2].[c][2]</div><div style=\"clear:right;\"></div></div></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px; vertical-align:top; width:106pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][2].[c][3]</div><div style=\"clear:right;\"></div></div></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px; vertical-align:top; width:89pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][2].[c][4]</div><div style=\"clear:right;\"></div></div></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px; vertical-align:top; width:89pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][2].[c][5]</div><div style=\"clear:right;\"></div></div></td></tr></table>";
		assertEquals(expectedTableContent, actualTableContent);
		
		testCounter++;
		break;
	    default:
		break;
	    }	    
	}
	assertEquals(1, testCounter);			
	
	assertEquals(expectedTree, getTree(requirementRoot, 0));
    }

}
