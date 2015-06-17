package reqifwriter;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import reqifwriter.SpecObjectMapper.Type;
import requirement.RequirementProxy;
import requirement.RequirementRoot;
import requirement.RequirementWParent;
import requirement.TraceableArtifact;
import docreader.ReaderData;

/**
 * Writer for the hierarchy part of the ReqIF
 * 
 * <p><em>Note:</em> Can only write "singletons" (i.e. one source requirement can only occur once in the output tree)</p> 
 */
class SpecHierarchyWriter implements ReqIfPartWriter {
    private final ReaderData readerData;
    private final XmlReqifWriter xmlwriter = new XmlReqifWriter();
    private final SpecObjectsWriter specObjectsWriter;
    private final SpecRelationsWriter specRelationsWriter;
    private final SpecObjectMapper specObjectMapper;
    private final Set<RequirementProxy> proxyRequirements = new HashSet<>();
    private final GraphWriter graphWriter = new GraphWriter();
    private static final Logger logger = Logger.getLogger(SpecHierarchyWriter.class.getName()); // NOPMD - intentionally small-case
    
    public SpecHierarchyWriter(final ReaderData readerData, final RequirementRoot root, final SpecObjectMapper specObjectMapper) {
	assert readerData != null;
	assert specObjectMapper != null;
	this.readerData = readerData;	
	this.specObjectsWriter = new SpecObjectsWriter(this.graphWriter);
	this.specRelationsWriter = new SpecRelationsWriter(this.graphWriter);
	this.specObjectMapper = specObjectMapper;
	
	this.xmlwriter.writeStartElement("SPECIFICATIONS");
	PreambleWriter.write(readerData, this.xmlwriter);
	generateTree(root);
	EpilogueWriter.write(this.xmlwriter);
	this.xmlwriter.writeEndElement("SPECIFICATIONS");

	writeStatisticalData();
    }
    
    @Override
    public void writeToStream(final OutputStream outputStream) {
	this.specObjectsWriter.writeToStream(outputStream);
	this.specRelationsWriter.writeToStream(outputStream);
	this.xmlwriter.writeToStream(outputStream);
    }
    
    /**
     * Start the tree generation from root
     * 
     * @param rootRequirement root of the requirement tree
     */
    private void generateTree(final RequirementRoot rootRequirement) {
	// Note: the root itself will only be written to the statistics output but not to the reqif
	assert rootRequirement != null;
	final String identifier = XmlReqifWriter.sanitizeForIdentifier("rootObject" + "_singleton");
	final Iterator<RequirementWParent> iterator = rootRequirement.getChildIterator();
	while (iterator.hasNext()) {
	    final RequirementWParent childRequirement = iterator.next();
	    generateTree(childRequirement);
	    this.graphWriter.addParentChildEdge(identifier, SpecObjectsWriter.queryIdentifier(childRequirement));
	}
	this.specObjectsWriter.processProxyElements(this.proxyRequirements, this.specObjectMapper.getType(Type.PROXY));
	
	this.graphWriter.addRootNode(identifier);
    }
    
    /**
     * Generate the the tree part for anything below the root
     * 
     * @param currentRequirement requirement to process
     */
    private void generateTree(final RequirementWParent currentRequirement) {
	assert currentRequirement != null;
	this.xmlwriter.writeStartElement("SPEC-HIERARCHY");
	final String identifier = this.specObjectsWriter.addSpecObject(currentRequirement, this.specObjectMapper.getType(Type.DEFAULT));
	for (final TraceableArtifact linkTarget : currentRequirement.getRequirementLinks().getLinkedRequirements(this.readerData)) {
	    if (linkTarget instanceof RequirementProxy) {
		this.proxyRequirements.add((RequirementProxy) linkTarget); // will not add if already present		
	    }
	    this.specRelationsWriter.addSpecRelationCrossRef(identifier, SpecObjectsWriter.queryIdentifier(linkTarget));
	}
	for (final TraceableArtifact linkTarget : currentRequirement.getRequirementKnownTermLinks().getLinkedRequirements(this.readerData)) {
	    // TODO actually we cannot link to proxies here
	    if (linkTarget instanceof RequirementProxy) {
		this.proxyRequirements.add((RequirementProxy) linkTarget); // will not add if already present		
	    }
	    this.specRelationsWriter.addSpecRelationKnownTerm(identifier, SpecObjectsWriter.queryIdentifier(linkTarget));
	}
	this.xmlwriter.writeIdentifier(identifier + "_singleton");
	this.xmlwriter.writeLastChange();
	this.xmlwriter.writeStartElement("OBJECT");
	this.xmlwriter.writeStartElement("SPEC-OBJECT-REF");
	this.xmlwriter.writeCharacters(identifier);
	this.xmlwriter.writeEndElement("SPEC-OBJECT-REF");
	this.xmlwriter.writeEndElement("OBJECT");

	recurse(currentRequirement, identifier);
	this.xmlwriter.writeEndElement("SPEC-HIERARCHY");
    }
    
