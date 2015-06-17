package helper;

import java.util.regex.Pattern;

import helper.annotations.DomainSpecific;

/**
 * Non-instantiable helper class which defines global constants
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public final class Constants {
    
    
    /**
     * Constants for internal use
     */
    public final static class Internal {
	/**
	 * Version number of this tool
	 */
	public static final String VERSION = "0.6.1";
	
	
	/**
	 * Constant characters to parse MS Word
	 *
	 */
	public final static class MSWord {
	    	    
	    /**
	     * Placeholder character for an image
	     */
	    public static final char PLACEHOLDER_IMAGE = '\u0001';	    
	    
	    /**
	     * Placeholder character for a footnote/endnote
	     */
	    public static final char PLACEHOLDER_FOOTNOTE = '\u0002';
	    
	    /**
	     * Placeholder character for an OfficeDrawing
	     */
	    public static final char PLACEHOLER_OFFICEDRAWING = '\u0008';
	}
    }
    

    /**
     * Generic constants     
     */
    public final static class Generic {
	/**
	 * Relative storage directory of images / equations and other embedded media
	 */
	public static final String MEDIA_STORE_DIR = "media";
	
	/**
	 * line to write for each image to be converted; may be either a command (batch mode) or a semicolon separated value list (csv-mode) 
	 * 
	 * <p>Placeholders are mapped as follows:
	 * <dl>
	 * <dt>{1}</dt><dd>input filename</dd>
	 * <dt>{2}</dt><dd>output filename</dd>
	 * <dt>{3}</dt><dd>width of output image</dd>
	 * <dt>{4}</dt><dd>height of output image</dd>
	 * </dl>
	 * </p>
	 */
	// public static final String IMAGE_CONVERSION_TOOL_PATTERN = "convert.exe {1} -resize {3}x{4}^! {2}";
	// public static final String IMAGE_CONVERSION_TOOL_PATTERN = "wmf2gd --maxwidth={3} --maxheight={4} --maxpect -t png -o \"{2}\" \"{1}\"";
	public static final String IMAGE_CONVERSION_TOOL_PATTERN = "\"{2}\";\"{3}\";\"{4}\"";
	
	/**
	 * command to invoke for each source image to be deleted; if {@code null} no image will be deleted; only applicable if images are converted through a batch file
	 * 
	 * <p>Placeholders are mapped as follows:
	 * <dl>
	 * <dt>{1}</dt><dd>input filename</dd>
	 * </dl>
	 * </p>
	 */
	public static final String IMAGE_REMOVAL_TOOL_PATTERN = "del {1}";
	
	/**
	 * line to write for each shape to be converted; semicolon separated value list (csv-mode) 
	 * 
	 * <p>Placeholders are mapped as follows:
	 * <dl>
	 * <dt>{1}</dt><dd>range start offset</dd>
	 * <dt>{2}</dt><dd>output filename</dd>	 
	 * </dl>
	 * </p>
	 */
	public static final String SHAPE_CONVERSION_TOOL_PATTERN = "\"{1}\";\"{2}\"";
	
	/**
	 * If {@code true} then create CSV-files containing all artifacts and their relations for statistical purposes
	 */
	public static final boolean WRITE_STATISTICAL_DATA = true;
	
	/**
	 * Relative storage directory of statistical data; only applicable if {@link #WRITE_STATISTICAL_DATA} is set
	 */
	public static final String STATISTICS_STORE_DIR = "statistics";
    }


    /**
     * Constants related to MS Word     
     */    
    public final static class MSWord {
	
	/**
	 * separates the [figure|table]-number from the caption text
	 * <p>
	 * Example:<br/>
	 * <tt>Figure 2a<b>:</b> Some caption text</tt>
	 * </p>
	 *  
	 */
	@DomainSpecific
	public static final char DELIMITER_CAPTION = ':';
	
	/**
	 * separates individual list levels
	 * <p>
	 * Example:</br>
	 * <tt>1<b>.</b>1<b>.</b>2</tt>
	 * </p>
	 */
	@DomainSpecific
	public static final char DELIMITER_LISTLEVEL = '.';

	/**
	 * identifier for all paragraphs belonging to the appendix of a chapter 
	 */
	@DomainSpecific
	public static final char IDENTIFIER_APPENDIX = 'A';
	
	/**
	 * Pattern of the first appendix paragraph
	 * <p><em>Note:</em> This must contain exactly one group which matches the appendix number.</p>
	 */
	@DomainSpecific
	public static final String PATTERN_APPENDIX = "(?i)^Appendix to Chapter ([0-9]+)$";
		
	/**
	 * Pattern of the main title of the document
	 * <p><em>Note:</em> This must contain exactly one group which matches the chapter number.</p>
	 */
	@DomainSpecific
	public static final String PATTERN_TITLE = "^System Requirements Specification\\s+Chapter ([0-9]+)\\s+.*$";
		
	/**
	 * String which must be contained in the name of a style to consider it as a heading style
	 * <p>i.e. <em>Überschrift</em> would match <em>Überschrift 1</em></p>
	 */
	@DomainSpecific
	public static final String[] STYLENAME_HEADING = { "Heading", "Überschrift", "Titre" };
	
	/**
	 * The name of the style of the document title.
	 * <p>
	 * There must be {@code n >= 1} subsequent paragraphs with this style attached,
	 * which (combined) constitute the title of this document.
	 * </p>  
	 */
	@DomainSpecific
	public static final String STYLENAME_TITLE = "Document Title";
	
	/**
	 * Maximum number of words in a paragraph to consider it a possible heading
	 */
	@DomainSpecific
	public static final int WORDS_MAX_HEADING = 12;
	
	private MSWord() {}
    }

    /**
     * Constants related to Traceability strings 
     */
    public final static class Traceability {

	
	/**
	 * if {@code true} then placeholders for skipped levels will be added
	 */
	public static final boolean LEVELPLACEHOLDERS = true;
	
	
	/**
	 * if {@code true} then nested structures will be split into individual requirements
	 */
	public static final boolean NESTING = true;
	
	/**
	 * number of characters a range (table cell, footnote, ...) must at least contain to split it
	 * into several distinct requirements
	 * <p>only applicable if {@link #NESTING} == {@code true}</p> 
	 */
	public static final int RANGE_SPLIT_CHARACTER_COUNT_THRESHOLD = 15;
	
	/**
	 * array of characters which should not appear in tracestrings
	 */
	public static final char[] ILLEGALCHARACTERS = { ' ' };
	
	/**
	 * separates hierarchical parts of the trace string
	 * <p>
	 * <em>Note:</em> This is not the same as {@link MSWord#DELIMITER_LISTLEVEL}.
	 * The former defines the character which separates list levels in the input document. Whereas this defines how list levels are separated in the output document.
	 * </p> 
	 */
	public static final char DELIMITER = '.';

	/**
	 * When writing to a file replace {@link #DELIMITER} with this
	 * <p>
	 * Example:<br/>
	 * An image in requirement <tt>1.1.2</tt> may be written to the file <tt>1<b>_</b>1<b>_</b>2<b>_</b>I.wmf</tt>.
	 * </p>
	 */
	public static final char DELIMITER_FILENAMECOMPATIBLE = '_';
	
	/**
	 * identifier for the absence of a number 
	 */
	public static final char NONUMBERINDICATOR = '*';
	
	/**
	 * When writing to a file replace {@link #NONUMBERINDICATOR} with this
	 */
	public static final char NONUMBERINDICATOR_FILENAMECOMPATIBLE = '+';
	
	/**
	 * identifier for a bulleted list item
	 */
	public static final char IDENTIFIER_BULLETLIST = '*';
	
	/**
	 * identifier for captions of floating entities; appendix to the trace string
	 */
	public static final char IDENTIFIER_CAPTION = 'C';
	
	/**
	 * identifier for inline images; appendix to the trace string
	 */
	public static final char IDENTIFIER_IMAGE = 'I';
	
	/**
	 * identifier for inline equations; appendix to the trace string
	 */
	public static final char IDENTIFIER_EQUATION = 'E';
	
	/**
	 * identifier for inline footnotes; appendix to the trace string
	 */
	public static final char PREFIX_FOOTNOTE = 'N';
	
	/**
	 * identifier for inline endnotes; appendix to the trace string
	 */
	public static final char PREFIX_ENDNOTE = 'n';
	
	/**
	 * prefixes each table number; wrapped in brackets
	 */
	public static final char PREFIX_TABLE = 't';	
	
	/**
	 * prefixes each row number in a table; wrapped in brackets
	 */
	public static final char PREFIX_ROW = 'r';
	
	/**
	 * prefixes each condition in a table; wrapped in brackets
	 */
	public static final char PREFIX_CONDITION = 'C';
		
	/**
	 * prefixes IDs in a table; wrapped in brackets 
	 */
	public static final char PREFIX_ID = 'I';
	
	/**
	 * prefixes each column number in a table; wrapped in brackets
	 */
	public static final char PREFIX_COLUMN = 'c';
	
	/**
	 * prefixes each figure number; wrapped in brackets
	 */
	public static final char PREFIX_FIGURE = 'f';
	
	private Traceability() {}
    }
    
    /**
     * Constants related to the actual specification (i.e. the subset026)
     */
    public final static class Specification {
	/**
	 * keywords (regex) which indicate the respective requirement must be implemented
	 */
	@DomainSpecific
	public static final String[] LEGALOBLIGATION_KEYWORDS_MANDATORY = new String[]{"SHALL"};
	
	
	/**
	 * keywords (regex) which indicate the respective requirement may be implemented
	 */
	@DomainSpecific
	public static final String[] LEGALOBLIGATION_KEYWORDS_OPTIONAL = new String[]{"MAY"};
	
	/**
	 * keywords (regex) which often appear but whose meaning is not defined
	 */
	@DomainSpecific
	public static final String[] LEGALOBLIGATION_KEYWORDS_UNKNOWN = new String[]{"CAN(?:NOT)?", "MUST", "WILL", "MIGHT", "(?<!MAY )OPTIONALLY"};
	
	/**
	 * Constructs in the specification which shall have special highlighting applied (use for the "implementerEnhanced" field in the ReqIF output)
	 */
	public final static class SpecialConstructs {
	    
	    /**
	     * use Natural Language Processing to enhance tagging of requirement texts
	     */
	    public static final boolean USE_NLP = false;
	    
	    
	    /**
	     * check for recurring phrases, highlight and link them
	     */
	    public static final boolean DETECT_KNOWNPHRASES = true;
	    
	    /**
	     * Words / Phrases to be marked as "weak"; from literature; regex syntax allowed
	     */
	    public static final String[] WEAKWORDS_LITERATURE = { // Note: "about" may be added to this list; but non in the context "inform .* about"
		    "above", "adequate", "anything", "approximately", "as soon as", "bad", "believe", "below", "best", "better", "but not limited to", "clear", "cyclically", "easy", "eventually", "extremely", "feel", "generally", "good", "hope", "if appropriate", "if needed", "if possible", "immediately", "in round numbers", "more or less", "overall", "possibly", "recent", "repeatedly", "rough", "seem", "significant", "something", "strong", "think", "useful", "very(?! (?:first|last))", "worst"
	    }; // compiled from \cite{Knight2012}, \cite{Dupre1998}, \cite{Fabbrini}, \cite[p. 164]{Wilson1997}, \cite[clause 5.2.7]{ISO2011}
	    
	    
	    /**
	     * Words / Phrases to be marked as "weak"; from actual specification; regex syntax allowed
	     */
	    public static final String[] WEAKWORDS_SPECIFICATION =  { "all necessary", "at (?:minimum|least)", "defined time", Pattern.quote("e.g.") , "for example", Pattern.quote("etc."), "even (?:if|when)", "if necessary", "no[nt] exhaustive(?:ly)?", "some (?:information|situation(?:s|\\(s\\))?)", "temporarily", "once (?:\\w+\\s)+?is terminated", "other (?:\\w+\\s)+?sources", "certain moment(?:s|\\(s\\))?", "obviously", "hereafter", "tends (?:to)?", "mostly", "suddenly", "accidental", "(?:(?:an)?other|different) reason(?:s|\\(s\\))?", "continuously", "when needed",
	    }; // own compilation
	    
	    /**
	     * Character sequence (regex) to be marked as "weak"; does not have to be a word; regex syntax allowed
	     */
	    public static final String[] WEAK_NOWORD = { Pattern.quote("...") };
	    
	    /**
	     * Words / Phrases to be marked as "Condition"; regex syntax allowed
	     */
	    public static final String[] CONDITION = { "if", "when(?: applicable)?", "in case(?: of)?", "whether", "where available" };
	    
	    /**
	     * Words / Phrases to be marked as "Loop"; regex syntax allowed
	     */
	    public static final String[] LOOP = { "For (?:all|each|every)", "again", "repeat(?:ed(?:ly)?)?", "repetition(?:s|\\(s\\))?" };
	    
	    /**
	     * Words / Phrases to be marked as "Time"; regex syntax allowed
	     */
	    public static final String[] TIME = { "while", "during", "until", "after", "not (?:\\w+\\s)?yet", "waiting time", "time delay", "timer?", "delay", "wait(?:ing)?", "as long as" };
	    
	    /**
	     * Words / Phrases to be marked as "Again"; regex syntax allowed
	     */
	    public static final String[] AGAIN = { "re-\\w+", "revalidat(?:ed?|ion)", "reenter(?:ed)?"};
	    
	    /**
	     * Words / Phrases to be marked as "External"; regex syntax allowed
	     */
	    public static final String[] EXTERNAL = { "driver(?:[’']s)?(?! ID)", "signalman(?:[’']s)?", "external(?: (?:interface|device))?", "(?-i:TRK)", "trackside", "(?-i:RBC)(?! ID)", "Radio Block Cent(?:er|re)", "(?-i:LEU)", "Line ?side electronic unit", "National system", "(?-i:RIU)", "Radio In-?fill Unit", "(?-i:LRBG(?:s|\\(s\\))?)", "(?:Last Relevant )?balise group(?:s|\\(s\\))?", "(?:Euro)?(?:balise|loop)(?:s|\\(s\\))?(?! (?:antenna|telegram))", "(?-i:LTM)", "Loop Transmission Module", };
	    
	    /**
	     * Words / Phrases to be marked as "Self"; regex syntax allowed
	     */
	    public static final String[] SELF = { "ERTMS/ETCS on-?board(?: equipment|unit)?", "on-?board (?:equipment|unit)" };
	}
    }

    /**
     * Constants related to SpecRelations (i.e. links betweeen requirements)
     */
    public final static class Links {
	
	/**
	 * Whether or not to extract internal links
	 * 
	 * <p> That is:
	 * <ol>
	 * <li> links within the same document </li>
	 * <li> which are clearly marked as such (by using Word's crossreferencing capabilities)</li>
	 * </ol>
	 * </p>
	 */
	public static final boolean EXTRACT_LINKS = true;	
	
	/**
	 * Whether or not to extract internal links which are <em>not</em> marked as such (i.e. this allows a heuristic to kick in)
	 * <p>(only evaluated if {@link #EXTRACT_LINKS} is {@code true})
	 */
	public static final boolean EXTRACT_FAKE_LINKS = true;
	
	/**
	 * Whether or not to extract external links
	 * <p>(only evaluated if {@link #EXTRACT_LINKS} and {@link #EXTRACT_FAKE_LINKS} are {@code true})</p>
	 */
	public static final boolean EXTRACT_EXTERNAL_LINKS = true;
    }
    
    private Constants() {}
}
