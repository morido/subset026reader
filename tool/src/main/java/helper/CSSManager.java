package helper;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * Stores and sorts CSS properties for use in element-styling
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 */
public class CSSManager {	
    /**
     * Internal properties store; will be sorted automatically
     */
    private final NavigableMap<String, String> cssProperties = new TreeMap<>();

    /**
     * Add a new css property
     * 
     * @param name name of the css attribute
     * @param argument argument value
     * @throws IllegalArgumentException if one of the arguments was {@code null} or {@code name} contains a colon
     */
    public void putProperty(final String name, final String argument) {
	if (name == null || argument == null) throw new IllegalArgumentException("Arguments cannot be null");
	if (name.contains(":")) throw new IllegalArgumentException("Name is malformed."); 
	this.cssProperties.put(name, argument);
    }

    /**
     * @return A string containing all stored properties
     */
    @Override
    public String toString() {	
	final StringBuilder cssString = new StringBuilder();
	String delimiter = "";
	for (final Entry<String, String> currentproperty: this.cssProperties.entrySet()) {
	    cssString.append(delimiter);
	    cssString.append(currentproperty.getKey());
	    cssString.append(':');
	    cssString.append(currentproperty.getValue());
	    cssString.append(';');

	    delimiter = " ";
	}

	return cssString.toString();
    }

    /**
     * @return {@code} true if at least one property has been stored; {@code false} otherwise
     */
    public boolean propertiesAvailable() {	
	return !this.cssProperties.isEmpty();
    }

    /**
     * Clear all stored properties (i.e. free the memory)
     */
    public void clear() {
	this.cssProperties.clear();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime
		* result
		+ ((this.cssProperties == null) ? 0 : this.cssProperties
			.hashCode());
	return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final CSSManager other = (CSSManager) obj;
	if (this.cssProperties == null) {
	    if (other.cssProperties != null) {
		return false;
	    }
	} else if (!this.cssProperties.equals(other.cssProperties)) {
	    return false;
	}
	return true;		
    }

    /**
     * Compresses the input of 4-element CSS properties (border, padding etc.) to leave out redundant information
     * <p>Implemented according to the respective <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/Shorthand_properties">Mozilla docs</a>.</p>
     * 
     * @param input A 4-element array with all extracted data
     * @return A compressed version of the input
     * @throws IllegalArgumentException If at least one input element is {@code null} or the dimension of the input is wrong
     */
    public static String getShorthandProperty(final String[] input) {
	if (input.length != 4) throw new IllegalArgumentException("We can only work on 4-element input tuples");
	for (final String element : input) if (element == null) throw new IllegalArgumentException("One of the elements is null.");		

	final String output;
	if (input[0].equals(input[1]) && input[1].equals(input[2]) && input[2].equals(input[3])) {
	    output = input[0];
	}
	else if (input[0].equals(input[2]) && input[1].equals(input[3])) {
	    output = input[0] + " " + input[1];
	}
	else if (input[1].equals(input[3])) {
	    output = input[0] + " " + input[1] + " " + input[2];
	}
	else {
	    output = input[0] + " " + input[1] + " " + input[2] + " " + input[3];
	}
	return output;
    }
    
    /**
     * Create a valid CSS identifier (used for class names, ids, ...) according to the <a href="http://www.w3.org/TR/CSS2/syndata.html#characters">W3C Recommendation on Syntax and basic data types</a>
     * 
     * @param input raw string to be used as the identifier
     * @return a sanitized identifier digestable by CSS
     * @throws IllegalArgumentException if the parameter is {@code null}
     */
    public static String getIdentifier(final String input) {
	if (input == null) throw new IllegalArgumentException("input cannot be null.");
	if (input.length() == 0) throw new IllegalArgumentException("input cannot be an empty string");
	String output = input;
	// [...] cannot start with a digit, or a hyphen followed by a digit [...]
	if (output.matches("^-?[0-9].+")) {
	    output = "_" + output;
	}
	// [...] can contain only the characters [a-zA-Z0-9] and ISO 10646 characters U+00A0 and higher, plus the hyphen (-) and the underscore (_) [...]
	// Note: we do not care about "characters U+00A0 and higher" here
	output = output.replaceAll("[^-_A-Za-z0-9]", "");
		
	return output;
    }
}
