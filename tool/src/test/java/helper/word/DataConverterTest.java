package helper.word;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.poi.hwpf.usermodel.Paragraph;
import org.junit.Test;


/**
 * Tests for Word-related data conversion
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class DataConverterTest {

    /**
     * Test for various paragraph texts which manually embed their respective numberTexts
     */
    @SuppressWarnings("static-method")
    @Test
    public void testSeparateFakeListParagraph() {
	class TestTuple {
	    final String paragraphText;
	    final String expectedNumberText;
	    final boolean isInTable;
	    
	    public TestTuple(final String paragraphText, final String expectedNumberText) {
		this(paragraphText, expectedNumberText, false);
	    }
	    
	    public TestTuple(final String paragraphText, final String expectedNumberText, final boolean isInTable) {
		this.paragraphText = paragraphText;
		this.expectedNumberText = expectedNumberText;
		this.isInTable = isInTable;
	    }
	}
	
	// Step 1: Setup test data
	final TestTuple[] testTuples = {
	    new TestTuple("This is a test", null),
	    new TestTuple("3	Test", "3"),
	    new TestTuple("A	test", "A"),
	    new TestTuple("A test", null),
	    new TestTuple("3.2. test", "3.2"),
	    new TestTuple("3.2.	test", "3.2"),
	    new TestTuple("1.0.1\n 990307", null, true), // source: Subset-026, Chapter 3, Baseline 3.3.0; underneath 3.1
	    new TestTuple("Full Supervision		(FS)", null), // source: Subset-026, Chapter 4, Baseline 3.3.0
	    new TestTuple("A.3.1	List of Fixed Value Data", "A.3.1"), // source: Subset-026, Chapter 3, Baseline 3.3.0
	    new TestTuple("A3.6.1	Standard case", "A.3.6.1"), // source: Subset-026, Chapter 3, Baseline 3.3.0
	    new TestTuple("A3.6.2.1.1	Note: The above information remains stored for the case of a reverse movement:", "A.3.6.2.1.1"), // source: Subset-026, Chapter 3, Baseline 3.3.0
	    new TestTuple("A3.3 List of events to be recorded in the Juridical Recorder", "A.3.3"), // source: Subset-026, Chapter 3, Baseline 2.3.0.d
	    new TestTuple("A.3.10.3	Two...", "A.3.10.3"), // source: Subset-026, Chapter 3, Baseline 3.3.0
	};
	
	// Step 2: actual test
	for (final TestTuple testTuple : testTuples) {
	    final Paragraph paragraph = mock(Paragraph.class);
	    when(paragraph.text()).thenReturn(testTuple.paragraphText);
	    when(paragraph.isInTable()).thenReturn(testTuple.isInTable);
	    	    
	    final String givenOutput = DataConverter.separateFakeListParagraph(paragraph, true);
	    assertEquals(testTuple.expectedNumberText, givenOutput);
	}
    }

}
