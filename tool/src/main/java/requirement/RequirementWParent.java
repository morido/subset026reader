package requirement;

import helper.RequirementHelper;
import helper.TraceabilityManagerHumanReadable;

import org.apache.poi.hwpf.usermodel.Range;

import requirement.data.RequirementText;
import docreader.ReaderData;


/**
 * Requirement with a mandatory parent
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class RequirementWParent extends RequirementTemporary implements TraceableArtifact {
    private final int traceId;    
    private final transient ReaderData readerData;
    private final RequirementRoot parent; // hides a field from RequirementRoot
    private transient TraceabilityManagerHumanReadable hrManager = null; // hides a field from RequrementRoot
    private final InlineElementCounter inlineElementCounter = new InlineElementCounter();

    /**
     * Storage for the counter of inline elements underneath this requirement     
     */
    public static final class InlineElementCounter {
	private int equationCounter = 0; // NOPMD - accessor below
	private int imageCounter = 0; // NOPMD - accessor below
	
	/**
	 * prevent any explicit construction of this class outside the scope of the parent class
	 */
	InlineElementCounter() {
	    // intentionally empty; nothing to set up here
	}
	
	/**
	 * @return the next unused equation number
	 */
	public int getNextEquationNumber() {
	    return ++this.equationCounter;
	}
	
	/**
	 * @return the next unused image number
	 */
	public int getNextImageNumber() {
	    return ++this.imageCounter;
	}
    }
    
    /**
     * Ordinary constructor
     * 
     * @param readerData global readerData
     * @param associatedRange source range of this requirement
     * @param parent treeParent of this requirement
     * @throws IllegalArgumentException if one of the given parameters is {@code null}
     */
    public RequirementWParent(final ReaderData readerData, final Range associatedRange, final RequirementRoot parent) {
	super(associatedRange);
	this.traceId = this.associatedRange.getStartOffset();		

	if (parent == null) throw new IllegalArgumentException("Parent cannot be null for this constructor");
	this.parent = parent;
	parent.addChild(this);

	if(readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	this.readerData = readerData;		
    }

    /**
     * return the hierarchical parent of this requirement, never {@code null}
     * 
     * @return hierarchical parent
     */
    @Override
    public final RequirementRoot getParent() {
	// we must override this here; otherwise we would always return the hardcoded null of the hidden parent-field in RequirementRoot.java
	return this.parent;
    }


    /**
     * Convenience method without the need to supply {@code readerData}
     * 
     * @see RequirementTemporary#setText(ReaderData, RequirementText)
     * @param text textual contents of this requirement
     */
    public void setText(final RequirementText text) {
	super.setText(this.readerData, text);
    }
    
    /* (non-Javadoc)
     * @see requirement.RequirementRoot#getHumanReadableManager()
     */
    @Override
    public final TraceabilityManagerHumanReadable getHumanReadableManager() {
	// we must override this here; otherwise we would always return the the hidden parent-field in RequirementRoot.java
	return this.hrManager;
    }
    
    @Override
    public final String getContent() {
	return this.getText() != null && this.getText().getRaw() != null ? this.getText().getRaw() : ""; 
    }

    @Override
    public final boolean getImplementationStatus() {
	return this.getMetadata().mustBeImplemented();
    }
    
    /**
     * @param hrManager hrManager which is to be combined with the parent's manager and then used for this requirement
     * @throws IllegalArgumentException parameter was {@code null}
     * @throws IllegalStateException method has been called more than once
     */
    public void setHumanReadableManager(final TraceabilityManagerHumanReadable hrManager) {
	if (hrManager == null) throw new IllegalArgumentException("given hrManager cannot be null.");
	if (this.hrManager != null) throw new IllegalStateException("hrManager of the requirement has already been set.");
	combine(hrManager, this.parent);	
    }

    /**
     * @return character offset in the word document from where this requirement originates (i.e. backward tracing information)
     */
    public final int getTraceId() {
	return this.traceId;
    }
    
    /**
     * @return a handle to the counter of inline elements (images, figures) which are part of this requirement
     */
    public final InlineElementCounter getInlineElementCounter() {
	return this.inlineElementCounter;
    }
    
    /**
     * Merges the tracestring of two TraceabilityManagers, one of which must be already stored in a higher-level requirement. The result is saved as the TraceabilityManager of the current object.
     * 
     * @param hrManagerToAppend the new TraceabilityManager which is to be merged with an existing TraceabilityManager
     * @param hrParent a higher-level requirement whose TraceabilityManager is to be used as the base
     */
    protected final void combine(final TraceabilityManagerHumanReadable hrManagerToAppend, final RequirementRoot hrParent) {
	if (!isParentalRelation(hrParent)) throw new IllegalArgumentException("The tracestring base " + hrParent.toString() + " and the tree base " + this.parent.toString() + " are not in a parent/child relation");		
	this.hrManager = new TraceabilityManagerHumanReadable(hrParent.getHumanReadableManager(), hrManagerToAppend, this);

	if (RequirementHelper.isRooted(hrParent)) {
	    this.readerData.getTraceabilityLinker().addRequirementLink(this);
	}
    }

    /**
     * Check if a parent-child relation exists
     * 
     * @param suspectedParent Suspected parent
     * @return {@code true} if a parent-child relation exists; {@code false} otherwise
     */
    private final boolean isParentalRelation(final RequirementRoot suspectedParent) {	
	return RequirementHelper.isParentalRelation(this.parent, suspectedParent);		
    }       
}
