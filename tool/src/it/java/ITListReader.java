import static org.junit.Assert.*;

import org.apache.poi.hwpf.usermodel.Paragraph;
import org.junit.Before;
import org.junit.Test;

import requirement.RequirementRoot;
import test.helper.ITGenericReader;
import docreader.list.ListReader;
import docreader.list.ListToRequirementProcessor;

/**
 * Test for the specific functionalities of {@link docreader.list.ListReader}
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class ITListReader extends ITGenericReader {

    /* the following file actually deals with requirements 3.5.3.6 - 3.6.3.8
     * However, Word already messes up the list numbering when copy/pasting these paragraphs into a new file.
     * Hence, they are referred to here as 3.1.1.1 - 3.1.1.4.
     */
    private static String filename = getResourcesDir() + "3_5_3_6__ff.doc"; 
    private ListReader listReader;

    /**
     * Generic setup routine
     */
    @Before
    public void setup() {
	setupTest("List Reader Test", filename);
	this.listReader = this.readerData.getListToRequirementProcessor().getListReader();
    }

    /**
     * Test method for {@link docreader.list.ListReader#getFullyQualified()}.
     */
    @Test
    public void testGetFullyQualified() {
	final String[] comparisonData = {
		"3.1.1.1",
		"3.1.1.1.a",
		"3.1.1.1.b",
		"3.1.1.1.c",
		"3.1.1.2",
		"3.1.1.2.a",
		"3.1.1.2.a[2]",
		"3.1.1.2.a[2].*[1]",
		"3.1.1.2.a[2].*[2]",
		"3.1.1.2.a[2].*[3]",
		"3.1.1.2.a[2].*[4]",
		"3.1.1.2.a[2].*[5]",
		"3.1.1.2.a[2].*[6]",
		"3.1.1.2.a[2].*[7]",
		"3.1.1.2.a[2].*[8]",
		"3.1.1.2.a[3]",
		"3.1.1.2.b",
		"3.1.1.2.c",
		"3.1.1.3",
		"3.1.1.3.a",
		"3.1.1.3.b",
		"3.1.1.4"
	};


	for(int i=0; i<this.readerData.getRange().numParagraphs(); i++) {			
	    Paragraph paragraph = this.readerData.getRange().getParagraph(i);	    

	    this.listReader.processParagraph(paragraph);
	    assertEquals("List identifiers do not match at iteration " + i, comparisonData[i], this.listReader.getFullyQualified());
	}
    }

    /**
     * Test method for {@link docreader.list.ListReader#getAsPrinted()}.
     */
    @Test
    public void testGetAsPrinted() {
	final String[] comparisonData = {
		"3.1.1.1",
		"a)",
		"b)",
		"c)",
		"3.1.1.2",
		"a)",
		"",
		"\u00b7", // a bullet point
		"\u00b7",
		"\u00b7",
		"\u00b7",
		"\u00b7",
		"\u00b7",
		"\u00b7",
		"\u00b7",
		"",
		"b)",
		"c)",
		"3.1.1.3",
		"a)",
		"b)",
		"3.1.1.4"
	};

	for(int i=0; i<this.readerData.getRange().numParagraphs(); i++) {			
	    final Paragraph paragraph = this.readerData.getRange().getParagraph(i);        		        	

	    this.listReader.processParagraph(paragraph);
	    assertEquals("List identifiers do not match", comparisonData[i], this.listReader.getAsPrinted());        	
	}
    }

    /**
     * Test method for {@link docreader.list.ListReader#getParent(RequirementRoot)}.
     */
    @Test
    public void testListRequirementStructure() {	
	final ListToRequirementProcessor listToRequirementProcessor = new ListToRequirementProcessor(this.readerData);
	// we do not really need the class-global listReader here since ListToRequirementProcessor has its own

	for(int currentRangeNum=0; currentRangeNum<this.readerData.getRange().numParagraphs(); currentRangeNum++) {
	    currentRangeNum = listToRequirementProcessor.processParagraph(currentRangeNum);	    
	}

	final String tree = getTree(listToRequirementProcessor.getRootRequirement(), 0);
	final String expectedResult[] = {
		"",
		" 3",
		"  3.1",
		"   3.1.1",
		"    3.1.1.1",		
		"     3.1.1.1.a",
		"     3.1.1.1.b",
		"     3.1.1.1.c",
		"    3.1.1.2",
		"     3.1.1.2.a",
		"     3.1.1.2.a[2]",
		"      3.1.1.2.a[2].*[1]",
		"      3.1.1.2.a[2].*[2]",
		"      3.1.1.2.a[2].*[3]",
		"      3.1.1.2.a[2].*[4]",
		"      3.1.1.2.a[2].*[5]",
		"      3.1.1.2.a[2].*[6]",
		"      3.1.1.2.a[2].*[7]",
		"      3.1.1.2.a[2].*[8]",
		"     3.1.1.2.a[3]",
		"     3.1.1.2.b",
		"     3.1.1.2.c",
		"    3.1.1.3",
		"     3.1.1.3.a",        		
		"     3.1.1.3.b",
		"    3.1.1.4"
	};
	
	assertArrayEquals(expectedResult, tree.split("\n"));
    }
}
