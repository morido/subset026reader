import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import docreader.list.ListToRequirementProcessor;
import docreader.range.RequirementReader;
import requirement.RequirementOrdinary;
import test.helper.ITGenericReader;

/**
 * Test for the appendix of a document
 * 
 * <p>Challenges:
 * <ol>
 * <li>Create a new baselist (appendix is not a child of the ordinary requirements)</li>
 * <li>correctly handle fake list elements (appendix headings are technically not in a list)</li>
 * <li>Missing level in "3.2 Juridical Data"</li>
 * </ol></p>
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class ITDocumentAppendix extends ITGenericReader {

    
    /**
     * Test the appendix of chapter 3, Baseline 3
     */
    @Test
    public void test_330() {
	final String filename = getResourcesDir() + "3_appendix.doc";
	setupTest("List Reader Test New Baselist", filename);
	final ListToRequirementProcessor listToRequirementProcessor = new ListToRequirementProcessor(this.readerData);
	
	int individualRequirementTestCounter = 0;
	for(int currentRangeNum=0; currentRangeNum<this.readerData.getRange().numParagraphs(); currentRangeNum++) {
	    currentRangeNum = listToRequirementProcessor.processParagraph(currentRangeNum);	    
	    
	    // check for correct fake number attribution
	    switch(currentRangeNum) {
	    case 12:
		assertEquals("A.3", listToRequirementProcessor.getCurrentRequirement().getHumanReadableManager().getTag()); individualRequirementTestCounter++; break;
	    case 13:
		assertEquals("A.3.1", listToRequirementProcessor.getCurrentRequirement().getHumanReadableManager().getTag()); individualRequirementTestCounter++; break;
	    case 292:
		assertEquals("A.3.4.1.2", listToRequirementProcessor.getCurrentRequirement().getHumanReadableManager().getTag()); individualRequirementTestCounter++; break;
	    case 602:
		assertEquals("A.3.4.1.3.d[17]", listToRequirementProcessor.getCurrentRequirement().getHumanReadableManager().getTag()); individualRequirementTestCounter++; break;
	    default:
		break;
	    }
	    
	    final RequirementOrdinary currentRequirement = listToRequirementProcessor.getCurrentRequirement();
	    if (currentRequirement != null) {
		currentRangeNum += new RequirementReader(this.readerData, currentRequirement, currentRangeNum).read();
	    }
	}
	
	assertEquals(4, individualRequirementTestCounter);
	
	final String tree = getTree(listToRequirementProcessor.getRootRequirement(), 0);
	final String[] actualTree = tree.split("\n");
	assertEquals(586, actualTree.length);	
	// check for the beginning of the tree (new baselist case)
	final String[] expectedTreeBeginning = {
		"",		
		" 3",
		"  3.1",
		"  3.2",
		"   3.2.1",
		"    3.2.1.1",
		"    3.2.1.2",
		"     3.2.1.2.1",
		"    3.2.1.3",
		"    3.2.1.4",
		"    3.2.1.5",
		"    3.2.1.6",
		"    3.2.1.7",
		"    3.2.1.8",
		"    3.2.1.9",
		" A",
		"  A.3",
		"   A.3.1",
		"   A.3.1[2]",
		"    A.3.1[2].[t]*"
	};
	assertArrayEquals(expectedTreeBeginning, Arrays.copyOfRange(actualTree, 0, expectedTreeBeginning.length));
	
	// check for certain tree elements
	assertEquals("      A.3.1[2].[t]*.[r][31].Name", actualTree[139]);
	assertEquals("      A.3.2[2].[t]*.[r][36].Name", actualTree[282]);
	assertEquals("         A.3.4.1.3.d[2].[t]*.[r][1].TBR", actualTree[314]);
	assertEquals("         A.3.4.1.3.d[3].[t]*.[r][54].[c][4]", actualTree[571]);
	assertEquals("      A.3.4.1.3.d[17]", actualTree[actualTree.length-1]);	
    }
    
    
    /**
     * Test the appendix of chapter 3, Baseline 2
     */
    @Test
    public void test_230d() {
	final String filename = getResourcesDir() + "3_appendix_230.doc";
	setupTest("List Reader Test New Baselist", filename);
	final ListToRequirementProcessor listToRequirementProcessor = new ListToRequirementProcessor(this.readerData);
	
	int individualRequirementTestCounter = 0;
	for(int currentRangeNum=0; currentRangeNum<this.readerData.getRange().numParagraphs(); currentRangeNum++) {
	    currentRangeNum = listToRequirementProcessor.processParagraph(currentRangeNum);	    
	    
	    if (listToRequirementProcessor.getCurrentRequirement() != null)
		//System.out.println(currentRangeNum + " " + listToRequirementProcessor.getCurrentRequirement().getHumanReadableManager().getTag());
	    
	    // check for correct fake number attribution
	    switch(currentRangeNum) {
	    case 0:
		assertEquals("A.3", listToRequirementProcessor.getCurrentRequirement().getHumanReadableManager().getTag()); individualRequirementTestCounter++; break;
	    case 1:
		assertEquals("A.3.1", listToRequirementProcessor.getCurrentRequirement().getHumanReadableManager().getTag()); individualRequirementTestCounter++; break;
	    case 57:
		assertEquals("A.3.2", listToRequirementProcessor.getCurrentRequirement().getHumanReadableManager().getTag()); individualRequirementTestCounter++; break;
	    case 136:
		assertEquals("A.3.3", listToRequirementProcessor.getCurrentRequirement().getHumanReadableManager().getTag()); individualRequirementTestCounter++; break;
	    case 138:
		assertEquals("A.3.3[2].1", listToRequirementProcessor.getCurrentRequirement().getHumanReadableManager().getTag()); individualRequirementTestCounter++; break;
	    default:
		break;
	    }
	    
	    final RequirementOrdinary currentRequirement = listToRequirementProcessor.getCurrentRequirement();
	    if (currentRequirement != null) {
		currentRangeNum += new RequirementReader(this.readerData, currentRequirement, currentRangeNum).read();
	    }
	}
	
	assertEquals(5, individualRequirementTestCounter);
    }
}
