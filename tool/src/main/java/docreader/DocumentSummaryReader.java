package docreader;

import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.DocumentProperties;
import org.apache.poi.hwpf.model.RevisionMarkAuthorTable;
import org.apache.poi.hwpf.model.SavedByEntry;

import static helper.Constants.Internal.VERSION;

/**
 * Read out metadata of the Word document (title, author, Word version, ...)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class DocumentSummaryReader implements GenericReader<Void> {
    private final transient ReaderData readerData;
    private final transient String inputFilename;
    private final transient String outputFilename;
    private final DateFormat df = new SimpleDateFormat();    
    private final BufferedOutputStream outputStream = new BufferedOutputStream(System.out);
    private final PrintWriter printWriter = new PrintWriter(this.outputStream);
    final static String FORMAT_STRING = "%-20s: %-58s";

    /**
     * @param readerData global reader Data
     * @param inputFilename input *.doc
     * @param outputFilename destination reqif
     */
    public DocumentSummaryReader(final ReaderData readerData, final String inputFilename, final String outputFilename){
	assert readerData !=null & inputFilename != null & outputFilename != null;
	this.readerData = readerData;
	this.inputFilename = inputFilename;
	this.outputFilename = outputFilename;
    }

    @Override
    public Void read() {		
	final HWPFDocument document = this.readerData.getDocument();
	final DocumentProperties properties = document.getDocProperties();
	final SummaryInformation summaryInformation = document.getSummaryInformation();
	
	final String lastSavedByUserName;
	final String lastSavedByFileName;
	if (document.getSavedByTable() != null) {
	    final List<SavedByEntry> savedByEntries = document.getSavedByTable().getEntries();
	    final SavedByEntry lastSavedBy = savedByEntries.get(savedByEntries.size()-1);
	    lastSavedByUserName = nullToString(lastSavedBy.getUserName());
	    lastSavedByFileName = nullToString(lastSavedBy.getSaveLocation());
	}
	else {
	    lastSavedByUserName = nullToString(null);
	    lastSavedByFileName = nullToString(null);	    
	}
	
	final String lastRevMarkAuthor;
	final RevisionMarkAuthorTable revMarkTable = document.getRevisionMarkAuthorTable();
	if (revMarkTable != null) {
	    lastRevMarkAuthor = nullToString(revMarkTable.getAuthor(revMarkTable.getSize()-1));
	}
	else lastRevMarkAuthor = nullToString(null);
	

	this.printWriter.println("--------------------------------------------------------------------------------");
	this.printWriter.println("Subset26 reader Version " + VERSION);	
	this.printWriter.println();
	this.printWriter.println("INPUT");
	print("Input filename", this.inputFilename);
	print("Document title", nullToString(summaryInformation.getTitle()));
	print("Document subject", nullToString(summaryInformation.getSubject()));	
	print("Revision", nullToString(summaryInformation.getRevNumber()));
	print("Creation time", getDate(summaryInformation.getCreateDateTime()));
	print("Last saved time", getDate(summaryInformation.getLastSaveDateTime()));
	print("Last saved by", lastSavedByUserName);
	print("Last saved location", lastSavedByFileName);
	
	print("Creation tool", nullToString(summaryInformation.getApplicationName()));
	print("Operating System", getOperatingSystem(summaryInformation.getOSVersion()));
	// [MS-DOC], v20140721, 2.7.2
	final String estimate = properties.isFExactCWords() ? " (estimate)" : "";
	print("#Pages", getPageCount(summaryInformation, properties) + estimate);
	print("#Paragraphs", getNumberCount(properties.getCParas()) + estimate);
	print("#Words", getNumberCount(summaryInformation.getWordCount()) + estimate);
	print("#Characters", getNumberCount(summaryInformation.getCharCount()) + estimate);
	print("Author", nullToString(summaryInformation.getAuthor()));
	print("Last Author", nullToString(summaryInformation.getLastAuthor()));
	print("Last RevMark Author", lastRevMarkAuthor);
	print("Keywords", nullToString(summaryInformation.getKeywords()));
	print("Comments", nullToString(summaryInformation.getComments()));
	print("Security", getSecurity(summaryInformation.getSecurity()));
	this.printWriter.println();

	this.printWriter.println("OUTPUT");
	print("Output filename", this.outputFilename);
	print("Media output dir", this.readerData.getMediaStoreDirRelative());
	print("Prefix", this.readerData.getDocumentPrefix());

	this.printWriter.println("--------------------------------------------------------------------------------");
	this.printWriter.flush();
	
	this.readerData.flushLogMessages();

	return null;
    }


    private void print(final String identifier, final String value) {	
	this.printWriter.printf(FORMAT_STRING, identifier, value).println();
    }

    private String getDate(final Date date) {
	return date != null ? this.df.format(date) : nullToString(null);
    }    

    private static String getOperatingSystem(final int input) {
	final String output;
	switch (input) {
	// following three cases come from POI
	case PropertySet.OS_MACINTOSH : output = "MacOS"; break;
	case PropertySet.OS_WIN16 : output = "Windows <= 3.1"; break;
	case PropertySet.OS_WIN32 : output = "Windows"; break;
	// other cases come from [MS-OLEPS], v20140502, 5, Note 6
	case 0x00020004 : output = "Windows"; break;
	case 0x00020005 : output = "Windows 2000"; break;
	case 0x00020105 : output = "Windows XP"; break;
	case 0x00020205 : output = "Windows Server 2003"; break;
	case 0x00020006 : output = "Windows Vista or Windows Server 2008"; break;
	case 0x00020106 : output = "Windows 7 or Windows Server 2008 R2"; break;
	case 0x00020206 : output = "Windows 8 (8.1) or Windows Server 2012 (R2)"; break;	
	default: output = "Unknown"; break;
	}
	return output;
    }

    private static String getPageCount(final SummaryInformation summaryInformation, final DocumentProperties properties) {
	assert summaryInformation != null && properties != null;
	final String output;

	if (summaryInformation.getPageCount() != 0) {
	    output = Integer.toString(summaryInformation.getPageCount());
	}
	else if (properties.getCPg() != 0) {
	    output = Integer.toString(properties.getCPg());
	}
	else {
	    output = nullToString(null);
	}
	return output;
    }

    private static String getNumberCount(final int input) {
	return (input == 0) ? nullToString(null) : Integer.toString(input);
    }

    /**
     * @param input a string which; may be {@code null}
     * @return a more descriptive string which is never {@code null}
     */
    private static String nullToString(final String input) {
	return input != null ? input : "Field not set";
    }

    /**
     * Return a string representation of the security field of this document
     * 
     * <p>based on the javadoc comment in {@link org.apache.poi.hpsf.SummaryInformation#getSecurity()}</p>
     * 
     * @param input number descriptor of the security field
     * @return string representation of the security field
     */
    private static String getSecurity(final int input) {	
	final String output;
	switch (input) {
	case 0: output = "No security"; break;
	case 1: output = "Password protected"; break;
	case 2: output = "Read only (recommended)"; break;
	case 4: output = "Read only (enforced)"; break;
	case 8: output = "Locked for annotations"; break;
	default: output = "Unknown"; break;
	}

	return output;
    }
}
