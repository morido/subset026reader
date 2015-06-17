package helper.word;


import java.util.Arrays;

import org.apache.poi.hwpf.usermodel.BorderCode;

import helper.CSSManager;

/**
 * Handles word border data as it is applied to table-cells (and presumably paragraphs as well)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class BorderManager {
    private final CSSManager cssmanager;
    private final BorderCode[] bordercodes;
    private final int[] paddingSupplement;
    private final static int NUMBER_OF_SIDES = 4;

    /**
     * @param cssmanager The manager where the output gets written
     * @param bordercodes A 4-element array with the border properties in the order required by CSS (i.e. top, right, bottom, left) 
     * @param paddingSupplement A 4-element array with padding data in the order required by CSS (i.e. top, right, bottom, left) 
     * @throws IllegalArgumentException One of the arguments is {@code null} or invalid
     */
    public BorderManager(final CSSManager cssmanager, final BorderCode[] bordercodes, final int[] paddingSupplement) {	
	if (cssmanager == null) throw new IllegalArgumentException("cssmanager cannot be null.");
	if (bordercodes.length != NUMBER_OF_SIDES) throw new IllegalArgumentException("Bordercode length is illegal.");
	for (final BorderCode element : bordercodes) if (element == null) throw new IllegalArgumentException("One of the bordercodes was null.");
	if (paddingSupplement.length != NUMBER_OF_SIDES) throw new IllegalArgumentException("PaddingSupplement length is illegal.");

	this.cssmanager = cssmanager;
	this.bordercodes = Arrays.copyOf(bordercodes, bordercodes.length);
	this.paddingSupplement = Arrays.copyOf(paddingSupplement, paddingSupplement.length);
    }

    /**
     * Compute the border- and padding properties and write them to cssmanager
     */
    public void writeBorderProperties() {
	final byte iterations = NUMBER_OF_SIDES;
	final String[] padding = new String[iterations];
	final String[] borderWidth = new String[iterations];
	final String[] borderStyle = new String[iterations];
	final String[] borderColor = new String[iterations];

	assert this.bordercodes.length == iterations;
	for (int i = 0; i < this.bordercodes.length; i++) {
	    final int currentPaddingSupplement = DataConverter.twipsToPixels(this.paddingSupplement[i]);
	    final String curPadding, curBorderWidth, curBorderStyle, curBorderColor;

	    assert this.bordercodes[i] != null;
	    if (this.bordercodes[i].isNoBorder()) {					
		curPadding = currentPaddingSupplement + "px";
		curBorderWidth = "0px";
		curBorderStyle = "none";
		curBorderColor = "transparent";		
	    }
	    else {					
		curPadding = Integer.toString(DataConverter.pointsToPixels(this.bordercodes[i].getSpace()) + currentPaddingSupplement) + "px";

		final int linewidthinpx = DataConverter.pointsToPixels(getBorderWidth(this.bordercodes[i].getLineWidth()));
		if (linewidthinpx == 0) {
		    curBorderWidth = "0px";
		    curBorderStyle = "none";
		    curBorderColor = "transparent";
		}
		else {
		    curBorderWidth = Integer.toString(linewidthinpx) + "px";
		    curBorderStyle = getStyle(this.bordercodes[i].getBorderType());
		    curBorderColor = DataConverter.getColorName(this.bordercodes[i].getColor());						
		}
	    }

	    //write output of current iteration
	    padding[i] = curPadding;
	    borderWidth[i] = curBorderWidth;
	    borderStyle[i] = curBorderStyle;
	    borderColor[i] = curBorderColor;
	}

	//write to cssmanager
	this.cssmanager.putProperty("padding", CSSManager.getShorthandProperty(padding));
	this.cssmanager.putProperty("border-width", CSSManager.getShorthandProperty(borderWidth));
	this.cssmanager.putProperty("border-style", CSSManager.getShorthandProperty(borderStyle));
	this.cssmanager.putProperty("border-color", CSSManager.getShorthandProperty(borderColor));
    }

    /**
     * Reads out the correct border width according to MS DOC spec, 2014, 2.9.17f
     * 
     * @param the lineWidth as it is stored in dptLineWidth
     * @return The border width in points
     */
    private static float getBorderWidth(final int input) {								
	// do not allow values smaller than 2 - [MS-DOC], v20140721, 2.9.17	
	final int lineWidth = input < 2 ? 2 : input;	
	return (float) (lineWidth / 8.0);
    }

    /**
     * Converts a brcType into a CSS border descriptor
     * <br />based on [MS-DOC], v20080215, p.99
     * 
     * @param input A numeric representation of a brcType
     * @return A string representing a css border type, never {@code null}
     */
    private static String getStyle(final int input) {	
	final String defaultValue = "solid";
	
	switch(input) {
	case 0: return defaultValue; //none is intentionally neglected here
	case 1: return defaultValue;
	case 2: return defaultValue;
	case 3: return "double";
	case 5: return defaultValue;
	case 6: return "dotted";
	case 7: return "dashed";
	case 8: return "dashed";
	case 9: return "dotted";
	case 10: return "double";
	default: return defaultValue;
	}
    }
}
