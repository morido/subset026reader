package helper;


/**
 * HTML Tag names of common MS Word structures 
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 */
public enum HTMLHelper {
	;	
	
	/**
	 * @return tag to use for bold formatting
	 */
	public static String getBold() {
		return "b";
		//return "strong";
	}
	
	/**
	 * @return tag to use for italic formatting
	 */
	public static String getItalic() {
		return "i";
		//return "em";
	}
}
