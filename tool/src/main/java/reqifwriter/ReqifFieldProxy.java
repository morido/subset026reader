package reqifwriter;

import requirement.RequirementProxy;

/**
 * Special field for proxy requirements (= placeholders for requirements which are outside of the current document)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
class ReqifFieldProxy extends ReqifField<String, RequirementProxy> {
    
    public ReqifFieldProxy(final String displayName, final Class<String> javaDatatype, final RequirementCall<RequirementProxy> dataSource) {
	super(displayName, javaDatatype, null, dataSource);
    } 
    
    @Override    
    String getReqifSpectypeIdentifier() {
	// replaceAll comes from XmlReqifWriter.sanitizeForIdentifier()
	// TODO external helper method?
	return "_stype_requirementproxy_" + getReqifSpectypeLongName().replaceAll("[^\\w\\-\\.]", "-"); 
    }
}
