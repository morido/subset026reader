package helper.subset26;

import static helper.Constants.MSWord.STYLENAME_HEADING;
import static helper.Constants.MSWord.WORDS_MAX_HEADING;
import static helper.Constants.Specification.SpecialConstructs.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import helper.RegexHelper;
import helper.annotations.DomainSpecific;
import helper.formatting.textannotation.AnnotationBuilder;
import helper.poi.PoiHelpers;
import helper.subset26.LegalObligationDeterminer.StopwordTuple;

import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;

import docreader.ReaderData;
import requirement.RequirementTemporary;
import requirement.metadata.Kind;
import requirement.metadata.LegalObligation;
import requirement.metadata.MetadataReqif;
import requirement.metadata.TextAnnotator;

/**
 * Determine metadata (i.e. reqif-fields) of requirements based on their textual contents, class is reusable
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class MetadataDeterminer {
    private final ReaderData readerData;
    
    private final static class PatternToAnnotator {
	final Pattern stopwordPattern;
	final Pattern stopwordExceptionPattern;
	final AnnotationBuilder annotationBuilder;
	
	PatternToAnnotator(final Pattern pattern, final Pattern exceptionPattern, final AnnotationBuilder annotationBuilder) {
	    assert pattern != null && exceptionPattern != null && annotationBuilder != null;
	    this.stopwordPattern = pattern;
	    this.stopwordExceptionPattern = exceptionPattern;
	    this.annotationBuilder = annotationBuilder;
	}
	
	PatternToAnnotator(final Pattern pattern, final AnnotationBuilder annotationBuilder) {
	    this.stopwordPattern = pattern;
	    this.stopwordExceptionPattern = null;
	    this.annotationBuilder = annotationBuilder;
	}
    }
    
    private final static PatternToAnnotator PA_WEAKWORD;
    private final static PatternToAnnotator PA_CONDITION;
    private final static PatternToAnnotator PA_LOOP;
    private final static PatternToAnnotator PA_TIME;
    private final static PatternToAnnotator PA_AGAIN;
    private final static PatternToAnnotator PA_EXTERNAL;
    private final static PatternToAnnotator PA_SELF;
    private final static PatternToAnnotator PA_ENTITY;
    private final static PatternToAnnotator PA_EMBRACED;
        
    private final static PatternToAnnotator PA_NOTE;
    private final static PatternToAnnotator PA_EXAMPLE;
    private final static PatternToAnnotator PA_DELETED;
    private final static PatternToAnnotator PA_JUSTIFICATION;
    private final static PatternToAnnotator PA_EXCEPTION;    

    static {
	// Step 1: user-configurable patterns
	{
	    final String[] compilations = new String[WEAKWORDS_LITERATURE.length + WEAKWORDS_SPECIFICATION.length];
	    int i = 0;
	    for (final String stopword : WEAKWORDS_LITERATURE) compilations[i++] = stopword;
	    for (final String stopword : WEAKWORDS_SPECIFICATION) compilations[i++] = stopword;
	    
	    PA_WEAKWORD = new PatternToAnnotator(RegexHelper.createWordPatternWLiterals(WEAK_NOWORD, compilations), AnnotationBuilder.WEAKWORD);
	}
	{
	    PA_CONDITION = new PatternToAnnotator(RegexHelper.createWordPattern(CONDITION), AnnotationBuilder.CONDITION);
	    PA_LOOP = new PatternToAnnotator(RegexHelper.createWordPattern(LOOP), AnnotationBuilder.LOOP);
	    PA_TIME = new PatternToAnnotator(RegexHelper.createWordPattern(TIME), AnnotationBuilder.TIME);
	    PA_AGAIN = new PatternToAnnotator(RegexHelper.createWordPattern(AGAIN, "\\/"), AnnotationBuilder.AGAIN);
	    PA_EXTERNAL = new PatternToAnnotator(RegexHelper.createWordPattern(EXTERNAL, "\\/“”\""), AnnotationBuilder.EXTERNAL_ENTITY);
	    PA_SELF = new PatternToAnnotator(RegexHelper.createWordPattern(SELF), AnnotationBuilder.SELF_REFERENCE);	    
	}
	
	// Step 2: non user-configurable patterns
	{	    
	    final Pattern entityPattern = Pattern.compile(RegexHelper.getLeadingPhraseBoundaryRegex("\\/") + "((?:[“”\"][^“”\"]+[“”\"]|[A-Z](?:\\w-?)*[A-Z](?:s|\\(s\\))?|(?:[A-Za-z]+_)+[A-Za-z]+))" + RegexHelper.getTrailingPhraseBoundaryRegex("\\/"));
	    final Pattern entityExceptionPattern = RegexHelper.createWordPattern(new String[]{"OR", "AND", "SRS", "MIN", "MAX", "BEGIN", "END"});
	    PA_ENTITY = new PatternToAnnotator(entityPattern, entityExceptionPattern, AnnotationBuilder.ENTITY.setName("Named Entity"));
	    
	    // Note: java does not support recursive patterns. Hence, this is limited to one nesting level of braces (second non-matching group from the left)
	    final Pattern embracedPattern = Pattern.compile(RegexHelper.getLeadingPhraseBoundaryRegex() + "(\\((?>(?: [a-z]\\)|[^()])|(?:\\((?>(?: [a-z]\\)|[^()]))*\\)))*\\))" + RegexHelper.getTrailingPhraseBoundaryRegex());
	    PA_EMBRACED = new PatternToAnnotator(embracedPattern, AnnotationBuilder.NO_IMPORTANCE.setName("Embraced"));
	}	
	{
	    final String optionalNumberPrepender = "^(?:(?:\\{[0-9]+\\}|\\[[0-9]+\\])\\s?)?"; // matches [1] or {13}, but not {60]	    	   
	    PA_NOTE = new PatternToAnnotator(Pattern.compile(optionalNumberPrepender + "(Note(?:\\s(?:[0-9]+|regarding [a-zA-Z0-9]+\\)?))?:).*"), AnnotationBuilder.NO_IMPORTANCE.setName("NoteIdentifier"));	    	    
	    PA_EXAMPLE = new PatternToAnnotator(Pattern.compile(optionalNumberPrepender + "(Example\\s?[0-9]*:).*"), AnnotationBuilder.NO_IMPORTANCE.setName("ExampleIdentifier"));
	    PA_DELETED = new PatternToAnnotator(Pattern.compile("^((?:(?:(?:Figure|Table).*:\\s*)?(?:Deleted|Intentionally (?:deleted|moved))|Void)\\s?\\.?)$"), AnnotationBuilder.NO_IMPORTANCE.setName("DeletedIdentifier"));
	    PA_JUSTIFICATION = new PatternToAnnotator(Pattern.compile(optionalNumberPrepender + "(Justification(?: for [a-zA-Z0-9]\\)?)?\\s?:).*"), AnnotationBuilder.NO_IMPORTANCE.setName("JustificationIdentifier"));
	    PA_EXCEPTION = new PatternToAnnotator(Pattern.compile(optionalNumberPrepender + "((?:Exception(?: (?:to|for) .*)?|Regarding .*):).*"), AnnotationBuilder.NO_IMPORTANCE.setName("ExceptionIdentifier"));	    
	}
    }      

    /**
     * @param readerData global readerData
     * @throws IllegalArgumentException if any of the arguments is {@code null}
     */
    public MetadataDeterminer(final ReaderData readerData) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	this.readerData = readerData;
    }


    /**
     * @param requirement source for requirement and associated range
     * @throws IllegalArgumentException if one of the arguments is {@code null}
     */
    public void processRequirement(final RequirementTemporary requirement) {
	if (requirement == null) throw new IllegalArgumentException("requirement cannot be null");
	final String rawTextualContent = requirement.getText().getRaw();
	if (rawTextualContent == null) return; // TODO does this ever trigger?
	
	final MetadataReqif metadata = requirement.getMetadata();
	// inject the textual contents
	metadata.injectText(rawTextualContent);

	final Kind requirementKind = determineRequirementKind(requirement.getAssociatedRange(), rawTextualContent, metadata.getTextAnnotator());	
	final LegalObligationDeterminer legalObligationDeterminer = determineLegalObligation(rawTextualContent, requirementKind, metadata.getTextAnnotator());
	final boolean atomicity = determineAtomicity(rawTextualContent, requirementKind, legalObligationDeterminer);
	if (requirementKind != Kind.PLACEHOLDER) {
	    final TextAnnotator outputAnnotator = metadata.getTextAnnotator();
	    
	    // it does not make sense to parallelize this as the synchronization overhead will effectively make it slower
	    matchStopwords(PA_WEAKWORD, rawTextualContent, outputAnnotator);
	    matchStopwords(PA_CONDITION, rawTextualContent, outputAnnotator);
	    matchStopwords(PA_LOOP, rawTextualContent, outputAnnotator);
	    matchStopwords(PA_EMBRACED, rawTextualContent, outputAnnotator);
	    matchStopwords(PA_TIME, rawTextualContent, outputAnnotator);
	    matchStopwords(PA_AGAIN, rawTextualContent, outputAnnotator);
	    matchStopwords(PA_EXTERNAL, rawTextualContent, outputAnnotator);
	    matchStopwords(PA_SELF, rawTextualContent, outputAnnotator);
	    matchStopwords(PA_ENTITY, rawTextualContent, outputAnnotator);	    
	}
	metadata.setKind(requirementKind);
	metadata.setLegalObligation(legalObligationDeterminer.getLegalObligation());
	metadata.setAtomic(atomicity);
	
	// process known text mentions
	this.readerData.getKnownPhrasesLinker().processRequirement(requirement);
    }

    /**
     * Determine the most probable content type of a textual requirement based on a regex
     * 
     * @param range associated range of the requirement
     * @param rawTextualContent plain text to analyze
     * @return the type of the requirement, never {@code null}
     */
    @DomainSpecific
    private Kind determineRequirementKind(final Range range, final String rawTextualContent, final TextAnnotator textAnnotator) {
	assert range != null && rawTextualContent != null;
	// TODO needs proper test

	final Kind output;	
	Matcher matcher;

	ifCases:
	    if ((matcher = PA_NOTE.stopwordPattern.matcher(rawTextualContent)) != null && matcher.matches()) {
		output = Kind.NOTE;
		textAnnotator.addAnnotation(matcher.start(1), matcher.end(1), PA_NOTE.annotationBuilder.getAnnotator());
	    }
	    else if ((matcher = PA_EXAMPLE.stopwordPattern.matcher(rawTextualContent)) != null && matcher.matches()) {
		output = Kind.EXAMPLE;
		textAnnotator.addAnnotation(matcher.start(1), matcher.end(1), PA_EXAMPLE.annotationBuilder.getAnnotator());
	    }
	    else if ((matcher = PA_DELETED.stopwordPattern.matcher(rawTextualContent)) != null && matcher.matches()) {
		output = Kind.PLACEHOLDER;
		textAnnotator.addAnnotation(matcher.start(1), matcher.end(1), PA_DELETED.annotationBuilder.getAnnotator());
	    }
	    else if ((matcher = PA_JUSTIFICATION.stopwordPattern.matcher(rawTextualContent)) != null && matcher.matches()) {
		output = Kind.JUSTIFICATION;
		textAnnotator.addAnnotation(matcher.start(1), matcher.end(1), PA_JUSTIFICATION.annotationBuilder.getAnnotator());
	    }
	    else if ((matcher = PA_EXCEPTION.stopwordPattern.matcher(rawTextualContent)) != null && matcher.matches()) {
		output = Kind.ORDINARY;
		textAnnotator.addAnnotation(matcher.start(1), matcher.end(1), PA_EXCEPTION.annotationBuilder.getAnnotator());
	    }	
	    else if (isDefinition(rawTextualContent)) {
		output = Kind.DEFINITION;
	    }
	    else {
		// check for possible heading
		// TODO this is a quick hack - the assumption that this.range is what we are actually interested in may not always hold	
		// Idea: only do this if shall/may are not contained in string? I.e. stopwordsCount == 0
		headingChecker:
		    if (range.numParagraphs() == 1) {
			final Paragraph headingCandidate = range.getParagraph(0);		
			int boldCounter = 0;
			int smallCapsCounter = 0;
			// TODO refactor the bold/small-caps detection
			for (int i = 0; i < headingCandidate.numCharacterRuns(); i++) {
			    final CharacterRun currentRun = headingCandidate.getCharacterRun(i);
			    if (currentRun.isBold()) boldCounter++;		    
			    else if (currentRun.isSmallCaps()) smallCapsCounter++;		   
			    else break headingChecker;		    
			}
			if (boldCounter == headingCandidate.numCharacterRuns() || smallCapsCounter == headingCandidate.numCharacterRuns()) {
			    final char[] disallowedChars = { '.', '?', '!' };
			    for (final char currentchar : disallowedChars) {
				if (rawTextualContent.contains(Character.toString(currentchar))) {
				    break headingChecker;
				}
			    }
			}
			// checking for the outline level does not make sense here since it is completely messed up in the input documents
			// (i.e. there are headings on olvl == 9)				
			final String headingCandidateStyleName = PoiHelpers.getStyleName(this.readerData, headingCandidate.getStyleIndex());
			styleNameChecker: {
			    for (final String currentStyleName : STYLENAME_HEADING) {
				if (headingCandidateStyleName.contains(currentStyleName)) {
				    // all good; found a matching name
				    break styleNameChecker;		    		    
				}
			    }
			    // still alive; headingCandidateStyleName does not contain any heading stopwords
			    break headingChecker;
			}
			final int numOfWords = rawTextualContent.split(" ").length;
			if (numOfWords > WORDS_MAX_HEADING) {
			    break headingChecker;
			}
			output = Kind.HEADING;
			break ifCases;
		    }
	output = Kind.ORDINARY;
	    }

	return output;
    }


    /**
     * @param rawTextualContent plain text to be analyzed
     * @param requirementKind kind of the requirement in question
     * @param legalObligationDeterminer LegalObligation of the requirement in question
     * @return {@code true} if this requirement may be considered atomic; {@code false} otherwise
     */
    @DomainSpecific
    private static boolean determineAtomicity(final String rawTextualContent, final Kind requirementKind, final LegalObligationDeterminer legalObligationDeterminer) {
	assert rawTextualContent != null && requirementKind != null && legalObligationDeterminer != null;	
	final boolean output;

	if (requirementKind == Kind.ORDINARY || requirementKind == Kind.DEFINITION) {
	    final LegalObligation legalObligation = legalObligationDeterminer.getLegalObligation();
	    if (legalObligation != LegalObligation.NA && legalObligation != LegalObligation.MIXED) {
		if (legalObligationDeterminer.getStopwordCount() <= 1) {
		    final int numberOfSentences = rawTextualContent.split("(\\.[ ][A-Z]|\\;[ ][a-zA-Z])").length;
		    if (numberOfSentences == 1) output = true;
		    else output = false;
		}
		// stopWordCount too high
		else output = false;
	    }
	    // legalObligation does not qualify
	    else output = false;
	}
	// requirementKind does not qualify
	else output = false;

	return output;
    }


    private static LegalObligationDeterminer determineLegalObligation(final String rawTextualContent, final Kind requirementKind, final TextAnnotator textAnnotator) {
	assert rawTextualContent != null && requirementKind != null;
	final LegalObligationDeterminer legalObligationDeterminer = new LegalObligationDeterminer(rawTextualContent, requirementKind);
	legalObligationDeterminer.process();

	for (final StopwordTuple currentOffset : legalObligationDeterminer.getStopwordOffsets()) {	    
	    textAnnotator.addAnnotation(currentOffset.getStartOffset(), currentOffset.getEndOffset(), currentOffset.isReal() ? AnnotationBuilder.LEGALOBLIGATION.getAnnotator() : AnnotationBuilder.LEGALOBLIGATION_UNKNOWN.getAnnotator());
	}

	return legalObligationDeterminer;
    }

    /**
     * Match a string against a list of stopwords and write each occurence into an outputAnnotator
     * 
     * @param patternAnnotator 
     * @param inputString string which the stopwords shall be matched again
     * @param outputAnnotator Annotator where the output will be written
     */
    private static void matchStopwords(final PatternToAnnotator patternAnnotator, final String inputString, final TextAnnotator outputAnnotator) {
	assert patternAnnotator != null && inputString != null && outputAnnotator != null;

	final Matcher stopwordMatcher = patternAnnotator.stopwordPattern.matcher(inputString);	
	while(stopwordMatcher.find()) {
	    if (patternAnnotator.stopwordExceptionPattern != null) {
		final Matcher stopwordExceptionMatcher = patternAnnotator.stopwordExceptionPattern.matcher(stopwordMatcher.group(1));
		if (stopwordExceptionMatcher.matches()) continue; // proceed with the next word in the inputString
	    }
	    outputAnnotator.addAnnotation(stopwordMatcher.start(1), stopwordMatcher.end(1), patternAnnotator.annotationBuilder.getAnnotator());
	}
    }

    @DomainSpecific
    private static boolean isDefinition(final String rawTextualContent) {
	assert rawTextualContent != null;

	final String[] patterns = {
		".+ (((is|are)( not)?)|(shall|must) (not )?be) (defined|interpreted|regarded|reported) as .+",		
		"^Definitions.+are given in .*",
		".* (describes|defines) .*", // note: do not match describe, described or define
		".*[, ]is called a[n ].*",
		".+ by definition .* considered .+",
		".+ marked with [“\"].+[”\"].+",
		"^Definition (of|for) [“\"]?.+[”\"]?: .+",
	};

	for (final String pattern : patterns) {
	    if (rawTextualContent.matches(pattern)) {		
		return true;
	    }
	}
	return false;
    }
}
