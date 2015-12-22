package reqifwriter;

import java.util.HashSet;
import java.util.Set;

import reqifwriter.ReqifDataType.Datatype;
import requirement.TraceableArtifact;

import static helper.Constants.Generic.WRITE_CLASS_ATTRIBUTES;

class SpecObjectType <T extends TraceableArtifact> {
    private final Set<ReqifField<?,T>> reqifFields = new HashSet<>();
    private final String identifier;
    private final String humanReadableName;
    
    SpecObjectType (final String identifier, final String humanReadableName) {
	assert identifier != null && humanReadableName != null;
	this.identifier = identifier;
	this.humanReadableName = humanReadableName;
    }
        
    @SuppressWarnings("unchecked")
    boolean addField(final ReqifField<?, ? extends TraceableArtifact> reqifField) {
	assert reqifField != null;
	return this.reqifFields.add((ReqifField<?, T>) reqifField);	
    }
    
    String getIdentifier() {
	return XmlReqifWriter.sanitizeForIdentifier(this.identifier);
    }
    
    void definitionToXML(final XmlReqifWriter xmlwriter) {
	assert xmlwriter != null;
	
	xmlwriter.writeStartElement("SPEC-OBJECT-TYPE");
	xmlwriter.writeIdentifier(this.identifier);
	xmlwriter.writeLastChange();	
	xmlwriter.writeAttribute("LONG-NAME", this.humanReadableName);
	xmlwriter.writeStartElement("SPEC-ATTRIBUTES");

	for (final ReqifField<?, ?> reqifField : this.reqifFields) {		
	    xmlwriter.writeStartElement(reqifField.getReqifSpectypeTag());
	    xmlwriter.writeIdentifier(reqifField.getReqifSpectypeIdentifier());
	    xmlwriter.writeLastChange();	
	    xmlwriter.writeAttribute("LONG-NAME", reqifField.getReqifSpectypeLongName());
	    if (reqifField.getReqifDatatypeIdentifierRaw() == Datatype.ENUM) {
		assert reqifField instanceof ReqifFieldEnum<?, ?>;
		xmlwriter.writeAttribute("MULTI-VALUED", ((ReqifFieldEnum<?, ?>) reqifField).isMultivalued());
	    }

	    xmlwriter.writeAttribute("IS-EDITABLE", reqifField.getEditable());
	    xmlwriter.writeStartElement("TYPE");
	    xmlwriter.writeStartElement(reqifField.getReqifDatatypeTagRef());
	    xmlwriter.writeCharacters(reqifField.getReqifDatatypeIdentifier());
	    xmlwriter.writeEndElement(reqifField.getReqifDatatypeTagRef());
	    xmlwriter.writeEndElement("TYPE");

	    if (reqifField.getReqifDatatypeIdentifierRaw() == Datatype.BOOLEAN) {
		assert reqifField instanceof ReqifFieldBoolean;
		xmlwriter.writeStartElement("DEFAULT-VALUE");
		xmlwriter.writeStartElement("ATTRIBUTE-VALUE-BOOLEAN");
		xmlwriter.writeAttribute("THE-VALUE", ((ReqifFieldBoolean<?>) reqifField).getDefaultValue());
		xmlwriter.writeStartElement("DEFINITION");
		xmlwriter.writeStartElement("ATTRIBUTE-DEFINITION-BOOLEAN-REF");
		xmlwriter.writeCharacters(reqifField.getReqifSpectypeIdentifier());
		xmlwriter.writeEndElement("ATTRIBUTE-DEFINITION-BOOLEAN-REF");            
		xmlwriter.writeEndElement("DEFINITION");
		xmlwriter.writeEndElement("ATTRIBUTE-VALUE-BOOLEAN");
		xmlwriter.writeEndElement("DEFAULT-VALUE");
	    }

	    xmlwriter.writeEndElement(reqifField.getReqifSpectypeTag());
	}	    	    
	xmlwriter.writeEndElement("SPEC-ATTRIBUTES");
	xmlwriter.writeEndElement("SPEC-OBJECT-TYPE");
    }
    
    void valuesToXML(final T requirement, final XmlReqifWriter xmlwriter) {
	assert xmlwriter != null;
	
	for (final ReqifField<?, T> reqifField : this.reqifFields) {
	    xmlwriter.writeStartElement(reqifField.getReqifSpecobjectTag());
	    
	    switch(reqifField.getReqifDatatypeIdentifierRaw()) {	    
	    case ENUM:
		xmlwriter.writeStartElement("VALUES");
		xmlwriter.writeStartElement("ENUM-VALUE-REF");
		xmlwriter.writeCharacters(reqifField.getReqifDatatypeIdentifier(reqifField.computeValueForRequirement(requirement)));
		xmlwriter.writeEndElement("ENUM-VALUE-REF");
		xmlwriter.writeEndElement("VALUES");
		addDefinitionPart(xmlwriter, reqifField);
		break;	    
	    case INTEGER: case STRING: case BOOLEAN:
		xmlwriter.writeAttribute("THE-VALUE", reqifField.computeValueForRequirement(requirement));
		addDefinitionPart(xmlwriter, reqifField);
		break;
	    case XHTML:
		addDefinitionPart(xmlwriter, reqifField);
		xmlwriter.writeStartElement("THE-VALUE");
		xmlwriter.writeRaw(addXHTMLNamespace("<div>" + reqifField.computeValueForRequirement(requirement) + "</div>"));
		xmlwriter.writeEndElement("THE-VALUE");
		break;
	    default:
		throw new IllegalStateException(); 
	    }	    	   
	    
	    xmlwriter.writeEndElement(reqifField.getReqifSpecobjectTag());
	}
    }
    
    private static void addDefinitionPart(final XmlReqifWriter xmlwriter, final ReqifField<?, ?> reqifField) {
	assert xmlwriter != null && reqifField != null;
	xmlwriter.writeStartElement("DEFINITION");
	xmlwriter.writeStartElement(reqifField.getReqifSpecobjectTagRef());
	xmlwriter.writeCharacters(reqifField.getReqifSpectypeIdentifier());
	xmlwriter.writeEndElement(reqifField.getReqifSpecobjectTagRef());
	xmlwriter.writeEndElement("DEFINITION");
    }
    
    private static String addXHTMLNamespace(final String input) {
	// TODO
	// Warning: this is very slow and for testing purposes, only!
	//return input.replaceAll("</(.+?)>", "</xhtml:$1>").replaceAll("<(.+?)>", "<xhtml:$1>");	
	
	// DEBUG -- change back
	String output;
	output = input;
	output = output.replaceAll("<(.+?)>", "<xhtml:$1>");
	output = output.replaceAll("<xhtml:/(.+?)>", "</xhtml:$1>");
	
	// remove class-attributes
	if (!WRITE_CLASS_ATTRIBUTES) {	    
	    //output = output.replaceAll("class=\".+?\"", "");
	    output = output.replaceAll("<(xhtml:[A-Za-z]+.*?) class=\".+?\"(.*?)>", "<$1$2>");	    
	}
	
	return output;
	// return input.replaceAll("</(.+?)>", "</xhtml:$1>").replaceAll("<([^/].+?)>", "<xhtml:$1>");
	// return input;
    }
}
