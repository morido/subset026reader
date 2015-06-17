package docreader.range;


import helper.annotations.DomainSpecific;
import helper.word.DataConverter;

import org.apache.poi.hwpf.usermodel.Paragraph;

import docreader.GenericReader;
import docreader.ReaderData;
import docreader.range.paragraph.ParagraphListAware;

/**
 * Reader which skips certain unnecessary stuff
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class SkipReader implements GenericReader<Integer> {
    private final transient ReaderData readerData;   
    private final transient int initialParagraphIndex;
    private final transient Paragraph paragraph;    
               
    /**
     * Ordinary constructor
     * 
     * @param readerData global readerData
     * @param initialParagraphIndex index of first paragraph to examine for unnecessary content
     */
    public SkipReader(final ReaderData readerData, final int initialParagraphIndex) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");	
	
	this.readerData = readerData;	
	this.initialParagraphIndex = initialParagraphIndex;
	this.paragraph = readerData.getRange().getParagraph(initialParagraphIndex);	
    }

    /**
     * Main method; check if skip data is present
     * 
     * @return number of paragraphs to skip
     * @see docreader.GenericReader#read()
     */
    @Override
    @DomainSpecific
    public Integer read() {	
	// Case 1: Is this an empty paragraph?	
	if (DataConverter.isEmptyParagraph(this.readerData, new ParagraphListAware(this.readerData, this.paragraph))) return 1;
	// Case 2: Is this a TOC?
	if (isTOCCandidate()) {
	    return new TOCReader(this.readerData, this.initialParagraphIndex).read();
	}
	// None of the above; do not skip any paragraphs
	return 0;
    }

    /**
     * Checks if this paragraph is a candidate for a table of contents listing
     * 
     * @return {@code true} if this range contains the starting paragraph of a TOC; {@code false} otherwise
     */    
    private boolean isTOCCandidate() {			
	final String pattern = "^\u0013 TOC .*\u0014.*\\r$";	
	return this.paragraph.text().matches(pattern) && this.paragraph.getCharacterRun(0).isSpecialCharacter();
    }
}
