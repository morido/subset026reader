import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import docreader.DocumentReader;
import test.helper.ITConstants;
import static helper.Constants.Generic.MEDIA_STORE_DIR;
import static helper.Constants.Generic.WRITE_STATISTICAL_DATA;
import static helper.Constants.Generic.STATISTICS_STORE_DIR;

/**
 * Reads a minimal version of a Specification document and validates the output
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class ITMinispec extends ITConstants {

    private static String filename = getResourcesDir() + "minispec.doc";
    private Path temp = null;    
    
    /**
     * Create a temporary directory
     */
    @Before
    public void setup() {
	try {
	    this.temp = Files.createTempDirectory("minispec");
	} catch (IOException e) {
	    e.printStackTrace();
	    fail("Encountered an error while executing the test");	    
	}
    }
    
    /**
     * Delete all temporary artifacts
     */
    @After
    public void tearDown() {
	// recursively delete the temporary directory
	deleteDir(this.temp.toFile());
    }    
    
    /**
     * Main test
     */
    @Test
    public void test() {	

	final String outputFilename = this.temp.toString() + File.separator + "out.reqif";	
	final int returnValue = new DocumentReader("minispec", filename, outputFilename).read();
	assertEquals(0, returnValue);
	
	// Step 1: check which files have been written
	final File outputFile = new File(outputFilename);
	assertTrue(outputFile.isFile() && outputFile.length() > 0); // something has been written
	
	assertTrue(new File(this.temp.toString() + File.separator + MEDIA_STORE_DIR).isDirectory());
	if (WRITE_STATISTICAL_DATA) {
	    final String statisticsDir = this.temp.toString() + File.separator + STATISTICS_STORE_DIR;
	    assertTrue(new File(statisticsDir).isDirectory());
	    
	    final File nodesFile = new File(statisticsDir + File.separator + "nodes.csv");	    
	    assertTrue(nodesFile.isFile() && nodesFile.length() > 0);
	    final File edgesFile = new File(statisticsDir + File.separator + "edges.csv");
	    assertTrue(edgesFile.isFile() && edgesFile.length() > 0);
	}
		
	// Step 2: check ReqIF
	final ArrayList<String> lines = new ArrayList<>();
	try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(outputFile))) {
	    String line;
	    while((line = bufferedReader.readLine()) != null) {
		lines.add(line);
	    }
	}
	catch (IOException e) {
	    e.printStackTrace();
	    fail("Encountered an error while executing the test");
	}
	
	assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", lines.get(0));
	assertEquals("<REQ-IF xmlns=\"http://www.omg.org/spec/ReqIF/20110401/reqif.xsd\" xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:configuration=\"http://eclipse.org/rmf/pror/toolextensions/1.0\">", lines.get(1));
	// why do we have to precede the lines by one more space than in the actual file?
	assertEquals("  <THE-HEADER>", lines.get(2));
	assertEquals("</REQ-IF>", lines.get(lines.size()-1));
	assertEquals("  </TOOL-EXTENSIONS>", lines.get(lines.size()-2));
	assertTrue(lines.contains("            <ATTRIBUTE-VALUE-STRING THE-VALUE=\"System Requirements Specification  Chapter 1  Introduction\">"));
	assertTrue(lines.contains("            <ATTRIBUTE-VALUE-STRING THE-VALUE=\"This is a requirement artifact\">"));	
    }

    private void deleteDir(final File directory) {
	final File[] files = directory.listFiles();
	for (final File file : files) {
	    if (file.isDirectory()) deleteDir(file);	    
	    else file.delete();	    
	}
	directory.delete();
    }
}
