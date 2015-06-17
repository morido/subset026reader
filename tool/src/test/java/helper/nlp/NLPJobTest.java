package helper.nlp;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import helper.ConsoleOutputFilter;
import helper.formatting.textannotation.Annotator;
import helper.nlp.NLPJob;

import org.junit.Test;

import requirement.metadata.TextAnnotator;


/**
 * Test methods for {@link NLPJob}
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 */
public class NLPJobTest {
    /**
     * Ordinary constructor
     */
    public NLPJobTest() {
	new ConsoleOutputFilter().addCurrentThread();	
    }

    private final static class TestDatum {
	final String text;
	final String[] annotatedPhrases;

	TestDatum(final String text, final String[] annotatedPhrases) {
	    this.text = text;
	    this.annotatedPhrases = annotatedPhrases;
	}
    }

    /**
     * Various test strings for NLP from the subset026
     */
    @SuppressWarnings("static-method")
    @Test
    public void test() {	

	final TestDatum[] testData = {
		new TestDatum("It is the RBC responsibility to give an SR authorisation, or a Full Supervision MA or an On Sight/Shunting MA to an ERTMS/ETCS equipment that is in Post Trip mode.", new String[]{"RBC responsibility"}), // source: 4.4.14.1.6
		new TestDatum("The description of the procedures shows all states of the ERTMS/ETCS onboard unit and the conditions that must be fulfilled to switch from one state to another.", new String[]{ "shows", "The description of the procedures"}), // source: 1.8.6.1"
		new TestDatum("Technical interoperability requires specifications of a detailed level", new String[]{"requires", "Technical"}), // source: 1.5.1.3; NLP Error: "interoperability" is missing
		new TestDatum("Note 1: Regarding the repositioning information, see chapter 3.8.5.3.5 and 3.8.5.2.", new String[]{"Regarding"}), // TODO add source
		new TestDatum("Note: as long as the same MRSP or LOA target is displayed, the on-board will revoke the Indication status only when the estimated speed does no longer exceed the target speed.", new String[]{"revoke", "the on-board"}), // TODO add source
		new TestDatum("The start location of the release speed monitoring (i.e. where the EBI supervision limit related to EBD is replaced with an EBI supervision limit equal to the release speed value) shall be the location of the FLOI supervision limit, calculated for the Release Speed value, taking into account the following assumptions:", new String[]{"location"}), // TODO source
		new TestDatum("The accuracy of this location. Note: If the reference balise is duplicated, it is the trackside responsibility to define the location accuracy to cover at least the location of the two duplicated balises.", new String[]{"accuracy", "Note"}), // source: 3.4.4.2.1.c; Note will be flagged here as it is not in the beginning of the string
	};
	process(testData);

	// final String textToAnnotate = "For single balise groups reported as LRBG and stored according to 3.6.2.2.2c, awaiting an assignment of a co-ordinate system, the ERTMS/ETCS on-board equipment shall be able to discriminate if a single balise has been reported more than once and with different “previous LRBGs” to the RBC.";
	//final String textToAnnotate = "Note: The release speed may be necessary for two reasons.";	
	//final String textToAnnotate = "if not inhibited by National Value, the compensation of the inaccuracy of the speed measurement shall be set to a value calculated from the release speed, as defined in SUBSET-041 § 5.3.1.2: V_delta0rs = f41(V_release)";
	//final String textToAnnotate = "Note: as long as the same MRSP or LOA target is displayed, the on-board will revoke the Indication status only when the estimated speed does no longer exceed the target speed. On the other hand, as long as the same EOA/SvL target is displayed, the Indication status, once it is triggered, is never revoked. However, for all types of target and only in case of MRDT change, the on-board will revoke the Indication status if the Indication supervision limit (speed and location) is not exceeded (see Table 10 and Table 11).";
	//final String textToAnnotate = "Note 1: Regarding the repositioning information, see chapter 3.8.5.3.5 and 3.8.5.2.";
	// final String textToAnnotate = "Note 2: if ERTMS/ETCS on-board equipment is powered-up in an area not covered by the memorized or default Radio Network, attempts to register to this Radio Network will be repeated unconditionally by the Mobile Terminal(s) until either an attempt is successful or a new Radio Network identity is received from trackside or from driver, preventing Mobile Terminal(s) from registering to any unwanted Radio Network.";		
    }


    private static void process(final TestDatum[] testData) {		
	class TestTextAnnotator extends TextAnnotator {
	    protected List<String> phrases = new ArrayList<>();
	    
	    protected TestTextAnnotator(String textToAnnotate) {
		super(textToAnnotate);
	    }	    

	    public List<String> getPhrases() {
		return this.phrases;
	    }
	}	
	final List<TestTextAnnotator> textAnnotators = new ArrayList<>();

	// create input
	final NLPManager nlpManager = new NLPManager(Runtime.getRuntime().availableProcessors());
	for (final TestDatum testDatum : testData) {	    
	    final String currentText = testDatum.text;

	    final TestTextAnnotator textAnnotator = new TestTextAnnotator(currentText){
		@Override
		public synchronized void addAnnotation(final int startOffset, final int endOffset, final Annotator annotator) {
		    this.phrases.add(currentText.substring(startOffset, endOffset));
		}
	    };

	    final NLPJob nlpProcessor = new NLPJob(currentText, textAnnotator);
	    nlpManager.submitNLPJob(nlpProcessor);	    
	    textAnnotators.add(textAnnotator);
	}
	
	// let process
	nlpManager.waitForNLPJobsToFinish();
	
	// check output (actual test)
	for (int i = 0; i < testData.length; i++) {
	    final String[] currentOutput = textAnnotators.get(i).getPhrases().toArray(new String[0]);
	    assertArrayEquals("Arrays differed in iteration " + Integer.toString(i), testData[i].annotatedPhrases, currentOutput);
	}
    }
}
