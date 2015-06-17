package docreader.range.paragraph;

import java.lang.reflect.Field;

import org.apache.poi.hwpf.model.StyleSheet;
import org.apache.poi.hwpf.sprm.SprmBuffer;
import org.apache.poi.hwpf.sprm.SprmOperation;
import org.apache.poi.hwpf.usermodel.HWPFList;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.ParagraphProperties;

import docreader.ReaderData;


/**
 * Class to circumvent a bug in POI when it does not correctly detect a list paragraph
 * 
 * <p><em>Note:</em> Ideally this should simply subclass {@link org.apache.poi.hwpf.usermodel.Paragraph}.
 * However, POI does not provide any suitable constructor to do so.</p>
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public final class ParagraphListAware {        
    /**
     * overridden handle to the true list this paragraph belongs to
     */
    private final HWPFList list;
    /**
     * overridden value of the list level format override
     */
    private final int ilfo;
    /**
     * overridden value of the list level
     */
    private final int ilvl;
    
    /**
     * overridden outline level
     */
    private final int olvl;
    
    /**
     * overridden value of the indentation of the paragraph
     */
    private final int dxaLeft;
    
    /**
     * overridden value of the indentation of the first line 
     */
    private final int dxaLeft1;
    
    /**
     * original paragraph as stored by POI 
     */
    private final Paragraph paragraph;
    
    /**
     * illegal value for both {@code ilvl} and {@code ilfo}
     */
    private final static int LIST_VALUE_ILLEGAL = 0x7FF;
    
    /**
     * illegal value for {@code olvl}
     */
    private final static int OLVL_VALUE_ILLEGAL = 10;
    
    /**
     * illegal value for {@code dxaLeft} and {@code dxaLeft1}
     * according to [MS-DOC], v20140721, 2.9.349
     */
    private final static int INDENT_VALUE_ILLEGAL = 31681;
    
    /**
     * Construct a new wrapper
     * 
     * @param readerData global readerData
     * @param paragraph paragraph to process
     * @throws IllegalArgumentException if a given argument is {@code null}
     */
    @SuppressWarnings("deprecation")
    public ParagraphListAware(final ReaderData readerData, final Paragraph paragraph) {
	if (readerData == null) throw new IllegalArgumentException("readerData cannot be null.");
	if (paragraph == null) throw new IllegalArgumentException("paragraph cannot be null.");
	this.paragraph = paragraph;
	
	if (paragraph.isInList()) {
	    // ordinary POI case
	    this.ilvl = paragraph.getIlvl();
	    this.ilfo = paragraph.getIlfo();
	    this.olvl = paragraph.getLvl();
	    this.dxaLeft = paragraph.getIndentFromLeft();
	    this.dxaLeft1 = paragraph.getFirstLineIndent();
	    this.list = paragraph.getList();	    
	}
	else {
	    // backup case when POI fails
	    // Step 1: See if this concrete paragraph has overridden values
	    final ListDataStore listDataTuple = getOverriddenParagraphValues(paragraph);
	    int ilvlTmp = listDataTuple.ilvl;
	    int ilfoTmp = listDataTuple.ilfo;
	    int olvlTmp = listDataTuple.olvl;
	    int dxaLeftTmp = listDataTuple.dxaLeft;
	    int dxaLeft1Tmp = listDataTuple.dxaLeft1;
	    
	    // Step 2: Determine unset values from styles
	    final StyleSheet styleSheet = readerData.getDocument().getStyleSheet();	    
	    assert styleSheet != null && styleSheet.numStyles() > paragraph.getStyleIndex();
	    final ParagraphProperties propertiesFromStyle = styleSheet.getStyleDescription(paragraph.getStyleIndex()).getPAP();	    
	    assert propertiesFromStyle != null;
	   
	    if (ilvlTmp == LIST_VALUE_ILLEGAL) ilvlTmp = propertiesFromStyle.getIlvl();
	    if (ilfoTmp == LIST_VALUE_ILLEGAL) ilfoTmp = propertiesFromStyle.getIlfo();	    
	    if (isInList(ilvlTmp, ilfoTmp)) {
		this.ilvl = ilvlTmp;
		this.ilfo = ilfoTmp;
		assert readerData.getDocument().getListTables() != null;
		// inspired by org.apache.poi.hwpf.usermodel.Paragraph#getList()
		this.list = new HWPFList(styleSheet, readerData.getDocument().getListTables(), ilfoTmp);
		assert this.list != null;
	    }
	    else {		
		this.ilvl = 0; // mimic the behavior of stock POI and assign 0 if not in list; see org.apache.poi.hwpf.usermodel.Paragraph#getIlvl()
		this.ilfo = 0; // mimic the behavior of stock POI and assign 0 if not in list; see org.apache.poi.hwpf.usermodel.Paragraph#getIlfo()
		this.list = null; // NOPMD - intentional null assignment
	    }
	    this.olvl = (olvlTmp == OLVL_VALUE_ILLEGAL) ? propertiesFromStyle.getLvl(): olvlTmp; 
	    this.dxaLeft = (dxaLeftTmp == INDENT_VALUE_ILLEGAL) ? propertiesFromStyle.getDxaLeft() : dxaLeftTmp;
	    this.dxaLeft1 = (dxaLeft1Tmp == INDENT_VALUE_ILLEGAL) ? propertiesFromStyle.getDxaLeft1() : dxaLeft1Tmp;
	}
    }
    
    /**
     * @return the list this paragraph belongs to, never {@code null}
     * @see org.apache.poi.hwpf.usermodel.Paragraph#getList()
     * @throws IllegalStateException if this paragraph does not belong to any list
     */
    public HWPFList getList() {
	if (this.list == null) {
	    throw new IllegalStateException("Paragraph is not part of a list.");
	}
	return this.list;	
    }
    
    /**
     * @return overridden {@code ilvl}-value of this paragraph
     * @see org.apache.poi.hwpf.usermodel.Paragraph#getIlvl()
     */
    public int getIlvl() {	
	return this.ilvl;
    }
    
    /**
     * @return overridden {@code ilfo}-value of this paragraph
     * @see org.apache.poi.hwpf.usermodel.Paragraph#getIlfo()
     */
    public int getIlfo() {
	return this.ilfo;
    }
    
    /**
     * @return overridden paragraph indentation
     * @see org.apache.poi.hwpf.usermodel.Paragraph#getIndentFromLeft()
     */
    public int getIndentFromLeft() {
	return this.dxaLeft;
    }
    
    /**
     * @return overridden first line indentation
     * @see org.apache.poi.hwpf.usermodel.Paragraph#getFirstLineIndent()
     */
    public int getFirstLineIndent() {
	return this.dxaLeft1;
    }
    
    /**
     * @return overridden outline level
     * @see org.apache.poi.hwpf.usermodel.Paragraph#getLvl()
     */
    public int getLvl() {
	return this.olvl;
    }
    
    /**
     * @return the original paragraph from POI, never {@code null}
     */
    public Paragraph getParagraph() {
	return this.paragraph;
    }
    
    /**
     * @return {@code true} if this paragraph is part of a list; {@code false} otherwise
     * @see org.apache.poi.hwpf.usermodel.Paragraph#isInList()
     */
    public boolean isInList() {
	return this.list != null;
    }
    
    /**
     * Check the list membership of a paragraph on the basis of a given {@code ilvl} and {@code ilfo}
     * 
     * @param ilvl ilvl to check
     * @param ilfo ilfo to check
     * @return {@code true} if this paragraph is part of a list; {@code false} otherwise
     */
    private static boolean isInList(final int ilvl, final int ilfo) {
	// [MS-DOC], v20140721, 2.6.2, sprmPIlvl
	final boolean ilvlIsValid = ilvl >= 0x0 && ilvl <= 0x8;
	// [MS-DOC], v20140721, 2.6.2, sprmPIlfo
	final boolean ilfoIsValid = (ilfo >= 0x001 && ilfo <= 0x07FE) || (ilfo >= 0xF802 && ilfo <= 0xFFFF);

	return ilvlIsValid && ilfoIsValid;
    }
    
    /**
     * Obtain list properties which were overridden for a particular paragraph
     * 
     * @param paragraph Paragraph for which to obtain overridden values
     * @return a tuple containing both {@code ilfo} and {@code ilvl}, never {@code null}
     */
    private static ListDataStore getOverriddenParagraphValues(final Paragraph paragraph) {
	assert paragraph != null;
	// Note: This is based on reflection -- it would be much better to change POI's source instead
	int ilfo = LIST_VALUE_ILLEGAL;
	int ilvl = LIST_VALUE_ILLEGAL;
	int olvl = OLVL_VALUE_ILLEGAL;
	int dxaLeft = INDENT_VALUE_ILLEGAL;
	int dxaLeft1 = INDENT_VALUE_ILLEGAL;
	
	try {
	    final Field field = paragraph.getClass().getDeclaredField("_papx");
	    field.setAccessible(true);
	    final SprmBuffer sprmBuffer = (SprmBuffer) field.get(paragraph);

	    final Integer ilfoTmp = getSprmValue(sprmBuffer, Paragraph.SPRM_ILFO);
	    if (ilfoTmp != null) ilfo = ilfoTmp; // autoboxing is safe

	    final Integer ilvlTmp = getSprmValue(sprmBuffer, Paragraph.SPRM_ILVL);
	    if (ilvlTmp != null) ilvl = ilvlTmp; // autoboxing is safe

	    final Integer olvlTmp = getSprmValue(sprmBuffer, Paragraph.SPRM_OUTLVL);
	    if (olvlTmp != null) olvl = olvlTmp; // autoboxing is safe

	    final Integer dxaLeftTmp = getSprmValue(sprmBuffer, Paragraph.SPRM_DXALEFT);
	    if (dxaLeftTmp != null) dxaLeft = dxaLeftTmp; // autoboxing is safe		

	    final Integer dxaLeft180Tmp = getSprmValue(sprmBuffer, Paragraph.SPRM_DXALEFT1); // this is sprmPDxaLeft180
	    if (dxaLeft180Tmp != null) dxaLeft1 = dxaLeft180Tmp; // autoboxing is safe

	    final Integer dxaLeft1Tmp = getSprmValue(sprmBuffer, (short) 0x8460); // this is sprmPDxaLeft1
	    if (dxaLeft1Tmp != null && dxaLeft1 == INDENT_VALUE_ILLEGAL) dxaLeft1 = dxaLeft1Tmp; // autoboxing is safe

	} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
	    assert false;
	    // intentionally discard this
	    // will never happen
	}
	
	return new ListDataStore(ilvl, ilfo, olvl, dxaLeft, dxaLeft1);
    }
    
    private static Integer getSprmValue(final SprmBuffer sprmBuffer, final short opcode) {
	assert sprmBuffer != null;	
	final SprmOperation sprmOperation = sprmBuffer.findSprm(opcode);
	return (sprmOperation != null) ? sprmOperation.getOperand() : null;	     
    }
    
    private final static class ListDataStore {	
	/**
	 * will be {@link #LIST_VALUE_ILLEGAL} if not set
	 */
	public final int ilvl;
	/**
	 * will be {@link #LIST_VALUE_ILLEGAL} if not set
	 */
	public final int ilfo;
	
	/**
	 * will be {@link #OLVL_VALUE_ILLEGAL} if not set
	 */
	public final int olvl;
	/**
	 * will be {@link #INDENT_VALUE_ILLEGAL} if not set
	 */
	public final int dxaLeft;
	/**
	 * will be {@link #INDENT_VALUE_ILLEGAL} if not set
	 */
	public final int dxaLeft1;
	
	public ListDataStore(final int ilvl, final int ilfo, final int olvl, final int dxaLeft, final int dxaLeft1) {
	    this.ilfo = ilfo;
	    this.ilvl = ilvl;
	    this.olvl = olvl;
	    this.dxaLeft = dxaLeft;
	    this.dxaLeft1 = dxaLeft1;
	}
    }
}