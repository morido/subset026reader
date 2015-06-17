package test.helper;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.poi.hwpf.HWPFDocument;
import org.junit.After;

import docreader.ReaderData;
import requirement.RequirementRoot;
import requirement.RequirementWParent;

/**
 * Generic methods for integration tests
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public abstract class ITGenericReader extends ITConstants {
    protected transient ReaderData readerData;

    protected void setupTest(final String testcaseName, final String filename) {
	try (final FileInputStream fileInputStream = new FileInputStream(filename)){
	    final HWPFDocument document = new HWPFDocument(fileInputStream);
	    this.readerData = new ReaderData(document, testcaseName, System.getProperty("user.dir") + File.separator + "out.reqif");
	} catch (IOException e) {
	    throw new IllegalArgumentException("File " + filename + " not found.", e);
	}
    }

    protected String getTree(final RequirementRoot baseNode, final int level) {
	assert baseNode != null;
	final String levelIndenter = " ";		

	final StringBuilder output = new StringBuilder();	
	for (int i = 0; i < level; i++) {
	    output.append(levelIndenter);
	}		
	output.append(baseNode.getHumanReadableManager().getTag());

	final Iterator<RequirementWParent> iterator = baseNode.getChildIterator();		
	while (iterator.hasNext()) {
	    output.append('\n').append(getTree(iterator.next(), level+1));	    
	}

	return output.toString(); 
    }

    /**
     * @return string array of the basenames of all written files in alphabetical order
     */
    protected String[] getWrittenFilenames() {
	final File[] writtenFiles = new File(this.readerData.getAbsoluteFilePathPrefix() + File.separator + this.readerData.getMediaStoreDirRelative()).listFiles();
	final String[] writtenFilenames = new String[writtenFiles.length];	    
	for (int j = 0; j < writtenFilenames.length; j++) writtenFilenames[j] = writtenFiles[j].getName();
	Arrays.sort(writtenFilenames); // make sure they are in alphabetical order
	return writtenFilenames;
    }    

    /**
     * Generic tearDown method for jUnit; cleans up the mediastore
     */
    @After
    public void tearDown() {
	if (this.readerData == null) return; // apparently we do not need to delete anything 

	// delete the temporary media store
	final File mediaStoreDir = new File(this.readerData.getAbsoluteFilePathPrefix() + File.separator + this.readerData.getMediaStoreDirRelative());
	final File[] files = mediaStoreDir.listFiles();
	for (final File file : files) file.delete();
	mediaStoreDir.delete();		
    }
}
