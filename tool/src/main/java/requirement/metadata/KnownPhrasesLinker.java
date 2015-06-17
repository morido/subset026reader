package requirement.metadata;

import helper.RegexHelper;
import helper.RequirementHelper;
import helper.annotations.DomainSpecific;
import helper.formatting.textannotation.AnnotationBuilder;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static helper.Constants.Specification.SpecialConstructs.DETECT_KNOWNPHRASES;
import requirement.RequirementTemporary;

/**
 * Global class to store links between first mentions of defining terms / phrases and their respective requirement
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 */
public class KnownPhrasesLinker {

    @DomainSpecific
    // this is similar to the annotation detection for ENTITIES in helper.subset26.MetadataDeterminer
    private static Pattern QUOTED_PHRASE = Pattern.compile(RegexHelper.getLeadingPhraseBoundaryRegex("\\/") + "[“”\"]([^“”\"]+)[“”\"]" + RegexHelper.getTrailingPhraseBoundaryRegex("\\/"));
    
    // key is the phrase in all lower case; keys are sorted by length
    private Map<String, RequirementTemporary> phraseToRequirementMapper = new TreeMap<>(
	    new Comparator<String>() {
		@Override
		public int compare(final String o1, final String o2) {
		    // fallback to a lexical comparison if lengths are equal; since we do not allow equal Strings this method should never return 0
		    return o1.length() != o2.length() ? o2.length()-o1.length() : o1.compareTo(o2);		    
		}
	    });


    /**
     * checks if a range is contained within another
     */
    private static class RangeChecker {
	private static class Range implements Comparable<Range>{
	    private final int begin;
	    private final int end;

	    private Range (int begin, int end) {
		this.begin = begin;
		this.end = end;
	    }

	    /**
	     * Sorts by begin offset; if equal sorts by length (ascending)
	     * 
	     * @see java.lang.Comparable#compareTo(java.lang.Object)
	     */
	    @Override
	    public int compareTo(final Range o) {
		final int output;
		if (this.begin == o.begin) output = this.end != o.end ? this.end-o.end : -1;		
		else output = this.begin-o.begin;

		return output;
	    }	    	   
	}
	private final Set<Range> ranges = new TreeSet<>();	

	/**
	 * Checks if this range must be considered a child of another range; if not it adds this to the store for later comparison (side-effect)
	 * 
	 * @param begin begin offset of the range
	 * @param end end offset of the range
	 * @return {@code true} if this range is a subset of another range; {@code false} otherwise
	 */
	private boolean isChildRange(final int begin, final int end) {
	    for (final Range currentRange : this.ranges) {
		if (currentRange.begin <= begin && currentRange.end >= end) {
		    return true;
		}
	    }
	    // is not a child of anyone; add to the store
	    this.ranges.add(new Range(begin, end));
	    return false;
	}
    }


