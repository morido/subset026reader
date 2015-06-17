package helper.formatting.textannotation;

import helper.CSSManager;
import helper.XmlStringWriter;

class AnnotatorMonospace implements Annotator {
    private final CSSManager cssmanager;
    private final String className; // may be null

    public AnnotatorMonospace(final String className) {	

	final CSSManager internalcssmanager = new CSSManager();
	internalcssmanager.putProperty("font-family", "monospace");
	internalcssmanager.putProperty("font-style", "italic");

	this.cssmanager = internalcssmanager;
	this.className = className;
    }

    @Override
    public void writeStart(XmlStringWriter xmlwriter) {
	if (xmlwriter == null) throw new IllegalArgumentException("xmlwriter cannot be null.");
	xmlwriter.writeStartElement("span");
	if (this.className != null) {
	    final String nameForClass = CSSManager.getIdentifier(this.className);
	    xmlwriter.writeAttribute("class", nameForClass);
	}	
	xmlwriter.writeAttribute("style", this.cssmanager.toString());
    }

    @Override
    public void writeEnd(XmlStringWriter xmlwriter) {
	if (xmlwriter == null) throw new IllegalArgumentException("xmlwriter cannot be null.");
	xmlwriter.writeEndElement("span");
    }
}
