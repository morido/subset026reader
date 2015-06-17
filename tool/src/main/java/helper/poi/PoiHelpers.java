package helper.poi;

import docreader.ReaderData;

/**
 * Wraps certain functionality of Apache POI which is not nicely exposed otherwise;
 * necessary for proper mocking in tests
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public enum PoiHelpers {
    ;

    /**
     * Return the human-readable name associated with a particular style-index
     * 
     * @param readerData global readerData
     * @param styleIndex index of the style in question
     * @return human-readable name
     * @throws IllegalArgumentException if one of the arguments is {@code null}
     */
    public static String getStyleName(final ReaderData readerData, final int styleIndex) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	return readerData.getDocument().getStyleSheet().getStyleDescription(styleIndex).getName();
    }
    
}
