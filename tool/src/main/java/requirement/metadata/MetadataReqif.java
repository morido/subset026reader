package requirement.metadata;

/**
 * This holds all metadata required by reqif
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 */
public final class MetadataReqif {
    private String numberText;
    private Kind kind;
    private LegalObligation legalObligation = LegalObligation.NA;
    private boolean atomic;
    private Boolean implement = null;
    private TextAnnotator textAnnotator = null;

    /**
     * Inject the textual contents of the associated requirement
     * 
     * @param requirementText textual contents, may be {@code null}
     * @throws IllegalArgumentException if the argument is {@code null}
     * @throws IllegalStateException if this has been called several times
     */
    public void injectText(final String requirementText) {
	if (requirementText == null) throw new IllegalArgumentException("requirementText cannot be null.");
	if (this.textAnnotator != null) throw new IllegalStateException("text has already been injected.");
	this.textAnnotator = new TextAnnotator(requirementText);	    
    }

    /**
     * @return the plain numberText of this requirement (i.e. the numberText as obtained from Word without any modifications)
     */
    public String getNumberText() {
	return this.numberText;
    }

    /**
     * @return kind of this requirement
     */
    public Kind getKind() {
	return this.kind;
    }

    /**
     * @return legelObligation of this requirement
     */
    public LegalObligation getLegalObligation() {
	return this.legalObligation;
    }

    /**
     * @return atomicity flag of this requirement
     */
    public boolean isAtomic() {
	return this.atomic;
    }
    
    /**
     * @return {@code true} if the associated requirement must be implemented; {@code false otherwise}
     */
    public boolean mustBeImplemented() {
	// check if a flag has been explicitly set; otherwise fallback to generic detection
	return this.implement != null ? this.implement : this.kind == Kind.ORDINARY;
    }

    /**
     * Get a handle to the text annotator
     * 
     * @return a handle; may be {@code null}
     */
    public TextAnnotator getTextAnnotator() {	
	return this.textAnnotator;
    }

    /**
     * @param numberText numberText of this requirement
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public void setNumberText(final String numberText) {
	if (numberText == null) throw new IllegalArgumentException("numberText cannot be null.");
	this.numberText = numberText;
    }

    /**
     * @param kind kind of this requirement
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public void setKind(final Kind kind) {
	if (kind == null) throw new IllegalArgumentException("kind cannot be null.");
	this.kind = kind;
    }

    /**
     * Set the flag for the implementation status
     * 
     * @param flag {@code true} if the linked requirement must be implemented; {@code false} otherwise
     */
    public void setImplementationFlag(final boolean flag) {
	this.implement = flag;
    }
    
    /**
     * @param legalObligation legalObligation of this requirement
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public void setLegalObligation(final LegalObligation legalObligation) {
	if (legalObligation == null) throw new IllegalArgumentException("legalObligation cannot be null.");
	this.legalObligation = legalObligation;
    }

    /**
     * @param input {@code true} if this requirement is atomic; {@code false} otherwise
     */
    public void setAtomic(final boolean input) {
	this.atomic = input;
    }    
}
