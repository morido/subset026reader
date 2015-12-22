import static org.junit.Assert.*;

import org.apache.poi.hwpf.usermodel.Paragraph;
import org.junit.Test;

import test.helper.ITGenericReader;
import docreader.list.ListToRequirementProcessor;
import docreader.range.paragraph.ParagraphListAware;

/**
 * Test methods for {@link docreader.list.ListReaderPlain}
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
@SuppressWarnings("javadoc")
public class ITListReaderPlain extends ITGenericReader {    

    /**
     * Test method for {@link docreader.list.ListReaderPlain#getFormattedNumber(ParagraphListAware)}.
     */    
    @Test
    public void testListNumberString() {
	// compares the numberTexts with a hardcoded version stored in the respective list paragraph
	final String filename = getResourcesDir() + "complex_list.doc";
	setupTest("List Reader Test 2", filename);
	final ListToRequirementProcessor listToRequirementProcessor = new ListToRequirementProcessor(this.readerData);

	for(int i=0; i<this.readerData.getRange().numParagraphs(); i++) {
	    final Paragraph paragraph = this.readerData.getRange().getParagraph(i);        	
	    if (!paragraph.isInList()) continue; // skip non-list paragraphs
	    final String numberText = listToRequirementProcessor.getListReader().processParagraphPlain(new ParagraphListAware(this.readerData, paragraph));
	    final String paragraphText = paragraph.text().trim();	    
	    assertEquals("Did not match at iteration: " + i, paragraphText, numberText);
	}
    }
    
    @Test
    public void testDeletedListParagraph() {
	// this triggers if revision marking was on and we technically have two paragraphs with the same numberText (one of which has been marked as "deleted")
	final String filename = getResourcesDir() + "numberingWRevisionMarks.doc";
	setupTest("List Reader Test 3", filename);
	final ListToRequirementProcessor listToRequirementProcessor = new ListToRequirementProcessor(this.readerData);

	try {
	    for(int i=0; i<this.readerData.getRange().numParagraphs(); i++) {
		final Paragraph paragraph = this.readerData.getRange().getParagraph(i);        	
		if (!paragraph.isInList()) continue; // skip non-list paragraphs
		@SuppressWarnings("unused")
		final String numberText = listToRequirementProcessor.getListReader().processParagraphPlain(new ParagraphListAware(this.readerData, paragraph));	    	    
	    }
	    fail("This loop shall not complete.");
	}
	catch (IllegalStateException e) {
	    assertEquals(e.getClass().getCanonicalName(), "docreader.list.ListReaderPlain.ListProcessingException");
	}
    }
}