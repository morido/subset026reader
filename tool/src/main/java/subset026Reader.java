import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import docreader.DocumentReader;
import static helper.Constants.Internal.VERSION;

/**
 * Main class; intended to be used directly from the commandline (uses {@code System.exit()}
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class subset026Reader {

    /**
     * Main method
     * 
     * @param args command line arguments
     */
    public static void main(final String[] args) {
	int returnValue;		

	if (args.length != 3) {
	    printUsage();
	    returnValue = 1;
	}
	else {
	    final long startTime = System.currentTimeMillis();

	    final String prefix = args[0];
	    final String input = args[1];
	    final String output = args[2];

	    try {
		returnValue = new DocumentReader(prefix, input, output).read();
	    }
	    catch (RuntimeException e) {
		e.printStackTrace();
		System.err.println("FAIL: " + e.getMessage());
		returnValue = 1;
	    }

	    System.out.println();
	    final long endTime = System.currentTimeMillis();
	    final long runningTime = endTime - startTime;	    
	    final String runningTimeHumanReadable = String.format("%d min, %02d sec", 
		    TimeUnit.MILLISECONDS.toMinutes(runningTime),
		    TimeUnit.MILLISECONDS.toSeconds(runningTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(runningTime))
		    );
	    System.out.println("Running time: " + runningTimeHumanReadable);
	}

	System.exit(returnValue);
    }

    private static void printUsage() {
	final PrintWriter printWriter = new PrintWriter(System.out);
	printWriter.println("subset026 writer - Version " + VERSION);
	printWriter.println();
	printWriter.println("USAGE:");
	printWriter.println("subset026writer PREFIX INPUT OUTPUT");
	printWriter.println();
	printWriter.println("PREFIX - Prefix for media files");
	printWriter.println("INPUT  - input *.doc");
	printWriter.println("OUTPUT - output *.reqif");
	printWriter.flush();	
    }
}