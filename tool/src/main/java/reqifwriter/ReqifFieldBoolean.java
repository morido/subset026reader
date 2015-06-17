package reqifwriter;

import requirement.TraceableArtifact;

/**
 * ReqIF field for booleans
 * 
 * @see ReqifField
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 * @param <V> type of the artifact whose fields are being managed
 *
 */
public class ReqifFieldBoolean<V extends TraceableArtifact> extends ReqifField<Boolean, V> {
    boolean defaultValue = false;

    /**
     * Ordinary constructor
     * 
     * @param displayName name which is shown to the user in the left column of ProR's property view
     * @param javaDatatype java datatype of this field
     * @param dataSource source of the field data
     */
    public ReqifFieldBoolean(final String displayName, final Class<Boolean> javaDatatype, final RequirementCall<V> dataSource) {
	super(displayName, javaDatatype, null, dataSource);
    }
    
    /**
     * Set the default value field in the reqif output
     * 
     * @param input {@code true} if this field shall be ticked by default; {@code false} otherwise
     * @return a reference to this object to make the call chainable
     */
    public ReqifFieldBoolean<V> setDefaultValue(final boolean input) {
	this.defaultValue = input;
	return this;
    }
    
    String getDefaultValue() {
	return this.defaultValue ? "true" : "false";
    }
}
