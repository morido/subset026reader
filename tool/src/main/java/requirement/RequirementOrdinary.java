package requirement;

import helper.TraceabilityManagerHumanReadable;

import org.apache.poi.hwpf.usermodel.Range;

import docreader.ReaderData;

/**
 * Class for plain, ordinary requirements (i.e. something with all features of a requirement enabled)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class RequirementOrdinary extends RequirementWParent {    
   
    /**
     * Constructor for table/list requirements
     * 
     * @param readerData global readerData
     * @param associatedRange range in the original Word document which is represented by this requirement
     * @param hrManager human readable manager to attach to this requirement
     * @param treeParent parent in the requirement tree
     * @param hrParent requirement on which to base the human readable trace string
     * @throws IllegalArgumentException one of the given arguments was {@code null}
     */
    public RequirementOrdinary(final ReaderData readerData, final Range associatedRange, final TraceabilityManagerHumanReadable hrManager, final RequirementRoot treeParent, final RequirementRoot hrParent) {
	super(readerData, associatedRange, treeParent);
	if (hrParent == null) throw new IllegalArgumentException("Human-readable parent cannot be null for this constructor");			
	if (hrManager == null) throw new IllegalArgumentException("The hrIdentifier cannot be null for this constructor");

	combine(hrManager, hrParent);
    }

    /**
     * Constructor for ordinary requirements
     * 
     * @param readerData global readerData
     * @param associatedRange range in the original Word document which is represented by this requirement
     * @param hrManager human readable manager to attach to this requirement
     * @param parent parent in the requirement tree
     */
    public RequirementOrdinary(final ReaderData readerData, final Range associatedRange, final TraceabilityManagerHumanReadable hrManager, final RequirementRoot parent) {
	this(readerData, associatedRange, hrManager, parent, parent);		
    }

    /** 
     * @see requirement.RequirementWParent#setHumanReadableManager(helper.TraceabilityManagerHumanReadable)
     * @throws IllegalAccessError <em>always</em>: method may not be called here at all
     */
    @Override
    public final void setHumanReadableManager(final TraceabilityManagerHumanReadable hrManager) {
	throw new IllegalAccessError("This method must not be called for this object of type " + this.getClass().toString());		
    }
    
    /**
     * Sets the original numberText of this new requirement
     * 
     * @param numberText numberText, never {@code null}
     */
    public final void setNumberText(final String numberText) {
	if (numberText == null) throw new IllegalArgumentException("numberText cannot be null.");
	getMetadata().setNumberText(numberText);
	this.readerData.getTraceabilityLinker().addNumberTextLink(this);	
    }
}