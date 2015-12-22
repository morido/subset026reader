import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hwpf.usermodel.Paragraph;
import org.junit.Test;

import requirement.RequirementTemporary;
import test.helper.ITGenericReader;
import docreader.range.RequirementReader;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class ITCharacterReader extends ITGenericReader {

    /**
     * Test method for {@link docreader.range.paragraph.ParagraphReader#read()}.
     */
    @Test
    public void testSpecialCharacters() {
	final String filename = getResourcesDir() + "special_characters.doc";
	final List<String> paragraphText = new ArrayList<>();
	paragraphText.add("<span style=\"font-family:Times New Roman; font-size:12pt;\">Here are some special characters in „Symbol“-Font: </span><span style=\"font-family:Symbol; font-size:12pt;\"> λπ∋γ∞∑Δ</span>");
	paragraphText.add("");
	paragraphText.add("<span style=\"font-family:Times New Roman; font-size:12pt;\">Here are some special characters in “Times New Roman”:  ♠♀♫†≥</span>");
	paragraphText.add("");
	paragraphText.add("<span style=\"font-family:Times New Roman; font-size:12pt;\">Here are some special characters in “Arial”: </span><span style=\"font-family:Arial; font-size:12pt;\">≥≤∑∞◊</span>");

	runIndividualTest(filename, paragraphText);
    }

    /**
     * Test method for {@link docreader.range.paragraph.ParagraphReader#read()}.
     */
    @Test
    public void testCommonFormatting() {
	final String filename = getResourcesDir() + "character_formatting.doc";	
	final List<String> paragraphText = new ArrayList<>();
	paragraphText.add("<b><span style=\"font-family:Arial; font-size:16pt;\">This is a headline</span></b>");
	paragraphText.add("<span style=\"font-family:Times New Roman; font-size:12pt;\">Here comes some standard text.</span>");
	paragraphText.add("");
	paragraphText.add("<b><i><span style=\"font-family:Arial; font-size:14pt;\">This is a subheading</span></i></b>");
	paragraphText.add("<span style=\"color:#FF0000; font-family:Times New Roman; font-size:12pt;\">This is red</span><span style=\"font-family:Times New Roman; font-size:12pt;\">. </span><i><span style=\"font-family:Times New Roman; font-size:12pt;\">While this is italic</span></i><span style=\"font-family:Times New Roman; font-size:12pt;\">. </span><b><span style=\"font-family:Times New Roman; font-size:12pt;\">And bold</span></b><span style=\"font-family:Times New Roman; font-size:12pt;\">. </span><b><i><span style=\"font-family:Times New Roman; font-size:12pt;\">And both</span></i></b><span style=\"font-family:Times New Roman; font-size:12pt;\">. </span><del><span style=\"font-family:Times New Roman; font-size:12pt;\">And strikethrough</span></del><span style=\"font-family:Times New Roman; font-size:12pt;\">, </span><del><span style=\"font-family:Times New Roman; font-size:12pt;\">and doublestrikethrough</span></del><span style=\"font-family:Times New Roman; font-size:12pt;\">. Not to forget about </span><span style=\"font-family:Times New Roman; font-size:12pt; text-decoration:underline;\">underlined text</span><span style=\"font-family:Times New Roman; font-size:12pt;\">. Here is a </span><span style=\"font-family:Times New Roman; font-size:12pt; vertical-align:sub;\">subscript</span><span style=\"font-family:Times New Roman; font-size:12pt;\"> and a </span><span style=\"font-family:Times New Roman; font-size:12pt; vertical-align:super;\">superscript</span><span style=\"font-family:Times New Roman; font-size:12pt;\">. </span><span style=\"font-family:Times New Roman; font-size:12pt; text-transform:uppercase;\">And all caps</span><span style=\"font-family:Times New Roman; font-size:12pt;\">. </span><span style=\"font-family:Times New Roman; font-size:12pt; font-variant:small-caps;\">And small caps</span><span style=\"font-family:Times New Roman; font-size:12pt;\">.</span>");		
	paragraphText.add("");
	paragraphText.add("<p style=\"background-color:#448000;\"><span style=\"font-family:Times New Roman; font-size:12pt;\">This is a paragraph with beautiful green background shading.</span></p>");
	paragraphText.add("");
	paragraphText.add("<del><span style=\"font-family:Times New Roman; font-size:12pt;\">If the session is open, then the process shall go to </span><b><span style=\"font-family:Times New Roman; font-size:12pt;\">S10</span></b><span style=\"font-family:Times New Roman; font-size:12pt;\">, otherwise the process shall go to </span><b><span style=\"font-family:Times New Roman; font-size:12pt;\">S4</span></b></del>"); // deleted text from revision marks
	
	runIndividualTest(filename, paragraphText);		
    }


    /**
     * Wraps common functionality of all tests in this file
     * 
     * @param filename Name of the file to read
     * @param paragraphText comparison data
     * @throws Exception
     */
    private void runIndividualTest(final String filename, final List<String> paragraphText) {
	final TestReader testReader = new TestReader(filename);

	assertEquals("Number of paragraphs in file do not match with test fixtures", ITCharacterReader.this.readerData.getRange().numParagraphs(), paragraphText.size());
	int offset = -1;	
	for (int i = 0; i<paragraphText.size(); i++) {			
	    offset = testReader.read(i);

	    //Check for number of processed paragraphs
	    assertEquals(1, offset);

	    //Check if text has been read out correctly
	    String expectedResult = paragraphText.get(i);
	    assertEquals(expectedResult, testReader.getText());
	}
    }


    /**
     * Simple reader for multiple paragraphs without any overhead (i.e. a lightweight replacement for {@link docreader.DocumentReader})
     * 
     * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
     *
     */
    private class TestReader {
	private RequirementTemporary requirement = null;

	public TestReader(final String filename) {
	    setupTest("test", filename);
	}

	/**
	 * Wraps common functionality of all tests in this file
	 * 
	 * @param offset offset of the paragraph to read
	 * @return number of paragraphs which were read
	 */
	public int read(final int offset) {			
	    final Paragraph paragraph = ITCharacterReader.this.readerData.getRange().getParagraph(offset);
	    this.requirement = new RequirementTemporary(paragraph);					

	    return (new RequirementReader(ITCharacterReader.this.readerData, this.requirement)).read()+1;						
	}

	public String getText() {
	    return this.requirement.getText().getRich();
	}
    }
}
