package helper.formatting.textannotation;

import helper.CSSManager;
import helper.XmlStringWriter;

class AnnotatorUnderline implements Annotator {
    private final CSSManager cssmanager;
    private final String className; // may be null

    public AnnotatorUnderline(final String className, final String color) {	
	assert className != null && color != null;	
	final CSSManager internalcssmanager = new CSSManager();
	internalcssmanager.putProperty("border-bottom", "1px solid " + color);
	internalcssmanager.putProperty("display", "inline-block");

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