    /**
     * Recurse into child requirements
     * 
     * @param currentRequirement requirement from which to obtain children
     * @param identifier identifier of the currentRequirement
     */
    private void recurse(final RequirementRoot currentRequirement, final String identifier) {
	assert currentRequirement != null;
	final Iterator<RequirementWParent> iterator = currentRequirement.getChildIterator();
	if (iterator.hasNext()) {
	    this.xmlwriter.writeStartElement("CHILDREN");
	    while (iterator.hasNext()) {
		final RequirementWParent childRequirement = iterator.next();
		generateTree(childRequirement);
		this.graphWriter.addParentChildEdge(identifier, SpecObjectsWriter.queryIdentifier(childRequirement));
	    }
	    this.xmlwriter.writeEndElement("CHILDREN");
	}
    }
    
    
    private void writeStatisticalData() {
	try {
	    this.graphWriter.writeStatisticsData(this.readerData.getAbsoluteFilePathPrefix());
	}
	catch (IllegalStateException e) {
	    logger.log(Level.WARNING, "Requested to write statistical data. But encountered errors while doing so. Will skip this step.", e);
	}
    }
    
    private final static class PreambleWriter {
	private PreambleWriter() { }
	
	public static void write(final ReaderData readerData, final XmlReqifWriter xmlwriter) {
	    assert xmlwriter != null;
	    writeInitialGlue(readerData, xmlwriter);
	}
	
	private static void writeInitialGlue(final ReaderData readerData, final XmlReqifWriter xmlwriter) {
	    assert readerData != null;
	    assert xmlwriter != null;
	    
	    xmlwriter.writeStartElement("SPECIFICATION");
	    xmlwriter.writeIdentifier("specificationDocument");
	    xmlwriter.writeLastChange();
	    xmlwriter.writeAttribute("LONG-NAME", "Specification Document");
	    xmlwriter.writeStartElement("VALUES");
	    xmlwriter.writeStartElement("ATTRIBUTE-VALUE-STRING");
	    xmlwriter.writeAttribute("THE-VALUE", readerData.getDocumentTitle());
	    xmlwriter.writeStartElement("DEFINITION");
	    xmlwriter.writeStartElement("ATTRIBUTE-DEFINITION-STRING-REF");
	    xmlwriter.writeCharacters("_stype_specification_description");
	    xmlwriter.writeEndElement("ATTRIBUTE-DEFINITION-STRING-REF");
	    xmlwriter.writeEndElement("DEFINITION");
	    xmlwriter.writeEndElement("ATTRIBUTE-VALUE-STRING");
	    xmlwriter.writeEndElement("VALUES");
	    xmlwriter.writeStartElement("TYPE");
	    xmlwriter.writeStartElement("SPECIFICATION-TYPE-REF");
	    xmlwriter.writeCharacters("_stype_specification");
	    xmlwriter.writeEndElement("SPECIFICATION-TYPE-REF");
	    xmlwriter.writeEndElement("TYPE");
	    xmlwriter.writeStartElement("CHILDREN");
	}
    }
    
    private final static class EpilogueWriter {
	private EpilogueWriter() { }
	
	public static void write(final XmlReqifWriter xmlwriter) {
	    assert xmlwriter != null;
	    writeEnd(xmlwriter);
	}
	
	private static void writeEnd(final XmlReqifWriter xmlwriter) {
	    assert xmlwriter != null;
	    xmlwriter.writeEndElement("CHILDREN");
	    xmlwriter.writeEndElement("SPECIFICATION");
	}
    }
    
}
