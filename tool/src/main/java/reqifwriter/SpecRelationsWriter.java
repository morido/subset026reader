package reqifwriter;

import java.io.OutputStream;

class SpecRelationsWriter implements ReqIfPartWriter {    
    final XmlReqifWriter xmlwriter = new XmlReqifWriter();
    final GraphWriter graphWriter;
    final SpecRelationManager crossRefManager = new SpecRelationManager("CrossRef", "_stype_relation_crossref", this.xmlwriter);
    final SpecRelationManager knownTermManager = new SpecRelationManager("KnownTerm", "_stype_relation_term", this.xmlwriter);
    
    
    public SpecRelationsWriter(final GraphWriter graphWriter) {
	this.graphWriter = graphWriter;
	this.xmlwriter.writeStartElement("SPEC-RELATIONS");	
    }
    
    @Override
    public void writeToStream(OutputStream outputStream) {
	assert outputStream != null;
	this.xmlwriter.writeEndElement("SPEC-RELATIONS");
	this.xmlwriter.writeToStream(outputStream);
    }
    
    public void addSpecRelationCrossRef(final String sourceIdentifier, final String targetIdentifier) {
	this.crossRefManager.addSpecRelation(sourceIdentifier, targetIdentifier);	
	this.graphWriter.addLinkEdge(sourceIdentifier, targetIdentifier);
    }
    
    public void addSpecRelationKnownTerm(final String sourceIdentifier, final String targetIdentifier) {
	this.knownTermManager.addSpecRelation(sourceIdentifier, targetIdentifier);	
	this.graphWriter.addKnownTermLinkEdge(sourceIdentifier, targetIdentifier);
    }    
}
