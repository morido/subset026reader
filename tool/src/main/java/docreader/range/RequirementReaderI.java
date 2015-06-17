package docreader.range;

import helper.word.FieldStore;
import docreader.GenericReader;

/**
 * Interface for all classes which deal with textual requirements
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public interface RequirementReaderI extends GenericReader<Integer> {
    
    /* (non-Javadoc)
     * @see docreader.GenericReader#read()
     */
    @Override
    Integer read();
    
    /**
     * @return first printable field in this requirement
     */
    FieldStore<Integer> getFirstField();
}