package reqifwriter;

import java.io.OutputStream;
import java.util.Set;

import requirement.RequirementProxy;
import requirement.TraceableArtifact;

class SpecObjectsWriter implements ReqIfPartWriter {
    final XmlReqifWriter xmlwriter = new XmlReqifWriter();
    final GraphWriter graphWriter;
    
    public SpecObjectsWriter(final GraphWriter graphWriter) {
	assert graphWriter != null;
	this.graphWriter = graphWriter;
	this.xmlwriter.writeStartElement("SPEC-OBJECTS");	
    }
    
    @Override
    public void writeToStream(final OutputStream outputStream) {
	assert outputStream != null;
	this.xmlwriter.writeEndElement("SPEC-OBJECTS");
	this.xmlwriter.writeToStream(outputStream);
    }
    
    public String addSpecObject(final TraceableArtifact requirement, final SpecObjectType<TraceableArtifact> specObjectType) {
	assert requirement != null;	
	
	this.xmlwriter.writeStartElement("SPEC-OBJECT");
	final String tracestring = requirement.getHumanReadableManager().getTag();
	final String identifier = this.xmlwriter.writeIdentifier(tracestring);
	this.xmlwriter.writeLastChange();
	this.xmlwriter.writeStartElement("VALUES");
	
	specObjectType.valuesToXML(requirement, this.xmlwriter);
	
	this.xmlwriter.writeEndElement("VALUES");
	this.xmlwriter.writeStartElement("TYPE");
	this.xmlwriter.writeStartElement("SPEC-OBJECT-TYPE-REF");
	this.xmlwriter.writeCharacters(specObjectType.getIdentifier());
	this.xmlwriter.writeEndElement("SPEC-OBJECT-TYPE-REF");
	this.xmlwriter.writeEndElement("TYPE");
	this.xmlwriter.writeEndElement("SPEC-OBJECT");
		
	this.graphWriter.addNode(requirement);
	
	return identifier;
    }
    
    public void processProxyElements(final Set<RequirementProxy> proxyRequirements, final SpecObjectType<TraceableArtifact> specObjectType) {		
	for (final RequirementProxy currentArtifact : proxyRequirements) {
	    addSpecObject(currentArtifact, specObjectType);
	}
    }       
    
    /**
     * @param requirement requirement whose identifier shall be computed
     * @return the identifier used in ReqIF for a given requirement
     */
    public static String queryIdentifier(final TraceableArtifact requirement) {
	assert requirement != null;
	final String tracestring = requirement.getHumanReadableManager().getTag();
	return XmlReqifWriter.sanitizeForIdentifier(tracestring);
    }
}