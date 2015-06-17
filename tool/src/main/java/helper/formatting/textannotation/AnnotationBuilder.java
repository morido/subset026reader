package helper.formatting.textannotation;

/**
 * Create annotations for all sorts of use-cases
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 */
/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 */
public enum AnnotationBuilder {

    /**
     * Annotator for words which indicate the legal obligation (shall, may, ...)     
     */
    LEGALOBLIGATION {		
	@Override
	public Annotator getAnnotator() {
	    return new AnnotatorBGColor("Legal Obligation", "#D3D3D3", true);
	}
    },
    
    /**
     * Annotator for words which indicate some sort of undefined legal obligation (must, can, ...)     
     */
    LEGALOBLIGATION_UNKNOWN {	
	@Override
	public Annotator getAnnotator() {
	    return new AnnotatorBGColor("Legal Obligation Unknown", "#D3D3D3", false);
	}
    },
    
    /**    
     * Annotator for words which indicate an action (usually some sort of predicate)
     */
    SENTENCE_ROOT_VERB {
	@Override
	public Annotator getAnnotator() {
	    return new AnnotatorUnderline("Predicate", "black");
	}
    },
    
    /**
     * Annotator for the root of a sentence if that is not a {@link #SENTENCE_ROOT_VERB}
     */
    SENTENCE_ROOT_ADJECTIVE {
	@Override
	public Annotator getAnnotator() {
	    return new AnnotatorUnderline("Predicate", "#C0C0C0");
	}
    },
        
    /**
     * Annotator for the headphrase (subject + modifiers) of a sentence
     */
    HEADPHRASE {
	@Override
	public Annotator getAnnotator() {
	    return new AnnotatorBGColor("Headphrase", "#FFE4B5", false);
	}
    },
    
    /**
     * Annotator for any vague / weak words ("perhaps", "possibly", ...) 
     */
    WEAKWORD("weak", "#FF8C00"),
    
    /**
     * Annotator to indicate a condition ("if", "when", "in case of", ...)
     */
    CONDITION("Condition", "#97B4D4"),
    
    /**
     * Annotator to indicate repetition ("for each", ...) 
     */
    LOOP("Loop", "#C1CC7C"),
    
    /**
     * Annotator for stuff like "re-evaluate", "re-calculate" etc. (darker color than LOOP)
     */
    AGAIN("Again", "#858C55"),
    
    /**
     * Annotator for keywords indicating some time process ("while", "as long as", ...) 
     */
    TIME("Time", "#D79BFF"),      
    
    /**
     * Annotator for words which refer to special entities
     */
    ENTITY {
	@Override
	public AnnotationBuilder setName(final String name) {
	    this.name = name;
	    return this;
	}
	
	@Override
	public Annotator getAnnotator() {
	    return new AnnotatorMonospace(this.name);
	}
    },
    
    /**
     * Annotator for known external entities
     */
    EXTERNAL_ENTITY("External", "#D9766E"),
    
    /**
     * Annotator for self-references 
     */
    SELF_REFERENCE("Self", "#A66844"),
    
    /**
     * Annotator to mark embraced parts, type prefixes (Note:, Example:) and the like
     */
    NO_IMPORTANCE {
	@Override
	public AnnotationBuilder setName(final String name) {
	    this.name = name;
	    return this;
	}
	
	@Override
	public Annotator getAnnotator() {
	    return new AnnotatorFGColor(this.name, "#C0C0C0");
	}
    },
    
    
    
    /**
     * Annotator for phrases which have been previously seen in other requirements 
     */
    LINKED_PHRASE("Linked_Phrase", "#40D440"),
        
    
    /**
     * Annotator for the term of a definition (definiendum); currently unused 
     */
    DEFINITION_TERM("Term", "#00CC00"),
    
    
    /**
     * Annotator for the domain a definition belongs to (e.g. the apple of a tree -> tree is the domain); currently unused 
     */
    DEFINITION_DOMAIN("Domain", "#006600"),
    
    /**
     * Annotator for the actual definition of a term (definiens); currently unused 
     */
    DEFINITION_EXPLANATION("Explanation", "#008000");
    
    protected String name;
    private final String color;
    
    private AnnotationBuilder(final String name, final String color) {
	assert name != null && color != null;
	this.name = name;
	this.color = color;
    }
    
    private AnnotationBuilder() {
	this.name = null;
	this.color = null;
    }
    
    /**
     * Set the (visible/css)-name of this annotation; only has an effect for certain annotations
     * 
     * @param name name to set, may be {@code null}
     * @return a reference to this object
     */
    public AnnotationBuilder setName(final String name) {
	return this; 
    }
    
    /**
     * @return get the actual annotator which is associated with this enum value; never {@code null}
     */
    public Annotator getAnnotator() {
	return new AnnotatorWType(this.name, this.color);
    }
}
