package requirement;

import helper.TraceabilityManagerHumanReadable;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Requirement which constitutes the root of a requirement tree
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class RequirementRoot {    
    private final static RequirementRoot PARENT = null;
    private final Set<RequirementWParent> children = new LinkedHashSet<>(); // NOPMD - intentionally non-transient; iterator below
    private final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable(); // NOPMD - accessor below    

    /**
     * @return an iterator over all the hierarchical children of this requirement
     */
    public final Iterator<RequirementWParent> getChildIterator() {
	return this.children.iterator();
    }
    
    /**
     * @return the human readable manager associated with this requirement
     */
    public TraceabilityManagerHumanReadable getHumanReadableManager() {
	return this.hrManager;
    }

    /**
     * Get the parent of this requirement
     * 
     * @return always {@code null}
     */
    @SuppressWarnings("static-method")
    public RequirementRoot getParent() {
	// cannot be static because it will be overridden in the class-hierarchy with non-static data
	return PARENT;
    }

    /**
     * Add a child
     * 
     * @param child hierarchical child to add to this requirement
     * @throws IllegalArgumentException if the given child is {@code null}
     */
    protected final void addChild(final RequirementWParent child) {
	if (child == null) throw new IllegalArgumentException("child cannot be null.");
	
	this.children.add(child);
    }
}