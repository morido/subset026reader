package test.helper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.apache.poi.hwpf.usermodel.Paragraph;

import docreader.range.table.TableReader;
import requirement.RequirementTemporary;
import requirement.RequirementWParent;

/**
 * Generic helper methods for integration tests which deal with tables
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public abstract class ITGenericTableReader extends ITSpecializedReader {
    private transient RequirementTemporary umbrellaRequirement;	
        
    @Override
    protected int runIndividualTest(final String testcaseName, final String filename) {
	assert testcaseName != null && filename != null;
	setupTest(testcaseName, filename);
	final Paragraph paragraph = this.readerData.getRange().getParagraph(0);
	
	// setup a correct requirement structure so the tree is rooted and we can obtain tracetags from the tracetag store	
	this.umbrellaRequirement = new RequirementTemporary(paragraph, true);
	
	return (new TableReader(this.readerData, this.umbrellaRequirement, 0)).read();		
    }

    /**
     * Determines the number of children of a requirement
     * 
     * @return number of children
     */
    protected int numOfChildren() {
	int children = 0;
	final Iterator<RequirementWParent> iterator = getActualTable().getChildIterator();
	while (iterator.hasNext()) {
	    iterator.next();
	    children++;
	}
	return children;
    }


    protected String getRichText() {
	return getActualTable().getText().getRich();
    }

    /**
     * Gets the first child of the umbrellaRequirement
     * 
     * @return
     */
    protected RequirementWParent getActualTable() {
	return this.umbrellaRequirement.getChildIterator().next();
    }

    private int getNumberOfRequirementsRead() {
	return this.readerData.getTraceabilityLinker().getNumberOfRequirements();
    }

    protected void checkStructure(final String[] expectedStructure) {
	final String tree = getTree(this.umbrellaRequirement, 0);
	assertArrayEquals(expectedStructure, tree.split("\n"));	

	// Check for number of requirements
	// -1 because we do not count the root requirement
	assertEquals(expectedStructure.length-1, getNumberOfRequirementsRead());
    }
}
