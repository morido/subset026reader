package requirement;

import helper.TraceabilityManagerHumanReadable;
import helper.TraceabilityManagerHumanReadable.TagType;
import helper.annotations.DomainSpecific;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a special traceability linker which only manages non-qualified references to previously encountered requirements
 * 
 * <p>Thus it allows to find the last occurrens of some "see b)"-requirement and must be used during first-pass.</p>
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class TraceabilityLinkerNonQualified {
    private final Map<String, RequirementWParent> nonQualifiedRequirementResolver = new HashMap<>();
        
    /**
     * Ordinary constructor
     */
    protected TraceabilityLinkerNonQualified() {
	// class may not be instantiated from outside this package
    }


    /**
     * Add this requirement to the store for non-qualified requirement references,
     * call may overwrite previously stored requirements
     * 
     * @param requirement requirement to process
     */
    @DomainSpecific
    protected void addRequirement(final RequirementWParent requirement) {
	assert requirement != null;
	final TraceabilityManagerHumanReadable hrManager = requirement.getHumanReadableManager();
	if (hrManager.getCurrentTagType() == TagType.LEVELNUMBER) {
	    final String leastSignificantPart = hrManager.getLeastSignificantTagContents();
	    // such requirements are only referenced if their very last part if in the a-z range
	    if (leastSignificantPart.matches("[a-z]")) {		
		// override any previously stored values (so lookups will return the last stored value)		
		this.nonQualifiedRequirementResolver.put(leastSignificantPart, requirement);
	    }
	}	
    }
    
    /**
     * Get the fully qualified ID of the last encountered requirement whose ID matches with the given leastSignificantPart 
     * 
     * @param leastSignificantIdPart part of the ID after the last delimiter
     * @return the fully qualified ID of the requirement or {@code null} if there is no such requirement
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public String resolveNonQualifiedId(final String leastSignificantIdPart) {
	if (leastSignificantIdPart == null) throw new IllegalArgumentException("leastSignificantIdPart cannot be null.");
	final RequirementWParent targetRequirement = getRequirementNonQualified(leastSignificantIdPart);
	final String output;	
	if (targetRequirement != null) output = targetRequirement.getHumanReadableManager().getTag();	
	else output = null;
	return output;
    }
    
    /**
     * Get the last requirement whose least-significant id part equals the given one
     * 
     * @param leastSignificantIdPart part of the ID after the last delimiter
     * @return the requirement corresponding to the query or {@code null} if no such requirement exists
     */
    private RequirementWParent getRequirementNonQualified(final String leastSignificantIdPart) {
	assert leastSignificantIdPart != null;
	return this.nonQualifiedRequirementResolver.get(leastSignificantIdPart);
    }        
}
