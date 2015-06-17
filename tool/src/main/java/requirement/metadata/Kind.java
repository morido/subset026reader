package requirement.metadata;

import helper.annotations.DomainSpecific;

/**
 * Specifies the kind of the requirement
 *  
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public enum Kind {
    /**
     * a requirement consisting only of plain text     
     */    
    ORDINARY {	
	@Override
	public String toString() {
	    return "ordinary";
	}
    },
    /**
     * requirement holds the richText representation of a table or is the caption of a table     
     */
    TABLE {
	@Override
	public String toString() {
	    return "Table";
	}
    },
    /**
     * requirement holds a figure or is the caption of a figure
     */
    FIGURE {
	@Override
	public String toString() {
	    return "Figure";
	}
    },
    /**
     * plain text requirement which is marked as a "Note"     
     */
    @DomainSpecific
    NOTE {
	@Override
	public String toString() {
	    return "Note";
	}
    },
    /**
     * plain text requirement which is marked as an "Example"
     */
    @DomainSpecific
    EXAMPLE {
	@Override
	public String toString() {
	    return "Example";
	}
    },    
    /**
     * plain text requirement which is marked as a "Justification"
     */
    @DomainSpecific
    JUSTIFICATION {
	@Override
	public String toString() {
	    return "Justification";
	}
    },    
    /**     
     * plain text requirement which is most probably a heading
     */
    @DomainSpecific
    HEADING {
	@Override
	public String toString() {
	    return "Heading";
	}
    },
    /**
     * requirement exists only to ensure proper hierarchy (includes requirements marked as "deleted")
     */
    @DomainSpecific
    PLACEHOLDER {
	@Override
	public String toString() {
	    return "Placeholder";
	}
    },
    
    /**
     * plain text requirement which is most probably a definition
     */
    @DomainSpecific
    DEFINITION {
	@Override
	public String toString() {
	    return "Definition";
	}
    }
}
