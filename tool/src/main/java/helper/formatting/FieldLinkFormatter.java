package helper.formatting;


import helper.CSSManager;
import helper.XmlStringWriter;

/**
 * Takes care of pretty printing links in Word's fields
 * (they will not become clickable; but at least they are nice and blue)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public final class FieldLinkFormatter implements GenericFormatter {
    private final transient XmlStringWriter xmlwriter;
    private final transient boolean isBrokenReference;

    /**
     * Create a new formatter for links in fields
     * 
     * @param xmlwriter output writer
     * @param isBrokenReference {@code true} if this is a reference to nowhere; {@code false} otherwise
     * @throws IllegalArgumentException if the given argument is {@code null}
     */
    public FieldLinkFormatter(final XmlStringWriter xmlwriter, final boolean isBrokenReference) {
    	if (xmlwriter == null) throw new IllegalArgumentException("xmlwriter cannot be null.");
	this.xmlwriter = xmlwriter;
	this.isBrokenReference = isBrokenReference;
    }

    /* (non-Javadoc)
     * @see helper.formatting.GenericFormatter#writeStartElement()
     */
    @Override   
    public void writeStartElement() {
	this.xmlwriter.writeStartElement("span");
	this.xmlwriter.writeAttribute("class", "field");		
	if (!this.isBrokenReference) {
	    final CSSManager cssmanager = new CSSManager();
	    cssmanager.putProperty("text-decoration", "underline");
	    cssmanager.putProperty("color", "blue");
	    this.xmlwriter.writeAttribute("style", cssmanager.toString());
	}		
    }

    /* (non-Javadoc)
     * @see helper.formatting.GenericFormatter#writeEndElement()
     */
    @Override
    public void writeEndElement() {		
	this.xmlwriter.writeEndElement("span");	
    }

}
