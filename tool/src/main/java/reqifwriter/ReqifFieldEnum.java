package reqifwriter;

import requirement.TraceableArtifact;

/**
 * ReqIF field for enums
 * 
 * @see ReqifField
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 * @param <T> type of the enum
 * @param <V> type of the artifact whose fields are being managed
 */
public class ReqifFieldEnum<T extends Enum<T>, V extends TraceableArtifact> extends ReqifField<T, V> {
    private boolean multivalued = false;
    
    /**
     * Ordinary constructor
     * 
     * @param displayName name which is shown to the user in the left column of ProR's property view
     * @param javaDatatype java datatype of this field
     * @param dataSource source of the field data
     */
    public ReqifFieldEnum(final String displayName, final Class<T> javaDatatype, final RequirementCall<V> dataSource) {
	super(displayName, javaDatatype, null, dataSource);
    }    
    
    /**
     * Set the multivalued field for the reqif output
     * 
     * @param input {@code true} if several values are applicable (OR behavior) or {@code false} if only one value is applicable at a given time (XOR)
     * @return a reference to this object to make the call chainable
     */
    public ReqifFieldEnum<T, V> setMultivalued(final boolean input) {
	this.multivalued = input;
	return this;
    }
    
    String isMultivalued() {
	return this.multivalued ? "true" : "false";
    }    
}
