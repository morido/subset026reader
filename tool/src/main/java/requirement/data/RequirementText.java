package requirement.data;

/**
 * Structure to hold the text of a single requirement
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class RequirementText {
    /**
     * raw plaintext without any formatting 
     */
    private final String raw;

    /**
     * richtext with all formatting applied
     */
    private final String rich;	


    /**
     *  richtext + human-readable trace tags; used for tables
     */
    private final String richWithTraceTags;

    /**
     * richtext based on the plaintext with special formatting intended for easier implementation (based on NLP analysis) 
     */
    //TODO remove?
    public String implementerEnhanced;

    /**
     * Ordinary constructor
     * 
     * @param raw raw representation, may be {@code null}
     * @param rich rich representation, may be {@code null}
     */
    public RequirementText(final String raw, final String rich) {
	this(raw, rich, null);
    }

    /**
     * Constructor for computed contents (split-up requirements, placeholders, ...)
     * 
     * @param rich rich rich representation, may be {@code null}
     */
    public RequirementText(final String rich) {
	this(null, rich, null);
    }
    
    /**
     * Constructor for table cells
     * 
     * @param raw raw representation, may be {@code null}
     * @param rich rich representation, may be {@code null}
     * @param richWithTraceTags rich representation including a tracetag, may be {@code null}
     */
    public RequirementText(final String raw, final String rich, final String richWithTraceTags) {
	this.raw = raw;
	this.rich = rich;
	this.richWithTraceTags = richWithTraceTags;		
    }

    /**
     * @return the raw representation, may be {@code null}
     */
    public String getRaw() {
	return this.raw;
    }

    /**
     * @return the rich representation, may be {@code null}
     */
    public String getRich() {
	return this.rich;
    }

    /**
     * @return the rich representation including traceTags (used in table cells), may be code {null}
     */
    public String getRichWithTraceTags() {
	return this.richWithTraceTags;
    }

    /**
     * Textual representation of this object; for debugging purporses only
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
	final StringBuilder output = new StringBuilder();
	if (this.getRaw() != null) output.append("Raw: ").append(this.getRaw()).append('\n');
	if (this.getRich() != null) output.append("Rich: ").append(this.getRich()).append('\n');
	if (this.getRichWithTraceTags() != null) output.append("RichWTags: ").append(this.getRichWithTraceTags()).append('\n');
	return output.toString();
    }
}
