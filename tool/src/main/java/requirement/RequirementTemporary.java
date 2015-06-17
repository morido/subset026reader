package requirement;

import helper.RequirementHelper;
import helper.subset26.MetadataDeterminer;

import org.apache.poi.hwpf.usermodel.Range;

import docreader.ReaderData;
import requirement.data.RequirementLinks;
import requirement.data.RequirementText;
import requirement.metadata.MetadataReqif;
import requirement.metadata.Kind;

/**
 * Requirement which can hold text, but is not linked to any surrounding tree of requirements
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class RequirementTemporary extends RequirementRoot {
    protected final Range associatedRange;	
    protected RequirementText text = null;
    protected MetadataReqif metadata = new MetadataReqif(); // NOPMD - intentionally non-transient, accessor below
    private final RequirementLinks links = new RequirementLinks(this); // NOPMD - intentionally non-transient, accessor below
    private final RequirementLinks knownTermLinks = new RequirementLinks(this); // NOPMD - intentionally non-transient, accessor below
    private final boolean forceRooted;

    /**
     * Ordinary constructor
     * 
     * @param associatedRange Range which is represented by the requirement
     * @throws IllegalArgumentException if the given parameter is {@code null}
     */
    public RequirementTemporary(final Range associatedRange) {
	this(associatedRange, false);
    }
    
    /**
     * Special constructor for tests
     * 
     * @param associatedRange Range which is represented by the requirement
     * @param forceRooted if {@code true} then behave as if this requirement is somewhere in a real hierarchy
     */
    public RequirementTemporary(final Range associatedRange, final boolean forceRooted) {
	super();
	if (associatedRange == null) throw new IllegalArgumentException("The associatedRange cannot be null for this constructor");		
	this.associatedRange = associatedRange;
	this.forceRooted = forceRooted;
	
	// initialize the requirement type to PLACEHOLDER
	this.metadata.setKind(Kind.PLACEHOLDER);
    }

    /**
     * @return range in the source document from where this requirement was extracted
     */
    public final Range getAssociatedRange() {		
	return this.associatedRange;
    }

    /**
     * @return a handle to the ordinary link manager of this requirement, never {@code null}
     */
    public final RequirementLinks getRequirementLinks() {
	return this.links;
    }
    
    /**
     * @return a handle to the link manager for definition links of this requirement, never {@code null}
     */
    public final RequirementLinks getRequirementKnownTermLinks() {
	return this.knownTermLinks;
    }

    /**
     * @return a handle the metadata manager of this requirement, never {@code null}
     */
    public final MetadataReqif getMetadata() {	
	return this.metadata;
    }
    
    /**
     * Setter for the text associated with this requirement; may only be called once
     * <p>also performs metadata analysis</p>
     * 
     * @param readerData global readerData; necessary for the metadata analysis
     * @param text requirement text to save into this requirement
     * 
     * @throws IllegalArgumentException if the argument is {@code null}
     * @throws IllegalStateException if this is not the first call to this method
     */
    public void setText(final ReaderData readerData, final RequirementText text) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null");
	if (text == null) throw new IllegalArgumentException("text cannot be null.");			
	if (this.text != null) throw new IllegalStateException("RequirementText of this Requirement has already been set.");

	this.text = text;
	new MetadataDeterminer(readerData).processRequirement(this);	
    }

    /**
     * @return Handle to the (textual) contents of this requirement; may be {@code null}
     */
    public final RequirementText getText() {
	return this.text;
    }
    
    
    /** 
     * This is necessary for fake rooting this requirement in tests
     * 
     * @see RequirementHelper#isRooted(RequirementRoot)
     * @see requirement.RequirementRoot#getParent()
     */
    @Override
    public RequirementRoot getParent() {
	return this.forceRooted ? new RequirementRoot() : super.getParent();
    }
}
