package reqifwriter;


import requirement.TraceableArtifact;

/**
 * Represents a single field in the ReqIF output (that is: one line in ProR's properties view)
 *
 * @param <T> Java type of the field
 * @param <V> type of the artifact whose fields are being managed
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class ReqifField<T, V extends TraceableArtifact> extends ReqifDataType<T> {
    private final String displayName;	
    private final RequirementCall<V> dataSource;    
    private boolean editable = false;
    
    /**
     * Wrapper for the source data which shall end up in this field
     * @param <T> type of the artifact to be called
     */
    public interface RequirementCall<T extends TraceableArtifact> {
	/**
	 * @param artifact requirement from which to obtain data
	 * @return the data, never {@code null}
	 */	
	String call(final T artifact);
    }
       
    /**
     * Constructor with a user-defined type
     * 
     * @param displayName name which is shown to the user in the left column of ProR's property view
     * @param javaDatatype java datatype of this field
     * @param reqifDatatype user-defined datatype for reqif; necessary if the reqif type has no direct java correspondence (e.g. XHTML)
     * @param dataSource source of the field data
     */
    public ReqifField(final String displayName, final Class<T> javaDatatype, final String reqifDatatype, final RequirementCall<V> dataSource) {
	super(javaDatatype, reqifDatatype);
	if (displayName == null) throw new IllegalArgumentException("displayName cannot be null.");
	if (dataSource == null) throw new IllegalArgumentException("dataSource cannot be null.");

	this.displayName = displayName;	
	this.dataSource = dataSource;
    }
    
    /**
     * Ordinary constructor
     * 
     * @param displayName name which is shown to the user in the left column of ProR's property view
     * @param javaDatatype java datatype of this field
     * @param dataSource source of the field data
     */
    public ReqifField(final String displayName, final Class<T> javaDatatype, final RequirementCall<V> dataSource) {
	this(displayName, javaDatatype, null, dataSource);
    }
    
    /**
     * Set the edit flag of this field in the reqif output
     * 
     * @param input {@code true} if the field shall be user-editable; {@code false} otherwise
     * @return reference to this object to make this call chainable
     */
    public ReqifField<T, V> setEditable(final boolean input) {
	this.editable = input;
	return this;
    }

    String getReqifSpectypeIdentifier() {
	// replaceAll comes from XmlReqifWriter.sanitizeForIdentifier()
	// TODO external helper method?
	return "_stype_requirement_" + getReqifSpectypeLongName().replaceAll("[^\\w\\-\\.]", "-"); 
    }
    
    String getReqifSpectypeLongName() {
	return this.displayName;
    }
    
    String getReqifSpectypeTag() {	
	return "ATTRIBUTE-DEFINITION-" + getTagAppendix();
    }
    
    String getReqifSpecobjectTag() {
	return "ATTRIBUTE-VALUE-" + getTagAppendix();
    }
    
    String getReqifSpecobjectTagRef() {
	return "ATTRIBUTE-DEFINITION-" + getTagAppendix() + "-REF";
    }
    
    String getReqifDatatypeTagRef() {
	return "DATATYPE-DEFINITION-" + getTagAppendix() + "-REF";
    }            
    
    String getEditable() {
	return this.editable ? "true" : "false";
    }
    
    String computeValueForRequirement(final V requirement) {	
	return this.dataSource.call(requirement);
    }	
}
