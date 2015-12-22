package helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


/**
 * This abstracts {@link javax.xml.stream.XMLStreamWriter} and provides means to directly write to a string
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class XmlStringWriter {
    /**
     * actual datastore for the XML contents of this writer
     */
    protected final transient ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
    protected final transient XMLStreamWriter xmlwriter;
    protected final transient Deque<String> tagList = new LinkedList<>(); 
    private static final Logger logger = Logger.getLogger(XmlStringWriter.class.getName()); // NOPMD - Reference rather than a static field

    /**
     * Generic constructor
     * @throws IllegalStateException if the internal writer could not be created
     */
    public XmlStringWriter() {
	XMLStreamWriter xmlWriter = null;
	
	try {
	    xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(new OutputStreamWriter(this.byteArrayOutput, "utf-8"));
	} catch (UnsupportedEncodingException | XMLStreamException | FactoryConfigurationError e) {		    	
	    logger.log(Level.SEVERE, "Could not create xmlWriter.", e);
	    throw new IllegalStateException(e);
	}
	
	this.xmlwriter = xmlWriter;
    }

    /**
     * Open a new tag and close it right away
     * <p>Use this for tags like {@code <br />}</p>
     * 
     * @param tag tag to be written
     * @throws IllegalArgumentException if the given tag is {@code null}
     * @throws IllegalStateException if the given tag could not be written
     */
    public void writeCombinedStartEndElement(final String tag) {
	if (tag == null) throw new IllegalArgumentException("Given tag cannot be null.");
	
	try {
	    this.xmlwriter.writeEmptyElement(tag);
	} catch (XMLStreamException e) {
	    logger.log(Level.SEVERE, "Could not write tag.", e);
	    throw new IllegalStateException(e);
	}
    }

    /**
     * Open a new tag
     * 
     * @param tag tag to be opened
     * @throws IllegalArgumentException if the given tag is {@code null}
     * @throws IllegalStateException if the given tag could not be written
     */
    public void writeStartElement(final String tag) {
	if (tag == null) throw new IllegalArgumentException("Given tag cannot be null.");
	
	this.tagList.add(tag);
	try {
	    this.xmlwriter.writeStartElement(tag);
	} catch (XMLStreamException e) {
	    logger.log(Level.SEVERE, "Could not write start element.", e);
	    throw new IllegalStateException(e);
	}
    }

    /**
     * Get the name of the current tag
     * 
     * @return string representation of the currently active tag (i.e. the innermost tag in the hierarchy) or an empty string if there are no open tags
     */
    public String getCurrentElement() {
	// return an empty string if there are no elements available
	return (!this.tagList.isEmpty()) ? this.tagList.getLast() : "";	
    }

    /**
     * Closes a previously opened tag
     * 
     * @param tag Tag which is to be closed
     * @throws IllegalArgumentException if the given tag is invalid
     * @throws IllegalStateException if the closing tag could not be written
     */
    public void writeEndElement(final String tag) {
	if (tag == null) throw new IllegalArgumentException("Given tag cannot be null.");
	
	if (this.tagList.getLast().equals(tag)) {
	    try {
		this.xmlwriter.writeEndElement();
	    } catch (XMLStreamException e) {
		logger.log(Level.SEVERE, "Could not write end element.", e);
		throw new IllegalStateException(e);
	    }
	    this.tagList.removeLast();
	}
	else {
	    System.err.println(toString()); // DEBUG
	    throw new IllegalArgumentException("Attempting to close a tag which has not been opened before.");			
	}
    }

    /**
     * Write an attribute to the last tag which has been added to this writer
     * via {@link #writeStartElement(String)} or {@link #writeCombinedStartEndElement(String)}
     * 
     * <p> The attribute will have the following form: {@code attributeName="value"}.</p>
     * 
     * @param attributeName name of the attribute
     * @param value value of the attribute
     * @throws IllegalArgumentException if one of the arguments is {@code null}
     * @throws IllegalStateException if the attribute could not be written
     */
    public void writeAttribute(final String attributeName, final String value) {
	if (attributeName == null) throw new IllegalArgumentException("Given attributeName cannot be null.");
	if (value == null) throw new IllegalArgumentException("Given value cannot be null.");
	
	try {
	    this.xmlwriter.writeAttribute(attributeName, value);
	} catch (XMLStreamException e) {
	    logger.log(Level.SEVERE, "Could not write attribute.", e);
	    throw new IllegalStateException(e);
	}
    }

    /**
     * Write characters (i.e. stuff which ends up in between tags).
     * <p> Any {@code <} and {@code >} signs will be escaped.
     * Hence, this cannot be used to write tags. Use {@link #writeRaw(String)} instead.</p>
     * 
     * @param data the characters to write
     * @throws IllegalArgumentException if data is {@code null}
     * @throws IllegalStateException if the characters could not be written
     */
    public void writeCharacters(final String data) {
	if (data == null) throw new IllegalArgumentException("Given data cannot be null.");
	
	try {
	    this.xmlwriter.writeCharacters(data);
	} catch (XMLStreamException e) {
	    logger.log(Level.SEVERE, "Could not write characters.", e);
	    throw new IllegalStateException(e);
	}
    }

    /**
     * Writes raw data to this xmlwriter
     * <p>Inspired by <a href="http://stackoverflow.com/questions/19998460/how-to-write-unescaped-xml-to-xmlstreamwriter">this stackoverflow question</a></p>
     * 
     * @param data the rawData to write
     * @throws IllegalArgumentException if the given data is {@code null}
     * @throws IllegalStateException if the data could not be appended
     */
    public void writeRaw(final String data) {
	if (data == null) throw new IllegalArgumentException("Given data cannot be null.");

	try {
	    this.xmlwriter.writeCharacters("");
	    this.xmlwriter.flush();
	    final OutputStreamWriter temporaryStreamWriter = new OutputStreamWriter(this.byteArrayOutput, "utf-8");
	    temporaryStreamWriter.write(data);
	    temporaryStreamWriter.flush();
	}
	catch (XMLStreamException | IOException e) {
	    logger.log(Level.SEVERE, "Could not write rawData.", e);
	    throw new IllegalStateException(e);
	}	
    }	

    /**
     * Closes all given elements in their correct order (i.e. preserving nesting)
     * 
     * @param elementsToClose The elements to close
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public void writeClosingElements(final Set<String> elementsToClose) {
	if (elementsToClose == null) throw new IllegalArgumentException("elementsToClose cannot be null.");
	String currentTag;
	final Iterator<String> tags = this.tagList.descendingIterator();

	nextTag: while (tags.hasNext()) {
	    currentTag = tags.next();
	    for (final String currentClosingElement : elementsToClose) {
		if (currentTag.equals(currentClosingElement)) {
		    tags.remove();
		    try {
			this.xmlwriter.writeEndElement();
		    } catch (XMLStreamException e) {
			logger.log(Level.SEVERE, "Cannot write XML close tag.", e);
			throw new IllegalStateException(e);
		    }
		    continue nextTag;
		}
	    }
	    break nextTag;
	}			
    }

    /**
     * Return a String representation of the current state of this writer (i.e. everything that has been written so far)
     * 
     * @see java.lang.Object#toString()
     * @throws IllegalStateException if not all tags have been closed properly or there were issues while closing the writer
     */
    @Override
    public String toString() {
	try {
	    this.xmlwriter.close();
	} catch (XMLStreamException e) {
	    logger.log(Level.SEVERE, "Cannot close XML stream.", e);
	    throw new IllegalStateException(e);
	}

	if (this.tagList.isEmpty()) {
	    try {
		return this.byteArrayOutput.toString("utf-8");
	    } catch (UnsupportedEncodingException e) {
		logger.log(Level.SEVERE, "Encountered an usupported encoding when trying to serialize XML.", e);
		throw new IllegalStateException(e);
	    }
	}

	assert !this.tagList.isEmpty();
	logger.log(Level.SEVERE, "tagList is not empty. Will not be able to write proper XML. This is the current state:\n {0}",
		this.byteArrayOutput.toString());
	throw new IllegalStateException();
    }
}
