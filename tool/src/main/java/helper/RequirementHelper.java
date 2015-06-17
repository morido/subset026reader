package helper;

import requirement.RequirementRoot;
import requirement.RequirementTemporary;

/**
 * Helper class for requirement tree processing
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public enum RequirementHelper {
    ;

    /**
     * Check if a parent-child relation exists between two requirements
     * 
     * @param child starting element
     * @param suspectedParent suspected parent 
     * @return {@code true} if a parent-child relation exists; {@code false} otherwise
     */
    public final static boolean isParentalRelation(final RequirementRoot child, final RequirementRoot suspectedParent) {	
	RequirementRoot current = child;

	final boolean output;
	HierarchyWalker: {
	    while (current != suspectedParent) { // NOPMD - intentional object reference comparison
		if (current != null) current = current.getParent();
		else { output = false; break HierarchyWalker; }			
	    }
	    output = true;
	}
	return output;		
    }
    
    
    /**
     * Check if a requirement is rooted or belongs to a temporary (throw-away) hierarchy
     * 
     * @param requirement requirement to check
     * @return {@code true} if rooted; {@code false} otherwise
     */
    public final static boolean isRooted(final RequirementRoot requirement) {
	RequirementRoot current = requirement;
	
	final boolean output;
	HierarchyWalker: {
	    while (current != null) {
		if (current instanceof RequirementTemporary && current.getParent() == null) {
		    output = false; break HierarchyWalker;
		}
		current = current.getParent();
	    }
	    output = true;
	}
	return output;
    }
}
