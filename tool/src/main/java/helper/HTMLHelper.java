package helper;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;


/**
 * Miscellaneous HTML / XML related helper functions
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 */
public enum HTMLHelper {
    ;	

    /**
     * @return HTML tag to use for bold formatting
     */
    public static String getBold() {
	return "b";
	//return "strong";
    }

    /**
     * @return HTML tag to use for italic formatting
     */
    public static String getItalic() {
	return "i";
	//return "em";
    }

    /**
     * Makes sure that all characters in the given filename are correctly escaped to create a valid URI (compliant with {@code anyURI})
     * 
     * @param filename unsanitized URI in the form {@code media/file.png} (i.e. a non-fully qualified path only)
     * @return sanitized URI
     * @throws IllegalStateException if the given filename is not parseable
     */
    public static String sanitizeUri(final String filename) {
	// Step 1: create valid URI
	final String splitChar = "/"; // this is always a forward-slash irrespective of the OS
	final String[] components = filename.split(splitChar);
	final StringBuilder uriBuilder = new StringBuilder(filename.length());

	String splitCharLoop = ""; // prepends this only for iterations n>0
	for (int i = 0; i < components.length; i++) {
	    uriBuilder.append(splitCharLoop);
	    try {
		uriBuilder.append(URLEncoder.encode(components[i], "UTF-8"));
	    } catch (UnsupportedEncodingException e) {
		throw new IllegalStateException("Encoding error.", e);
	    }
	    splitCharLoop = splitChar;
	}	

	// Step 2: Validate against RFC 2396
	final URI uri;
	try {	    
	    uri = new URI(uriBuilder.toString());
	} catch (URISyntaxException e) {
	    throw new IllegalStateException("Internal error while trying to construct a valid URI for an externally referenced object.", e);
	}
	return uri.toASCIIString();
    }

}
