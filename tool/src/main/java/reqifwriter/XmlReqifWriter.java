package reqifwriter;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import helper.XmlStringWriter;

class XmlReqifWriter extends XmlStringWriter {
    private final transient DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	
    private static final Logger logger = Logger.getLogger(XmlReqifWriter.class.getName()); // NOPMD - Reference rather than a static field
    
    public void writeStartDocument() {
	try {
	    this.xmlwriter.writeStartDocument("UTF-8", "1.0");
	} catch (XMLStreamException e) {
	    logger.log(Level.SEVERE, "Cannot write XML preamble.", e);
	    throw new IllegalStateException(e);
	}
    }
    
    public String writeIdentifier(final String input) {
	assert input != null && input.length() >= 1;
	final String output = sanitizeForIdentifier(input);
	super.writeAttribute("IDENTIFIER", output);
	return output;
    }
    
    public void writeLastChange() {
	super.writeAttribute("LAST-CHANGE", getCurrentTime());
    }    
    
    /**
     * @return the current time as an {@code xsd:dateTime}-formatted string
     */
    public String getCurrentTime() {		
	return this.dateFormat.format(new Date()).replaceFirst("([0-9]{2})$", ":$1");
    }
    
    /**
     * Write the current state of this writer to a stream
     * <p><em>Note:</em> In contrast to {@link #toString()} this works even if not all tags were properly closed.</p>
     * 
     * @param destinationStream the stream where the contents of this writer shall be written
     * @return the number of bytes written to the stream
     * @throws IllegalArgumentException if the destinationStream is {@code null}
     * @throws IllegalStateException if this writer could not be closed or writing to the destinationStream failed
     */
    @SuppressWarnings("javadoc")
    public int writeToStream(final OutputStream destinationStream) {
	if (destinationStream == null) throw new IllegalArgumentException("destinationStream cannot be null.");
	int output = 0;
	
	try {
	    this.xmlwriter.close();
	} catch (XMLStreamException e) {
	    logger.log(Level.SEVERE, "Cannot close XML stream.", e);
	    throw new IllegalStateException(e);
	}

	try {
	    this.byteArrayOutput.writeTo(destinationStream);
	    output = this.byteArrayOutput.size();
	} catch (IOException e) {
	    logger.log(Level.SEVERE, "Cannot write to destinationStream.", e);
	    throw new IllegalStateException(e);
	}
	return output;
    }
    
    /**
     * Make an input string compliant to {@code xsd:ID}
     * 
     * @param input the input string
     * @return an {@code xsd:ID} compliant version of the input; never {@code null}
     */
    public static String sanitizeForIdentifier(final String input) {
	assert input.length() >= 1;
	final StringBuilder output = new StringBuilder();	
	
	if (input.charAt(0) != '_') {
	    output.append('_');
	}
	
	String inputTmp;
	inputTmp = input.replaceAll("\\*", "_Star_");
	inputTmp = inputTmp.replaceAll("\\[", "_BrLeft_");
	inputTmp = inputTmp.replaceAll("\\]", "_BrRight_");
	inputTmp = inputTmp.replaceAll("[^\\w\\-\\.]", "-");
	output.append(inputTmp);
	
	return output.toString();
    }    
}
