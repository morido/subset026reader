package requirement;

import helper.TraceabilityManagerHumanReadable;
import helper.formatting.RequirementPlaceholderFormatter;

import org.apache.poi.hwpf.usermodel.Range;

import requirement.data.RequirementText;
import requirement.metadata.Kind;
import docreader.ReaderData;

/**
 * Requirement which does not actually exist in the input document but which needs to be inserted
 * in order to maintain a proper hierarchy (used for skipped levels)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class RequirementPlaceholder extends RequirementWParent {
    private final static String PLACEHOLDERTEXT = "PLACEHOLDER REQUIREMENT - DO NOT TRACE";
    
    /**
     * Ordinary constructor
     * 
     * @param readerData global readerData
     * @param associatedRange range of the last processed true requirement
     * @param hrManager hrManager of this requirement
     * @param treeParent parent requirement in the tree
     * @param hrParent parent requirement of the tracestring
     * @throws IllegalArgumentException if one if the arguments is {@code null}
     */
    public RequirementPlaceholder(final ReaderData readerData, final Range associatedRange, final TraceabilityManagerHumanReadable hrManager, final RequirementRoot treeParent, final RequirementRoot hrParent) {
	super(readerData, associatedRange, treeParent);
	if (hrParent == null) throw new IllegalArgumentException("hrParent cannot be null.");
	
	this.text = new RequirementText(new RequirementPlaceholderFormatter().writeString(PLACEHOLDERTEXT));
	this.metadata.setKind(Kind.PLACEHOLDER);
	combine(hrManager, hrParent);
    }
    
    /* (non-Javadoc)
     * @see requirement.RequirementTemporary#setText(requirement.data.RequirementText)
     */
    @Override
    public void setText(ReaderData readerData, final RequirementText text) {
	throw new IllegalStateException("This requirement cannot hold any dynamic text.");
    }

    /**
     * @return textual contents of a placeholder requirement
     */
    public static String getPlaceholderText() {
	return PLACEHOLDERTEXT;
    }
}
