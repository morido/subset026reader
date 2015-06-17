package helper.formatting.textannotation;

import helper.CSSManager;
import helper.XmlStringWriter;


class AnnotatorBGColor implements Annotator {    
    private final CSSManager cssmanager;
    private final String className; // may be null
    
    public AnnotatorBGColor(final String className, final String backgroundColor, final boolean isBold) {
	if (backgroundColor == null) throw new IllegalArgumentException("backgroundColor cannot be null.");	
	
	final CSSManager internalcssmanager = new CSSManager();
	internalcssmanager.putProperty("background-color", backgroundColor);
	internalcssmanager.putProperty("padding-left", "0.1em");
	internalcssmanager.putProperty("padding-right", "0.1em");
	if (isBold) internalcssmanager.putProperty("font-weight", "bold");
	
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
