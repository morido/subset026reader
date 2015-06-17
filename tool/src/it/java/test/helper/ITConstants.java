package test.helper;

import java.io.File;

/**
 * Constants for integration tests
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public abstract class ITConstants {

    /**
     * @return relative path to the directory where the test resource files (*.doc) are stored
     */
    protected static String getResourcesDir() {
	return "./src/it/resources/".replace('/', File.separatorChar);	
    }
}