    /**
     * Processes a given requirement; extracts all phrases which may become referenced later and checks if this requirement contains any known phrases
     * 
     * @param requirement requirement to process
     * @throws IllegalArgumentException if the given parameter is {@code null}
     */
    public void processRequirement(final RequirementTemporary requirement) {
	if (requirement == null) throw new IllegalArgumentException("requirement cannot be null.");
	if (!DETECT_KNOWNPHRASES) return; // user requested not to detect known phrases

	if (RequirementHelper.isRooted(requirement) && requirement.getText() != null && requirement.getText().getRaw() != null) {
	    final String textToProcess = requirement.getText().getRaw();

	    // Step 1: see if we have any defining phrases
	    final Set<String> phrases = new HashSet<>();
	    final Matcher quotedMatcher = QUOTED_PHRASE.matcher(textToProcess);
	    while(quotedMatcher.find()) {		
		final String rawMatch = quotedMatcher.group(1);
		// skip single words or negated stuff like "not active"
		final String[] rawMatchWords = rawMatch.split(" ");
		if (rawMatchWords.length > 2 || (rawMatchWords.length == 2 && !rawMatchWords[0].matches("not?"))) {
		    enhancePhrase(rawMatch, phrases);
		}		
	    }	    


	    // Step 2: match all previously stored phrases against this requirement
	    final RangeChecker rangeChecker = new RangeChecker();
	    for(final Entry<String, RequirementTemporary> currentPhrase : this.phraseToRequirementMapper.entrySet()) {
		final Pattern searchPattern = Pattern.compile(RegexHelper.quoteRegex(currentPhrase.getKey())); // we do not precompile these because patterns cant be easly compared by length for equality		
		final Matcher matcherStep2 = searchPattern.matcher(textToProcess.toLowerCase(Locale.ENGLISH));
		while (matcherStep2.find()) {
		    if (!rangeChecker.isChildRange(matcherStep2.start(), matcherStep2.end())) {
			// it is ok to link the same targetOffset several times (which may happen if a phrase occurs more than once per requirement
			final int targetOffset = currentPhrase.getValue().getAssociatedRange().getStartOffset();
			requirement.getRequirementKnownTermLinks().addLinkToExternalStartOffset(targetOffset);
			// mark the phrase in the implementerEnhanced field
			requirement.getMetadata().getTextAnnotator().addAnnotation(matcherStep2.start(), matcherStep2.end(), AnnotationBuilder.LINKED_PHRASE.getAnnotator());
		    }
		}
	    }

	    // Step 3: add the defining phrases from this run to the global store for the next requirements
	    for (final String currentPhrase : phrases) addPhraseToGlobalStore(currentPhrase, requirement);
	}	
    }


    /**
     * @param phrase definition phrase
     * @param requirement requirement where this phrase was seen
     */
    @DomainSpecific
    private void addPhraseToGlobalStore(final String phrase, final RequirementTemporary requirement) {
	assert phrase != null && requirement != null;

	String normalizedPhrase;
	// Strip any plural endings for known entities; very domain specific...
	// Note: making the string shorter is ok here; any other changes would break the matching in step 1 above
	final Pattern pattern = Pattern.compile("^(.*[A-Z])(?:s|\\(s\\))$");
	final Matcher matcher = pattern.matcher(phrase);
	if (matcher.matches()) normalizedPhrase = matcher.group(1);	
	else normalizedPhrase = phrase;	
	normalizedPhrase = normalizedPhrase.toLowerCase(Locale.ENGLISH);

	// do not overwrite if already present (i.e. seen earlier)
	if (!this.phraseToRequirementMapper.containsKey(normalizedPhrase)) {
	    this.phraseToRequirementMapper.put(normalizedPhrase, requirement);
	}	
    }

    @DomainSpecific
    private static void enhancePhrase(final String phrase, final Set<String> output) {
	assert phrase != null && output != null;
	final String cleanedPhrase = phrase.trim();
	final String[] words = cleanedPhrase.split(" ");

	// Disambiguate phrases with alternatives
	// i.e. Buy me a drink/banana/car will become
	// Buy me a drink, Buy me a banana, Buy me a car	
	Set<String> possibleInterpretations = new HashSet<>();
	possibleInterpretations.add(""); // pseudo element for 1st iteration
	String delimiter = "";
	boolean originalVersionNecessary = false; // if we have alternatives then preserve the original version (with the slashes) as well
	for (int i = 0; i < words.length; i++) {
	    final String[] alternatives = words[i].split("\\/"); // split at slashes
	    if (alternatives.length > 1) originalVersionNecessary = true;

	    final Set<String> possibleInterpretationsCurrent = new HashSet<>();
	    for (final String alternative : alternatives) {
		for (final String currentInterpretation : possibleInterpretations) {
		    possibleInterpretationsCurrent.add(currentInterpretation + delimiter + alternative);
		}
	    }
	    possibleInterpretations = possibleInterpretationsCurrent;
	    delimiter = " ";
	}

	// output
	for (final String currentInterpretation : possibleInterpretations) {
	    output.add(currentInterpretation);
	}
	if (originalVersionNecessary) output.add(cleanedPhrase);
    }
}
