package test.helper;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import requirement.RequirementOrdinary;
import docreader.list.ListToRequirementProcessor;
import docreader.range.RequirementReader;

/**
 * Generic helper methods for integration tests which deal with figures
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public abstract class ITGenericFigureReader extends ITGenericListReader {    
    private Map<Integer, CodeWrapper> iterationActions = new HashMap<>();
    private int actionCounter = 0;
        
    protected interface CodeWrapper {
	void call(final RequirementOrdinary currentRequirement);
    }    
    
    
    /* (non-Javadoc)
     * @see test.helper.ITGenericReaderAbstract#runIndividualTest(java.lang.String, java.lang.String)
     */
    @Override
    protected int runIndividualTest(final String testcaseName, final String filename) {
	assert testcaseName != null && filename != null;
	return runIndividualTest(testcaseName, filename, 0);
    }

    
    /**
     * Like {@link ITGenericListReader#runIndividualTest(String, String)} but allows to narrow down the relevant paragraphs for this test
     * 
     * @param testcaseName human-readable name of the test
     * @param filename filename to process
     * @param lastParagraphNumOffset number of trailing paragraphs to skip (0-based)
     * @see test.helper.ITSpecializedReader#runIndividualTest(java.lang.String, java.lang.String)
     * @return number of paragraphs read
     */
    @Override
    protected int runIndividualTest(final String testcaseName, final String filename, final int lastParagraphNumOffset) {
	assert testcaseName != null && filename != null;
	setupTest(testcaseName, filename);
	this.listToRequirementProcessor = new ListToRequirementProcessor(this.readerData);		
	
	int currentRangeNum;
	for(currentRangeNum=0; currentRangeNum<this.readerData.getRange().numParagraphs()-lastParagraphNumOffset; currentRangeNum++) {	   
	    currentRangeNum = this.listToRequirementProcessor.processParagraph(currentRangeNum);	    	   

	    final RequirementOrdinary currentRequirement = this.listToRequirementProcessor.getCurrentRequirement();	    
	    if (currentRequirement != null) {
		int oldRangeNum = currentRangeNum;		
		currentRangeNum += new RequirementReader(this.readerData, currentRequirement, currentRangeNum).read();
		
		final CodeWrapper action = this.iterationActions.get(oldRangeNum);
		if (action != null) {
		    this.actionCounter++;
		    action.call(currentRequirement);
		}
	    }
	}
	
	// check that we havent missed any tests
	assertEquals(this.iterationActions.size(), this.actionCounter);
	
	return currentRangeNum;
    }
    
    protected void addIterationAction(final int iteration, final CodeWrapper payload) {
	assert payload != null;
	this.iterationActions.put(iteration, payload);
    }        
}
