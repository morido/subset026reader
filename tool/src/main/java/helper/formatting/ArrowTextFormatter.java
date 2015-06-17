package helper.formatting;

import helper.CSSManager;
import helper.XmlStringWriter;

/**
 * Handles rich text formatting of transition arrows inside tables
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class ArrowTextFormatter implements GenericFormatter {
    private final transient XmlStringWriter xmlwriter;

    /**
     * Create a new formatter for an arrow
     * 
     * @param xmlwriter output writer
     * @throws IllegalArgumentException if the given argument is {@code null}
     */
    public ArrowTextFormatter(final XmlStringWriter xmlwriter) {
	if (xmlwriter == null) throw new IllegalArgumentException("xmlwriter cannot be null.");
	this.xmlwriter = xmlwriter;
    }
    
    /* (non-Javadoc)
     * @see helper.formatting.GenericFormatter#writeStartElement()
     */
    @Override    
    public void writeStartElement() {
	this.xmlwriter.writeStartElement("span");
	this.xmlwriter.writeAttribute("class", "arrow");
	final CSSManager cssmanager = new CSSManager();
	cssmanager.putProperty("white-space", "nowrap");
	cssmanager.putProperty("background-color", "rgb(255,182,193)");
	cssmanager.putProperty("font-family", "courier");	
	this.xmlwriter.writeAttribute("style", cssmanager.toString());	
    }

    /* (non-Javadoc)
     * @see helper.formatting.GenericFormatter#writeEndElement()
     */
    @Override    
    public void writeEndElement() {
	this.xmlwriter.writeEndElement("span");	
    }

}
