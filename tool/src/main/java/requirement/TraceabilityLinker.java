package requirement;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Manages tracedata. Provides means to:
 *  <ol>
 *  <li>check for uniqueness of generated tracestrings</li>
 *  <li>allow forward tracing from Word to the requirement</li>
 *  <li>allow backward tracing from the requirement to Word</li>
 *  </ol>
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 */
public final class TraceabilityLinker {
    /**
     * maps word offsets onto requirements
     */
    private final NavigableMap<Integer, RequirementWParent> wordToRequirementLinker = new TreeMap<>();
    /**
     * maps requirement id onto word offsets
     */
    private final Map<String, Integer> requirementIdToWordLinker = new HashMap<>();    
    private final Map<String, RequirementWParent> figureResolver = new HashMap<>();
    private final Map<String, RequirementWParent> tableResolver = new HashMap<>();
    private final TraceabilityLinkerNonQualified traceabilityLinkerNonQualified = new TraceabilityLinkerNonQualified();

    /**
     * Adds a new link between the MS Word Trace-Id and the actual requirement / human-readable Id 
     * 
     * @param requirement
     * @throws IllegalArgumentException the requirement's tracestring has been used elsewhere already
     */
    public void addRequirementLink(final RequirementWParent requirement) {
	if (requirement == null) throw new IllegalArgumentException("Given requirement cannot be null.");			
	final String hrTag = requirement.getHumanReadableManager().getTag();
	if (this.requirementIdToWordLinker.containsKey(hrTag)) throw new IllegalArgumentException("The tracestring for " + requirement.getHumanReadableManager().getTag() + " is not unique.");
		
	this.requirementIdToWordLinker.put(hrTag, requirement.getTraceId());
	this.wordToRequirementLinker.put(requirement.getTraceId(), requirement);
	this.traceabilityLinkerNonQualified.addRequirement(requirement);
    }
  
    /**
     * Add a new link between a given (non-fully qualified) figure number and its caption
     * 
     * @param figureNumber part of the tracestring containing the number
     * @param captionRequirement caption of the figure
     */
    public void addFigureLink(final String figureNumber, final RequirementWParent captionRequirement) {
	if (figureNumber == null) throw new IllegalArgumentException("figureNumber cannot be null.");	
	this.figureResolver.put(figureNumber, captionRequirement);
    }
    
    /**
     * Add a new link between a given (non-fully qualified) table number and its caption
     * 
     * @param tableNumber part of the tracestring containing the number
     * @param captionRequirement caption of the table
     */
    public void addTableLink(final String tableNumber, final RequirementWParent captionRequirement) {
	if (tableNumber == null) throw new IllegalArgumentException("figureNumber cannot be null.");	
	this.tableResolver.put(tableNumber, captionRequirement);
    }
    
    /**
     * Backward tracing
     * 
     * @param humanReadbleId
     * @return the character start offset in the Word document for a given requirement
     * @see #getRequirement(int) reverse method
     * @throws IllegalArgumentException if the humanReadableId is {@code null}
     */
    public Integer getWordId(final String humanReadbleId) {
	if (humanReadbleId == null) throw new IllegalArgumentException("humanReadableId cannot be null.");
	return this.requirementIdToWordLinker.get(humanReadbleId);
    }

    /**
     * Forward tracing
     * 
     * @param wordId character offset for which to find the respective requirement
     * @return a requirement or {@code null} if there is no requirement at the given offset
     * @see #getWordId(String) reverse method
     */
    public RequirementWParent getRequirement(final int wordId) {
	return this.wordToRequirementLinker.get(wordId);
    }

    
    /**
     * get a requirement by its identifier
     * 
     * @param humanReadableId
     * @return the requirement corresponding to the given humanReadableId or {@code null} if no such requirement exists
     */
    public RequirementWParent getRequirement(final String humanReadableId) {
	if (humanReadableId == null) throw new IllegalArgumentException("humanReadableId cannot be null.");
	final Integer wordId = getWordId(humanReadableId);
	return wordId == null ? null : getRequirement(wordId);
    }        
    
    /**
     * Resolve a non-fully qualified table number to the caption-requirement of the respective table
     * 
     * @param tableNumber number of the table (as written in the tracestring)
     * @return the requirement corresponding to the given table or {@code null} if no such requirement exists
     */
    public RequirementWParent getFullyQualifiedIdForTable(final String tableNumber) {
	return this.tableResolver.get(tableNumber);
    }
    
    /**
     * Resolve a non-fully qualified figure number to the caption-requirement of the respective figure
     * 
     * @param figureNumber number of the figure (as written in the tracestring)
     * @return the requirement corresponding to the given figure or {@code null} if no such requirement exists
     */
    public RequirementWParent getFullyQualifiedIdForFigure(final String figureNumber) {
	return this.figureResolver.get(figureNumber);
    }

    /**
     * Finds the requirement which contains (spans over) a given wordId; used for bookmark lookups  
     * 
     * @param wordId character offset for which to find the enclosing requirement
     * @return enclosing Requirement
     */
    public RequirementWParent getEnclosingRequirement(final int wordId) {
	return this.wordToRequirementLinker.floorEntry(wordId).getValue();
    }

    /**
     * @return a handle to the manager for non-qualified requirement references, never {@code null}
     */
    public TraceabilityLinkerNonQualified getNonQualifiedManager() {
	return this.traceabilityLinkerNonQualified;
    }
    
    /**
     * Get the number of requirements which are currently managed by this linker instance
     * 
     * @return the number of requirements
     */
    public int getNumberOfRequirements() {
	return this.requirementIdToWordLinker.size();
    }    
}
