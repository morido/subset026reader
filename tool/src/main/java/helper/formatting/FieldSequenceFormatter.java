package helper.formatting;


import helper.CSSManager;
import helper.XmlStringWriter;

/**
 * Pretty rich-text formatting of sequence fields
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public final class FieldSequenceFormatter implements GenericFormatter {
    private final transient XmlStringWriter xmlwriter;

    /**
     * Create a new formatter for SEQ-field data
     * 
     * @param xmlwriter output writer
     * @throws IllegalArgumentException if the given argument is {@code null}
     */
    public FieldSequenceFormatter(final XmlStringWriter xmlwriter) {
	if (xmlwriter == null) throw new IllegalArgumentException("xmlwriter cannot be null.");
	this.xmlwriter = xmlwriter;
    }

    /* (non-Javadoc)
     * @see helper.formatting.GenericFormatter#writeStartElement()
     */
    @Override    
    public void writeStartElement() {
	this.xmlwriter.writeStartElement("span");
	this.xmlwriter.writeAttribute("class", "field");
	final CSSManager cssmanager = new CSSManager();
	cssmanager.putProperty("background-color", "yellow");
	cssmanager.putProperty("font-weight", "bolder");
	cssmanager.putProperty("font-family", "courier");		
	this.xmlwriter.writeAttribute("style", cssmanager.toString());
	this.xmlwriter.writeCharacters("{");
    }

    /* (non-Javadoc)
     * @see helper.formatting.GenericFormatter#writeEndElement()
     */
    @Override    
    public void writeEndElement() {
	this.xmlwriter.writeCharacters("}");
	this.xmlwriter.writeEndElement("span");	
    }

}
