package helper.formatting;


import helper.CSSManager;
import helper.XmlStringWriter;

/**
 * Formatter for the rich text content of a placeholder requirement 
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class RequirementPlaceholderFormatter implements GenericFormatter {
    private final transient XmlStringWriter xmlwriter;
    
    /**
     * Create a new formatter
     */
    public RequirementPlaceholderFormatter() {
	this.xmlwriter = new XmlStringWriter();
    }
    
    /**
     * @param input input text to prettify
     * @return rich text version of the {@code input}, never {@code null}
     */
    public String writeString(final String input) {
	if (input == null) throw new IllegalArgumentException("Input cannot be null.");
	writeStartElement();
	this.xmlwriter.writeCharacters(input);
	writeEndElement();
	
	return this.xmlwriter.toString();
    }
    
    
    @Override    
    public void writeStartElement() {
	this.xmlwriter.writeStartElement("span");
	final CSSManager cssmanager = new CSSManager();
	cssmanager.putProperty("background-color", "#696969");
	cssmanager.putProperty("color", "white");
	cssmanager.putProperty("padding", "0.1em");
	cssmanager.putProperty("font-family", "monospace");
	this.xmlwriter.writeAttribute("style", cssmanager.toString());
    }

    @Override    
    public void writeEndElement() {
	this.xmlwriter.writeEndElement("span");
	
    }

}
