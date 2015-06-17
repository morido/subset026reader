package helper;


import org.apache.poi.hwpf.usermodel.Paragraph;


/**
 * Abstract class which deals with rich text properties of entire paragraphs (justification, indentation, ...)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public abstract class AbstractRichTextReaderParagraph extends AbstractRichTextReader implements Destructible {
    protected String startTag = null;

    /**
     * Ordinary constructor
     * 
     * @param xmlwriter output writer
     * @param paragraph paragraph to process
     */
    public AbstractRichTextReaderParagraph(final XmlStringWriter xmlwriter, final Paragraph paragraph) {
	super(xmlwriter, paragraph);
    }

    /**
     * Fake destructor
     */
    @Override
    public void close() {
	if (this.startTag != null) {
	    this.xmlwriter.writeEndElement(this.startTag);
	    this.startTag = null;
	}		
    }    
}