package docreader.range;

import helper.word.FieldStore;

import org.apache.poi.hwpf.usermodel.Range;

import requirement.RequirementTemporary;
import requirement.data.RequirementText;
import docreader.ReaderData;
import docreader.range.paragraph.ParagraphReader;

/**
 * Reader for requirements which are non-floating (no figures, no tables);
 * does not support nested structures (i.e. this will always create exactly one requirement no matter how lengthy / complex the input is)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class RequirementReaderTextual implements RequirementReaderI {
    protected final transient StringBuilder requirementContentRich = new StringBuilder(); // NOPMD - intentional StringBuilder field
    protected final transient StringBuilder requirementContentRaw = new StringBuilder(); // NOPMD - intentional StringBuilder field
    private FieldStore<Integer> firstField = null;
    protected final transient RequirementTemporary requirement;
    protected final transient ReaderData readerData;
    protected final transient Range range;
    private String delimiter = "";

    /**
     * @param readerData global ReaderData
     * @param requirement requirement where to store the results
     */
    public RequirementReaderTextual(final ReaderData readerData, final RequirementTemporary requirement) {
	if (readerData == null) throw new IllegalArgumentException("ReaderData cannot be null.");
	if (requirement == null) throw new IllegalArgumentException("requirement cannot be null.");	

	this.requirement = requirement;
	this.readerData = readerData;
	this.range = requirement.getAssociatedRange();
    }

    /** 
     * Reader for ordinary paragraphs
     * 
     * @see docreader.GenericReader#read()
     */
    @Override
    public Integer read() {
	final Integer output = readAbstract();
	this.requirement.setText(this.readerData, new RequirementText(this.requirementContentRaw.toString(), this.requirementContentRich.toString()));	

	assert output != null;
	return output; // we need to return an integer instead of an int due to limitations of Java Generics
    }

    /**
     * @return first printable field
     */
    @Override
    public final FieldStore<Integer> getFirstField() {
	return this.firstField;
    }

    /**
     * Reads a requirement which may consist of several paragraphs
     * 
     * @return number of paragraphs to skip
     */
    protected final int readAbstract() {
	assert this.range != null;
	// Loop will iterate once for ordinary requirements (which consist of a single paragraph only) but possibly several times for requirements in table cells.
	int iterator;
	for (iterator = 0; iterator < this.range.numParagraphs(); iterator++) {
	    iterator += readAbstractSingleParagraph(iterator);
	}
			
	assert iterator > 0 : "Apparently the loop above has never run. This should not happen.";
	return iterator-1;
    }

    /**
     * Reads a requirement under the assumption it only contains a single paragraph; from this paragraph only interesting parts may be specified
     * 
     * @param paragraphOffset offset of the paragraph in the enclosing range
     * @param characterRunStartOffset startOffset of the first characterRun to read
     * @param characterRunEndOffset end offset of the last characterRun to read (inclusive)
     * @return the number of paragraphs to skip (that is: 0-based number of paragraphs read); always {@code 0} here 
     */
    protected int readAbstractSingleParagraph(final int paragraphOffset) {	
	final ParagraphReader paragraphReader = new ParagraphReader(this.readerData, this.requirement, this.range.getParagraph(paragraphOffset), determineLastWordOfPreviousParagraph(paragraphOffset));
	final int output = paragraphReader.read(); // autoboxing is safe
	this.requirementContentRich.append(paragraphReader.getRichText());
	this.requirementContentRaw.append(this.delimiter + paragraphReader.getRawText());
	this.delimiter = "\n"; // delimit all following lines
	if (this.firstField == null) this.firstField = paragraphReader.getFirstField();
	return output;
    }
    
    /**
     * Determine the last word of the preceding paragraph; used for requirement links
     * 
     * @param paragraphOffset offset of the preceding paragraph
     * @return the last word and a trailing space or an empty string if there was no preceding paragraph; never {@code null}
     */
    private String determineLastWordOfPreviousParagraph(final int paragraphOffset) {
	final String output;
	if (paragraphOffset > 0) {
	    final StringBuilder paragraphText = new StringBuilder(this.range.getParagraph(paragraphOffset-1).text());
	    final String[] lastWordArray = paragraphText.reverse().toString().split(" ", 2);
	    final StringBuilder lastWord = new StringBuilder(lastWordArray[0]).reverse();
	    if (lastWord.charAt(lastWord.length()-1) == '\r') lastWord.deleteCharAt(lastWord.length()-1); // should always fire
	    output = lastWord.append(" ").toString();
	}
	else output = "";
	return output;
    }
}