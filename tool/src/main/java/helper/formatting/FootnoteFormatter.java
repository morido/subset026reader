package helper.formatting;

import helper.CSSManager;
import helper.XmlStringWriter;

/**
 * Formatter for footnotes and endnotes
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class FootnoteFormatter implements GenericFormatter {
    private final transient XmlStringWriter xmlwriter;
    
    /**
     * Create a new formatter for note data
     * 
     * @param xmlwriter output writer
     * @throws IllegalArgumentException if the given argument is {@code null}
     */
    public FootnoteFormatter(final XmlStringWriter xmlwriter) {
	if (xmlwriter == null) throw new IllegalArgumentException("xmlwriter cannot be null.");
	this.xmlwriter = xmlwriter;
    }
    
    
    @Override    
    public void writeStartElement() {
	this.xmlwriter.writeStartElement("sup");
	this.xmlwriter.writeAttribute("class", "note");
	final CSSManager cssmanager = new CSSManager();
	cssmanager.putProperty("background-color", "lime");
	cssmanager.putProperty("font-weight", "bolder");
	cssmanager.putProperty("font-family", "courier");		
	this.xmlwriter.writeAttribute("style", cssmanager.toString());	
    }

    @Override    
    public void writeEndElement() {	
	this.xmlwriter.writeEndElement("sup");
    }

}
