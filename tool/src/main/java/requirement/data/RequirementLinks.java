package requirement.data;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static helper.Constants.Links.EXTRACT_EXTERNAL_LINKS;
import docreader.ReaderData;
import requirement.RequirementProxy;
import requirement.RequirementTemporary;
import requirement.RequirementWParent;
import requirement.TraceableArtifact;
import requirement.metadata.Kind;

/**
 * Stores and resolves links to other requirements
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class RequirementLinks {
    private final transient RequirementTemporary sourceRequirement;
    private final Set<Link> rawLinks = new LinkedHashSet<>();     
    private static final Logger logger = Logger.getLogger(RequirementLinks.class.getName()); // NOPMD - Reference rather than a static field

    /**
     * Interface for a link; all implementing classes should override equals() and hashCode() so the set rawLinks wont contain duplicates 
     */
    private interface Link {	
	void process(final ReaderData readerData, final Set<TraceableArtifact> outputList);
    }
    
    private static abstract class AbstractLink<T> implements Link {
	protected final T reference;

	protected AbstractLink(final T reference) {
	    assert reference != null;
	    this.reference = reference;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
	    // autogenerated by Eclipse
	    final int prime = 31;
	    int result = 1;
	    result = prime
		    * result
		    + ((this.reference == null) ? 0 : this.reference.hashCode());
	    return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
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
	    final AbstractLink<?> other = (AbstractLink<?>) obj;
	    if (this.reference == null) {
		if (other.reference != null) {
		    return false;
		}
	    } else if (!this.reference.equals(other.reference)) {
		return false;
	    }
	    return true;
	}	
	
    }
    
    private static class RealLink extends AbstractLink<Integer> {	
	
	RealLink (final Integer reference) {
	    super(reference);	    
	}

	@Override
	public void process(final ReaderData readerData, final Set<TraceableArtifact> outputList) {
	    final RequirementWParent targetRequirement = readerData.getTraceabilityLinker().getEnclosingRequirement(this.reference);
	    if (targetRequirement != null) outputList.add(targetRequirement);	    
	    else logger.log(Level.INFO, "Could not find the target requirement for a link. Either the document is malformed or you did not process it in its entirety. Will skip this link.");
	}
    }
    
    private static class FakeLink extends AbstractLink<String> {	
	
	FakeLink (final String reference) {
	    super(reference);
	}
	
	@Override
	public void process(final ReaderData readerData, final Set<TraceableArtifact> outputList) {
	    final RequirementWParent targetRequirement = readerData.getTraceabilityLinker().getRequirement(this.reference);
	    if (targetRequirement != null) outputList.add(targetRequirement);	    
	    else if (EXTRACT_EXTERNAL_LINKS) {
		logger.log(Level.FINE, "target requirement seems to be out of my scope. Target ID is \"{0}\". Will add a proxy.", this.reference);
		final RequirementProxy proxyRequirement = new RequirementProxy(this.reference);
		outputList.add(proxyRequirement);	
	    }	    
	}
    }
    
    private static class FakeTableLink extends AbstractLink<String> {	
	
	FakeTableLink (final String reference) {
	    super(reference);
	}
	
	@Override
	public void process(final ReaderData readerData, final Set<TraceableArtifact> outputList) {
	    final RequirementWParent targetRequirement = readerData.getTraceabilityLinker().getFullyQualifiedIdForTable(this.reference);
	    if (targetRequirement != null) outputList.add(targetRequirement);	    
	    else logger.log(Level.FINE, "Could not find the target requirement for a fake table link. Target ID is \"{0}\". Will skip this link.", this.reference);
	}
    }
    
    private static class FakeFigureLink extends AbstractLink<String> {	
	
	FakeFigureLink (final String reference) {
	    super(reference);
	}
	
	@Override
	public void process(final ReaderData readerData, final Set<TraceableArtifact> outputList) {
	    final RequirementWParent targetRequirement = readerData.getTraceabilityLinker().getFullyQualifiedIdForFigure(this.reference);
	    if (targetRequirement != null) outputList.add(targetRequirement);	    
	    else logger.log(Level.FINE, "Could not find the target requirement for a fake figure link. Target ID is \"{0}\". Will skip this link.", this.reference);
	}
    }
    
    /**
     * Ordinary constructor
     * 
     * @param sourceRequirement requirement for which the links shall be managed
     * @throws IllegalArgumentException if the given argument is {@code null}
     */
    public RequirementLinks(final RequirementTemporary sourceRequirement) {
	if (sourceRequirement == null) throw new IllegalArgumentException("sourceRequirement cannot be null.");
	this.sourceRequirement = sourceRequirement;
    }
    
    /**
     * Method for first-pass use: Store the given character start offset from the word file away
     * <p>several calls of this method with the same parameter will cause only one link to be created</p>
     * 
     * @param startOffset
     */
    public void addLinkToExternalStartOffset(final int startOffset) {
	if (startOffset < 0) throw new IllegalArgumentException("startOffset must be >= 0");
	this.rawLinks.add(new RealLink(startOffset));
    }

    /**
     * Method for first-pass use: Store the given ID away (this is used for fake links to requirements)
     * <p>several calls of this method with the same parameter will cause only one link to be created</p>
     * 
     * @param requirementID a human-redable, normalized text string which might be regarded as a link
     */
    public void addLinkToGivenRequirementID(final String requirementID) {
	if (requirementID == null) throw new IllegalArgumentException("requirementID cannot be null.");
	this.rawLinks.add(new FakeLink(requirementID));
    }
    
    /**
     * Method for first-pass use: Store the given ID away (this is used for fake links to non-qualified tables)
     * <p>several calls of this method with the same parameter will cause only one link to be created</p>
     * 
     * @param tableID a human-readable, normalized text string which might be regarded as a reference to a table
     */
    public void addLinkToGivenTable(final String tableID) {
	if (tableID == null) throw new IllegalArgumentException("tableID cannot be null.");
	this.rawLinks.add(new FakeTableLink(tableID));
    }
    
    /**
     * Method for first-pass use: Store the given ID away (this is used for fake links to non-qualified figures)
     * <p>several calls of this method with the same parameter will cause only one link to be created</p>
     * 
     * @param figureID a human-readable, normalized text string which might be regarded as a reference to a figure
     */
    public void addLinkToGivenFigure(final String figureID) {
	if (figureID == null) throw new IllegalArgumentException("tableID cannot be null.");
	this.rawLinks.add(new FakeFigureLink(figureID));
    }
    
    /**
     * Method for second-pass use: Match the stored start offsets to the previously created requirements
     * 
     * @param readerData global ReaderData
     * @return a list of all requirements to which we link
     * @throws IllegalArgumentException if the given argument is {@code null}
     */
    public Set<TraceableArtifact> getLinkedRequirements(final ReaderData readerData) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	
	final Set<TraceableArtifact> outputList = new LinkedHashSet<TraceableArtifact>() {	    
	    private static final long serialVersionUID = 6161672072682820111L;

	    @Override
	    public boolean add(final TraceableArtifact requirement) {
		if (requirement instanceof RequirementWParent && ((RequirementWParent) requirement).getMetadata().getKind() == Kind.PLACEHOLDER) {
		    logger.log(Level.WARNING, "Found link which targets a placeholder/deleted requirement. Source: {0} | Target: {1}", new Object[]{RequirementLinks.this.sourceRequirement.getHumanReadableManager().getTag(), requirement.getHumanReadableManager().getTag()});
		}
		return super.add(requirement);
	    }
	};
	final Iterator<Link> iterator = this.rawLinks.iterator();	
	while (iterator.hasNext()) iterator.next().process(readerData, outputList);
	
	return outputList;
    }
}
