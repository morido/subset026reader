package helper;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various methods to deal with regex extraction
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public enum RegexHelper {
    ;

    /**
     * Extract a single substring given by a regex pattern from a base string 
     * 
     * @param input A string from which to extract data
     * @param regex regex which <em>must</em> contain exactly one group in parentheses
     * @return string which corresponds to the {@code regex} or {@code null} if no match was found
     * @throws IllegalArgumentException One of the input parameters is {@code null}
     */
    public static String extractRegex(final String input, final String regex) {	
	final String[] output = extractRegex(input, regex, 1);
	return output != null ? output[0] : null;
    }

    /**
     * Extract {@code n} substrings given by a regex pattern from a base string
     * 
     * @param input A string from which to extract data
     * @param regex regex which <em>must</em> contain exactly {@code n} groups in parentheses
     * @param numGroups the number {@code n}
     * @return A string array of length {@code n} with all the requested substrings, or {@code null} if no matches of the requested {@code numGroups} were found
     * @throws IllegalArgumentException If one of the input parameters is malformed.
     */
    public static String[] extractRegex(final String input, final String regex, final int numGroups) {
	if (input == null) throw new IllegalArgumentException("input cannot be null.");
	if (regex == null) throw new IllegalArgumentException("regex cannot be null.");
	if (numGroups < 1) throw new IllegalArgumentException("numGroups must be at least 1.");
	
	final Pattern pattern = Pattern.compile(regex);
	final Matcher matcher = pattern.matcher(input);
	
	final String[] output;
	if (matcher.find() && matcher.groupCount() == numGroups) {
	    output = new String[numGroups];
	    for (int i = 0; i < output.length; i++) {
		// matcher.group(0) is the entire string which we dont care about
		output[i] = matcher.group(i+1);
	    }
	}
	else output = null;
	
	return output;
    }
    
    
    /**
     * Extract a number from a string
     * 
     * @param input a string containing an integer
     * @return the first integer contained in a string <em>or</em> {@code null} if no integer was found
     * @throws IllegalArgumentException If the input is {@code null}.
     */
    public static Integer extractNumber(final String input) {
	if (input == null) throw new IllegalArgumentException("Input cannot be null.");

	final String output;
	if ((output = extractRegex(input, "(^[0-9]+)")) != null) {
	    return Integer.parseInt(output);
	}
	return null;
    }
    
    /**
     * Wrapper for {@link Pattern#quote(String)}
     * 
     * @param input The string to be quoted
     * @return A quoted string
     */
    public static String quoteRegex(final String input) {
	if (input == null) throw new IllegalArgumentException("Input cannot be null.");
	return Pattern.quote(input);
    }
    
    
    /**
     * Wrapper for {@link Pattern#quote(String)}.
     * 
     * @see RegexHelper#quoteRegex(String)
     * @param input Single character to be quoted
     * @return A quoted string
     */
    public static String quoteRegex(final char input) {
	final String inputAsString = Character.toString(input);
	return RegexHelper.quoteRegex(inputAsString);
    }
    
    /**
     * Construct a pattern to match a string against a list of words / phrases and literary patterns
     * 
     * @param literalPatterns a list of patterns (regexes) which shall be matched literally (i.e. without checks for word boundaries)
     * @param words array of words / phrases to match against
     * @param additionalSeparatorChars optional array of additional valid separators, must be correctly escaped; only single chars are allowed
     * @return a pattern to match a string against the occurrence of any given word / phrase; contains exactly one group with all the input
     * @throws IllegalArgumentException if one of the arguments is malformed
     */
    public static Pattern createWordPatternWLiterals(final String[] literalPatterns, final String[] words, final String... additionalSeparatorChars) {
	if (literalPatterns == null) throw new IllegalArgumentException("literalPhrases cannot be null. Pass an empty array instead.");
	if (words == null || words.length == 0) throw new IllegalArgumentException("words cannot be empty or null.");
	final StringBuilder compilationRegex = new StringBuilder();
	compilationRegex.append('('); // start matching group 1
	String regexDelimiter = "";
	for (final String literalPattern: literalPatterns) {
	    compilationRegex.append(regexDelimiter).append(literalPattern);
	    regexDelimiter = "|";
	}
	compilationRegex.append(regexDelimiter).append("(?i:");  // start word group; shall be case-insensitive		
	compilationRegex.append(getLeadingPhraseBoundaryRegex(additionalSeparatorChars)).append("(?:"); // may not be preceded by other characters (i.e. be a substring of another word)
	regexDelimiter = "";
	for (final String word : words) {
	    compilationRegex.append(regexDelimiter).append(word);
	    regexDelimiter = "|";
	}
	compilationRegex.append(')').append(getTrailingPhraseBoundaryRegex(additionalSeparatorChars)); // may not be suffixed by other characters (i.e. be a substring of another word)
	compilationRegex.append(')').append(')'); // end matching group 1
	return Pattern.compile(compilationRegex.toString());
    }
    
    /**
     * Construct a pattern to match a string against a list of words / phrases
     * 
     * @param words array of words / phrases to match against
     * @param additionalSeparatorChars optional array of additional valid separators, must be correctly escaped; only single chars are allowed
     * @return a pattern to match a string against the occurrence of any given word / phrase; contains exactly one group with all the input
     * @throws IllegalArgumentException if one of the arguments is malformed
     */
    public static Pattern createWordPattern(final String[] words, final String... additionalSeparatorChars) {
	return createWordPatternWLiterals(new String[]{}, words, additionalSeparatorChars);
    }
    
    /**
     * @param additionalSeparatorChars optional array of additional valid separators, must be correctly escaped; only single chars are allowed
     * 
     * @return a regex chunk which may be inserted to match (0-width) beginnings of phrases (i.e. an improved version of {@code \b})
     */
    public static String getLeadingPhraseBoundaryRegex(final String... additionalSeparatorChars) {
	final StringBuilder output = new StringBuilder();
	output.append("(?<=^|\\s|[\\(");	
	for (final String separatorchar : additionalSeparatorChars) {
	    output.append(separatorchar);  
	}
	output.append(']').append(')');
	
	return output.toString();
    }
    
    /**
     * @param additionalSeparatorChars optional array of additional valid separators, must be correctly escaped; only single chars are allowed
     * 
     * @return a regex chunk which may be inserted to match (0-width) endings of phrases (i.e. an improved version of {@code \b})
     */
    public static String getTrailingPhraseBoundaryRegex(final String... additionalSeparatorChars) {
	final StringBuilder output = new StringBuilder();
	output.append("(?=[\\.,;:\\)");	
	for (final String separatorchar : additionalSeparatorChars) {
	    output.append(separatorchar);
	}	
	output.append("]|\\s|$)");
	
	return output.toString();	
    }
}
