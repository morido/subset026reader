package helper.word;

import org.apache.poi.hwpf.usermodel.CharacterRun;

/**
 * Common interface for a single field
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 * @param <T> type of data associated with this field
 */
public interface FieldStore<T> {
    /**
     * Internal identifiers for different field types  
     */
    enum FieldIdentifier {
	TABLENUMBER {
	    @Override
	    public String toString() {
		return "Table";
	    }
	},
	FIGURENUMBER {
	    @Override
	    public String toString() {
		return "Figure";
	    }
	},
	PAGEREFERENCE {
	    @Override
	    public String toString() {
		return "Page";
	    }
	},
	CROSSREFERENCE, IMAGE, SHAPE, EQUATION, SYMBOL;	
    }

    /**
     * Immutable data store for a textual link
     */
    class LinkTuple {
	private final String linkTarget;
	private final String linkText;

	/**
	 * Store a new link tuple
	 * 
	 * @param linkTarget target of the link
	 * @param linkText linked text, can be {@code null}
	 * @throws IllegalArgumentException if the linkTarget is {@code null}
	 */
	public LinkTuple(final String linkTarget, final String linkText) {
	    if (linkTarget == null) throw new IllegalArgumentException("Link target cannot be null.");
	    // linkText can be null
	    this.linkTarget = linkTarget;
	    this.linkText = linkText;
	}

	public String getLinkTarget() {
	    return this.linkTarget;
	}

	public String getLinkText() {
	    return this.linkText;
	}
    }

    /**
     * Immutable store for a characterRun with an overridden font name     
     */
    class CharacterRunTuple {
	private final CharacterRun characterRun;
	private final String overriddenFont;
	private final Integer overriddenCharacterCode;
	
	/**
	 * Store a new characterRun
	 * 
	 * @param characterRun characterRun to store
	 * @param fontName name of the overridden font; can be {@code null} if there is no overridden font
	 * @param characterCode code of the character encoded in this field; can be {@code null} if there is no encoded code
	 */
	public CharacterRunTuple(final CharacterRun characterRun, final String fontName, final Integer characterCode) {
	    if (characterRun == null) throw new IllegalArgumentException("characterRun cannot be null.");
	    // fontName and characterCode can be null
	    this.characterRun = characterRun;
	    this.overriddenFont = fontName;
	    this.overriddenCharacterCode = characterCode;
	}
	
	public CharacterRun getCharacterRun() {
	    return this.characterRun;
	}
	
	public String getOverriddenFont() {
	    return this.overriddenFont;
	}
	
	public Integer getOverriddenCharacterCode() {
	    return this.overriddenCharacterCode;
	}
    }
    
    /**
     * immutable store for an image which is actually a shape
     */
    class ShapeTuple {
	private final int pictureOffset;
	private final int embeddingCharacterRunStartOffset;
	
	public ShapeTuple(final int pictureOffset, final int embeddingCharacterRunStartOffset) {
	    this.pictureOffset = pictureOffset;
	    this.embeddingCharacterRunStartOffset = embeddingCharacterRunStartOffset;
	}
	
	public int getPictureOffset() {
	    return this.pictureOffset;
	}
	
	public int getEmbeddingCharacterRunStartOffset() {
	    return this.embeddingCharacterRunStartOffset;
	}
    }
    
    /**
     * @return the identifier for this field (i.e. the "type" of it)
     */
    FieldIdentifier getIdentifier();
    
    /**
     * @return associated data (payload of this field)
     */
    T getData();
}