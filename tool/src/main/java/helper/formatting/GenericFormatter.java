package helper.formatting;


/**
 * Interface to all classes that do rich-text formatting of certain text chunks (usually fields)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
interface GenericFormatter {

    /**
     * formatting which prepends the data to be written
     */    
    void writeStartElement();

    /**
     * formatting which appends the data to be written 
     */    
    void writeEndElement();
}
