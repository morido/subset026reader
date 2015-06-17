/**
 * 
 */
package helper.formatting;

import helper.CSSManager;
import helper.XmlStringWriter;

/**
 * Takes care about the rich-text formatting of tracedata in table cells
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public final class CellIdFormatter implements GenericFormatter {
    private final transient XmlStringWriter xmlwriter;
    private final static String DIV = "div";

    /**
     * Create a new CellIdFormatter
     * 
     * @param xmlwriter output writer
     * @throws IllegalArgumentException if the given argument is {@code null}
     */
    public CellIdFormatter(final XmlStringWriter xmlwriter) {
	if (xmlwriter == null) throw new IllegalArgumentException("xmlwriter cannot be null.");
	this.xmlwriter = xmlwriter;
    }

    /* (non-Javadoc)
     * @see helper.formatting.GenericFormatter#writeStartElement()
     */
    @Override    
    public void writeStartElement() {
	this.xmlwriter.writeStartElement(DIV); // Outer div
	this.xmlwriter.writeAttribute("class", "hrMetadata");

	this.xmlwriter.writeStartElement(DIV); // Inner div
	final CSSManager cssmanager = new CSSManager();
	cssmanager.putProperty("background-color", "rgb(173,216,230)");
	cssmanager.putProperty("font-family", "courier");
	cssmanager.putProperty("font-size", "smaller");
	cssmanager.putProperty("font-weight", "lighter");			
	cssmanager.putProperty("display", "inline-block");
	cssmanager.putProperty("float", "right");
	this.xmlwriter.writeAttribute("style", cssmanager.toString());	
    }

    /* (non-Javadoc)
     * @see helper.formatting.GenericFormatter#writeEndElement()
     */
    @Override    
    public void writeEndElement() {
	this.xmlwriter.writeEndElement(DIV); //Inner div

	//CSS clear div
	this.xmlwriter.writeStartElement(DIV);
	final CSSManager cssmanager = new CSSManager();
	cssmanager.putProperty("clear", "right");
	this.xmlwriter.writeAttribute("style", cssmanager.toString());
	this.xmlwriter.writeEndElement(DIV);

	this.xmlwriter.writeEndElement(DIV); //Outer div
    }
}