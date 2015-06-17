package helper;

import static helper.Constants.MSWord.DELIMITER_LISTLEVEL;
import static helper.Constants.Traceability.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import requirement.RequirementWParent;


/**
 * Creates human-readable trace strings and ensures a proper hierarchy of the individual components
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public final class TraceabilityManagerHumanReadable {    
    /**
     * type of a tracestring element     
     */
    public enum TagType {
	/**
	 * root level; only used internally; has no visual representation
	 */
	ROOT(true), 
	/**
	 * text which should prepend each tracetag
	 */
	GLOBALPREPENDER(true), 	
	/**
	 * level in a list
	 */
	LEVELNUMBER(false),
	/**
	 * a floating table or figure 
	 */
	FLOATINGOBJECT(true),
	/**
	 * row in a table 
	 */
	ROW(true),
	/**
	 * column in a table 
	 */
	COLUMN(true),
	/**
	 * some inline element inside a paragraph 
	 */
	PARAGRAPHELEMENT(false);	
	
	private final boolean lowerLevelMustFollow;
	
	/**
	 * @param follower tag type which shall follow this type
	 * @return {@true} if a given tag may be legitimately appended after this tag; {@code false} otherwise
	 */
	boolean followerLegitimate(final TagType follower) {
	    assert follower != null;
	    final boolean output;	
	
	    if (follower == TagType.LEVELNUMBER && (this == TagType.COLUMN || this == TagType.PARAGRAPHELEMENT)) {
		// nested recursion exception; PARAGRAPHELEMENT is necessary for footnotes
		output = true;
	    }
	    else {
		// ordinary case
		if (this.lowerLevelMustFollow) output = (follower.getLevel() > this.getLevel());						
		else output = (follower.getLevel() >= this.getLevel());
	    }
	    return output;	    
	}

	private TagType(final boolean lowerLevelMustFollow) {
	    this.lowerLevelMustFollow = lowerLevelMustFollow;	    
	}
	
	/**
	 * @return an numerical representation of the significance of this level (the higher this number, the less significant is this level)
	 */
	private int getLevel() {
	    return this.ordinal();
	}	
    }

    /**
     * Possible prefixes for rows in tables
     */
    public enum RowLevelPrefix {
	/**
	 * row which contains a condition (i.e. entire row belongs to this condition)
	 */
	CONDITION(wrapInBrackets(PREFIX_CONDITION)),	
	/**
	 * row which contains an id (i.e. entire row is referred to by this ID)
	 */
	ID(wrapInBrackets(PREFIX_ID));

	private final String prepender;

	private RowLevelPrefix(final String prepender) {
	    this.prepender = prepender;
	}

	/**
	 * Get the prepender string for the current element
	 * 
	 * @return prepender string; never {@code null}
	 */
	private String getPrepender() {
	    // only called from within the surrounding class
	    return this.prepender;
	}
    }

    /**
     * Immutable class to store one concrete level in a traceTag     
     */
    private final static class TraceTagItem {
	public final TagType tagType;
	public final String text;
	
	public TraceTagItem(final TagType tagType, final String text) {
	    assert tagType != null && text != null;
	    this.tagType = tagType;
	    this.text = text;	    
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
	    // autogenerated by Eclipse
	    final int prime = 31;
	    int result = 1;
	    result = prime * result
		    + ((this.tagType == null) ? 0 : this.tagType.hashCode());
	    result = prime * result
		    + ((this.text == null) ? 0 : this.text.hashCode());
	    return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
	    // autogenerated by Eclipse
	    if (this == obj) {
		return true;
	    }
	    if (obj == null) {
		return false;
	    }
	    if (getClass() != obj.getClass()) {
		return false;
	    }
	    final TraceTagItem other = (TraceTagItem) obj;
	    if (this.tagType != other.tagType) {
		return false;
	    }
	    if (this.text == null) {
		if (other.text != null) {
		    return false;
		}
	    } else if (!this.text.equals(other.text)) {
		return false;
	    }
	    return true;
	}
    }
    
    private final List<TraceTagItem> traceTag;
    private RequirementWParent linkedRequirement = null;
    private static final Logger logger = Logger.getLogger(TraceabilityManagerHumanReadable.class.getName()); // NOPMD - Reference rather than a static field

    /**
     * Ordinary constructor
     */
    public TraceabilityManagerHumanReadable() {
	// Note: This manager does not necessarily start at TagLevel.Root	
	this.traceTag = new ArrayList<>();	
    }
    
    /**
     * Constructor which creates a tracetag from the existing tag of a parent and appends some injected data
     * 
     * @param parent manager which serves as the base (i.e. the left part of the resulting traceability string)
     * @param injectedData data which is to be appended to the parent
     * @throws IllegalArgumentException if one of the parameters is {@code null}
     */
    public TraceabilityManagerHumanReadable(final TraceabilityManagerHumanReadable parent, final TraceabilityManagerHumanReadable injectedData) {
	if (parent == null) throw new IllegalArgumentException("The parent of this traceability manager cannot be null.");
	if (injectedData == null) throw new IllegalArgumentException("The data to be injected cannot be null.");

	this.traceTag = new ArrayList<>(parent.traceTag);
	append(injectedData);
    }

    /**
     * Constructor which creates a tracetag from the existing tag of a parent, appends some injected data and is linked to a requirement
     * 
     * @param parent manager which serves as the base (i.e. the left part of the resulting traceability string)
     * @param injectedData data which is to be appended to the parent
     * @param linkedRequirement the requirement this hrManager shall be linked to 
     */
    public TraceabilityManagerHumanReadable(final TraceabilityManagerHumanReadable parent, final TraceabilityManagerHumanReadable injectedData, final RequirementWParent linkedRequirement) {
	this(parent, injectedData);
	if (linkedRequirement == null) throw new IllegalArgumentException("linkedRequirement cannot be null.");
	this.linkedRequirement = linkedRequirement;
    }
   
    /**
     * Copy constructor
     * 
     * @param copySource manager from where to copy
     */
    public TraceabilityManagerHumanReadable(final TraceabilityManagerHumanReadable copySource) {
	if (copySource == null) throw new IllegalArgumentException("The source manager cannot be null.");
	
	this.traceTag = new ArrayList<>(copySource.traceTag);
    }
    
    /**
     * Create a new manager which includes all elements of {@code copySource} until the least-significant (last) occurrence of {@code tagTypeOfInterest}
     * 
     * @param copySource manager from where to copy
     * @param tagTypeOfInterest type of last tag to include (if present several times then take the least significant one)
     */
    public TraceabilityManagerHumanReadable(final TraceabilityManagerHumanReadable copySource, final TagType tagTypeOfInterest) {
	if (copySource == null) throw new IllegalArgumentException("The source manager cannot be null.");	
	
	// iterate over the taglist of the source in reverse order until tagTypeOfInterest is found
	// if tagTypeOfInterest does not exist we will create an empty manager
	// this is a little tricky in terms of off-by-1; essentially the offset points one too far to the right, but that is ok for subList
	final ListIterator<TraceTagItem> iterator = copySource.traceTag.listIterator(copySource.traceTag.size());
	int offsetOfLastElementToCopy = 0;
	while (iterator.hasPrevious() && iterator.previous().tagType != tagTypeOfInterest) { offsetOfLastElementToCopy++; }
	
	assert offsetOfLastElementToCopy <= copySource.traceTag.size();
	this.traceTag = new ArrayList<>(copySource.traceTag.subList(0, copySource.traceTag.size()-offsetOfLastElementToCopy));
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
	// autogenerated by Eclipse
        final int prime = 31;
        int result = 1;
        result = prime * result
        	+ ((this.traceTag == null) ? 0 : this.traceTag.hashCode());
        return result;
    }

    /**
     * Check if this manager is equal to another one; will not compare the values of {{@link #getLinkedRequirement()}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
	// autogenerated by Eclipse
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TraceabilityManagerHumanReadable other = (TraceabilityManagerHumanReadable) obj;
        if (this.traceTag == null) {
            if (other.traceTag != null) {
        	return false;
            }
        } else if (!this.traceTag.equals(other.traceTag)) {
            return false;
        }
        return true;
    }

    /**
     * @return the requirement this manager is linked to or {@code null} if this manager is not linked to any requirement
     */
    public RequirementWParent getLinkedRequirement() {
	return this.linkedRequirement;
    }
    
    /**
     * Add a list to the current traceTag
     * 
     * @param list fully qualified traceString of the list
     * @throws IllegalArgumentException if the given list string is {@code null}
     */
    public void addList(final String list) {
	// may contain delimiter character; separation below
	if (list == null) throw new IllegalArgumentException("List string cannot be null.");
	final String text = list;
	final TagType tagType = TagType.LEVELNUMBER;

	// list may contain several levels; separate them
	for (final String currentLevelText : text.split(RegexHelper.quoteRegex(DELIMITER_LISTLEVEL))) {
	    addGeneric(tagType, currentLevelText);
	}	
    }

    /**
     * Add a bullet of a bulleted list to the current traceTag
     * 
     * @param bulletNum (implicit) running number of the bullet inside the list
     * @throws IllegalArgumentException If the bullet number was out of range
     */
    public void addBullet(final int bulletNum) {
	if (bulletNum < 1) throw new IllegalArgumentException("Illegal bullet number passed.");
	final String text = wrapInBrackets(IDENTIFIER_BULLETLIST) + Integer.toString(bulletNum);
	final TagType tagType = TagType.LEVELNUMBER;

	addGeneric(tagType, text);
    }

    /**
     * Add a floating figure to the current traceTag
     * 
     * @param figureNum running number of the figure; can be {@code null} to indicate there is no number
     * @throws IllegalArgumentException if the argument contains illegal characters
     */
    public void addFigure(final String figureNum) {	
	if (containsIllegalCharacter(figureNum)) throw new IllegalArgumentException("figureNum contains an illegal character.");
	final String text = wrapInBrackets(PREFIX_FIGURE) + numberRewriter(figureNum);
	final TagType tagType = TagType.FLOATINGOBJECT;

	addGeneric(tagType, text);	
    }
    
    /**
     * Add a floating table to the current traceTag
     * 
     * @param tableNum running number of the table; can be {@code null} to indicate there is no number
     * @throws IllegalArgumentException if the argument contains illegal characters
     */
    public void addTable(final String tableNum) {
	if (containsIllegalCharacter(tableNum)) throw new IllegalArgumentException("tableNum contains an illegal character.");
	final String text = wrapInBrackets(PREFIX_TABLE) + numberRewriter(tableNum);
	final TagType tagType = TagType.FLOATINGOBJECT;

	addGeneric(tagType, text);	
    }

    /**
     * Add a caption for floating tables/figures to the current traceTag
     */
    public void addCaption() {
	final String text = Character.toString(IDENTIFIER_CAPTION);
	final TagType tagType = TagType.COLUMN;

	addGeneric(tagType, text);
    }

    /**
     * Adds an image reference to the current traceTag
     * 
     * @param imageNum the running number of the image in the current paragraph
     * @throws IllegalArgumentException If the imageNum is out of range
     */
    public void addImage(final int imageNum) {
	if (imageNum < 1) throw new IllegalArgumentException("Illegal image number passed.");
	final StringBuilder text = new StringBuilder(4);
	text.append(IDENTIFIER_IMAGE);
	if (imageNum > 1) text.append(wrapInBrackets(imageNum));
	final TagType tagType = TagType.PARAGRAPHELEMENT;

	addGeneric(tagType, text.toString());
    }

    /**
     * Add an equation reference to the current traceTag
     * 
     * @param equationNum the running number of the equation in the current paragraph
     * @throws IllegalArgumentException If the equartionNum is out of range
     */
    public void addEquation(final int equationNum) {
	if (equationNum < 1) throw new IllegalArgumentException("Illegal equation number passed.");
	final StringBuilder text = new StringBuilder(4);
	text.append(IDENTIFIER_EQUATION);		
	if (equationNum > 1) text.append(wrapInBrackets(equationNum));		
	final TagType tagType = TagType.PARAGRAPHELEMENT;

	addGeneric(tagType, text.toString());
    }

    /**
     * Add a footnote-reference to the current traceTag
     * 
     * @param noteNum running number of the footnote
     * @throws IllegalArgumentException If the noteNum is out of range
     */
    public void addFootnote(final int noteNum) {
	if (noteNum < 1) throw new IllegalArgumentException("Illegal noteNum passed.");
	final StringBuilder text = new StringBuilder(4);
	text.append(wrapInBrackets(PREFIX_FOOTNOTE));
	text.append(noteNum);
	final TagType tagType = TagType.PARAGRAPHELEMENT;
	
	addGeneric(tagType, text.toString());
    }
    
    /**
     * Add an endnote-reference to the current traceTag
     * 
     * @param noteNum running number of the endnote
     * @throws IllegalArgumentException If the noteNum is out of range
     */
    public void addEndnote(final int noteNum) {
	if (noteNum < 1) throw new IllegalArgumentException("Illegal noteNum passed.");
	final StringBuilder text = new StringBuilder(4);
	text.append(wrapInBrackets(PREFIX_ENDNOTE));
	text.append(noteNum);
	final TagType tagType = TagType.PARAGRAPHELEMENT;
	
	addGeneric(tagType, text.toString());
    }
    
    /**
     * Add a table row to the current traceTag
     * 
     * @param rowNum running number of the row in the current table
     * @throws IllegalArgumentException If the rowNum was out of range
     */
    public void addRow(final int rowNum) {
	if (rowNum < 0) throw new IllegalArgumentException("Illegal row number passed.");
	//rows are 0-based; make them 1-based
	final String text = wrapInBrackets(PREFIX_ROW) + wrapInBrackets(rowNum+1);
	final TagType tagType = TagType.ROW;

	addGeneric(tagType, text);
    }

    /**
     * Add a custom row reference to the current traceTag
     * 
     * @param prepender enum-value whose associated string will be used as the prepender for this rowNum
     * @param rowNum running number of the row in the current table as a string
     * @throws IllegalArgumentException if one of the arguments is {@code null} or contains illegal characters 
     */
    public void addRow(final RowLevelPrefix prepender, final String rowNum) {
	// Note: rowNum is most likely an integer wrapped in a String.
	if (prepender == null) throw new IllegalArgumentException("prepender cannot be null.");
	if (rowNum == null) throw new IllegalArgumentException("rowNum cannot be null.");
	if (containsIllegalCharacter(rowNum)) throw new IllegalArgumentException("uniqueIdentifier contains an illegal character.");
	final String text = prepender.getPrepender() + rowNum;
	final TagType tagType = TagType.ROW;		

	addGeneric(tagType, text);
    }
    
    /**
     * Add a table column reference to the current traceTag
     * 
     * @param columnNum running number of the column in the current row
     * @throws IllegalArgumentException If the columnNum was out of range
     */
    public void addColumn(final int columnNum) {
	if (columnNum < 0) throw new IllegalArgumentException("Illegal column number passed.");
	//columns are 0-based; make them 1-based 
	final String text = wrapInBrackets(PREFIX_COLUMN) + wrapInBrackets(columnNum+1);
	final TagType tagType = TagType.COLUMN;

	addGeneric(tagType, text);
    }

    /**
     * Add a custom column reference to the current traceTag
     * 
     * @param uniqueIdentifier String to use for this traceTag-level; uniqueness of this identifier may be table-wide (i.e. no additional row identifier is necessary) 
     * @throws IllegalArgumentException if the uniqueIdentifier is {@code null} or contains invalid characters
     */
    public void addColumn(final String uniqueIdentifier) {
	if (uniqueIdentifier == null) throw new IllegalArgumentException("uniqueIdentifier cannot be null.");
	if (containsIllegalCharacter(uniqueIdentifier)) throw new IllegalArgumentException("uniqueIdentifier contains an illegal character.");
	// uniqueIdentifier is not wrapped in brackets
	final String text = uniqueIdentifier;
	final TagType tagType = TagType.COLUMN;		

	addGeneric(tagType, text);
    }
    
    /**
     * @return the current traceTag suitable for use as a filename component; never {@code null}
     */
    public String getTagForFilename() {
	assert this.traceTag != null;
	if (getCurrentTagType() != TagType.PARAGRAPHELEMENT) throw new IllegalStateException("A filename cannot be created at this level.");
	final String output = getTag(Character.toString(DELIMITER_FILENAMECOMPATIBLE)).replace(NONUMBERINDICATOR, NONUMBERINDICATOR_FILENAMECOMPATIBLE); 
		
	// Check if filename is suitable for writing to disk
	// Step 1: Generic check
	if (output.contains("\0") || output.contains("/")) throw new IllegalStateException("Filename is invalid. Cannot write to disk."); // this works nowhere. Not even on *nix.
	// Step 2: Check for MS Windows; see http://msdn.microsoft.com/en-us/library/aa365247%28VS.85%29#naming_conventions
	final char[] illegalFilenameCharsWin = {'\\', ':', '?', '"', '<', '>'};
	for (final char currentCandidate : illegalFilenameCharsWin) {
	    if (output.contains(Character.toString(currentCandidate))) {
		logger.log(Level.WARNING, "Filename string \"{0}\" contains characters which are illegal on MS Windows. Consider using *nix or changing the preferences for traceString generation.", output);
		break;
	    }	    
	}	
	return output;
    }
    
    /**
     * @return least significant tagType which is currently contained in this manager's tracestring
     */
    public TagType getCurrentTagType() {
	 return (!this.traceTag.isEmpty()) ? this.traceTag.get(this.traceTag.size()-1).tagType : TagType.ROOT;
    }
    
    /**
     * @return label of the least significant part of this tracestring
     */
    public String getLeastSignificantTagContents() {
	return (!this.traceTag.isEmpty()) ? this.traceTag.get(this.traceTag.size()-1).text : "";
    }
    
    /**
     * @return the current traceTag; never {@code null}
     */
    public String getTag() {
	return getTag(Character.toString(DELIMITER));	
    }
    
    /**
     * @return the hierarchical level of the tag this manager represents (0 means root; +1 for each level)
     */
    public int getHierarchicalLevel() {
	return this.traceTag.size();
    }
    
    /**
     * Compute the current traceTag
     * 
     * @param delimiterToUse delimiter for the individual levels of the resulting tracetag
     * @return a string representation of the current state of the traceTag in this manager
     */
    private String getTag(final String delimiterToUse) {
	assert this.traceTag != null;
	assert delimiterToUse != null;
	final StringBuilder output = new StringBuilder();
	String delimiter = "";
	for (int i = 0; i < this.traceTag.size(); i++) {
	    output.append(delimiter);
	    output.append(this.traceTag.get(i).text);
	    delimiter = delimiterToUse;
	}
	return output.toString();
    }
    
    /**
     * Add a traceTag element
     * 
     * @param levelTag
     * @param text
     * @throws IllegalStateException If a caller tried to insert a trace tag element which would corrupt the hierarchy
     */
    private void addGeneric(final TagType levelTag, final String text) {
	assert (levelTag != null && text != null): "Parameters are malformed.";
	if (checkLevel(levelTag)) {		
	    this.traceTag.add(new TraceTagItem(levelTag, text));	
	}
	else throw new IllegalStateException("Given trace tag element cannot be legally inserted at this point.");		
    }
    
    /**
     * Checks if a targetTag can be legally inserted to this hrManager instance
     * 
     * @param targetTag tag to insert
     * @return {@code true} if insertion is legal; {@code false} otherwise
     */
    private boolean checkLevel(final TagType targetTag) {
	return getCurrentTagType().followerLegitimate(targetTag);	
    }
    
    /**
     * Merges the traceTags of two hrManagers
     * 
     * @param injectedData the hrData to be appended
     */
    private void append(final TraceabilityManagerHumanReadable injectedData) {
	assert injectedData != null;
	
	// can we legally append?; in contrast to addGeneric() we only have to do this once
	if (!checkLevel(injectedData.getCurrentTagType())) throw new IllegalArgumentException("tracetag data cannot be legally appended here.");
	
	final Iterator<TraceTagItem> iterator = injectedData.traceTag.iterator();
	while (iterator.hasNext()) {
	    this.traceTag.add(iterator.next());
	}	
    }

    private static String wrapInBrackets(final char input) {
	return wrapInBrackets(Character.toString(input));
    }
    
    private static String wrapInBrackets(final int input) {
	return wrapInBrackets(Integer.toString(input));	
    }
    
    private static String wrapInBrackets(final String input) {
	assert input != null && !input.startsWith("[") && !input.endsWith("]");
	return '[' + input + ']';
    }
    
    /**
     * Takes care about items which actually have no number assigned
     * 
     * @param number Number under consideration
     * @return The item number or a special string indicating the absence of a number
     */
    private static String numberRewriter(final String number) {
	return (number == null) ? Character.toString(NONUMBERINDICATOR) : number;
    }
    
    /**
     * @param input String to check; may be {@code null}
     * @return {@code true} if a string which may be supplied by the user contains an illegal character; {@code false} otherwise
     */
    private static boolean containsIllegalCharacter(final String input) {
	final boolean output;

	if (input == null) output = false;	    
	else if (input.contains(Character.toString(DELIMITER))) output = true;		    		
	else {
	    illegalCharacterDeterminer: {
	    for (final char currentChar : ILLEGALCHARACTERS) {
		if (input.contains(Character.toString(currentChar))) {
		    output = true;
		    break illegalCharacterDeterminer;
		}
	    }
	    output = false;
	}
	}
	return output;
    }
}