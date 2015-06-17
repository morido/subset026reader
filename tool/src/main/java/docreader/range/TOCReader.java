package docreader.range;

import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;

import docreader.GenericReader;
import docreader.ReaderData;

/**
 * Reader for a "Table of Contents"
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class TOCReader implements GenericReader<Integer> {
    private final transient ReaderData readerData;
    private final transient int initialParagraphIndex;
    
    /**
     * @param readerData global reader data
     * @param initialParagraphIndex index of the first paragraph which is part of the TOC
     */
    public TOCReader(final ReaderData readerData, final int initialParagraphIndex) {
	this.readerData = readerData;
	this.initialParagraphIndex = initialParagraphIndex;
    }

    /**
     * Skips all TOC entries since those are not requirements and we do not need them for anything
     * 
     * @return number of paragraphs to skip because of the TOC
     * @see docreader.GenericReader#read()
     * @throws IllegalStateException if the field data is malformed
     */
    @Override
    public Integer read() {
	int fieldCounter = 0;
	int fieldStartCounter = 0;
	int currentParagraphOffset = this.initialParagraphIndex;
	
	// this is a very simple Word field reader which cannot do anything but to skip fields
	// to actually extract field data use docreader.range.paragraph.characterRun.FieldReader
	
	// look for 0013's (start of field) followed by 0014's (separator of field) and matching 0015's (end of field)
	// a TOC is typically a nested structure of one TOC field with a lot of PAGEREFs inside
	do {	    
	    final Paragraph paragraph = this.readerData.getRange().getParagraph(currentParagraphOffset);
	    for (int currentCharacterRunOffset = 0; currentCharacterRunOffset < paragraph.numCharacterRuns(); currentCharacterRunOffset++) {
		final CharacterRun currentRun = paragraph.getCharacterRun(currentCharacterRunOffset);
		if (currentRun.isSpecialCharacter()) {
		    // currentRun must be a special character, otherwise it never contains field data; [MS-DOC], v20140721, 2.6.1, sprmCFSpec
		    final String inputText = currentRun.text();
		    for (int i = 0; i < inputText.length(); i++) {
			final char currentChar = inputText.charAt(i);
			// match current char against list of special chars; [MS-DOC], v20140721, 2.8.25
			switch (currentChar) {
			case '\u0013':
			    // field start character 
			    fieldStartCounter++;
			    break;
			case '\u0014':
			    // field separator character
			    if (fieldStartCounter > 0) fieldCounter++;
			    else throw new IllegalStateException("Encountered malformed field data. Your document is corrupt.");
			    break;
			case '\u0015':
			    // field end character
			    fieldStartCounter--;
			    fieldCounter--;
			    break;
			default:
			    break; // do not care
			}
		    }
		}
	    }	    
	    currentParagraphOffset++;
	} while (fieldCounter > 0);
	
	// -1 because we do not know if the last paragraph perhaps contains other relevant data
	return currentParagraphOffset - this.initialParagraphIndex -1; 
    }
}
