package helper;

import org.apache.poi.hwpf.usermodel.Paragraph;

/**
 * Abstract class for reading all sorts of rich text
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
abstract class AbstractRichTextReader {
	protected XmlStringWriter xmlwriter;
	protected Paragraph paragraph;
	
	public AbstractRichTextReader(final XmlStringWriter xmlwriter, final Paragraph paragraph) {
		this.xmlwriter = xmlwriter;
		this.paragraph = paragraph;
	}	
}
