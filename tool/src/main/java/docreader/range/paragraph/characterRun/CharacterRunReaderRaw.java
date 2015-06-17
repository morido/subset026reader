package docreader.range.paragraph.characterRun;

/**
 * Very simple class which only reads the raw textual contents of a character run
 */
public class CharacterRunReaderRaw extends CharacterRunRawProcessor {
    private final StringBuilder outputWriter;

    /**
     * Ordinary constructor
     * 
     * @param startOffset used for fake list paragraphs when not the entire paragraph shall be read out
     * @param outputWriter writer for the output
     * @throws IllegalArgumentException if the given argument is {@code null}
     */
    public CharacterRunReaderRaw(final int startOffset, final StringBuilder outputWriter) {
	super(startOffset);
	if (outputWriter == null) throw new IllegalArgumentException("outputWriter cannot be null.");
	this.outputWriter = outputWriter;
    }

    @Override
    public void close() {	    
	// Trim this string before storing it away
	this.outputWriter.append(this.characterRuns.toString().trim());
    }
}
