package helper.subset26;

import helper.RegexHelper;
import helper.annotations.DomainSpecific;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import requirement.metadata.Kind;
import requirement.metadata.LegalObligation;
import static helper.Constants.Specification.LEGALOBLIGATION_KEYWORDS_MANDATORY;
import static helper.Constants.Specification.LEGALOBLIGATION_KEYWORDS_OPTIONAL;
import static helper.Constants.Specification.LEGALOBLIGATION_KEYWORDS_UNKNOWN;

class LegalObligationDeterminer {
    private final static Pattern STOPWORDS_OPTIONAL = RegexHelper.createWordPattern(LEGALOBLIGATION_KEYWORDS_OPTIONAL);
    private final static Pattern STOPWORDS_MANDATORY = RegexHelper.createWordPattern(LEGALOBLIGATION_KEYWORDS_MANDATORY); 
    private final static Pattern STOPWORDS_UNKNOWN = RegexHelper.createWordPattern(LEGALOBLIGATION_KEYWORDS_UNKNOWN);
       
    static class StopwordTuple {
	private final int startOffset;
	private final int endOffset;
	private boolean real;

	private StopwordTuple(final int startOffset, final int endOffset, final boolean real) {
	    this.startOffset = startOffset;
	    this.endOffset = endOffset;
	    this.real = real;
	}

	/**
	 * @return start offset; inclusive (0-based)
	 */
	int getStartOffset() {
	    return this.startOffset;
	}

	/**
	 * @return end offset; exclusive (0-based)
	 */
	int getEndOffset() {
	    return this.endOffset;			
	}
	
	/**
	 * @return {@code false} if it was matched by {@link #LEGALOBLIGATION_KEYWORDS_UNKNOWN}, {@code true} otherwise
	 */
	boolean isReal() {
	    return this.real;
	}
    }

    private final String rawTextualContent;
    private final Kind requirementKind;
    private LegalObligation legalObligation;
    private int stopwordCount;
    private final List<StopwordTuple> stopwordOffsets = new ArrayList<>();
    
    public LegalObligationDeterminer(final String rawTextualContent, final Kind requirementKind) {
	assert rawTextualContent != null && requirementKind != null;
	this.rawTextualContent = rawTextualContent;	
	this.requirementKind = requirementKind;
    }
    
    /**
     * Check if this requirement may/must be implemented
     */
    @DomainSpecific
    void process() {		
	final int currentPosInitial = 0;
	int currentPos = currentPosInitial;	
	LegalObligation output = LegalObligation.UNKNOWN;	
	
	{
	    final Matcher matcher = STOPWORDS_OPTIONAL.matcher(this.rawTextualContent);
	    int stopwordPos = -1;
	    while(matcher.find()) {
		this.stopwordCount++;
		this.stopwordOffsets.add(new StopwordTuple(matcher.start(1), matcher.end(1), true));
		stopwordPos = matcher.start(1);

		if (stopwordPos < currentPos || currentPos == currentPosInitial) {
		    output = LegalObligation.OPTIONAL;
		    currentPos = stopwordPos;
		}
	    }
	}
	{
	    final Matcher matcher = STOPWORDS_MANDATORY.matcher(this.rawTextualContent);
	    int stopwordPos = -1;
	    while(matcher.find()) {
		this.stopwordCount++;
		this.stopwordOffsets.add(new StopwordTuple(matcher.start(1), matcher.end(1), true));
		stopwordPos = matcher.start(1);

		if (stopwordPos < currentPos || currentPos == currentPosInitial) {
		    if (output == LegalObligation.OPTIONAL) output = LegalObligation.MIXED;		    
		    else output = LegalObligation.MANDATORY;
		    currentPos = stopwordPos;
		}
	    }
	}
	{
	    final Matcher matcher = STOPWORDS_UNKNOWN.matcher(this.rawTextualContent);	    
	    while(matcher.find()) {		
		this.stopwordOffsets.add(new StopwordTuple(matcher.start(1), matcher.end(1), false));		
	    }
	}

	// Step 2: check for stuff which (by definition) is no requirement
	// if this has been attributed an obligation then leave it; otherwise set to "not applicable" (which should be the default case)
	if (output == LegalObligation.UNKNOWN) {	    	    
	    switch (this.requirementKind) {
	    case EXAMPLE: case NOTE: case JUSTIFICATION: case HEADING: case PLACEHOLDER:
		output = LegalObligation.NA;
		break;
		//$CASES-OMITTED$
	    default:
		break; // intentionally do nothing
	    }
	}
	
	this.legalObligation = output;
    }
    
    LegalObligation getLegalObligation() {
	return this.legalObligation;
    }
    
    int getStopwordCount() {
	return this.stopwordCount;
    }
    
    List<StopwordTuple> getStopwordOffsets() {
	return this.stopwordOffsets;
    }
}