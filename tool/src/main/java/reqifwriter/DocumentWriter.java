package reqifwriter;

import helper.ParallelExecutor;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import reqifwriter.SpecObjectMapper.Type;
import requirement.RequirementRoot;
import requirement.TraceableArtifact;
import docreader.ReaderData;

/**
 * Converts a tree of requirements into a reqif file
 * 
 * <p><em>Limitations: </em>
 * <ol>
 * <li>Only supports exactly one SpecObject type.</li>
 * <li>Only supports Boolean, Integer, String, Enum and XHTML datatypes.</li>
 * <li>Does not support referencing the same SpecObject more than once in the hierarchy.</li>
 * <li>Tool extensions are only written for ProR.</li>
 * </ol>
 * </p>
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public final class DocumentWriter {        
    private final Map<String, ReqifDataType<?>> reqifDataTypes = new HashMap<>();    
    private final SpecObjectMapper specObjectMapper = new SpecObjectMapper();
    private String sortingField;    
    private final Map<String, Integer> columnFields = new LinkedHashMap<>(2);
    private static final Logger logger = Logger.getLogger(DocumentWriter.class.getName()); // NOPMD - Reference rather than a static field
    
    /**
     * Immutable wrapper for a ReqifField, ensures that only fields which exist in the output can be referenced to modify their visual appearance
     */
    public final class ReferenceableReqifField {
	private final ReqifField<?,?> wrappedField;
	
	ReferenceableReqifField(final ReqifField<?, ?> wrappedField) {
	    assert wrappedField != null;
	    this.wrappedField = wrappedField;
	}
	
	ReqifField<?, ?> getField() {
	    return this.wrappedField;
	}
    }

    /**
     * Ordinary constructor
     */
    public DocumentWriter() {	
	// make sure we always have a string type present in the reqif
	final ReqifDataType<String> stringType = new ReqifDataType<>(String.class);
	this.reqifDataTypes.put(stringType.getDatatypeEnumAware(), stringType);
    }
    
    /**
     * Writes a tree of requirements to a given output file
     * 
     * @param readerData global readerData
     * @param root root requirement of the tree which shall be processed
     * @param outputFile fully qualified filename of the resulting reqif file 
     */
    public void serializeTree(final ReaderData readerData, final RequirementRoot root, final String outputFile) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	if (root == null) throw new IllegalArgumentException("root requirement cannot be null.");
	if (outputFile == null) throw new IllegalArgumentException("outputFile cannot be null.");

	// Step 1: Setup piped streams for thread communication
	try (final PipedOutputStream outputStreamFromProducer = new PipedOutputStream()) {
	    try (final InputStream inputStreamForConsumer = new PipedInputStream(outputStreamFromProducer)) {
		final Runnable producer = new Runnable() {
		    // Step 2: Setup threads
		    @Override
		    public void run() {
			// Step 2.1., Thread 1: Generate XML
			{
			    // Step 2.1.1: Write the preamble
			    final XmlReqifWriter xmlwriter = new XmlReqifWriter();
			    new PreambleWriter(readerData, xmlwriter).write();
			    xmlwriter.writeToStream(outputStreamFromProducer);
			}
			{
			    // Step 2.1.2: Write the main content
			    final SpecHierarchyWriter specHierarchyWriter = new SpecHierarchyWriter(readerData, root, DocumentWriter.this.specObjectMapper);			    
			    specHierarchyWriter.writeToStream(outputStreamFromProducer);			
			}
			{
			    // Step 2.1.3: Write the epilogue
			    final XmlReqifWriter xmlwriter = new XmlReqifWriter();
			    new EpilogueWriter(xmlwriter).write();
			    xmlwriter.writeToStream(outputStreamFromProducer);
			}

			// close the output stream; otherwise the consumer thread would block
			try {
			    outputStreamFromProducer.flush();
			    outputStreamFromProducer.close();
			} catch (IOException e) {
			    logger.log(Level.SEVERE, "Error while closing output pipe for XML serialization.", e);
			    throw new IllegalStateException(e);
			}
		    }
		};

		final Runnable consumer = new Runnable() {	    
		    @Override
		    public void run() {
			// Thread 2: Serialize to file		    
			writeToFile(inputStreamForConsumer, outputFile);		    
		    }
		};
		
		ParallelExecutor.execute("ReqIF_Serialization", producer, consumer);
	    }
	} catch (IOException e) {
	    logger.log(Level.SEVERE, "Error while creating input pipe for XML serialization.", e);
	    throw new IllegalStateException(e);
	}
    }
    
    
    /**
     * Add a new field for a requirement ("SpecObject") in the reqIF output
     * 
     * @param reqifField field to add
     * @return a handle to the newly added field
     * @throws IllegalArgumentException if the given argument is {@code null}
     */
    public <T extends TraceableArtifact> ReferenceableReqifField addField(final ReqifField<?, T> reqifField) {
	if (reqifField == null) throw new IllegalArgumentException("reqifField cannot be null.");
	this.reqifDataTypes.put(reqifField.getDatatypeEnumAware(), reqifField);
	this.specObjectMapper.getType(Type.DEFAULT).addField(reqifField);
	return new ReferenceableReqifField(reqifField);
    }
    
    /**
     * Set the field which ProR will use as a label for the hierarchy view
     * 
     * <p><em>Note: </em> This must be set. Subsequent calls will overwrite the old stored value (thus this writer only allows exactly one sorting field).</p>
     * 
     * @param reqifField field to sort by as obtained from {@link #addField(ReqifField)}
     */
    public void setSortingField(final ReferenceableReqifField reqifField) {
	if (reqifField == null) throw new IllegalArgumentException("reqifField cannot be null.");
	this.sortingField = reqifField.getField().getReqifSpectypeLongName();
    }
    
    /**
     * Set a field which will appear in ProR's agile grid
     * 
     * <p><em>Note: </em> Call this subsequently for all fields to add. Order of calls is equal to the ordering of the columns in the resulting file.</p>
     * 
     * @param reqifField field to add to the grid as obtained from {@link #addField(ReqifField)}
     * @param width width of the column for this field
     */
    public void setColumnField(final ReferenceableReqifField reqifField, final int width) {
	if (reqifField == null) throw new IllegalArgumentException("reqifField cannot be null.");
	this.columnFields.put(reqifField.getField().getReqifSpectypeLongName(), width);
    }
    
    /**
     * Write a given input stream to an output file and prettify the XML contents
     * 
     * @param inputStream data source
     * @param outputFile fully qualified file name of resulting reqif file
     */
    private static void writeToFile(final InputStream inputStream, final String outputFile) {
	// TODO
	// http://stackoverflow.com/questions/139076/how-to-pretty-print-xml-from-java
	// http://stackoverflow.com/questions/1225909/most-efficient-way-to-create-inputstream-from-outputstream
	assert inputStream != null;
	assert outputFile != null;

	final Source xmlInput = new StreamSource(inputStream);	
	try (final FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
	    try (final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {
		final StreamResult xmlOutput = new StreamResult(bufferedOutputStream);
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();	    
		try {	
		    final Transformer transformer = transformerFactory.newTransformer();		
		    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes"); // return the xml declaration on a separate line 
		    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");		
		    transformer.transform(xmlInput, xmlOutput);
		} catch (TransformerException e) {
		    logger.log(Level.SEVERE, "Error while pretty-printing XML", e);
		    throw new RuntimeException(e);
		}
	    }
	}
	catch (IOException e) {
	    logger.log(Level.SEVERE, "Could not write output file", e);
	    throw new IllegalStateException(e);
	}		
    }
    
    /**
     * Common interface for writers which care about specific parts of the output file
     */
    private interface PartsWriter {
	void write();
    }
    
    /**
     * Writer for the very beginning of a reqif file
     * 
     * <em>Note:</em> leaves the leading {@code REQ-IF}-tag open
     * <br/> + {@code CORE-CONTENT}
     * <br/> + {@code REQ-IF-CONTENT}         
     */
    private final class PreambleWriter implements PartsWriter {
	final ReaderData readerData;
	final XmlReqifWriter xmlwriter;
	
	PreambleWriter(final ReaderData readerData, final XmlReqifWriter xmlwriter) {
	    assert readerData != null && xmlwriter != null;
	    this.xmlwriter = xmlwriter;
	    this.readerData = readerData;
	}
	
	@Override
	public void write() {	    
	    writeHeader();
	    writeGlueToMainDocumentPart(); 	   
	    writeDatatypes();
	    writeSpecFields();
	}
	
	private void writeHeader() {
	    assert this.xmlwriter != null;
	    final String toolID = "subset026-writer";

	    this.xmlwriter.writeStartDocument();
	    this.xmlwriter.writeStartElement("REQ-IF");
	    this.xmlwriter.writeAttribute("xmlns", "http://www.omg.org/spec/ReqIF/20110401/reqif.xsd");
	    this.xmlwriter.writeAttribute("xmlns:xhtml", "http://www.w3.org/1999/xhtml");
	    this.xmlwriter.writeAttribute("xmlns:configuration", "http://eclipse.org/rmf/pror/toolextensions/1.0");

	    this.xmlwriter.writeStartElement("THE-HEADER");
	    this.xmlwriter.writeStartElement("REQ-IF-HEADER");

	    this.xmlwriter.writeIdentifier(this.readerData.getDocumentPrefix() + "_reqif");	   
	    this.xmlwriter.writeStartElement("CREATION-TIME");
	    this.xmlwriter.writeCharacters(this.xmlwriter.getCurrentTime());
	    this.xmlwriter.writeEndElement("CREATION-TIME");
	    this.xmlwriter.writeStartElement("REQ-IF-TOOL-ID");
	    this.xmlwriter.writeCharacters(toolID);
	    this.xmlwriter.writeEndElement("REQ-IF-TOOL-ID");
	    this.xmlwriter.writeStartElement("REQ-IF-VERSION");
	    this.xmlwriter.writeCharacters("1.0");
	    this.xmlwriter.writeEndElement("REQ-IF-VERSION");
	    this.xmlwriter.writeStartElement("SOURCE-TOOL-ID");
	    this.xmlwriter.writeCharacters(toolID);
	    this.xmlwriter.writeEndElement("SOURCE-TOOL-ID");
	    this.xmlwriter.writeStartElement("TITLE");
	    this.xmlwriter.writeCharacters(this.readerData.getDocumentTitle());
	    this.xmlwriter.writeEndElement("TITLE");
	    this.xmlwriter.writeEndElement("REQ-IF-HEADER");
	    this.xmlwriter.writeEndElement("THE-HEADER");
	}

	private void writeGlueToMainDocumentPart() {	    
	    this.xmlwriter.writeStartElement("CORE-CONTENT");
	    this.xmlwriter.writeStartElement("REQ-IF-CONTENT");
	}

	
	private void writeDatatypes() {	    
	    this.xmlwriter.writeStartElement("DATATYPES");
	    for (final Entry<String, ReqifDataType<?>> currentDataType : DocumentWriter.this.reqifDataTypes.entrySet()) {
		final ReqifDataType<?> currentField = currentDataType.getValue();
		
		this.xmlwriter.writeStartElement(currentField.getReqifDatatypeTag());		
		this.xmlwriter.writeIdentifier(currentField.getReqifDatatypeIdentifier());
		this.xmlwriter.writeLastChange();
		this.xmlwriter.writeAttribute("LONG-NAME", currentField.getReqifDatatypeLongName());
		
		switch(currentField.getReqifDatatypeIdentifierRaw()) {
		case BOOLEAN: case XHTML:
		    // need no special treatment
		    break;
		case ENUM:
		    this.xmlwriter.writeStartElement("SPECIFIED-VALUES");
		    {
			int enumKeyCounter = 1;
			for (final String[] currentType : currentField.getEnumerationNames()) {
			    this.xmlwriter.writeStartElement("ENUM-VALUE");
			    this.xmlwriter.writeIdentifier(currentField.getReqifDatatypeIdentifier(currentType[0]));
			    this.xmlwriter.writeLastChange();
			    this.xmlwriter.writeAttribute("LONG-NAME", currentType[1]);
			    this.xmlwriter.writeStartElement("PROPERTIES");
			    this.xmlwriter.writeCombinedStartEndElement("EMBEDDED-VALUE");
			    this.xmlwriter.writeAttribute("KEY", Integer.toString(enumKeyCounter++));
			    this.xmlwriter.writeAttribute("OTHER-CONTENT", "Other content for: " + currentType[1]); // whatever this is good for
			    this.xmlwriter.writeEndElement("PROPERTIES");
			    this.xmlwriter.writeEndElement("ENUM-VALUE");
			}
		    }
		    this.xmlwriter.writeEndElement("SPECIFIED-VALUES");
		    break;
		case INTEGER:
		    this.xmlwriter.writeAttribute("MAX", Integer.toString(Integer.MAX_VALUE));
		    this.xmlwriter.writeAttribute("MIN", "0");
		    break;
		case STRING:
		    this.xmlwriter.writeAttribute("MAX-LENGTH", "32000");
		    break;
		default:
		    throw new IllegalStateException();
		}				
		this.xmlwriter.writeEndElement(currentField.getReqifDatatypeTag());
	    }
	    this.xmlwriter.writeEndElement("DATATYPES");	  
	}
	
	private void writeSpecFields() {
	    this.xmlwriter.writeStartElement("SPEC-TYPES");
	    
	    // Part 1: Fields in requirement	    
	    for (final SpecObjectType<?> currentType : DocumentWriter.this.specObjectMapper.getAvailableTypes()) {
		currentType.definitionToXML(this.xmlwriter);
	    }
	    
	    // Part 2: Fields in Specification
	    
	    this.xmlwriter.writeStartElement("SPECIFICATION-TYPE");
	    this.xmlwriter.writeIdentifier("stype_specification");
	    this.xmlwriter.writeLastChange();
	    this.xmlwriter.writeAttribute("LONG-NAME", "Specification Type");
	    this.xmlwriter.writeStartElement("SPEC-ATTRIBUTES");
	    this.xmlwriter.writeStartElement("ATTRIBUTE-DEFINITION-STRING");
	    this.xmlwriter.writeIdentifier("stype_specification_description");
	    this.xmlwriter.writeLastChange();	
	    this.xmlwriter.writeAttribute("LONG-NAME", "Description");	
	    this.xmlwriter.writeStartElement("TYPE");
	    this.xmlwriter.writeStartElement("DATATYPE-DEFINITION-STRING-REF");
	    assert DocumentWriter.this.reqifDataTypes.get(new ReqifDataType<>(String.class).getDatatypeEnumAware()) != null; // guaranteed by constructor
	    this.xmlwriter.writeCharacters(DocumentWriter.this.reqifDataTypes.get(new ReqifDataType<>(String.class).getDatatypeEnumAware()).getReqifDatatypeIdentifier());
	    this.xmlwriter.writeEndElement("DATATYPE-DEFINITION-STRING-REF");
	    this.xmlwriter.writeEndElement("TYPE");
	    this.xmlwriter.writeEndElement("ATTRIBUTE-DEFINITION-STRING");
	    this.xmlwriter.writeEndElement("SPEC-ATTRIBUTES");
	    this.xmlwriter.writeEndElement("SPECIFICATION-TYPE");

	    this.xmlwriter.writeCombinedStartEndElement("SPEC-RELATION-TYPE");
	    this.xmlwriter.writeIdentifier("stype_relation_crossref");
	    this.xmlwriter.writeLastChange();
	    this.xmlwriter.writeAttribute("LONG-NAME", "CrossRefLink");
	    
	    this.xmlwriter.writeCombinedStartEndElement("SPEC-RELATION-TYPE");
	    this.xmlwriter.writeIdentifier("stype_relation_term");
	    this.xmlwriter.writeLastChange();
	    this.xmlwriter.writeAttribute("LONG-NAME", "KnownTermLink");
	    
	    this.xmlwriter.writeEndElement("SPEC-TYPES");
	}
    }
        
    /**
     * Writer for the very end of a reqif file     
     */
    private final class EpilogueWriter implements PartsWriter {
	private final XmlReqifWriter xmlwriter;
	
	EpilogueWriter(final XmlReqifWriter xmlwriter) {
	    assert xmlwriter != null;
	    this.xmlwriter = xmlwriter;
	}
	
	@Override
	public void write() {	    
	    writeEpilogue();
	}	
	
	/**
	 * Write the epilogue and take care about {@link PreambleWriter}'s leftovers
	 */
	private void writeEpilogue() {	    
	    // TODO find a better solution than this!
	    this.xmlwriter.writeRaw("</REQ-IF-CONTENT></CORE-CONTENT>");
	    writeProRToolExtensions();
	    this.xmlwriter.writeRaw("</REQ-IF>");
	}
	
	
	/**
	 * Write extensions specific to ProR
	 */
	private void writeProRToolExtensions() {
	    // NOTE: this requires another namespace in the root tag	    
	    this.xmlwriter.writeStartElement("TOOL-EXTENSIONS");
	    this.xmlwriter.writeStartElement("REQ-IF-TOOL-EXTENSION");
	    this.xmlwriter.writeStartElement("configuration:ProrToolExtension");
	    
	    // circumvent a bug in ProR's validator - empty tag does not do any harm
	    this.xmlwriter.writeCombinedStartEndElement("configuration:presentationConfigurations");
	    
	    this.xmlwriter.writeStartElement("configuration:specViewConfigurations");
	    this.xmlwriter.writeStartElement("configuration:ProrSpecViewConfiguration");
	    this.xmlwriter.writeAttribute("specification", "_specificationDocument");
	    this.xmlwriter.writeStartElement("configuration:columns");
	    for (final Entry<String, Integer> column : DocumentWriter.this.columnFields.entrySet()) {
		this.xmlwriter.writeCombinedStartEndElement("configuration:Column");
		this.xmlwriter.writeAttribute("label", column.getKey());
		this.xmlwriter.writeAttribute("width", Integer.toString(column.getValue()));
	    }
	    this.xmlwriter.writeEndElement("configuration:columns");
	    
	    // new Attribute to comply with ProR's validator as suggested by Ingo Weigelt, 10.12.15
	    this.xmlwriter.writeStartElement("configuration:leftHeaderColumn");
	    this.xmlwriter.writeCombinedStartEndElement("configuration:Column");
	    this.xmlwriter.writeAttribute("label", "Lead Header Column");
	    this.xmlwriter.writeAttribute("width", "71");
	    this.xmlwriter.writeEndElement("configuration:leftHeaderColumn");
	    
	    this.xmlwriter.writeEndElement("configuration:ProrSpecViewConfiguration");
	    this.xmlwriter.writeEndElement("configuration:specViewConfigurations");
	    this.xmlwriter.writeStartElement("configuration:generalConfiguration");
	    this.xmlwriter.writeStartElement("configuration:ProrGeneralConfiguration");
	    this.xmlwriter.writeStartElement("configuration:labelConfiguration");
	    this.xmlwriter.writeStartElement("configuration:LabelConfiguration");
	    this.xmlwriter.writeStartElement("defaultLabel");
	    this.xmlwriter.writeCharacters(DocumentWriter.this.sortingField);
	    this.xmlwriter.writeEndElement("defaultLabel");
	    this.xmlwriter.writeStartElement("defaultLabel"); // necessary as fallback for all predefined elements (specification name, ...)
	    this.xmlwriter.writeCharacters("Description");
	    this.xmlwriter.writeEndElement("defaultLabel");
	    this.xmlwriter.writeEndElement("configuration:LabelConfiguration");
	    this.xmlwriter.writeEndElement("configuration:labelConfiguration");
	    this.xmlwriter.writeEndElement("configuration:ProrGeneralConfiguration");
	    this.xmlwriter.writeEndElement("configuration:generalConfiguration");
	    this.xmlwriter.writeEndElement("configuration:ProrToolExtension");
	    this.xmlwriter.writeEndElement("REQ-IF-TOOL-EXTENSION");
	    this.xmlwriter.writeEndElement("TOOL-EXTENSIONS");
	}
    }        
}