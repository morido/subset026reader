package docreader.range.paragraph;

import org.apache.poi.hwpf.usermodel.Paragraph;

import static helper.Constants.MSWord.DELIMITER_CAPTION;
import helper.RegexHelper;
import helper.TraceabilityManagerHumanReadable;
import helper.annotations.DomainSpecific;
import helper.word.FieldStore;
import helper.word.DataConverter;
import helper.word.FieldStore.FieldIdentifier;
import requirement.RequirementOrdinary;
import requirement.RequirementTemporary;
import requirement.RequirementWParent;
import requirement.metadata.Kind;
import docreader.ReaderData;
import docreader.range.RequirementReaderTextual;

/**
 * Extracts captions from floating entities (tables, figures)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public final class CaptionReader {    
    private final ReaderData readerData;
    /**
     * offset for trailing empty paragraphs (i.e. empty paragraphs between the table/figure and the caption
     */
    private int emptyParagraphSkipOffset;
    private final Paragraph captionParagraph;
    private final RequirementTemporary parentRequirement;
    private RequirementWParent resultingRequirement = null;
    
    
    /**
     * Caption reader for an explicit (artificial) captionParagraph
     * 
     * @param readerData global readerData
     * @param captionParagraph artificial paragraph containing nothing but the caption 
     * @param parentRequirement parent requirement to which the caption should be added
     */
    public CaptionReader(final ReaderData readerData, final Paragraph captionParagraph, final RequirementTemporary parentRequirement) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	if (captionParagraph == null) throw new IllegalArgumentException("captionRequirement cannot be null.");
	if (parentRequirement == null) throw new IllegalArgumentException("captionRequirement cannot be null.");
	
	this.readerData = readerData;	
	this.emptyParagraphSkipOffset = 0;
	this.captionParagraph = captionParagraph;
	this.parentRequirement = parentRequirement;
    }
    
    /**
     * Caption reader for a caption originating from an ordinary paragraph in the document
     * 
     * @param readerData global readerData
     * @param captionParagraphStartOffset offset of the captionParagraph in the document
     * @param parentRequirement parent requirement to which the caption should be added
     */
    public CaptionReader(final ReaderData readerData, final int captionParagraphStartOffset, final RequirementTemporary parentRequirement) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	if (parentRequirement == null) throw new IllegalArgumentException("captionRequirement cannot be null.");
	this.readerData = readerData;	
	
	this.emptyParagraphSkipOffset = 0;
	// allow exactly one empty paragraph prepending the caption
	if ((captionParagraphStartOffset + this.emptyParagraphSkipOffset +1) < readerData.getRange().numParagraphs()
		&& DataConverter.isEmptyParagraph(readerData, new ParagraphListAware(readerData, readerData.getRange().getParagraph(captionParagraphStartOffset)))) {
	    this.emptyParagraphSkipOffset++;
	}
	this.captionParagraph = this.readerData.getRange().getParagraph(captionParagraphStartOffset + this.emptyParagraphSkipOffset);
	this.parentRequirement = parentRequirement;
    }

    /**
     * Detect the caption of a table
     * 
     * @return the number of paragraphs to skip because of the caption
     */
    public int detectTableCaption() {
	return (new TableCaptionExtractor()).detectCaption();
    }

    /**
     * Detect the caption of a figure
     * 
     * @return the number of paragraphs to skip because of the caption, may be {@code -1} if there is no caption
     */
    public int detectFigureCaption() {
	return (new FigureCaptionExtractor()).detectCaption();
    }

    /**
     * @return the requirement containing the float + caption; can be {@code null} if this is not a floating entity
     */
    public RequirementWParent getResultingRequirement() {
	return this.resultingRequirement;
    }

    private abstract class AbstractCaptionExtractor {
	protected final RequirementTemporary captionRequirementTmp;
	protected final TraceabilityManagerHumanReadable hrManagerResult;
	protected int initialParagraphsToSkip;
	protected final String numberText;
	private final boolean isTableAsFigure;

	/**
	 * Extracts the running number from a caption based either on the available field data or an heuristic approach
	 */
	private final class RunningNumberExtractor {
	    private final String numberOfFloatingEntity;
	    private boolean tableFallbackActive = false;
	    
	    /**
	     * @param field Field as extracted from Word, can be {@code null} if there is no field
	     * @param rawParagraphText the raw text of this paragraph without any formatting
	     * @param fieldIdentifier Identifier of the field we are expecting
	     */
	    @DomainSpecific
	    public RunningNumberExtractor(final FieldStore<Integer> field, final String rawParagraphText, final FieldIdentifier fieldIdentifier) {
		assert rawParagraphText != null && fieldIdentifier != null; // field can be null
		// Case 1: assume the worst: Field is missing or does not cover entire number sequence
		// Rationale: requirement authors used SEQ-fields (if at all) which can only hold integers but not stuff like "21a" (which does appear as a Figure-number)
		// Hence, we cannot rely on the field data but need to use a regex instead
		final String quotedSeparator = RegexHelper.quoteRegex(DELIMITER_CAPTION);
		final String regex = fieldIdentifier.toString() + "\\s([0-9]+[a-z]?)" + quotedSeparator + "\\s?.*$";
		final String numberTextFromRegex = RegexHelper.extractRegex(rawParagraphText, regex);
		if (numberTextFromRegex != null) this.numberOfFloatingEntity = numberTextFromRegex;
		else if (field != null && field.getIdentifier() == fieldIdentifier) {
		    // Case 2: ordinary field based approach
		    // except for edge cases this should actually never fire; instead it is included in case 1
		    this.numberOfFloatingEntity = Integer.toString(field.getData());
		}
		else if (fieldIdentifier == FieldIdentifier.TABLENUMBER) {
		    // Case 3: fallback for tables
		    // this happens in chapter 4: tables which have a caption that says "Figure x: bla bla"
		    // force them to be a table instead
		    final String regexFallback = FieldIdentifier.FIGURENUMBER.toString() + "\\s([0-9]+[a-z]?)" + quotedSeparator + "\\s?.*$";
		    final String numberTextFallback = RegexHelper.extractRegex(rawParagraphText, regexFallback);
		    if (numberTextFallback != null) {
			this.numberOfFloatingEntity = numberTextFallback;
			this.tableFallbackActive = true;
		    }
		    else this.numberOfFloatingEntity = null;
		}
		else {
		    // Case 4: nothing found
		    this.numberOfFloatingEntity = null;
		}
	    }
	    
	    public String getNumber() {
		return this.numberOfFloatingEntity;
	    }
	    
	    public boolean isTableFallback() {
		return this.tableFallbackActive;
	    }
	}
	
	
	public AbstractCaptionExtractor() {
	    this.captionRequirementTmp = new RequirementTemporary(CaptionReader.this.captionParagraph);
	    // we do not expect captions to contain nested structures; so no support for them here; just plain text
	    RequirementReaderTextual captionReader = new RequirementReaderTextual(CaptionReader.this.readerData, this.captionRequirementTmp);
	    captionReader.read();
	    
	    // perform a generic check if a caption may be available	    
	    final boolean captionAvailableGeneric = paragraphHasBoldCharacters(CaptionReader.this.captionParagraph);
	    
	    final RunningNumberExtractor rnExtractor = new RunningNumberExtractor(captionReader.getFirstField(), this.captionRequirementTmp.getText().getRaw(), getIdentifier());
	    
	    // basically we have two cases here:
	    // 1. a table is always considered a floating entity
	    // 2. a figure is only a floating entity if there is also a caption; otherwise we have an inline image
	    if ((rnExtractor.getNumber() != null && captionAvailableGeneric) || getIdentifier() == FieldIdentifier.TABLENUMBER) {
		this.hrManagerResult = new TraceabilityManagerHumanReadable();
		CaptionReader.this.resultingRequirement = new RequirementWParent(CaptionReader.this.readerData, CaptionReader.this.captionParagraph, CaptionReader.this.parentRequirement);
		this.numberText = rnExtractor.getNumber();
		this.isTableAsFigure = rnExtractor.isTableFallback();
	    }
	    else {
		this.hrManagerResult = null;
		this.numberText = null;
		CaptionReader.this.resultingRequirement = null;
		this.isTableAsFigure = false; // not important here
	    }
	}
		
	/**
	 * @return number of paragraphs to skip because of the caption
	 */
	public abstract int detectCaption();
	
	/**
	 * @return the field identifier of the non-abstract class
	 */
	protected abstract FieldIdentifier getIdentifier();

	protected int writeCaptionFound() {
	    CaptionReader.this.resultingRequirement.setHumanReadableManager(this.hrManagerResult);       
	    final TraceabilityManagerHumanReadable hrManagerCaption = new TraceabilityManagerHumanReadable();	
	    hrManagerCaption.addCaption();
	    final RequirementOrdinary captionRequirement = new RequirementOrdinary(CaptionReader.this.readerData, CaptionReader.this.captionParagraph, hrManagerCaption, CaptionReader.this.resultingRequirement);
	    captionRequirement.setText(this.captionRequirementTmp.getText());	    
	    captionRequirement.getMetadata().setKind(CaptionReader.this.resultingRequirement.getMetadata().getKind()); // inherit the type from the parent (which contains the image / table)
	    
	    switch(getIdentifier()) {	    
	    case FIGURENUMBER:
		CaptionReader.this.readerData.getTraceabilityLinker().addFigureLink(this.numberText, captionRequirement); break;		
	    case TABLENUMBER:
		if (this.isTableAsFigure) {
		    CaptionReader.this.readerData.getTraceabilityLinker().addFigureLink(this.numberText, captionRequirement);
		}
		else {
		    CaptionReader.this.readerData.getTraceabilityLinker().addTableLink(this.numberText, captionRequirement);
		}
		break;
		//$CASES-OMITTED$
	    default:
		break; // do not care	    
	    }	    	   
	    
	    return this.initialParagraphsToSkip + 1;
	}

	protected int writeCaptionNotFound() {
	    CaptionReader.this.resultingRequirement.setHumanReadableManager(this.hrManagerResult);
	    return this.initialParagraphsToSkip;
	}

	/**
	 * Used as an (arguably weak) indicator whether a caption exists or not
	 * 
	 * @param paragraph Paragraph under consideration
	 * @return {@code true} if there is at least one bold character in this paragraph; {@code false} otherwise
	 */
	private boolean paragraphHasBoldCharacters(final Paragraph paragraph) {
	    assert paragraph != null;	    
	    for (int i = 0; i < paragraph.numCharacterRuns(); i++) {
		if (paragraph.getCharacterRun(i).isBold()) return true;
	    }
	    return false;
	}
    }

    private final class TableCaptionExtractor extends AbstractCaptionExtractor {
	@Override
	protected FieldIdentifier getIdentifier() {
	    return FieldIdentifier.TABLENUMBER;
	}

	@Override
	public int detectCaption() {
	    this.initialParagraphsToSkip = 0 + CaptionReader.this.emptyParagraphSkipOffset;
	    final int output;
	    
	    if (this.numberText != null && !CaptionReader.this.captionParagraph.isInTable()) {		
		// found a table caption				
		this.hrManagerResult.addTable(this.numberText);
		CaptionReader.this.resultingRequirement.getMetadata().setKind(Kind.TABLE);
		output = writeCaptionFound();
	    }
	    else {
		// no caption found
		this.hrManagerResult.addTable(null);
		output = writeCaptionNotFound();
	    }
	    assert output >= 0;
	    return output;
	}
    }

    private final class FigureCaptionExtractor extends AbstractCaptionExtractor {
	@Override
	protected FieldIdentifier getIdentifier() {
	    return FieldIdentifier.FIGURENUMBER;
	}
	
	@Override
	public int detectCaption() {
	    this.initialParagraphsToSkip = 0 + CaptionReader.this.emptyParagraphSkipOffset; // the figure spans exactly one paragraph if there is also a caption (i.e. it is somewhat of a "pseudo"-float)
	    final int output;	    

	    if (this.numberText != null) {
		// found a figure caption
		this.hrManagerResult.addFigure(this.numberText);
		CaptionReader.this.resultingRequirement.getMetadata().setKind(Kind.FIGURE);
		output = writeCaptionFound();
	    }
	    else {
		// no caption found
		output = -1; // -1 indicates we should fallback to ordinary inline image detection
	    }
	    assert output >= -1;
	    return output;
	}
    }
}
