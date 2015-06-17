package requirement.metadata;

/**
 * Specifies whether a certain requirement must be implemented or is optional
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public enum LegalObligation {    
    
    /**     
     * at least one of the requirements in this list must be implemented by all means
     */
    MANDATORY_LIST_OR {
	@Override
	public String toString() {
	    return "mandatory (>= 1)";
	}		
    },
    
    /**     
     * exactly one of the requirements in this list must be implemented by all means
     */
    MANDATORY_LIST_XOR {
	@Override
	public String toString() {
	    return "mandatory (== 1)";
	}
    },
    
    /**     
     * requirement must be implemented by all means
     */
    MANDATORY {
	@Override
	public String toString() {
	    return "mandatory";
	}
	
	@Override
	public LegalObligation getListOR() {
	    return MANDATORY_LIST_OR;
	}
	
	@Override
	public LegalObligation getListXOR() {
	    return MANDATORY_LIST_XOR;
	}
    },        
    
    /**     
     * exactly one of the requirements in this list may be implemented if desired
     */
    OPTIONAL_LIST_XOR {
	@Override
	public String toString() {
	    return "optional (== 1)";
	}
    },
    
    /**
     * requirement may be implemented if desired
     */
    OPTIONAL {
	@Override
	public String toString() {
	    return "optional";
	}
	
	@Override
	public LegalObligation getListOR() {
	    // an explicit OPTION_LIST_OR does not make sense - logic wise
	    return this;
	}
	
	@Override
	public LegalObligation getListXOR() {
	    return OPTIONAL_LIST_XOR;
	}		
    },
    
    /**
     * requirement is not atomic; contains both obligatory and optional elements
     */
    MIXED {
	@Override
	public String toString() {
	    return "mixed";
	}
    },
    
    /**
     * the legal obligation could not be determined
     */
    UNKNOWN {
	@Override
	public String toString() {
	    return "unknown";
	}
    },
    
    /**
     * there is no legal obligation for this artifact
     */
    NA {
	@Override
	public String toString() {
	    return "not applicable";
	}
    };
    
    /**
     * Get the OR'ed list version of this enum-value
     * 
     * @return the new enum-value or {@code null} if the call is not applicable for the current value 
     */
    @SuppressWarnings("static-method")
    public LegalObligation getListOR() {
	return null;
    }
    
    /**
     * Get the XOR'ed list version of this item
     * 
     * @return the new enum-value or {@code null} if the call is not applicable for the current value
     */
    @SuppressWarnings("static-method")
    public LegalObligation getListXOR() {
	return null;
    }
}
