package helper;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test the internal CSSManager
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
@SuppressWarnings("static-method")
public class CSSManagerTest {
     
    /**
     * Test method for {@link CSSManager#getIdentifier(String)}
     */
    @Test
    public void testGetIdentifier() {	
        assertEquals("lalilu", CSSManager.getIdentifier("la li lu"));
        assertEquals("_-9Test", CSSManager.getIdentifier("-9Test"));
        assertEquals("Test_", CSSManager.getIdentifier("#+\"Te;;;st_"));
    }
    
    
    /**
     * Test method for equality method; necessary to merge similar formatting in the Word-Document
     */
    @Test
    public void testEquality() {
	final CSSManager one = new CSSManager();
	one.putProperty("foo", "bar");
	one.putProperty("some", "value");
	final CSSManager two = new CSSManager();
	
	assertNotEquals(one, two);
	
	two.putProperty("foo", "bar");
	two.putProperty("some", "value");
	
	assertEquals(one, two);
    }

}
