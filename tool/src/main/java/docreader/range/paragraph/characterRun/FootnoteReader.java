package docreader.range.paragraph.characterRun;

import static helper.Constants.Internal.MSWord.PLACEHOLDER_FOOTNOTE;
import helper.RequirementHelper;
import helper.TraceabilityManagerHumanReadable;
import helper.XmlStringWriter;
import helper.formatting.FootnoteFormatter;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Notes;
import org.apache.poi.hwpf.usermodel.Range;

import requirement.RequirementTemporary;
import requirement.RequirementWParent;
import docreader.GenericReader;
import docreader.ReaderData;
import docreader.list.NestingType;
import docreader.range.RequirementReaderRange;


/**
 * Reader for footnotes and endnotes
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class FootnoteReader implements GenericReader<Void>{
    private final transient ReaderData readerData;
    private final transient RequirementTemporary parentRequirement;
    private final transient XmlStringWriter xmlwriter;
    private final transient CharacterRun inputRun;

    private static final Logger logger = Logger.getLogger(FootnoteReader.class.getName());  // NOPMD - Reference rather than a static field
    
    /**
     * Supported note types
     */
    private enum NoteType {
	FOOTNOTE,
	ENDNOTE
    }
    
    /**
     * @param readerData global readerData
     * @param xmlwriter xmlwriter where to add the note references
     * @param requirement parent requirement. Note(s) will be added as children
     * @param inputRun characterRun containing note references
     */
    public FootnoteReader(final ReaderData readerData, final XmlStringWriter xmlwriter, final RequirementTemporary requirement, final CharacterRun inputRun) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	if (xmlwriter == null) throw new IllegalArgumentException("xmlwriter cannot be null.");
	if (requirement == null) throw new IllegalArgumentException("requirement cannot be null.");
	if (inputRun == null) throw new IllegalArgumentException("inputRun cannot be null.");
	
	this.readerData = readerData;
	this.xmlwriter = xmlwriter;
	this.parentRequirement = requirement;
	this.inputRun = inputRun;
    }
    
    /**
     * Check if the given characterRun contains a note
     * 
     * @param inputRun characterRun to check
     * @return {@code true} if the given characterRun contains at least one autonumbered footnote or endnote; {@code false} otherwise
     */
    public static boolean noteAvailable(final CharacterRun inputRun) {
	if (inputRun == null) throw new IllegalArgumentException("inputRun cannot be null.");
	return (inputRun.isSpecialCharacter() && inputRun.text().charAt(0) == PLACEHOLDER_FOOTNOTE);
    }
    
    /**
     * Read out all footnotes and endnotes in this characterRun
     * 
     * @see docreader.GenericReader#read()
     */
    @Override
    public Void read() {	
	if (!noteAvailable(this.inputRun)) throw new IllegalStateException("No notes available. Cannot call this method.");
	
	if (RequirementHelper.isRooted(this.parentRequirement)) {
	    // do not read out notes if we are in some temporary structure (e.g. a CaptionReader)
	    int charOffset = -1;
	    final String inputRunText = this.inputRun.text();

	    for (int currentCharacterOffset = this.inputRun.getStartOffset(); currentCharacterOffset < this.inputRun.getEndOffset(); currentCharacterOffset++) {
		charOffset++;
		if (inputRunText.charAt(charOffset) != PLACEHOLDER_FOOTNOTE) {
		    logger.log(Level.WARNING, "Encountered a combined special character run which contains different placeholders. Expected a footnote but got {0}. Will skip this reference.", Character.toString(inputRunText.charAt(charOffset)));
		    continue;
		}

		{
		    final Notes footnotes = this.readerData.getDocument().getFootnotes();		
		    final int footnotePosition = footnotes.getNoteIndexByAnchorPosition(currentCharacterOffset);
		    if (footnotePosition != -1) {
			final Range footnoteRange = this.readerData.getDocument().getFootnoteRange();
			final Range noteTextRange = readNote(footnotes, footnoteRange, footnotePosition);
			writeNoteRequirement(noteTextRange, NoteType.FOOTNOTE);
			continue;
		    }
		}
		{
		    final Notes endnotes = this.readerData.getDocument().getEndnotes();
		    final int endnotePosition = endnotes.getNoteIndexByAnchorPosition(currentCharacterOffset);
		    if (endnotePosition != -1) {
			final Range endnoteRange = this.readerData.getDocument().getEndnoteRange();
			final Range noteTextRange = readNote(endnotes, endnoteRange, endnotePosition);
			writeNoteRequirement(noteTextRange, NoteType.ENDNOTE);
			continue;
		    }
		}
		// both footnotePosition and endnotePosition returned -1
		logger.log(Level.INFO, "Found a Footnote/Endnote-mark without any actual note.");
	    }
	}
	
	// we need to return null because of Java Generics
	return null;
    }
    
    /**
     * Read out the actual footnote/endnote
     * 
     * @param notes document notes information as provided by POI
     * @param noteRange range with the contents of the note
     * @param noteIndex internal index from Word to access the note
     * @return a range containing the footnote/endnote text
     */
    private Range readNote(final Notes notes, final Range noteRange, final int noteIndex) {	
        final int rangeStartOffset = noteRange.getStartOffset();
        final int noteTextStartOffset = notes.getNoteTextStartOffset(noteIndex);
        final int noteTextEndOffset = notes.getNoteTextEndOffset(noteIndex);
        final Range noteTextRange = new Range( rangeStartOffset + noteTextStartOffset, rangeStartOffset + noteTextEndOffset, this.readerData.getDocument());
        
        // strip out the leading \u0002 inside the footnote text (whatever its purpose is) otherwise we run into recursion while reading out the text below
        final Range noteTextRangeCleaned;
        final String rawNoteText = noteTextRange.text();
        if (rawNoteText.startsWith(PLACEHOLDER_FOOTNOTE + " ") || rawNoteText.startsWith(PLACEHOLDER_FOOTNOTE + "\t")) {
            noteTextRangeCleaned = new Range( rangeStartOffset + noteTextStartOffset +2, rangeStartOffset + noteTextEndOffset, this.readerData.getDocument());
        }
        else if (rawNoteText.length() > 0 && rawNoteText.charAt(0) == PLACEHOLDER_FOOTNOTE) {
            noteTextRangeCleaned = new Range( rangeStartOffset + noteTextStartOffset +1, rangeStartOffset + noteTextEndOffset, this.readerData.getDocument());
        }
        else {
            noteTextRangeCleaned = noteTextRange;
        }
        
        return noteTextRangeCleaned;                
    }
    
    /**
     * Create a new requirement containing the note text
     * 
     * @param noteRange text of the footnote/endnote
     * @param noteType type of the note (footnote or endnote)
     */
    private void writeNoteRequirement(final Range noteRange, final NoteType noteType) {
	assert noteRange != null;

        final RequirementWParent footnoteRequirement = new RequirementWParent(this.readerData, noteRange, this.parentRequirement);        
        final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();
        switch (noteType) {
        case FOOTNOTE:
            hrManager.addFootnote(this.readerData.getNextFootnoteRunningNumber());
            break;
        case ENDNOTE:
            hrManager.addEndnote(this.readerData.getNextEndnoteRunningNumber());
            break;
	default:
	    throw new IllegalStateException();
        }
        footnoteRequirement.setHumanReadableManager(hrManager);
        
        this.readerData.getListToRequirementProcessor().getListReader().addNestingLevel(footnoteRequirement, noteRange, NestingType.NOTE);        
        new RequirementReaderRange(this.readerData, footnoteRequirement).read(); // allows for all sorts of nested structures 
        this.readerData.getListToRequirementProcessor().getListReader().removeNestingLevel();
        
        final FootnoteFormatter footnoteFormatter = new FootnoteFormatter(this.xmlwriter);
        footnoteFormatter.writeStartElement();
        this.xmlwriter.writeCharacters(hrManager.getTag());
        footnoteFormatter.writeEndElement();
    }
}
