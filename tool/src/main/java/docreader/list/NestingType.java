package docreader.list;

/**
 * Different types of nested structures
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public enum NestingType {
    /**
     * not a nested structure at all
     */
    NOT_NESTED,

    /**
     * nested structure inside a table cell 
     */
    TABLE_CELL,

    /**
     * content of a footnote/endnote
     */
    NOTE
}