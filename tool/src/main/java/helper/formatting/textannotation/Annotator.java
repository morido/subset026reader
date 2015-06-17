package helper.formatting.textannotation;

import helper.XmlStringWriter;

/**
 * Annotator for a text fragment; used to highlight it in some way
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public interface Annotator {    
    
    /**
     * Write the tags to start a new annotation
     * 
     * @param xmlwriter writer for the output
     * @throws IllegalArgumentException if the given argument is {@code null}
     */
    void writeStart(final XmlStringWriter xmlwriter);
    
    /**
     * Write the tags to end the annotation started by {@link #writeStart(XmlStringWriter)}
     * 
     * @param xmlwriter writer for the output
     * @throws IllegalArgumentException if the given argument is {@code null}
     */
    void writeEnd(final XmlStringWriter xmlwriter);
}
