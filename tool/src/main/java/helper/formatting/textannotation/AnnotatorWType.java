package helper.formatting.textannotation;

import java.util.Locale;

import helper.CSSManager;
import helper.XmlStringWriter;

class AnnotatorWType implements Annotator {
    private final String name;
    private final CSSManager cssmanagerSurroundings;
    private final CSSManager cssmanagerAnnotation;
    private final static CSSManager CSSMANAGERCONTENT;
    private final String nameForClass;

    static {
	{
	    final CSSManager cssmanager = new CSSManager();
	    cssmanager.putProperty("padding-left", "0.2em");
	    cssmanager.putProperty("padding-right", "0.1em");

	    CSSMANAGERCONTENT = cssmanager;
	}	
    }

    AnnotatorWType(final String name, final String color) {
	if (name == null) throw new IllegalArgumentException("name cannot be null.");
	if (color == null) throw new IllegalArgumentException("color cannot be null.");
	this.name = name;
	this.nameForClass = CSSManager.getIdentifier(this.name);

	{
	    final CSSManager cssmanager = new CSSManager();
	    cssmanager.putProperty("border", "1px solid " + color);
	    cssmanager.putProperty("margin", "0.1em");
	    cssmanager.putProperty("display", "inline-table");
	    this.cssmanagerSurroundings = cssmanager;
	}
	{
	    final CSSManager cssmanager = new CSSManager();	
	    cssmanager.putProperty("font-size", "x-small");
	    cssmanager.putProperty("font-family", "sans-serif");
	    cssmanager.putProperty("font-style", "normal");
	    cssmanager.putProperty("color", "white");
	    cssmanager.putProperty("padding-left", "1em");
	    cssmanager.putProperty("padding-right", "0.2em");
	    cssmanager.putProperty("background-color", color);
	    cssmanager.putProperty("display", "table-cell");
	    this.cssmanagerAnnotation = cssmanager;
	}
    }


    @Override
    public void writeStart(XmlStringWriter xmlwriter) {	
	if (xmlwriter == null) throw new IllegalArgumentException("xmlwriter cannot be null.");		

	xmlwriter.writeStartElement("span");
	xmlwriter.writeAttribute("class", this.nameForClass);
	xmlwriter.writeAttribute("style", this.cssmanagerSurroundings.toString());
	xmlwriter.writeStartElement("span");
	xmlwriter.writeAttribute("class", "content");
	xmlwriter.writeAttribute("style", CSSMANAGERCONTENT.toString());

    }


    @Override
    public void writeEnd(XmlStringWriter xmlwriter) {
	if (xmlwriter == null) throw new IllegalArgumentException("xmlwriter cannot be null.");	

	xmlwriter.writeEndElement("span");
	xmlwriter.writeStartElement("span");
	xmlwriter.writeAttribute("class", this.nameForClass + "_annotation");
	xmlwriter.writeAttribute("style", this.cssmanagerAnnotation.toString());
	xmlwriter.writeCharacters('[' + this.name.toUpperCase(Locale.ENGLISH) + ']');
	xmlwriter.writeEndElement("span");
	xmlwriter.writeEndElement("span");
    }    
}
