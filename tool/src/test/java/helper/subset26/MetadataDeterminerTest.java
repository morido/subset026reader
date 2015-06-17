package helper.subset26;

import helper.annotations.DomainSpecific;
import helper.poi.PoiHelpers;

import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Range;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;
import requirement.RequirementTemporary;
import requirement.data.RequirementText;
import requirement.metadata.Kind;
import requirement.metadata.KnownPhrasesLinker;
import requirement.metadata.LegalObligation;
import docreader.ReaderData;

/**
 * Tests for {@link MetadataDeterminer}
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CharacterRun.class, PoiHelpers.class})
@SuppressWarnings("static-access")
public class MetadataDeterminerTest {
    private ReaderData readerData;
    private KnownPhrasesLinker knownPhrasesLinker;
    private Range associatedRange;   
    
    /**
     * generic setup routine
     */    
    @Before   
    public void setup() {
	this.readerData = mock(ReaderData.class);
	this.knownPhrasesLinker = new KnownPhrasesLinker();
	
	// unimportant for the test; but will be called by MetadataDeterminer.java
	when(this.readerData.getKnownPhrasesLinker()).thenReturn(this.knownPhrasesLinker);
	
	// TODO refactor this so RETURNS_DEEP_STUBS becomes obsolete
	this.associatedRange = mock(Range.class, Mockito.RETURNS_DEEP_STUBS);
	when(this.associatedRange.numParagraphs()).thenReturn(1);
	final CharacterRun characterRun = mock(CharacterRun.class);
	when(this.associatedRange.getParagraph(Mockito.anyInt()).getCharacterRun(Mockito.anyInt())).thenReturn(characterRun);
			
	mockStatic(PoiHelpers.class);
    }

    
    /**
     * Test for correct heading attribution based on textual contents and style name
     * <p><em>Note:</em> Formatting is not tested here since mocking would add quite a bit of bloat.</p>
     */
    @Test
    @DomainSpecific
    public void testHeading() {
	// Step 0: Setup fixture
	final class TestDatum {
	    public final String text;
	    public final Boolean matchExpected;
	    public final String styleName;
	    
	    public TestDatum(final String text, final boolean matchExpected, final String styleName) {
		this.text = text; 
		this.matchExpected = matchExpected;
		this.styleName = styleName;
	    }
	}
	
	// Step 1: Setup test data
	final TestDatum[] testData = {
		new TestDatum("I am a heading", true, "Heading"),
		new TestDatum("I am not a heading. Because I contain two sentences.", false, "Heading"),
		new TestDatum("I am also not a heading because I am much too long to constitute a reasonable heading text", false, "Heading"),
		new TestDatum("Aim of linking:", false, "Some arbitrary stylename"), // source: 3.4.4.1.1; not detectable as heading here
		new TestDatum("Examples of MA update", true, "Überschrift"), // source: 3.8.5.3
	};
	
	// Step 2: Actual test
        for (final TestDatum testDatum : testData) {
            when(PoiHelpers.getStyleName(Mockito.any(ReaderData.class), Mockito.anyInt())).thenReturn(testDatum.styleName);
            final RequirementTemporary requirement = new RequirementTemporary(this.associatedRange);
            requirement.setText(this.readerData, new RequirementText(testDatum.text, ""));
            final boolean condition = testDatum.matchExpected ? requirement.getMetadata().getKind() == Kind.HEADING : requirement.getMetadata().getKind() != Kind.HEADING;
            assertTrue("Heading text \"" + testDatum.text + "\" did not match.", condition);                       
        }
    }

    /**
     * Test for correct identification of requirements marked as "Note"
     */
    @Test
    @DomainSpecific
    public void testNote() {
	// Step 0: Setup fixture
	final class TestDatum {
	    public final String text;
	    public final Boolean matchExpected;	    
	    
	    public TestDatum(final String text, final boolean matchExpected) {
		this.text = text; 
		this.matchExpected = matchExpected;
	    }
	}	
	when(PoiHelpers.getStyleName(Mockito.any(ReaderData.class), Mockito.anyInt())).thenReturn("foobar");
	
	// Step 1: Setup test data
	final TestDatum[] testData = {
		new TestDatum("Note: If a short number is used (considering trackside call routing), that number can be programmed into the balise instead of the normal phone number.", true), // source: 3.5.3.14
		new TestDatum("Note 2: In case the identity of the next balise group is not unambiguously known because the route is not known by the trackside, this feature allows to link this balise group.", true), // source: 3.4.4.2.2.3)
		new TestDatum("Note: d) and e) can not be combined.", true), // source: 3.6.5.1.5.1)
		new TestDatum("{1}Note: used on lines where trains are operated with on-board equipment supporting only system version = 1.0.", true), // source: 6.5.1.5.3
		new TestDatum("Note regarding b): If the level transition leads to TR mode, the request for RBC contact information is only displayed once the ERTMS/ETCS on-board equipment is in PT mode.", true), // source: 5.10.3.15.2.1 
	};
	
	// Step 2: Actual test
	for (final TestDatum testDatum : testData) {
	    final RequirementTemporary requirement = new RequirementTemporary(this.associatedRange);
	    requirement.setText(this.readerData, new RequirementText(testDatum.text, ""));
	    final boolean condition = testDatum.matchExpected ? requirement.getMetadata().getKind() == Kind.NOTE : requirement.getMetadata().getKind() != Kind.NOTE;
	    assertTrue("Note text \"" + testDatum.text + "\" did not match.", condition);
	}
    }
    
    /**
     * Test for correct identification of requirements marked as "deleted"
     */
    @Test
    @DomainSpecific
    public void testPlaceholder() {
	// Step 0: Setup fixture
	final class TestDatum {
	    public final String text;
	    public final Boolean matchExpected;	    
	    
	    public TestDatum(final String text, final boolean matchExpected) {
		this.text = text; 
		this.matchExpected = matchExpected;
	    }
	}	
	when(PoiHelpers.getStyleName(Mockito.any(ReaderData.class), Mockito.anyInt())).thenReturn("foobar");
	
	// Step 1: Setup test data
	final TestDatum[] testData = {
		new TestDatum("Notes, Justifications and Examples are only informative and shall not be regarded as requirements.", false), // source: 3.3.1.4
		new TestDatum("Deleted", true), // source: 4.4.12.1.8
		new TestDatum("Intentionally deleted", true), // source: 3.4.3.2.b
		new TestDatum("Intentionally deleted.", true), // source: 3.5.3.9.1
		new TestDatum("Figure 11: Intentionally deleted", true), // source: 3.6.3.1.4.1[2]
		new TestDatum("Intentionally moved.", true), // source: 3.6.5.1.4.i
		new TestDatum("D = Deleted", false), // source: 6.6.3.4.5.d[2].[t]*.[r][1].D
		new TestDatum("If another balise group marked as unlinked is received before the additional confidence interval is deleted:", false), // source: 3.6.4.7.2
		new TestDatum("Void.", true), // source: 6.5.1.3.1
	};
	
	// Step 2: Actual test
	for (final TestDatum testDatum : testData) {
	    final RequirementTemporary requirement = new RequirementTemporary(this.associatedRange);
	    requirement.setText(this.readerData, new RequirementText(testDatum.text, ""));
	    final boolean condition = testDatum.matchExpected ? requirement.getMetadata().getKind() == Kind.PLACEHOLDER : requirement.getMetadata().getKind() != Kind.PLACEHOLDER;
	    assertTrue("Placeholder text \"" + testDatum.text + "\" did not match.", condition);
	}
    }
    
    /**
     * Test for correct identification of requirements marked as an "Example"
     */
    @Test
    @DomainSpecific
    public void testExample() {
	// Step 0: Setup fixture
	final class TestDatum {
	    public final String text;
	    public final Boolean matchExpected;	    
	    
	    public TestDatum(final String text, final boolean matchExpected) {
		this.text = text; 
		this.matchExpected = matchExpected;
	    }
	}	
	when(PoiHelpers.getStyleName(Mockito.any(ReaderData.class), Mockito.anyInt())).thenReturn("foobar");
	
	// Step 1: Setup test data
	final TestDatum[] testData = {
		new TestDatum("Notes, Justifications and Examples are only informative and shall not be regarded as requirements.", false), // source: 3.3.1.4
		new TestDatum("Figure 2b: Example for assigning a co-ordinate system", false), // source: 3.4.2.3.3.6.1[2].[f]2b.C
		new TestDatum("Example: The following figure illustrates the on-board and RBC views of LRBGs:", true), // source: 3.6.2.2.3
		new TestDatum("Examples of MA update", false), // source: 3.8.5.3; this is a heading; only the children are true examples
		new TestDatum("Example 1: in level 1, the crossing of the EOA/LOA location with the min safe antenna, before a new extended MA (received when the min safe antenna was in rear of the EOA/LOA) has been processed, will not lead to train trip. In other terms the replacement of the EOA/LOA is considered by the on-board as happening before the min safe antenna crosses the EOA/LOA location (i.e. preventing that clause 3.13.10.2.7 applies).", true), // source: A.3.5.2.1		
	};
	
	// Step 2: Actual test
	for (final TestDatum testDatum : testData) {
	    final RequirementTemporary requirement = new RequirementTemporary(this.associatedRange);
	    requirement.setText(this.readerData, new RequirementText(testDatum.text, ""));
	    final boolean condition = testDatum.matchExpected ? requirement.getMetadata().getKind() == Kind.EXAMPLE : requirement.getMetadata().getKind() != Kind.EXAMPLE;
	    assertTrue("Example text \"" + testDatum.text + "\" did not match.", condition);
	}
    }
    
    /**
     * Test for correct identification of requirements marked as a "Justification"
     */
    @Test
    @DomainSpecific
    public void testJustification() {
	// Step 0: Setup fixture
	final class TestDatum {
	    public final String text;
	    public final Boolean matchExpected;	    
	    
	    public TestDatum(final String text, final boolean matchExpected) {
		this.text = text;
		this.matchExpected = matchExpected;
	    }
	}	
	when(PoiHelpers.getStyleName(Mockito.any(ReaderData.class), Mockito.anyInt())).thenReturn("foobar");
	
	// Step 1: Setup test data
	final TestDatum[] testData = {
		new TestDatum("Notes, Justifications and Examples are only informative and shall not be regarded as requirements.", false), // source: 3.3.1.4
		new TestDatum("Justification: The location of an unlinked balise group, or the balise group itself, may not be known to the RBC.", true), // source: 3.6.1.4.1
		new TestDatum("Exception: if not rejected due to balise group message consistency check (see 3.16.2.4.4.1 and 3.16.2.5.1.1), data to be forwarded to a National System (see section 3.15.6) shall be accepted. Justification: the co-ordinate system of the balise group might be known to the National System by other means inherent to the National System itself.", false), // source: 3.6.3.1.4.1
		new TestDatum("Justification: Refer to Figure 13. To make it possible to shift the location reference if – due to the location of the LRBG and the start location – distance (1) would become a negative value.", true), // source: 3.6.3.2.5.1
		new TestDatum("Justification for b): This is to ensure that the timer is always started before or at the same time as the related variable information is received. Thus the timer start is independent of in which balise the variable information is given.", true), // source: 3.8.4.2.1.1		
	};
	
	// Step 2: Actual test
	for (final TestDatum testDatum : testData) {
	    final RequirementTemporary requirement = new RequirementTemporary(this.associatedRange);
	    requirement.setText(this.readerData, new RequirementText(testDatum.text, ""));
	    final boolean condition = testDatum.matchExpected ? requirement.getMetadata().getKind() == Kind.JUSTIFICATION : requirement.getMetadata().getKind() != Kind.JUSTIFICATION;
	    assertTrue("Example text \"" + testDatum.text + "\" did not match.", condition);
	}
    }
    
    /**
     * Test for correct identification of requirements which define something
     */
    @Test
    @DomainSpecific
    public void testDefinition() {
	// Step 0: Setup fixture
	final class TestDatum {
	    public final String text;
	    public final Boolean matchExpected;	    
	    
	    public TestDatum(final String text, final boolean matchExpected) {
		this.text = text;
		this.matchExpected = matchExpected;
	    }
	}	
	when(PoiHelpers.getStyleName(Mockito.any(ReaderData.class), Mockito.anyInt())).thenReturn("foobar");
	
	// Step 1: Setup test data
	final TestDatum[] testData = {
		new TestDatum("Notes, Justifications and Examples are only informative and shall not be regarded as requirements.", true), // source: 3.3.1.4
		new TestDatum("Definitions, high level principles and rules regarding the offline management of ERTMS/ETCS system version during the ERTMS/ETCS system life time are given in SUBSET-104.", true), // source: 3.17.1.1
		new TestDatum("The coefficients for the polynomials shall be defined as follows:", true), // source: A.3.7.6
		new TestDatum("The correction factor kto shall be defined as in A.3.8.5", true), // source: A.3.9.5
		new TestDatum("\"Linking information is used\" shall be interpreted as when balise group(s) are announced and the minimum safe antenna position has not yet passed the expectation window of the furthest announced balise group.", true), // source: 3.4.4.2.1.1
		new TestDatum("Whenever the type of specific SSP category is not explicitly specified in the following requirements, it shall be interpreted as being applicable for both types of specific SSP categories.", true), // source: 3.11.3.2.1.2
		new TestDatum("The on-board equipment shall display to the driver the information related to one target at a time: the Most Restrictive Displayed Target (MRDT). The MRDT shall be defined as the target of which the braking to target Permitted speed supervision limit (refer to section 3.13.9.3.5), calculated for the current position of the train, is the lowest one amongst the supervised targets:", true), // source: 3.13.10.4.2; this is a hybrid...
		new TestDatum("The equivalent brake build up time (T_brake_build_up) is defined as T_brake_build_up = T_brake_react + 0.5*T_brake_increase.", true), // source: 3.13.2.2.3.2.4
		new TestDatum("The “train orientation relative to LRBG” is defined as the train orientation related to the orientation of the LRBG, see Figure 14. It can be either “nominal” or “reverse”.", true), // source: 3.6.1.6
		new TestDatum("From the SBD curve, Service brake intervention (SBI1), Warning (W), Permitted speed (P) and Indication (I) supervision limits, valid for the estimated speed, are defined as follows (see Figure 46):", true), // source: 3.13.9.3.1.3
		new TestDatum("dV_ebi_min, dV_ebi_max, V_ebi_min and V_ebi_max are defined as fixed values (See Appendix A3.1)", true), // source: 3.13.9.2.4
		new TestDatum("A balise within a balise group shall be regarded as missed if", true), // source: 3.16.2.1.3.
		new TestDatum("The nominal direction of each balise group is defined by increasing internal balise numbers.", false), // source: 3.4.2.2.2
		new TestDatum("The internal number of the balise describes the relative position of the balise in the group.", true), // source: 3.4.1.3
		new TestDatum("This document defines the modes of the ERTMS/ETCS on-board equipment (see chapter 4.4 “Definition of the modes” and chapter 4.5 “Modes and on-board functions”.", true), // source: 4.3.1.1
		new TestDatum("This document describes how the received information is filtered, respect to several criteria such as the level, the mode, etc.. (see chapter 4.8 “Acceptance of received information”).", true), // source: 4.3.1.4
		new TestDatum("A balise group, which contains information that must be considered even when the balise group is not announced by linking, is called an unlinked balise group.", true), // source: 3.4.4.3.1
		new TestDatum("It shall be possible to define the nominal rotating mass to be used for compensating the gradient, instead of the two related fixed values defined in A3.1.", false), // source: 3.13.2.2.10.1
		new TestDatum("The last step of the Kn+(V) or Kn-(V) shall by definition be considered as open ended, i.e. it has no upper speed limit.", true), // source: 3.13.2.2.9.2.6
		new TestDatum("Throughout the following sections, all the distances marked with “d” (lower case), which are referred in parameters, formulas and figures, are counted from the current reference location of the on-board equipment (e.g. the LRBG).", true), // source: 3.13.1.4
		new TestDatum("When a single balise group is detected and the previous LRBG is known, the position report based on two balise groups shall use as direction reference a move from the “previous LRBG” towards this single balise group (being the new LRBG): directional information in the position report pointing in the same direction as the direction reference shall be reported as “nominal”, otherwise as “reverse”.", true), // source: 3.4.2.3.3.2
		new TestDatum("Definition for splitting: The “train to be split” is the train at standstill, waiting for being split. The “front train after splitting” refers to the front part of the train before splitting, the “new train after splitting”, refers to the other part.", true), // source: 5.14.1.1
	};
	
	// Step 2: Actual test
	for (final TestDatum testDatum : testData) {
	    final RequirementTemporary requirement = new RequirementTemporary(this.associatedRange);
	    requirement.setText(this.readerData, new RequirementText(testDatum.text, ""));
	    final boolean condition = testDatum.matchExpected ? requirement.getMetadata().getKind() == Kind.DEFINITION : requirement.getMetadata().getKind() != Kind.DEFINITION; 
	    assertTrue("Example text \"" + testDatum.text + "\" did not match.", condition);
	}
    }
    
    /**
     * Test if requirements can be correctly classified as being atomic
     */
    @Test
    @DomainSpecific
    public void testAtomicity() {
	// Step 0: Setup fixture
	final class TestDatum {
	    public final String text;
	    public final Boolean matchExpected;	    
	    
	    public TestDatum(final String text, final boolean matchExpected) {
		this.text = text;
		this.matchExpected = matchExpected;
	    }
	}	
	when(PoiHelpers.getStyleName(Mockito.any(ReaderData.class), Mockito.anyInt())).thenReturn("foobar");
	
	// Step 1: Setup test data
	final TestDatum[] testData = {
		new TestDatum("Notes, Justifications and Examples are only informative and shall not be regarded as requirements.", true), // source: 3.3.1.4
		new TestDatum("The on-board equipment shall compare the estimated speed and train position with the ceiling and braking to target supervision limits and shall trigger/revoke commands to the train interface (traction cut-off if implemented, service brake if available for use or emergency brake) and supervision statuses, as described in Table 8 and Table 10 (for target related to a MRSP speed decrease or LOA), and as described in Table 9 and Table 11 (for target EOA/SvL with release speed).", false), // source: 3.13.10.4.10
		new TestDatum("The on-board equipment shall execute the transitions between the different supervision statuses as described in Table 12 (see section 4.6.1 for details about the symbols). This table takes into account the order of precedence between the supervision statuses and the possible changes of the displayed target (MRDT), e.g. when the list of supervised targets is updated.", false), // source: 3.13.10.4.5
		new TestDatum("It shall be possible to change the train running number while running, from driver input, from the RBC or from other ERTMS/ETCS external sources.", true), // source: 3.18.4.5.3
	};
	
	// Step 2: Actual test
	for (final TestDatum testDatum : testData) {
	    final RequirementTemporary requirement = new RequirementTemporary(this.associatedRange);
	    requirement.setText(this.readerData, new RequirementText(testDatum.text, ""));
	    final boolean condition = testDatum.matchExpected == requirement.getMetadata().isAtomic(); 
	    assertTrue("Example text \"" + testDatum.text + "\" did not match.", condition);
	}
    }
    
    
    /**
     * Test for various kinds of different legal obligations
     */
    @Test
    @DomainSpecific
    public void testLegalObligation() {
	// Step 0: Setup fixture
	final class TestDatum {
	    public final String text;
	    public final LegalObligation legalObligationExpected;	    
	    
	    public TestDatum(final String text, final LegalObligation legalObligationExpected) {
		this.text = text;
		this.legalObligationExpected = legalObligationExpected;
	    }
	}	
	when(PoiHelpers.getStyleName(Mockito.any(ReaderData.class), Mockito.anyInt())).thenReturn("foobar");
	
	// Step 1: Setup test data	
	final TestDatum[] testData = {
		new TestDatum("Notes, Justifications and Examples are only informative and shall not be regarded as requirements.", LegalObligation.MANDATORY), // source: 3.3.1.4
		new TestDatum("It shall be possible to enter train running number also in a non-leading engine.", LegalObligation.MANDATORY), // source: 3.18.4.5.2		
		new TestDatum("The on-board equipment shall compare the estimated speed and train position with the ceiling and braking to target supervision limits and shall trigger/revoke commands to the train interface (traction cut-off if implemented, service brake if available for use or emergency brake) and supervision statuses, as described in Table 8 and Table 10 (for target related to a MRSP speed decrease or LOA), and as described in Table 9 and Table 11 (for target EOA/SvL with release speed).", LegalObligation.MANDATORY), // source: 3.13.10.4.10
		new TestDatum("The adhesion factor may be changed while the train is running.", LegalObligation.OPTIONAL), // source: 3.18.4.6.2
		new TestDatum("All data that can be stored onboard after being accepted may be influenced in special situations.", LegalObligation.OPTIONAL), // source: A.3.4.1.1		
		new TestDatum("The values of a, b, c and kto used in A.3.9.1, A.3.9.2, A.3.9.3 and A.3.9.4 define reference values for the equivalent brake build up time for the service brake, which shall be considered as maximum ones. If justified by the specific brake system of the train other values of these coefficients, which lead to shorter values of the equivalent brake build up time for the service brake, may be used.", LegalObligation.MIXED), // source: A.3.9.6
		new TestDatum("Depending on the situation, the action can be:", LegalObligation.UNKNOWN), // source: A.3.4.1.3
		new TestDatum("The driver must observe the existing line-side information (signals, speed boards etc.) and National operating rules.", LegalObligation.UNKNOWN), // source: 4.4.19.3.2
		new TestDatum("Indicates whether the telegram must be acknowledged or not", LegalObligation.UNKNOWN), // source: 7.5.1.59[2].[t]*.[r][4]
		new TestDatum("For trains with variable composition (loco hauled trains), the brake characteristics can vary together with the composition of the train. In this case, it is not convenient to pre-program the brake parameters necessary to calculate the braking curves. The only practical way to obtain the correct values for the current train composition is to include them into the data entry process by the driver. However, it cannot be expected from the driver to know deceleration values and brake build up times. Conversion models are therefore defined to convert the parameters entered by the driver (brake percentage and brake position) into the parameters of the corresponding brake model.", LegalObligation.UNKNOWN), // source: 3.13.3.1.1
	};
	
	// Step 2: Actual test	
	for (final TestDatum testDatum : testData) {
	    final RequirementTemporary requirement = new RequirementTemporary(this.associatedRange);
	    requirement.setText(this.readerData, new RequirementText(testDatum.text, ""));	    
	    final boolean condition = testDatum.legalObligationExpected == requirement.getMetadata().getLegalObligation();
	    assertTrue("Example text \"" + testDatum.text + "\" did not match.", condition);	    
	}
    }

    /**
     * Test how many annotations a text contains
     */
    @Test
    @DomainSpecific
    public void testAnnotations() {	
	// Step 0: Setup fixture
	final class TestDatum {
	    public final String text;
	    public final int numberOfAnnotations;

	    public TestDatum(final String text, final int numberOfAnnotations) {
		this.text = text;
		this.numberOfAnnotations = numberOfAnnotations;
	    }
	}	
	when(PoiHelpers.getStyleName(Mockito.any(ReaderData.class), Mockito.anyInt())).thenReturn("foobar");

	// Step 1: Setup test data
	final TestDatum[] testData = {
		// TODO more data necessary
		new TestDatum("Unlinked balise groups shall consist at minimum of two balises.", 4), // source: 3.4.4.3.2
		new TestDatum("In every balise shall at least be stored:", 3), // source: 3.4.1.2
		new TestDatum("As soon as the safe radio connection is set-up, the trackside shall send the message Initiation of communication session to the on-board.", 3), // source: 3.5.3.10.b
		new TestDatum("The RBC shall use the last relevant balise group which was reported by the on-board equipment as a reference (in the following termed as LRBGRBC). At a certain moment LRBGRBC and LRBGONB can be different.", 11), // source: 3.6.2.2.2.b
		new TestDatum("When the speed and distance monitoring function becomes active and the ceiling speed monitoring is the first one entered, the triggering condition t1 defined in Table 5 shall be checked in order to determine whether the Normal status applies. If it is not the case, the on-board shall immediately set the supervision status to the relevant value, applying a transition from the Normal status according to Table 7.", 6), // source: 3.13.10.3.5
		new TestDatum("The locations corresponding to a speed increase of the MRSP shall be supervised by the on-board equipment taking into account the min safe front end of the train.", 3), // source: 3.13.10.3.7
		new TestDatum("The safe brake build up time, T_be, is safety relevant. This means that for the calculation of the safe brake build up time, all necessary track and train characteristics shall be taken into account.", 5), // source: 3.13.6.2.2.1
		new TestDatum("As long as it uses a track condition profile given by trackside, the on-board shall consider locations without special brake contribution over a distance going from the start location of the profile to the foot of the deceleration curve (EBD, SBD or GUI, see sections 3.13.8.3, 3.13.8.4 and 3.13.8.5).", 7), // source: 3.13.5.1
		new TestDatum("Assuming that a fictive train front end would be at any location between the current (actual) train front end location and the SvL, the acceleration due to the gradient shall be determined using the lowest (taking the sign into account) gradient value given by the gradient profile between the location of the fictive train front end and the location of the fictive train rear end (see Figure 35).", 5), // source: 3.13.4.2.1
		new TestDatum("The on-board shall be configured to define whether the service brake feedback is implemented or not, i.e. whether it is able to acquire from the service brake interface the information that the service brake is currently applied (e.g. from the main brake pipe pressure or brake cylinder pressure).", 5), // source: 3.13.2.2.7.2
		new TestDatum("Throughout the following sections, all the distances marked with “d” (lower case), which are referred in parameters, formulas and figures, are counted from the current reference location of the on-board equipment.", 3), // source: 3.13.1.4; shortened
		new TestDatum("A Track Condition shall be given as profile data (e.g. non-stopping area), i.e. start and end of the data is given, or location data (e.g. change of traction system) i.e. start location given, depending on the type of track condition.", 5), // source: 3.12.1.2; contains two stopwords
		new TestDatum("When the on-board equipment reads the next main signal balise group or when it detects that the next main signal balise group was missed, new infill information possibly received from the loop shall be ignored.", 8), // source: 3.9.2.11
		new TestDatum("The end of an overlap (if used in the existing interlocking system) is a location beyond the Danger Point that can be reached by the front end of the train without a risk for a hazardous situation. This additional distance is only valid for a defined time.", 5), // source: 3.8.1.1.d
		new TestDatum("In some situations, the track description and linking information shall be deleted (or initial state shall be resumed) by the on-board equipment. These various cases where the data is affected (e.g. the MA is shortened) are described in detail in Appendix A3.4.", 8), // source: 3.7.3.3; contains two stopwords
		new TestDatum("By exception to clause 3.6.1.3, the train position is referred to this balise group marked as unlinked. The ERTMS/ETCS on-board equipment shall temporarily apply by analogy clauses 3.6.4.2, 3.6.4.2.1, 3.6.4.2.3 and 3.6.4.4 to determine an additional confidence interval, until a further received balise group becomes the LRBG or the location related information is deleted on-board.", 10), // source: 3.6.4.7.1
		new TestDatum("dismay mayor shallow", 0), // contrived
		new TestDatum("may: (shall", 2), // contrived
	};

	// Step 2: Actual test
	for (final TestDatum testDatum : testData) {
	    final RequirementTemporary requirement = new RequirementTemporary(this.associatedRange);
	    requirement.setText(this.readerData, new RequirementText(testDatum.text, ""));	   
	    assertEquals("Example text \"" + testDatum.text + "\" did not match.", testDatum.numberOfAnnotations, requirement.getMetadata().getTextAnnotator().getNumberOfAnnotations());	    
	}
    }
}
