import static org.junit.Assert.*;

import org.junit.Test;

import requirement.RequirementOrdinary;
import test.helper.ITGenericReader;
import docreader.list.ListToRequirementProcessor;
import docreader.range.RequirementReader;

/**
 * Test for the specific functionalities of {@link docreader.range.paragraph.characterRun.ImageReader}
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class ITImageReader extends ITGenericReader {

    /**
     * Generic test for various ImageReader capabilities
     */
    @Test
    public void test() {
	// Setup
	final String filename = getResourcesDir() + "3_13_9_2_3_formula.doc";
	setupTest("Formula Test", filename);
	final ListToRequirementProcessor listToRequirementProcessor = new ListToRequirementProcessor(this.readerData);

	final String[] comparisonHTML = {
		"<span style=\"font-family:Arial; font-size:11pt;\">For dv_ebi, the following formula shall be applied:</span>",
		"<p style=\"display:block; text-align:justify;\"><span style=\"font-family:Arial; font-size:11pt;\">when <object data=\"media/Formula_Test-3_1_1_1_1%5B2%5D_E.png\" type=\"image/png\" width=\"118\" height=\"32\">Picture missing. No alternative text available.</object>:</span></p>",
		"<p style=\"display:block; text-align:justify;\"><object data=\"media/Formula_Test-3_1_1_1_1%5B3%5D_E.png\" type=\"image/png\" width=\"424\" height=\"32\">Picture missing. No alternative text available.</object></p>",
		"<p style=\"display:block; text-align:justify;\"><span style=\"font-family:Arial; font-size:11pt;\">with <object data=\"media/Formula_Test-3_1_1_1_1%5B4%5D_E.png\" type=\"image/png\" width=\"215\" height=\"60\">Picture missing. No alternative text available.</object></span></p>",
		"<p style=\"display:block; text-align:justify;\"><span style=\"font-family:Arial; font-size:11pt;\">when <object data=\"media/Formula_Test-3_1_1_1_1%5B5%5D_E.png\" type=\"image/png\" width=\"118\" height=\"32\">Picture missing. No alternative text available.</object>:<object data=\"media/Formula_Test-3_1_1_1_1%5B5%5D_E%5B2%5D.png\" type=\"image/png\" width=\"127\" height=\"32\">Picture missing. No alternative text available.</object></span></p>",
		"<object data=\"media/Formula_Test-3_1_1_1_1%5B6%5D_%5Bf%5D44_I.png\" type=\"image/png\" width=\"592\" height=\"190\">Picture missing. No alternative text available.</object>"
	};

	//TODO
	//1[3] should rather be 1[2].[1] and 1[4] rather be 1[2].[2] -- or is this too confusing?
	final String[] expectedTree = { 
		"",
		" 3", // placeholder
		"  3.1", // placeholder
		"   3.1.1", // placeholder
		"    3.1.1.1", // placeholder
		"     3.1.1.1.1",
		"     3.1.1.1.1[2]",
		"     3.1.1.1.1[3]",
		"     3.1.1.1.1[4]",
		"     3.1.1.1.1[5]",
		"     3.1.1.1.1[6]", 
		"      3.1.1.1.1[6].[f]44",
		"       3.1.1.1.1[6].[f]44.C"
	};

	final String[] expectedFilenames = {
		"Formula_Test-3_1_1_1_1[2]_E.wmf",
		"Formula_Test-3_1_1_1_1[3]_E.wmf",
		"Formula_Test-3_1_1_1_1[4]_E.wmf",
		"Formula_Test-3_1_1_1_1[5]_E.wmf",
		"Formula_Test-3_1_1_1_1[5]_E[2].wmf",
		"Formula_Test-3_1_1_1_1[6]_[f]44_I.emf"
	};

	// actual test			

	int testComparisonIncrementer = 0;	
	for(int currentRangeNum=0; currentRangeNum<this.readerData.getRange().numParagraphs(); currentRangeNum++) {
	    currentRangeNum = listToRequirementProcessor.processParagraph(currentRangeNum);	    	   

	    final RequirementOrdinary currentRequirement = listToRequirementProcessor.getCurrentRequirement();
	    if (currentRequirement != null) {
		currentRangeNum += new RequirementReader(this.readerData, currentRequirement, currentRangeNum).read();

		if (currentRequirement.getText() != null) {					
		    assertEquals(comparisonHTML[testComparisonIncrementer], currentRequirement.getText().getRich());
		}
		else if (testComparisonIncrementer == comparisonHTML.length-1) {
		    // last iteration; the figure		    
		    assertEquals(comparisonHTML[testComparisonIncrementer], currentRequirement.getChildIterator().next().getText().getRich());					
		}
		testComparisonIncrementer++;
	    }
	}
	assertArrayEquals(expectedTree, getTree(listToRequirementProcessor.getRootRequirement(), 0).split("\n"));

	final String[] writtenFilenames = getWrittenFilenames();
	assertArrayEquals(expectedFilenames, writtenFilenames);	
    }

    /**
     * Test if we can correctly read out subsequent images which are not part of a figure
     */
    @Test
    public void testSubsequentImages() {
	// Setup
	final String filename = getResourcesDir() + "5_4_4_230d.doc";
	setupTest("Images Test", filename);
	final ListToRequirementProcessor listToRequirementProcessor = new ListToRequirementProcessor(this.readerData);
	
	final String[] expectedTree = { 
		"",
		" 5", // placeholder
		"  5.1", // placeholder
		"   5.1.1",
		"   5.1.1[2]",
		"   5.1.1[3]",
		"   5.1.2",
	};
	
	// Actual test
	for(int currentRangeNum=0; currentRangeNum<this.readerData.getRange().numParagraphs(); currentRangeNum++) {
	    currentRangeNum = listToRequirementProcessor.processParagraph(currentRangeNum);	    	   

	    final RequirementOrdinary currentRequirement = listToRequirementProcessor.getCurrentRequirement();
	    if (currentRequirement != null) {
		currentRangeNum += new RequirementReader(this.readerData, currentRequirement, currentRangeNum).read();
		
	    }
	}
	assertArrayEquals(expectedTree, getTree(listToRequirementProcessor.getRootRequirement(), 0).split("\n"));
	
    }

}
