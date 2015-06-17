package test.helper;

import requirement.RequirementOrdinary;
import requirement.RequirementRoot;
import docreader.list.ListToRequirementProcessor;
import docreader.range.RequirementReader;

/**
 * Generic helper methods for integration tests which deal with lists and their proper hierarchy
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public abstract class ITGenericListReader extends ITSpecializedReader {
    protected ListToRequirementProcessor listToRequirementProcessor;
    
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
    protected int runIndividualTest(final String testcaseName, final String filename, final int lastParagraphNumOffset) {
	assert testcaseName != null && filename != null;
	setupTest(testcaseName, filename);
	this.listToRequirementProcessor = new ListToRequirementProcessor(this.readerData);		
	
	int currentRangeNum;
	for(currentRangeNum=0; currentRangeNum<this.readerData.getRange().numParagraphs()-lastParagraphNumOffset; currentRangeNum++) {
	    currentRangeNum = this.listToRequirementProcessor.processParagraph(currentRangeNum);	    	   

	    final RequirementOrdinary currentRequirement = this.listToRequirementProcessor.getCurrentRequirement();
	    if (currentRequirement != null) {
		currentRangeNum += new RequirementReader(this.readerData, currentRequirement, currentRangeNum).read();
	    }
	}
	
	return currentRangeNum;
    }
    
    protected RequirementRoot getRootRequirement() {
	return this.listToRequirementProcessor.getRootRequirement();
    }
    
    protected String[] getTree() {
	final String tree = getTree(this.listToRequirementProcessor.getRootRequirement(), 0);	
	return tree.split("\n");	
    }
        
}
