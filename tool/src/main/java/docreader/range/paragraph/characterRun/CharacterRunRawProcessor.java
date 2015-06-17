package docreader.range.paragraph.characterRun;

import static helper.Constants.Internal.MSWord.PLACEHOLDER_FOOTNOTE;
import static helper.Constants.Internal.MSWord.PLACEHOLDER_IMAGE;
import helper.word.DataConverter;

import java.util.Locale;

import org.apache.poi.hwpf.usermodel.CharacterRun;


abstract class CharacterRunRawProcessor implements CharacterRunReaderGeneric {
    protected final transient StringBuilder characterRuns = new StringBuilder();	
    protected final transient int startOffset;    

    protected CharacterRunRawProcessor(final int startOffset) {
	this.startOffset = startOffset;
    }

    /**
     * This is different from {@link CharacterRunReaderRich#read(CharacterRun)} since it emits a raw string.
     * 
     * @param characterRun characterRun to read
     * @throws IllegalArgumentException if the given arguement is {@code null}
     */
    @Override
    public void read(final CharacterRun characterRun) {
	if (characterRun == null) throw new IllegalArgumentException("characterRun cannot be null.");
	final String output;

	if (characterRun.getStartOffset() >= this.startOffset) {
	    // entire characterRun shall be read out
	    if (characterRun.isVanished()) {
		output = ""; // do not read out those characters
	    }
	    else if (characterRun.isSymbol()) {
		// we do handle symbol-characters here;
		// implementation-wise this requires a completely different approach than Range.stripFields()
		output = Character.toString(DataConverter.convertToUnicodeChar(characterRun));
	    }
	    else if (DataConverter.isSpace(characterRun)) {
		output = " ";
	    }
	    else {
		output = readOrdinaryRun(0, characterRun);
	    }		
	}
	else if (this.startOffset < characterRun.getEndOffset()){
	    // parts of this characterRun shall be read out

	    final int startIndex = this.startOffset - characterRun.getStartOffset();
	    assert startIndex > 0;
	    output = readOrdinaryRun(startIndex, characterRun);		
	}
	else {
	    // characterRun only consists of a fake numberText
	    output = "";
	}

	appendRaw(output);
    }

    /**
     * append the given text literally to the reader
     * 
     * @param text text to append, may be {@code null}
     */
    public void appendRaw(final String text) {
	if (text == null) return; // nothing to append 
	this.characterRuns.append(text);
    }

    /**
     * Generic output routine
     * 
     * @param startIndex
     * @param characterRun
     * @return
     */
    private static String readOrdinaryRun(final int startIndex, final CharacterRun characterRun) {
	assert characterRun != null;
	String output;	

	output = characterRun.text();
	assert startIndex < output.length();
	output = output.substring(startIndex); // delete any fake numberTexts

	output = output.replace(UNICODECHAR_VERTICALTAB, ' ');
	output = output.replace('\t', ' ');
	output = output.replace('\r', ' ');
	output = output.replace("\n", "");
	output = output.replace(Character.toString(PLACEHOLDER_IMAGE), "");
	output = output.replace(Character.toString(PLACEHOLDER_FOOTNOTE), "");
	output = DataConverter.cleanupText(output); // remove all control characters

	// flatten capitalization
	if (characterRun.isCapitalized()) {
	    output = output.toUpperCase(Locale.ENGLISH);
	}

	return output;
    }
}
