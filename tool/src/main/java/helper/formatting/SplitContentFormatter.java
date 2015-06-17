package helper.formatting;

import helper.CSSManager;
import helper.XmlStringWriter;

/**
 * Formatter for the rich text content of a cell whose paragraphs have been
 * split up into individual requirements
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class SplitContentFormatter implements GenericFormatter {
private final transient XmlStringWriter xmlwriter;
    
    /**
     * Create a new formatter
     */
    public SplitContentFormatter() {
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
	cssmanager.putProperty("background-color", "#C0C0C0");
	cssmanager.putProperty("color", "black");
	cssmanager.putProperty("padding", "0.1em");
	cssmanager.putProperty("font-family", "monospace");
	cssmanager.putProperty("font-size", "smaller");
	this.xmlwriter.writeAttribute("style", cssmanager.toString());		
    }

    @Override    
    public void writeEndElement() {
	this.xmlwriter.writeEndElement("span");
	
    }
}
