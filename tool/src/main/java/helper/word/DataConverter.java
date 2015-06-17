/**
 * 
 */
package helper.word;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static helper.Constants.Internal.MSWord.PLACEHOLDER_IMAGE;
import static helper.Constants.MSWord.DELIMITER_LISTLEVEL;
import helper.RegexHelper;
import helper.annotations.DomainSpecific;

import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;

import docreader.ReaderData;
import docreader.range.paragraph.ParagraphListAware;

/**
 * Helper class to handle various conversions from Word data formats to something usable
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public enum DataConverter {	
    ;

    private static final Logger logger = Logger.getLogger(DataConverter.class.getName()); // NOPMD - Reference rather than a static field

    /**
     * Converts an ico into a named CSS-color
     * <p>based on MS DOC spec, 2008, p. 99</p>
     * <p>similar to {@link org.apache.poi.hwpf.usermodel.CharacterProperties#getIco24()}, but emits CSS named colors</p>
     * 
     * @param input A numeric representation of an ico
     * @return A string representing a CSS color-name, never {@code null}
     */
    public static String getColorName(final short input) { // NOPMD - short comes from POI
	switch (input) {
	case 0: return "Black";
	case 1: return "Black";
	case 2: return "Blue";
	case 3: return "Cyan";
	case 4: return "Green";
	case 5: return "Magenta";
	case 6: return "Red";
	case 7: return "Yellow";
	case 8: return "White";
	case 9: return "DarkBlue";
	case 10: return "DarkCyan";
	case 11: return "DarkGreen";
	case 12: return "DarkMagenta";
	case 13: return "DarkRed";
	case 14: return "GoldenRod"; // no direct equivalent in CSS available
	case 15: return "DarkGray";
	case 16: return "LightGray";
	default: return "Black";
	}
    }

    /**
     * Simple conversion from (print-) points to (display-) pixels
     * 
     * @param points
     * @return A pixel representation of the input
     */
    public static int pointsToPixels(final float points) {
	final int output = Math.round(points * 96 / 72);

	// set very small values to 1px in size so they get rendered
	return (points > 0.0 && output == 0) ? 1 : output;			
    }

    /**
     * Converts twips, a measurement unit used by MS Word, to pixels
     * 
     * @param twips twips to convert
     * @return value in pixels
     */
    public static int twipsToPixels(final int twips) {
	return pointsToPixels((float)(twips / 20.0));
    }


    /**
     * Simple conversion from display pixels to Word's internal twips
     * 
     * @param pixels pixels to convert
     * @see #twipsToPixels(int)
     * @see #pointsToPixels(float)
     * @return value in twips
     */
    public static int pixelsToTwips(final int pixels) {
	return (20 * 72 / 96 * pixels);
    }

    /**
     * Check if the given paragraph is merely a visual placeholder (i.e. nothing of interest in there)
     * @param readerData global readerData
     * @param paragraph Paragraph to be checked
     * 
     * @return {@code true} if the given paragraph contains no usable information; {@code false} otherwise
     * @throws IllegalArgumentException if the given paragraph is {@code null}
     */
    public static boolean isEmptyParagraph(final ReaderData readerData, final ParagraphListAware paragraph) {
	if (paragraph == null) throw new IllegalArgumentException("Paragraph cannot be null.");
	return !paragraph.isInList() && !isInTable(readerData, paragraph.getParagraph()) && isEmptyParagraph(paragraph.getParagraph());	
    }

    
    /**
     * Same as {@link #isEmptyParagraph(ReaderData, ParagraphListAware)} but only checks for textual contents
     * 
     * @param paragraph Paragraph to be checked
     * @return {@code true} if the given paragraph contains no usable information; {@code false} otherwise
     */
    public static boolean isEmptyParagraph(final Paragraph paragraph) {
	if (paragraph == null) throw new IllegalArgumentException("Paragraph cannot be null.");
	final String paragraphText = cleanupText(paragraph.text());
	return ("\r").equals(paragraphText) || ("").equals(paragraphText) || paragraphText.matches("\\s+");
    }

    /**
     * Remove unwanted characters from a string as it is obtained from Word
     * <p>based on {@link org.apache.poi.hwpf.converter.AbstractWordConverter#processCharacters}</p>
     * 
     * @param input The string to be processed, may not be {@null}
     * @return A cleaned up version of the input, never {@code null}
     * @throws IllegalArgumentException If the given text is {@code null}
     */
    @DomainSpecific
    public static String cleanupText(final String input) {	
	if (input == null) throw new IllegalArgumentException("input cannot be null.");
	final char UNICODECHAR_NONBREAKING_HYPHEN = '\u2011';
	final char UNICODECHAR_ZERO_WIDTH_SPACE = '\u200b';	
	final char UNICODECHAR_NBSP = '\u00A0';
	final char TABLECELL_END_MARKER = '\u0007';

	String output = input;
	if (output.endsWith(Character.toString(TABLECELL_END_MARKER))) {
	    // Strip the table cell end marker
	    output = output.substring(0, output.length()-1);
	}

	// Non-breaking hyphens are returned as char 30
	output = output.replace((char) 30, UNICODECHAR_NONBREAKING_HYPHEN);

	// Non-required hyphens to zero-width space
	output = output.replace((char) 31, UNICODECHAR_ZERO_WIDTH_SPACE);

	// tabs as NBSP (anything else wont make sense since the real tab character would become collapsed in HTML)
	// Note: regex "\s"-class only matches this with the "(?U)"-flag! 
	output = output.replace('\t', UNICODECHAR_NBSP);

	// Control characters as nothing
	// but leave the \u0001 (image placeholder) in there, [MS-DOC], v20140721, 1.3.5
	// \u0002 (footnote placeholder) must stay as well 
	output = output.replaceAll("[\u0000\u0003-\u001f]", "");

	return output;
    }

    
    /**
     * Same as {@link #cleanupText(String)} but takes special care of multiple whitespaces for proper XHTML output
     * 
     * @param input The string to be processed, may not be {@null}
     * @return A cleaned up, whitespace-enhanced version of the input, never {@code null}
     * @throws IllegalArgumentException If the given text is {@code null}
     */
    public static String cleanupTextWSpaces(final String input) {
	final char UNICODECHAR_NBSP = '\u00A0';
	if (input == null) throw new IllegalArgumentException("input cannot be null.");
	String inputCleaned = cleanupText(input);
	
	final Pattern pattern = Pattern.compile("[ ]{2,}");
	final Matcher matcher = pattern.matcher(inputCleaned);
	
	final String output;
	if (matcher.find()) {
	    // we do have whitespace chunks - so get to work	    
	    final char[] outputAsChar = inputCleaned.toCharArray();
	    do {	    
		for (int i = matcher.start()+1; i<matcher.end(); i++) {
		    // replace everything from the second space to the end with NBSPs
		    outputAsChar[i] = UNICODECHAR_NBSP;
		}
	    } while (matcher.find());
	    output = String.valueOf(outputAsChar);
	}
	else output = inputCleaned;
	
	return output;
    }

    /**
     * Obtain a picture offset from a range
     * 
     * @param inputRange range where the offset should be obtained from; may be {@code null}
     * @return the picture offset or {@code null} if no picture offset could be found
     */
    public static Integer getPicOffset(final Range inputRange) {
	if (inputRange != null && inputRange.numCharacterRuns() == 1) {
	    final CharacterRun firstCharacterRun = inputRange.getCharacterRun(0);
	    if (firstCharacterRun.isData()) {
		logger.log(Level.INFO, "We have NilPICFAndBinData here which we probably cannot handle. Will skip the picture.");
		return null;
	    }
	    if (firstCharacterRun.isObj() && firstCharacterRun.isOle2()) {
		// both of the above properties need to be set; see [MS-DOC], v20140721, 2.6.1, sprmCFOle2
		logger.log(Level.INFO, "We have embedded OLE data here which we probably cannot handle. Will skip the picture.");
		return null;
	    }
	    if (firstCharacterRun.isSpecialCharacter() && (Character.toString(PLACEHOLDER_IMAGE)).equals(firstCharacterRun.text())) return firstCharacterRun.getPicOffset();
	}
	return null;
    }    

    /**
     * Checks if a character run contains a special (wide) space
     * 
     * @param characterRun CharacterRun under consideration
     * @return {@code true} if this characterRun represents some sort of a space character; {@code false} otherwise
     */
    public static boolean isSpace(final CharacterRun characterRun) {
	// [MS-DOC], v20140721, 2.6.1, sprmCFSpec
	if (characterRun.isSpecialCharacter()) {
	    final String text = characterRun.text();
	    if (text.length() == 1) {
		switch (text.charAt(0)) {
		case '\u2002':
		    // en space
		    return true;
		case '\u2003':
		    // em space
		    return true;
		default:
		    break; // do not care
		}
	    }
	}
	return false;
    }

    /**
     * Converts Word's <em>symbol characters</em> into their unicode representation; <em>special character method</em>
     * <p>Contrary to [MS-DOC], v20140721, 2.9.47, the {@code xchar} (= the symbol character) is not always stored in unicode representation. So we need some conversion.</p>  
     * 
     * @param characterRun A character run which contains nothing but a special character
     * @return unicode representation of the character or a question mark ({@code ?}) if it was not possible to obtain any more meaningful character
     * @throws IllegalArgumentException if the given {@code characterRun} is {@code null} or cannot be processed
     */
    public static char convertToUnicodeChar(final CharacterRun characterRun) {
	if (characterRun == null) throw new IllegalArgumentException("Given characterRun cannot be null.");
	if (!characterRun.isSymbol()) throw new IllegalArgumentException("Given characterRun is not a symbol character run.");

	if (characterRun.getSymbolFont() == null) {
	    logger.log(Level.WARNING, "We have a symbol character but there is no font associated to describe its meaning. Perhaps your document is corrupted. Will emit a question mark, instead.");
	    return '?';
	}

	if ("Symbol".equals(characterRun.getSymbolFont().getMainFontName())) {
	    // if the used font is "Symbol" then the xchar does not encode an unicode (UTF16-LE) char, but only an 8-bit value designating the character position in the actual font
	    return convertToUnicodeChar(characterRun.getSymbolCharacter() & 0xFF);
	}
	// TODO check if this is correct -- write a testcase for this
	return characterRun.getSymbolCharacter();
    }

    /**
     * Converts Word's <em>symbol characters</em> into their unicode representation; <em>field character method</em>
     * <p>Contrary to [MS-DOC], v20140721, 2.9.47, the {@code xchar} (= the symbol character) is not always stored in unicode representation. So we need some conversion.</p>
     * 
     * @param inputChar char to be converted
     * @return correct unicode representation of the input char
     */
    public static char convertToUnicodeChar(final int inputChar) { 
	// map values according to ftp://ftp.unicode.org/Public/MAPPINGS/VENDORS/ADOBE/symbol.txt
	switch (inputChar) {
	case 0x20: return '\u00A0';
	case 0x21: return '\u0021';
	case 0x22: return '\u2200';
	case 0x23: return '\u0023';
	case 0x24: return '\u2203';
	case 0x25: return '\u0025';
	case 0x26: return '\u0026';
	case 0x27: return '\u220B';
	case 0x28: return '\u0028';
	case 0x29: return '\u0029';
	case 0x2A: return '\u2217';
	case 0x2B: return '\u002B';
	case 0x2C: return '\u002C';
	case 0x2D: return '\u2212';
	case 0x2E: return '\u002E';
	case 0x2F: return '\u002F';
	case 0x30: return '\u0030';
	case 0x31: return '\u0031';
	case 0x32: return '\u0032';
	case 0x33: return '\u0033';
	case 0x34: return '\u0034';
	case 0x35: return '\u0035';
	case 0x36: return '\u0036';
	case 0x37: return '\u0037';
	case 0x38: return '\u0038';
	case 0x39: return '\u0039';
	case 0x3A: return '\u003A';
	case 0x3B: return '\u003B';
	case 0x3C: return '\u003C';
	case 0x3D: return '\u003D';
	case 0x3E: return '\u003E';
	case 0x3F: return '\u003F';
	case 0x40: return '\u2245';
	case 0x41: return '\u0391';
	case 0x42: return '\u0392';
	case 0x43: return '\u03A7';
	case 0x44: return '\u0394'; //this is a delta sign
	//case 0x44: return '\u2206'; //this is an "increment"-sign
	case 0x45: return '\u0395';
	case 0x46: return '\u03A6';
	case 0x47: return '\u0393';
	case 0x48: return '\u0397';
	case 0x49: return '\u0399';
	case 0x4A: return '\u03D1';
	case 0x4B: return '\u039A';
	case 0x4C: return '\u039B';
	case 0x4D: return '\u039C';
	case 0x4E: return '\u039D';
	case 0x4F: return '\u039F';
	case 0x50: return '\u03A0';
	case 0x51: return '\u0398';
	case 0x52: return '\u03A1';
	case 0x53: return '\u03A3';
	case 0x54: return '\u03A4';
	case 0x55: return '\u03A5';
	case 0x56: return '\u03C2';
	case 0x57: return '\u03A9';
	//case 0x57: return '\u2126';
	case 0x58: return '\u039E';
	case 0x59: return '\u03A8';
	case 0x5A: return '\u0396';
	case 0x5B: return '\u005B';
	case 0x5C: return '\u2234';
	case 0x5D: return '\u005D';
	case 0x5E: return '\u22A5';
	case 0x5F: return '\u005F';
	case 0x60: return '\uF8E5';
	case 0x61: return '\u03B1';
	case 0x62: return '\u03B2';
	case 0x63: return '\u03C7';
	case 0x64: return '\u03B4';
	case 0x65: return '\u03B5';
	case 0x66: return '\u03C6';
	case 0x67: return '\u03B3';
	case 0x68: return '\u03B7';
	case 0x69: return '\u03B9';
	case 0x6A: return '\u03D5';
	case 0x6B: return '\u03BA';
	case 0x6C: return '\u03BB';
	//case 0x6D: return '\u00B5';
	case 0x6D: return '\u03BC';
	case 0x6E: return '\u03BD';
	case 0x6F: return '\u03BF';
	case 0x70: return '\u03C0';
	case 0x71: return '\u03B8';
	case 0x72: return '\u03C1';
	case 0x73: return '\u03C3';
	case 0x74: return '\u03C4';
	case 0x75: return '\u03C5';
	case 0x76: return '\u03D6';
	case 0x77: return '\u03C9';
	case 0x78: return '\u03BE';
	case 0x79: return '\u03C8';
	case 0x7A: return '\u03B6';
	case 0x7B: return '\u007B';
	case 0x7C: return '\u007C';
	case 0x7D: return '\u007D';
	case 0x7E: return '\u223C';
	case 0xA0: return '\u20AC';
	case 0xA1: return '\u03D2';
	case 0xA2: return '\u2032';
	case 0xA3: return '\u2264';
	//case 0xA4: return '\u2044';
	case 0xA4: return '\u2215';
	case 0xA5: return '\u221E';
	case 0xA6: return '\u0192';
	case 0xA7: return '\u2663';
	case 0xA8: return '\u2666';
	case 0xA9: return '\u2665';
	case 0xAA: return '\u2660';
	case 0xAB: return '\u2194';
	case 0xAC: return '\u2190';
	case 0xAD: return '\u2191';
	case 0xAE: return '\u2192';
	case 0xAF: return '\u2193';
	case 0xB0: return '\u00B0';
	case 0xB1: return '\u00B1';
	case 0xB2: return '\u2033';
	case 0xB3: return '\u2265';
	case 0xB4: return '\u00D7';
	case 0xB5: return '\u221D';
	case 0xB6: return '\u2202';
	case 0xB7: return '\u2022';
	case 0xB8: return '\u00F7';
	case 0xB9: return '\u2260';
	case 0xBA: return '\u2261';
	case 0xBB: return '\u2248';
	case 0xBC: return '\u2026';
	case 0xBD: return '\uF8E6';
	case 0xBE: return '\uF8E7';
	case 0xBF: return '\u21B5';
	case 0xC0: return '\u2135';
	case 0xC1: return '\u2111';
	case 0xC2: return '\u211C';
	case 0xC3: return '\u2118';
	case 0xC4: return '\u2297';
	case 0xC5: return '\u2295';
	case 0xC6: return '\u2205';
	case 0xC7: return '\u2229';
	case 0xC8: return '\u222A';
	case 0xC9: return '\u2283';
	case 0xCA: return '\u2287';
	case 0xCB: return '\u2284';
	case 0xCC: return '\u2282';
	case 0xCD: return '\u2286';
	case 0xCE: return '\u2208';
	case 0xCF: return '\u2209';
	case 0xD0: return '\u2220';
	case 0xD1: return '\u2207';
	case 0xD2: return '\uF6DA';
	case 0xD3: return '\uF6D9';
	case 0xD4: return '\uF6DB';
	case 0xD5: return '\u220F';
	case 0xD6: return '\u221A';
	case 0xD7: return '\u22C5';
	case 0xD8: return '\u00AC';
	case 0xD9: return '\u2227';
	case 0xDA: return '\u2228';
	case 0xDB: return '\u21D4';
	case 0xDC: return '\u21D0';
	case 0xDD: return '\u21D1';
	case 0xDE: return '\u21D2';
	case 0xDF: return '\u21D3';
	case 0xE0: return '\u25CA';
	case 0xE1: return '\u2329';
	case 0xE2: return '\uF8E8';
	case 0xE3: return '\uF8E9';
	case 0xE4: return '\uF8EA';
	case 0xE5: return '\u2211';
	case 0xE6: return '\uF8EB';
	case 0xE7: return '\uF8EC';
	case 0xE8: return '\uF8ED';
	case 0xE9: return '\uF8EE';
	case 0xEA: return '\uF8EF';
	case 0xEB: return '\uF8F0';
	case 0xEC: return '\uF8F1';
	case 0xED: return '\uF8F2';
	case 0xEE: return '\uF8F3';
	case 0xEF: return '\uF8F4';
	case 0xF1: return '\u232A';
	case 0xF2: return '\u222B';
	case 0xF3: return '\u2320';
	case 0xF4: return '\uF8F5';
	case 0xF5: return '\u2321';
	case 0xF6: return '\uF8F6';
	case 0xF7: return '\uF8F7';
	case 0xF8: return '\uF8F8';
	case 0xF9: return '\uF8F9';
	case 0xFA: return '\uF8FA';
	case 0xFB: return '\uF8FB';
	case 0xFC: return '\uF8FC';
	case 0xFD: return '\uF8FD';
	case 0xFE: return '\uF8FE';
	default: logger.log(Level.WARNING, "Encountered a symbol character in Adobe's Symbol font which cannot be matched to any unicode character. Will emit a question mark, instead."); return '?'; 
	}
    }

    /**
     * Separate numberText and paragraph text of a fake list paragraph
     * 
     * @param paragraph paragraph to process
     * @param getNumberText if {@code true} return the numberText; otherwise return the paragraph text
     * @return {@code null} if this was no fake list paragraph or a String representing the requested part
     */
    @DomainSpecific
    public static String separateFakeListParagraph(final Paragraph paragraph, final boolean getNumberText) {
	final String paragraphText = paragraph.text();
	final String quotedListLevelDelimiter = RegexHelper.quoteRegex(DELIMITER_LISTLEVEL);	
	final String pattern = "^((?:A\\d+|(?:[A-Z]+|[a-z]+|\\d+))(?:(?:" + quotedListLevelDelimiter + ")(?:[A-Z]+|[a-z]+|\\d+))*(?:" + quotedListLevelDelimiter + ")?)[ ]?\\t+(.+)$";		
	final String[] separatedParts = RegexHelper.extractRegex(paragraphText, pattern, 2);
	final String output;
	if (separatedParts == null) {
	    // this is no fake list paragraph
	    output = null;
	}
	else if (getNumberText) {
	    // return the numberText
	    if (separatedParts[0].matches("^A[0-9]" + quotedListLevelDelimiter + ".*")) {
		// insert a missing list level delimiter for malformed numberTexts
		final String original = separatedParts[0];
		separatedParts[0] = original.substring(0, 1) + DELIMITER_LISTLEVEL + original.substring(1);
	    }
	    output = separatedParts[0];
	}
	else {
	    // return the text of the paragraph (so everything right of the numberText)
	    output = separatedParts[1];
	}
	return output;
    }
    
    /**
     * Wrapper for {@link Paragraph#isInTable()}
     * 
     * @param readerData global readerData
     * @param paragraph paragraph to check
     * @return {@code true} if the given paragraph is part of a table at the current nesting level; {@code false} otherwise
     */
    public static boolean isInTable(final ReaderData readerData, final Paragraph paragraph) {
	// comparing the itap/ptab (i.e. getTableLevel()) with readerData.getNnestingLevel is legitimate; see [MS-DOC], v20140721, 1.1, "table depth"
	return (paragraph.isInTable() && paragraph.getTableLevel() == readerData.getTableNestingLevel());
    }
}
