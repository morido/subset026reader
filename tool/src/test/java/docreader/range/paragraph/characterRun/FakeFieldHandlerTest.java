package docreader.range.paragraph.characterRun;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import helper.TraceabilityManagerHumanReadable;
import helper.annotations.DomainSpecific;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import requirement.RequirementTemporary;
import requirement.RequirementWParent;
import requirement.TraceabilityLinker;
import requirement.data.RequirementLinks;

/**
 * Tests for fake field detection (currently only Links)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CharacterRun.class, RequirementTemporary.class, RequirementWParent.class})
public class FakeFieldHandlerTest {        
    
    /**
     * Test for fake link extraction (that is in-line references to other artifacts without any explicit formatting)
     */
    @SuppressWarnings("static-method")
    @Test
    @DomainSpecific
    public void testFakeLinkExtraction() {
	// Step 0: Setup fixture
	final class TestDatum {
	    public final String text;
	    private final String[] expectedLinks;
	    
	    public TestDatum(final String text, final String[] expectedLinks) {
		this.text = text; 
		this.expectedLinks = expectedLinks;
	    }
	    
	    public Set<String> getExpectations() {
		final LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>();
		for (final String currentVal : this.expectedLinks) linkedHashSet.add(currentVal);		
		return linkedHashSet;
	    }
	}	
	
	// Step 1: Setup test data
	final TestDatum[] testData = {
		new TestDatum("Note: For a single balise group reported as LRBG awaiting the assignment of a co-ordinate system also the rules for LRBGs reported to the RBC (see 3.6.2.2.2) apply.", new String[]{"3.6.2.2.2"}), // source: 3.4.2.3.3.7.1		
		new TestDatum("Note: If a single balise group is memorised, according to 3.6.2.2.2c, more than once, and with different “previous LRBGs”, the assignment of the co-ordinate system is ambiguous.", new String[]{"3.6.2.2.2.c"}), // source: 3.4.2.3.3.8.1
		new TestDatum("Note 1: Regarding the repositioning information, see chapter 3.8.5.3.5 and 3.8.5.2", new String[]{"3.8.5.3.5", "3.8.5.2"}), // source: 3.4.4.2.2.2; first is actually a real link
		new TestDatum("a linking consistency error is found, see 3.16.2.3.1", new String[]{"3.16.2.3.1"}), // source: 3.4.4.4.6.b
		new TestDatum("Linking consistency error due to early reception of balise group expected later (see 3.16.2.3.1 c)): if the balise group found is the next one announced in the linking information, the ERTMS/ETCS on-board equipment shall check its linking consistency and apply again clause 3.4.4.4.6, i.e. it will immediately expect the further next balise group announced in the linking information.", new String[]{"3.16.2.3.1.c", "3.4.4.4.6"}), // source: 3.4.4.4.6.1
		new TestDatum("When the previous communication session is considered as terminated due to loss of safe radio connection (refer to 3.5.4.2.1)", new String[]{"3.5.4.2.1"}), // source: 3.5.3.4.f
		new TestDatum("In respect of a), b), c), d) and e) of 3.5.3.4, the on-board shall not establish a new communication session with an RBC/RIU in case a communication session is currently being established or is already established with this RBC/RIU.", new String[]{"3.5.3.4.a", "3.5.3.4.b", "3.5.3.4.c", "3.5.3.4.d", "3.5.3.4.e"}), // source: 3.5.3.4.1
		new TestDatum("The on-board shall request the set-up of a safe radio connection with the trackside. If this request is part of an on-going Start of Mission procedure, it shall be repeated until successful or a defined number of times (see Appendix A3.1).", new String[]{"A.3.1"}), // source: 3.5.3.7.a
		new TestDatum("Exception: In case a communication session is established and no acknowledgement is received within a fixed waiting time (see Appendix A.3.1) after sending the “Termination of communication session” message, the message shall be repeated with the fixed waiting time after each repetition.", new String[]{"A.3.1"}), // source: 3.5.5.3.1
		new TestDatum("Note: if the session is considered as terminated due to 3.5.4.2.1, the attempts will be resumed immediately according to 3.5.3.4 f).", new String[]{"3.5.4.2.1", "3.5.3.4.f"}), // source: 3.5.4.3.1
		new TestDatum("Exception to 3.5.4.2 and 3.5.4.3: the on-board equipment shall not try to set up a new safe radio connection and shall stop any on-going attempts if the train front is inside an announced radio hole (see 3.12.1.3). The on-board equipment shall try to set it up again when the train front reaches the end of the radio hole.", new String[]{"3.5.4.2", "3.5.4.3", "3.12.1.3"}), // source: 3.5.4.4.
		new TestDatum("for what regards the session establishment, see items b), c), d), e) in 3.5.3.4", new String[]{"3.5.3.4.b", "3.5.3.4.c", "3.5.3.4.d", "3.5.3.4.e"}), // source: 3.5.7.3.a
		new TestDatum("All train related inputs except the fixed values are acquired as Train Data (see 3.18.3.2 items b) c) and d)).", new String[]{"3.18.3.2.b", "3.18.3.2.c", "3.18.3.2.d"}), // source: 3.13.2.2.1.2
		new TestDatum("the safe radio connection is released with the Handing over RBC and the minimum safe rear end of the train has crossed the border, see 3.5.5.1 e) and 3.15.1.2.7", new String[]{"3.5.5.1.e", "3.15.1.2.7"}), // source: 3.5.7.6.b
		new TestDatum("Directional train position information in reference to the balise group orientation (see 3.4.2, also Figure 14) of the LRBG, regarding:", new String[]{"3.4.2", "14"}), // source: 3.6.1.3.*[3]
		new TestDatum("A list of LRBGs, which may alternatively be used by trackside for referencing location dependent information (see 3.6.2.2.2 c)).", new String[]{"3.6.2.2.2.c"}), // source: 3.6.1.3.*[4]
		new TestDatum("Exception: Regarding infill information see section 3.6.2.3.1.", new String[]{"3.6.2.3.1"}), // source: 3.6.2.1.2
		new TestDatum("By exception to clause 3.6.1.3, the train position is referred to this balise group marked as unlinked. The ERTMS/ETCS on-board equipment shall temporarily apply by analogy clauses 3.6.4.2, 3.6.4.2.1, 3.6.4.2.3 and 3.6.4.4 to determine an additional confidence interval, until a further received balise group becomes the LRBG or the location related information is deleted on-board.", new String[]{"3.6.1.3", "3.6.4.2", "3.6.4.2.1", "3.6.4.2.3", "3.6.4.4"}), // source: 3.6.4.7.1
		new TestDatum("Together with the other information ( as listed in section 3.7.1.1 c) and d))", new String[]{"3.7.1.1.c", "3.7.1.1.d"}), // source: 3.7.2.1.*[1]; actually this is three links since "3.7.1.1" is a true link
		new TestDatum("In level 2/3: An MA request shall be sent to the RBC when any part of the track description is deleted according to A3.4, except for situations a, b, f, k.", new String[]{"A.3.4"}), // source: 3.8.2.7.3; exceptions cannot be processed
		new TestDatum("Note: the End Section and Overlap timer start locations may be outside their corresponding section. One example can be seen referring to figure 22c: An infill MA towards a signal at stop will replace the previous End Section by a new short End Section starting at the infill location reference and ending at the next main signal, however the End Section and Overlap timer start locations still have to be consistent with the Interlocking timer start locations. Another example is when a timer start location is in rear of the LRBG.", new String[]{"22c"}), // source: 3.8.3.4.1
		new TestDatum("Case 1: the on-board can handle only one communication session at a time. In this case, the onboard shall (unless §3.9.3.5.1.1 applies):", new String[]{"3.9.3.5.1.1"}), // source: 3.9.3.5.1
		new TestDatum("Note: A Radio infill unit may manage several signals, thus several Radio Infill Areas (see Figure 25a)", new String[]{"25a"}), // source: 3.9.3.12.2 
		new TestDatum("It shall be possible to define whether one or all of the events used from the list in 3.12.3.4.2/3.12.3.4.3 have to be fulfilled to define the start/end condition. This definition shall apply to both start and end conditions.", new String[]{"3.12.3.4.2", "3.12.3.4.3"}), // source: 3.12.3.4.3.1
		new TestDatum("g = 9.81 m/s2", new String[]{}), // source: 3.13.4.3.2.b[4]
		new TestDatum("If the brake position is “Passenger train in P”, the set of Kv_int shall be calculated as a function of the maximum emergency brake deceleration (A_ebmax) in the following way (see also figure 10):", new String[]{"10"}), // source: 3.13.6.2.1.8.1
		new TestDatum("if not inhibited by National Value, the compensation of the inaccuracy of the speed measurement shall be set to a value calculated from the target speed, as defined in SUBSET-041 § 5.3.1.2: V_delta0t =", new String[]{}), // source 3.13.9.3.5.9.b; external reference
		new TestDatum("If it is able to handle one communication session only at a given time, it shall wait until the session with the Handing over RBC is terminated due to crossing the border (refer to 3.5.5.1e and 3.15.1.2.7) and then establish the session with the Accepting RBC.", new String[]{"3.5.5.1.e", "3.15.1.2.7"}), // source: 3.15.1.3.2.b
		new TestDatum("Big metal object in the track, exceeding the limits for big metal masses as defined in Subset-036, section 6.5.2 “Metal Masses in the Track” may trigger an alarm reporting a malfunction for the onboard balise transmission function.", new String[]{}), // source: 3.15.7.1; external reference
		new TestDatum("Exceptions: Concerning a) and b) of clause 3.16.2.5.1, the ERTMS/ETCS on-board equipment:", new String[]{"3.16.2.5.1.a", "3.16.2.5.1.b"}), // source: 3.16.2.5.1.1
		new TestDatum("The chapters 3.16.3.2 to 3.16.3.5 define data consistency principles and corresponding checks for data transmitted as normal priority data. For high priority data, the checks shall not apply.", new String[]{"3.16.3.2", "3.16.3.5"}), // source: 3.16.3.1.4; bug: we do not detect ranges
		new TestDatum("Intentionally deleted (moved to 3.5.5.1 e).", new String[]{"3.5.5.1.e"}), // source: 3.16.3.5.4
		new TestDatum("the reception of a shortened MA (3.8.5.1.3, 3.8.5.1.4);", new String[]{"3.8.5.1.3", "3.8.5.1.4"}), // source: A.3.4.1.2.b
		new TestDatum("Start and stop Track Ahead free request to driver (see 3.15.5)”", new String[]{"3.15.5"}), // source: A.3.5.1.*[9]
		new TestDatum("the safe radio connection is released with the Handing over RBC and the minimum safe rear end of the train has crossed the border, see 3.5.5.1 e) and 3.15.1.2.7.", new String[]{"3.5.5.1.e", "3.15.1.2.7"}), // source: 3.5.7.6.b
		new TestDatum("4.4.11", new String[]{"4.4.11"}), // source: 4.5.2.1[2].[t]1.[r][27].SRSRef.[1]
		new TestDatum("b) and c)", new String[]{}), // source: 4.5.2.1[2].[t]1.[r][56].SRSRef.[2]; this is in fact a link; but split across two paragraphs; cannot read out
		new TestDatum("Note: the term “set of information” refers to the part of a message being stored in the transition buffer (i.e. information which is neither accepted nor rejected immediately) according to the conditions stated in 4.8.3.1 [1] and [2] (for level transition) or according to 4.8.2.1c (for RBC/RBC handover).", new String[]{"4.8.3.1", "4.8.2.1.c"}), // source: 4.8.5.2.1; the bracketed numbers are actually links, too - but they refer to exceptions without proper numbering
		new TestDatum("For UN and SN mode, conditions for re-activation of transition to Trip mode (see § 5.8.4.1a) & b)) shall be supervised.", new String[]{"5.8.4.1.a", "5.8.4.1.b"}), // source: 4.5.2.1[2].[t]1.[r][67].Function.[N]4
		new TestDatum("(“override” function is active) AND (ERTMS/ETCS level switches to 1)  see {3} here under", new String[]{}), // source: 4.6.3[2].[t]*.[C]44.Content; {3} is in fact a link but not qualified; cannot read out
		new TestDatum("b = 2.77", new String[]{}), // source: A.3.9.2[7]
		new TestDatum("k1 = vehicle dependent constant (set by engineering of ETCS on-board; k1 is normally between 2.0 and 2.7)", new String[]{}), // source: A.3.10.3[4]
		new TestDatum("1.1", new String[]{"1.1"}), // source: A.3.2[2].[t]*.[r][31].Value; false positive
		new TestDatum("Within a trackside infrastructure operated with the system version number X = 1, it shall be allowed to use the following values of M_VERSION: 1.0, and 1.1", new String[]{}), // source: 6.5.1.1.2
		new TestDatum("1.5 kV DC, France", new String[]{}), // source: 6.5.1.5.33[3].[t]*.[r][10].Meaning
		new TestDatum("Any balise telegram, which includes the packet 2, the packet 6, the packet 135, the packet 145, the packet 200, the packet 203, the packet 206, the packet 207 or the packet 239, shall be marked with the system version number 1.1.", new String[]{}), // source: 6.5.1.7.1
		new TestDatum("Within a trackside infrastructure operated with the system version number X =2, it shall be allowed to use the following values of M_VERSION: 1.0, 1.1 and 2.0", new String[]{}), // source: 6.5.2.1.2
		new TestDatum("For the balise telegrams/loop messages marked with the system version number 1.0 or 1.1 and for messages transmitted by RIUs certified to the system version number 1.0 or 1.1, the exceptions listed in sections 6.5.1.5 and 6.5.1.6 shall apply by analogy.", new String[]{"6.5.1.5", "6.5.1.6"}), // source: 6.5.2.3.1; the extracted links are actually real links
		new TestDatum("[1b] The National Values Q_NVLOCACC, V_NVLIMSUPERV (introduced in system version number X = 2), if already stored on-board and applicable, shall not be affected by the content of the packet 3 (i.e. if these National Values were already applicable and 2nd bullet of clause 3.18.2.5 is not applied, they shall remain applicable with their country identifier(s) previously stored).", new String[]{"3.18.2.5.*[2]"}), // source: 6.6.3.2.3.d[5]
		new TestDatum("Version 1.0, introduced in SRS 1.2.0 and re-used in SRSs 2.0.0, 2.2.2, 2.3.0", new String[]{}), // source: 7.5.1.79[2].[t]*.[r][6].Meaning
		new TestDatum("3.15 m/s2", new String[]{}), // source: 7.5.0.1[2].[t]*.[r][4].Max
		
		// non qualified links; everything is prepended by a fake "1.2.3."
		new TestDatum("Exception to a): When on-board position is unknown or when position data has been deleted during SoM procedure, the on-board equipment shall use an LRBG identifier set to \"unknown\" until the onboard has validated its position again by passing a balise group.", new String[]{"1.2.3.a"}), // source: 3.6.2.2.2.1
		new TestDatum("Regarding c): From the time it has reported an unknown position, or an invalid position during SoM procedure, to the time it has received from the RBC a message with an LRBG not set to “unknown”, the on-board equipment shall also be able to accept messages from the RBC containing LRBG “unknown”.", new String[]{"1.2.3.c"}), // source: 3.6.2.2.2.3
	};
	
	// Step 2: Actual test
	for (final TestDatum testDatum : testData) {
	    final Set<String> actualValues = new LinkedHashSet<>();
	    process(testDatum.text, actualValues);	    
	    assertEquals(testDatum.getExpectations(), actualValues);	    
	}
    }   
    
    /**
     * Process a chunk of text against the actual implementation
     * 
     * @param paragraphText text to analyze
     * @param actualValues output will go here
     */
    private static void process(final String paragraphText, final Set<String> actualValues) {
	final Paragraph paragraph = mock(Paragraph.class, Mockito.RETURNS_DEEP_STUBS);
		
	when(paragraph.numCharacterRuns()).thenReturn(1);
	final CharacterRun characterRun = mock(CharacterRun.class);
	when(paragraph.getCharacterRun(0)).thenReturn(characterRun);
	when(characterRun.text()).thenReturn(paragraphText);
		
	final RequirementTemporary requirement = mock(RequirementTemporary.class);
	
	final RequirementLinks requirementLinks = new RequirementLinks(requirement) {
		@Override
		public void addLinkToGivenRequirementID(final String requirementID) {
		    actualValues.add(requirementID);
		}
		
		@Override
		public void addLinkToGivenTable(final String tableID) {
		    actualValues.add(tableID);
		}
		
		@Override
		public void addLinkToGivenFigure(final String tableID) {
		    actualValues.add(tableID);
		}
	    };
	when(requirement.getRequirementLinks()).thenReturn(requirementLinks);
	
	// setup a fake history for non qualified links
	final TraceabilityLinker traceabilityLinker = new TraceabilityLinker();
	final char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
	for (int i = 0; i < alphabet.length; i++) {
	    final RequirementWParent fakeHistoricalRequirement = mock(RequirementWParent.class);
	    final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();
	    hrManager.addList("1.2.3." + alphabet[i]);
	    when(fakeHistoricalRequirement.getHumanReadableManager()).thenReturn(hrManager);
	    traceabilityLinker.addRequirementLink(fakeHistoricalRequirement);
	}
	
	final FakeFieldHandler fakeFieldHandler = new FakeFieldHandler(traceabilityLinker.getNonQualifiedManager(), 0, requirement, "");
	fakeFieldHandler.read(characterRun);
	fakeFieldHandler.close();
    }
}
