package reqifwriter;

/**
 * Manages SpecRelations of a given type
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class SpecRelationManager {
    private final String namePrefix;
    private final String type;
    private final XmlReqifWriter xmlwriter;
    private int relationCounter = 0;

    /**
     * Ordinary constructor
     * 
     * @param namePrefix prefix for the identifier part of the resulting SpecRelation
     * @param type internal name of the ReqIF type of the SpecRelation
     * @param xmlwriter output writer
     */
    public SpecRelationManager(final String namePrefix, final String type, final XmlReqifWriter xmlwriter) {
	assert namePrefix != null && type != null && xmlwriter != null;
	this.namePrefix = namePrefix;
	this.type = type;
	this.xmlwriter = xmlwriter;
    }

    /**
     * Write a SpecRelation of the type which is being managed by this class
     * 
     * @param sourceIdentifier source of the relation
     * @param targetIdentifier target of the relation
     */
    void addSpecRelation(final String sourceIdentifier, final String targetIdentifier) {
	assert sourceIdentifier != null && targetIdentifier != null;

	this.xmlwriter.writeStartElement("SPEC-RELATION");
	this.xmlwriter.writeIdentifier(this.namePrefix + "_" + Integer.toString(++this.relationCounter));
	this.xmlwriter.writeLastChange();

	this.xmlwriter.writeStartElement("TARGET");
	this.xmlwriter.writeStartElement("SPEC-OBJECT-REF");
	this.xmlwriter.writeCharacters(targetIdentifier);
	this.xmlwriter.writeEndElement("SPEC-OBJECT-REF");
	this.xmlwriter.writeEndElement("TARGET");

	this.xmlwriter.writeStartElement("SOURCE");
	this.xmlwriter.writeStartElement("SPEC-OBJECT-REF");
	this.xmlwriter.writeCharacters(sourceIdentifier);
	this.xmlwriter.writeEndElement("SPEC-OBJECT-REF");
	this.xmlwriter.writeEndElement("SOURCE");

	this.xmlwriter.writeStartElement("TYPE");
	this.xmlwriter.writeStartElement("SPEC-RELATION-TYPE-REF");
	this.xmlwriter.writeCharacters(this.type);
	this.xmlwriter.writeEndElement("SPEC-RELATION-TYPE-REF");	
	this.xmlwriter.writeEndElement("TYPE");

	this.xmlwriter.writeEndElement("SPEC-RELATION");
    }
}
