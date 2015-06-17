package docreader.range;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.hwpf.usermodel.Paragraph;

import helper.XmlStringWriter;
import requirement.RequirementTemporary;
import requirement.RequirementWParent;
import requirement.data.RequirementText;
import requirement.metadata.Kind;
import docreader.ReaderData;
import docreader.range.paragraph.CaptionReader;
import docreader.range.paragraph.characterRun.ImageReader;

/**
 * Extracts "pseudo floating" figures from the input document
 * <p>These figures usually have the following structure:
 * <ol>
 * <li>a paragraph consisting of nothing but an image-reference (= a special character sequence).
 * This image is technically non-floating (i.e. it does not use MS Word's limited capabilities
 * of floating figures but is rather attached to the surrounding text)
 * <em>AND</em></li>
 * <li>a paragraph with certain formatting following the image, which
 * holds the figure number and caption text</li>
 * </ol>
 * <em>Exception:</em> Sometimes number + caption can also be in the same paragraph as the image itself, separated only by a line break.<br />
 * <em>Exception 2:</em> Sometimes there may be an empty paragraph between the image and the caption.
 * </p>
 *
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 */
class FigureReader extends RangeReader {

    private final Map<Integer, Integer> pictureOffsets;
    private final Paragraph captionParagraph;
    private RequirementWParent figureRequirement = null;

    /**
     * @param readerData global readerData
     * @param parentRequirement enclosing requirement to which the resulting figure will be attached as a child
     * @param initialParagraphIndex index number of the paragraph to read
     * @param pictureOffsets offsets of the characters which contain the link to the pictures; {@code key} is the offset, {@code value} only applies for shapes (startOffset of containing range)
     * @param captionParagraph "artificial" paragraph containing the caption or {@code null} if the caption is in the following paragraph of the document
     * @throws IllegalArgumentException if one of the arguments is illegal
     */
    public FigureReader(final ReaderData readerData, final RequirementTemporary parentRequirement, final int initialParagraphIndex, final Map<Integer, Integer> pictureOffsets, final Paragraph captionParagraph) {
	super(readerData, parentRequirement, initialParagraphIndex);
	if (pictureOffsets == null) throw new IllegalArgumentException("pictureOffsets cannot be null.");
	this.pictureOffsets = pictureOffsets;
	this.captionParagraph = captionParagraph;

	if (! (this.range instanceof Paragraph)) throw new IllegalArgumentException("We can only work on Paragraphs. However, we got " + this.range.getClass().toString() + ".");
    }

    /**
     * Read a "pseudo floating" figure
     * <p>"pseudo floating" means we have an image which spans an entire line (or paragraph)
     * immediately followed (either on the next line or the next paragraph) by a caption.</p>
     * 
     * @see docreader.GenericReader#read()
     * @return number of paragraphs to skip (that is: 0-based number of paragraphs read)
     */
    @Override
    public Integer read() {
	
	final int additionalParagraphsToSkip = detectFigureCaption();
	if (additionalParagraphsToSkip >= 0) {
	    assert this.figureRequirement != null;
	    final XmlStringWriter xmlwriter = new XmlStringWriter();
	    for (final Entry<Integer, Integer> currentOffset : this.pictureOffsets.entrySet()) {
		final ImageReader imageReader = new ImageReader(this.readerData, xmlwriter, currentOffset.getKey(), false, currentOffset.getValue());
		imageReader.writeImage(this.figureRequirement.getHumanReadableManager());
	    }

	    // write output	    
	    this.figureRequirement.getMetadata().setKind(Kind.FIGURE);
	    this.figureRequirement.setText(new RequirementText(null, xmlwriter.toString()));
	}
	
	return additionalParagraphsToSkip;
    }

    /**
     * extract a figure caption from the word input stream
     * 
     * @return number of paragraphs to skip because of the caption; may be {@code -1} if there is no figure
     */
    private int detectFigureCaption() {		
	final CaptionReader captionReader;
	final int output;
	if (this.captionParagraph == null) {
	    final int absoluteStartOffset = this.initialParagraphIndex +1;
	    captionReader = new CaptionReader(this.readerData, absoluteStartOffset, this.requirement);
	    output = captionReader.detectFigureCaption();
	    
	}
	else {
	    assert this.captionParagraph != null;
	    captionReader = new CaptionReader(this.readerData, this.captionParagraph, this.requirement);
	    captionReader.detectFigureCaption(); // discard the output
	    output = 0;
	}	
	this.figureRequirement = captionReader.getResultingRequirement();
	return output;
    }
}
