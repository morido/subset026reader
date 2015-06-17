package docreader.range;

import org.apache.poi.hwpf.usermodel.Range;

import requirement.RequirementTemporary;
import docreader.GenericReader;
import docreader.ReaderData;

/**
 * Abstract reader for ranges in the document
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public abstract class RangeReader implements GenericReader<Integer> {
    protected final Range range;
    protected final RequirementTemporary requirement;
    protected final ReaderData readerData;
    protected final Integer initialParagraphIndex;

    /**
     * @param readerData global readerData
     * @param requirement Requirement which will be populated by the reader (i.e. where to store the results)
     * @param initialParagraphIndex index of the paragraph in the overall document
     * @throws IllegalArgumentException some of the given data was {@code null}
     */
    protected RangeReader(final ReaderData readerData, final RequirementTemporary requirement, final Integer initialParagraphIndex) {
	if (requirement == null) throw new IllegalArgumentException("Requirement cannot be null.");
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	// Note: initialParagraphIndex may be null

	this.requirement = requirement;		
	this.readerData = readerData;
	this.initialParagraphIndex = initialParagraphIndex;
	this.range = requirement.getAssociatedRange();
    }
}
