package helper.subset26;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import helper.TraceabilityManagerHumanReadable;
import helper.nlp.NLPManager;

import java.util.Iterator;

import org.apache.poi.hwpf.usermodel.Range;
import org.junit.Test;

import docreader.ReaderData;
import requirement.RequirementTemporary;
import requirement.RequirementWParent;
import requirement.data.RequirementText;
import requirement.metadata.Kind;
import requirement.metadata.KnownPhrasesLinker;
import requirement.metadata.LegalObligation;

/**
 * Tests for Metadata refinement during the second pass of the requirement hierarchy
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public class MetadataDeterminerSecondPassTest {

    private final class HierarchyMock {
	
	private final ReaderData readerData = mock(ReaderData.class);
	private final KnownPhrasesLinker knownPhrasesLinker = new KnownPhrasesLinker();	
	private final NLPManager nlpManager = new NLPManager(1);
	final Range associatedRange = mock(Range.class);
	private final RequirementTemporary root = new RequirementTemporary(this.associatedRange, false);

	final class RequirementHandle {
	    private final RequirementWParent wrappedRequirement;

	    RequirementHandle (final RequirementWParent requirement) {
		this.wrappedRequirement = requirement;
	    }

	    RequirementWParent getRequirement() {
		return this.wrappedRequirement;
	    }
	}

	public HierarchyMock() {
	    // unimportant for the test; but will be called by MetadataDeterminer.java
	    when(this.readerData.getKnownPhrasesLinker()).thenReturn(this.knownPhrasesLinker);
	}
	
	/**
	 * @param parent parent requirement or {@null} if this should be directly below the root
	 * @param text textual contents of this requirement
	 * @param kind kind of this requirement
	 * @param legalObligation legalObligation; may be {@code null} for default
	 * @return a handle to this requirement, never {@code null}
	 */
	RequirementHandle addChild(final RequirementHandle parent, final String text, final Kind kind, final LegalObligation legalObligation) {
	    assert text != null && kind != null;
	    final RequirementText requirementText = new RequirementText(text, null);
	    final RequirementWParent child = new RequirementWParent(this.readerData, this.associatedRange, parent != null ? parent.getRequirement() : this.root);
	    child.setText(requirementText);
	    child.getMetadata().setKind(kind);
	    final TraceabilityManagerHumanReadable hrManager = new TraceabilityManagerHumanReadable();
	    hrManager.addList("1");
	    child.setHumanReadableManager(hrManager);
	    if (legalObligation != null) child.getMetadata().setLegalObligation(legalObligation);
	    return new RequirementHandle(child);
	}	

	/**
	 * @return the parent of this mocked hierarchy (= the first element below the root)
	 */
	RequirementWParent getParent() {
	    return this.root.getChildIterator().next();
	}

	/**
	 * Execute a test against this hierarchy tree
	 * 
	 * @param requirement parent requirement of the hierarchy
	 */
	void executeTest(final RequirementWParent requirement) {	    
	    MetadataDeterminerSecondPass.processRequirement(requirement, this.nlpManager);
	    final Iterator<RequirementWParent> iterator = requirement.getChildIterator();
	    while (iterator.hasNext()) executeTest(iterator.next());
	}
    }

    /**
     * Test for correct attribution of headings based on hierarchical properties
     */
    @Test
    public void testHeading() {
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "I pretend to be a heading but have no children", Kind.HEADING, null);

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // changed	    
	}

	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "With regards to Figure 12 the following applies to linking information", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.6.3.2.6
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "The distance (1) shall be given to the first balise group included in the linking information", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 3.6.3.2.6.a
	    hierarchy.addChild(parent, "The distance (n) shall be given as the distance between two consecutive balise groups", Kind.ORDINARY, LegalObligation.MANDATORY); // // source: 3.6.3.2.6.b

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.HEADING, parent.getRequirement().getMetadata().getKind()); // changed
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	}
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "When another balise group becomes the LRBG or when evaluating (see section 4.8) location related trackside information, which is referred to a previously received balise group different from the LRBG, all the location related information shall be relocated by subtracting from the distances that are counted from the reference balise group of the location related information:", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 3.6.4.3
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "the distance between the reference balise group of the location related information and the LRBG, retrieved from linking information if it is available and it includes both the reference balise group and the LRBG, OR", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.6.4.3.a
	    hierarchy.addChild(parent, "in all other cases, the estimated travelled distance between the reference balise group of the location related information and the LRBG.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.6.4.3.b

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	}
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "The RBC/RBC message is consistent when all checks have been completed successfully:", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.15.1.5.2
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "It has passed the checks performed by the RBC/RBC Safe Communication Interface protocol (see SUBSET-098);", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.15.1.5.2.a
	    hierarchy.addChild(parent, "Variables in the message do not have invalid values.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.15.1.5.2.b

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.HEADING, parent.getRequirement().getMetadata().getKind()); // changed
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	}	
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "The purpose of the Shunting mode is to enable shunting movements. In Shunting mode, The ERTMS/ETCS on-board equipment supervises the train movements against:", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 4.4.8.1.1
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "a ceiling speed: the shunting mode speed limit", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 4.4.8.1.1.a
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "a list of expected balise groups (if such list was sent by the trackside equipment). The train shall be tripped if a balise group, not contained in the list, is passed (When an empty list is sent, no balise group can be passed. When no list is sent, all balise groups can be passed)", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 4.4.8.1.1.b

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.UNKNOWN, parent.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.UNKNOWN, firstChild.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.UNKNOWN, secondChild.getRequirement().getMetadata().getLegalObligation()); // not altered
	}
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "When the max safe front end of the train reaches the start location (point D) of the air tightness area:", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 5.18.6.3
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "“Close air conditioning intake announcement” information shall no longer be displayed.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 5.18.6.3.*[1]
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "“Air conditioning intake closed” information shall be displayed to the driver.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 5.18.6.3.*[1]

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.UNKNOWN, parent.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.UNKNOWN, firstChild.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.UNKNOWN, secondChild.getRequirement().getMetadata().getLegalObligation()); // not altered
	}
    }

    /**
     * Test for correct inheritance of NOTEs
     */
    @Test
    public void testNote() {
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "Note: The orientation of infill information given by an infill device is defined in reference to (see also section 3.9):", Kind.NOTE, LegalObligation.NA); // source: 3.6.2.3.1.2
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "In case of a balise group, the orientation of the balise group sending the infill information", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.6.2.3.1.2.*[1]
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "In case of loop, the orientation indicated by the End Of Loop Marker", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.6.2.3.1.2.*[2]
	    final HierarchyMock.RequirementHandle thirdChild = hierarchy.addChild(parent, "In case of radio, the orientation of the LRBG indicated in the message", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.6.2.3.1.2.*[3]

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.NOTE, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(Kind.NOTE, firstChild.getRequirement().getMetadata().getKind()); // changed
	    assertEquals(Kind.NOTE, secondChild.getRequirement().getMetadata().getKind()); // changed
	    assertEquals(Kind.NOTE, thirdChild.getRequirement().getMetadata().getKind()); // changed
	}
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "Note: Some note", Kind.NOTE, LegalObligation.NA);
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "I am deleted", Kind.PLACEHOLDER, LegalObligation.UNKNOWN);	   
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "bla bla bla", Kind.ORDINARY, LegalObligation.UNKNOWN);

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.NOTE, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(Kind.PLACEHOLDER, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(Kind.NOTE, secondChild.getRequirement().getMetadata().getKind()); // changed
	}
    }

    /**
     * A collection of examples of sublists whose items constitute definitions
     */
    @Test
    public void testDefinition() {
	final HierarchyMock hierarchy = new HierarchyMock();
	final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "The following types of speed and distance monitoring are defined:", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.13.10.1.2
	final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "Ceiling speed monitoring (CSM)", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.13.10.1.2.*[1]
	final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "Target speed monitoring (TSM)", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.13.10.1.2.*[2]
	final HierarchyMock.RequirementHandle thirdChild = hierarchy.addChild(parent, "Release speed monitoring (RSM)", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.13.10.1.2.*[3]

	hierarchy.executeTest(hierarchy.getParent());

	assertEquals(Kind.HEADING, parent.getRequirement().getMetadata().getKind()); // changed
	assertEquals(Kind.DEFINITION, firstChild.getRequirement().getMetadata().getKind()); // changed
	assertEquals(Kind.DEFINITION, secondChild.getRequirement().getMetadata().getKind()); // changed
	assertEquals(Kind.DEFINITION, thirdChild.getRequirement().getMetadata().getKind()); // changed
    }
    
    /**
     * Examples of deleted hierarchies
     */
    @Test
    public void testPlaceholder() {
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "deleted", Kind.PLACEHOLDER, LegalObligation.UNKNOWN);
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "deleted", Kind.PLACEHOLDER, LegalObligation.UNKNOWN);
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(firstChild, "Here is content!", Kind.ORDINARY, LegalObligation.UNKNOWN);
	    final HierarchyMock.RequirementHandle thirdChild = hierarchy.addChild(parent, "deleted", Kind.PLACEHOLDER, LegalObligation.UNKNOWN);

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.PLACEHOLDER, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(Kind.PLACEHOLDER, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(Kind.PLACEHOLDER, thirdChild.getRequirement().getMetadata().getKind()); // not altered
	    
	    // this should trigger the logger which we cannot assert against
	}
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "deleted", Kind.PLACEHOLDER, LegalObligation.UNKNOWN);
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "deleted", Kind.PLACEHOLDER, LegalObligation.UNKNOWN);
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(firstChild, "deleted", Kind.PLACEHOLDER, LegalObligation.UNKNOWN);
	    final HierarchyMock.RequirementHandle thirdChild = hierarchy.addChild(parent, "deleted", Kind.PLACEHOLDER, LegalObligation.UNKNOWN);

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.PLACEHOLDER, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(Kind.PLACEHOLDER, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(Kind.PLACEHOLDER, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(Kind.PLACEHOLDER, thirdChild.getRequirement().getMetadata().getKind()); // not altered
	    
	    // this should not trigger the logger
	}
    }

    /**
     * Test for a table hierarchy
     */
    @Test
    public void testTable() {
	final HierarchyMock hierarchy = new HierarchyMock();
	final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "", Kind.TABLE, null);
	final HierarchyMock.RequirementHandle row1 = hierarchy.addChild(parent, "", Kind.TABLE, null);
	final HierarchyMock.RequirementHandle cell11 = hierarchy.addChild(row1, "bla bla", Kind.ORDINARY, LegalObligation.UNKNOWN);
	final HierarchyMock.RequirementHandle cell12 = hierarchy.addChild(row1, "bla bla", Kind.ORDINARY, LegalObligation.UNKNOWN);
	final HierarchyMock.RequirementHandle row2 = hierarchy.addChild(parent, "", Kind.TABLE, null);
	final HierarchyMock.RequirementHandle cell21 = hierarchy.addChild(row2, "bla bla", Kind.ORDINARY, LegalObligation.UNKNOWN);
	final HierarchyMock.RequirementHandle cell22 = hierarchy.addChild(row2, "bla bla", Kind.ORDINARY, LegalObligation.UNKNOWN);
	final HierarchyMock.RequirementHandle caption = hierarchy.addChild(parent, "Table 0815: bla bla", Kind.TABLE, LegalObligation.UNKNOWN);

	hierarchy.executeTest(hierarchy.getParent());

	assertEquals(Kind.TABLE, parent.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(Kind.TABLE, row1.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(Kind.ORDINARY, cell11.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(Kind.ORDINARY, cell12.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(Kind.TABLE, row2.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(Kind.ORDINARY, cell21.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(Kind.ORDINARY, cell22.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(Kind.TABLE, caption.getRequirement().getMetadata().getKind()); // not altered
    }

    /**
     * A collection of examples of sublists which are connected by AND or which need to infer their legal obligation from the list heading
     */
    @Test
    public void testMandatorySublist() {
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "The track ahead free request from the RBC shall indicate to the on-board", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 3.15.5.2
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "at which location the ERTMS/ETCS on-board equipment shall begin to display the request to the driver.", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 3.15.5.2.a
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "at which location the ERTMS/ETCS on-board equipment shall stop to display the request to the driver (in case the driver did not acknowledge).", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 3.15.5.2.b	

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, parent.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, firstChild.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, secondChild.getRequirement().getMetadata().getLegalObligation()); // not altered
	}
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "For trackside information only differing by Y with regards to the highest system version number X supported by on-board, the on-board equipment shall not consider the reception of unknown packet/message as a message data consistency error (i.e. use of spare value for NID_PACKET or NID_MESSAGE) and shall ignore the content of the unknown packet/message in the following cases:", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 3.17.3.11
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "unknown packet included in a balise telegram/loop message related to the higher system version;", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.17.3.11.a
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "unknown radio message from an RBC or RIU operating with the higher system version;", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.15.5.2.b
	    final HierarchyMock.RequirementHandle thirdChild = hierarchy.addChild(parent, "unknown packet from an RBC or RIU operating with the higher system version, included in a message in which one or more optional packet can be added according to the version operated by on-board.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.15.5.2.c

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, parent.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, firstChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, thirdChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, thirdChild.getRequirement().getMetadata().getLegalObligation()); // changed
	}
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "Together with start and end of reversing area, the following supervision information shall be sent:", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 3.15.4.2
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "Maximum distance to run in the direction opposite to the orientation of the reversing area, the fixed reference location being the end location of the area where reversing of movement is permitted at the time of reception of this reversing area information.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.15.4.2.a
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "Reversing mode speed limit allowed during reverse movement.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.15.4.2.b

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, parent.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, firstChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed
	}
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "The following tables shall be applied assuming that:", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 4.8.2.1
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "the information complies with the data consistency checks.(see section 3.16)", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 4.8.2.1.a
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "the direction for which the information is valid matches the current train orientation, or the balise group crossing direction (for SL, PS and SH engines).(see section 3.6.3)", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 4.8.2.1.b

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.HEADING, parent.getRequirement().getMetadata().getKind()); // changed
	    assertEquals(LegalObligation.NA, parent.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, firstChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed
	}
    }

    /**
     * A collection of examples which are connected by OR
     */
    @Test
    public void testMandatoryOrSublist() {
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "A VBC shall be retained on-board when the on-board equipment is switched off (i.e. enters No Power mode) and shall remain applicable when powered on again. It shall be deleted when:", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 3.15.9.5
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "it is ordered by trackside, or", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.15.9.5.a
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "its validity period has elapsed, or", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.15.9.5.b
	    final HierarchyMock.RequirementHandle thirdChild = hierarchy.addChild(parent, "it is removed by the driver (during Start of Mission), or", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.15.9.5.c
	    final HierarchyMock.RequirementHandle fourthChild = hierarchy.addChild(parent, "a mismatch is detected between the country/region identity read from a balise group and the country/region identity of the VBC. Note: this means that the reception of a consistent balise group message is a necessary condition for deleting a VBC due to mismatching country/region identities.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.15.9.5.d

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, parent.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_OR, firstChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_OR, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, thirdChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_OR, thirdChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, fourthChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_OR, fourthChild.getRequirement().getMetadata().getLegalObligation()); // changed
	}
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "Exceptions: Concerning a) and b) of clause 3.16.2.4.4, the ERTMS/ETCS on-board equipment:", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.16.2.4.4.1
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "shall not reject the message and shall not command application of the service brake if the balise not found, or not decoded, is duplicated within the balise group, the duplicating one is correctly read and contains:", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 3.16.2.4.4.1.a
	    final HierarchyMock.RequirementHandle firstSecondLevelChild = hierarchy.addChild(firstChild, "its validity period has elapsed, or", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.16.2.4.4.1.a.*[1]
	    final HierarchyMock.RequirementHandle secondSecondLevelChild = hierarchy.addChild(firstChild, "only information valid for both directions, or", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.16.2.4.4.1.a.*[2]
	    // some children omitted
	    final HierarchyMock.RequirementHandle thirdSecondLevelChild = hierarchy.addChild(firstChild, "only data to be used by applications outside ERTMS/ETCS together with other information valid for both directions.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.16.2.4.4.1.a.*[5]

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not changed
	    assertEquals(LegalObligation.UNKNOWN, parent.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, firstChild.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstSecondLevelChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_OR, firstSecondLevelChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, secondSecondLevelChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_OR, secondSecondLevelChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, thirdSecondLevelChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_OR, thirdSecondLevelChild.getRequirement().getMetadata().getLegalObligation()); // changed
	}
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "For each National Value, the corresponding Default Value shall be used as fall back value if:", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 3.18.2.5
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "the National Value  is not available, or", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.18.2.5.*[1]
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "a mismatch has been detected between the country or region identifier read from a balise group and the corresponding identifier(s) of the applicable set with which the National Value was received and stored.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.18.2.5.*[2]

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, parent.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_OR, firstChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_OR, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed	    
	}
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "The ERTMS/ETCS on-board equipment shall open a communication session with the RBC when at least one of the following events occurs:", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 4.4.6.1.10
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "in all levels, on receipt of the order to contact the RBC.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 4.4.6.1.10.a
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "In level 2/3, when entering or exiting Sleeping mode (to report the change of mode to the RBC).", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 4.4.6.1.10.b
	    final HierarchyMock.RequirementHandle thirdChild = hierarchy.addChild(parent, "In level 2/3, when a safety critical fault of the ERTMS/ETCS on-board equipment occurs (to report the fault to the RBC).", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 4.4.6.1.10.c

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, parent.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_OR, firstChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_OR, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, thirdChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_OR, thirdChild.getRequirement().getMetadata().getLegalObligation()); // changed
	}
    }

    /**
     * A collection of different examples of mandatory sublists which are connected by exclusive OR (aka XOR)
     */
    @Test
    public void testMandatoryXORSublist() {
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "Depending on which mode is entered, the action shall be one of the following:", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 4.10.1.3
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "data is deleted,", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 4.10.1.3.a
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "data is to be revalidated,", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 4.10.1.3.b
	    final HierarchyMock.RequirementHandle thirdChild = hierarchy.addChild(parent, "data is reset (set to default values)", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 4.10.1.3.c

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, parent.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_XOR, firstChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_XOR, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, thirdChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_XOR, thirdChild.getRequirement().getMetadata().getLegalObligation()); // changed
	}
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "If the driver changes the level to 2 or 3, the ERTMS/ETCS on-board equipment shall establish a communication session with the RBC:", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 5.10.3.15.2
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "immediately if at least one Mobile Terminal is registered to a Radio Network and a valid RBC ID/ phone number is available, OR", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 5.10.3.15.2.a
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "once the driver has selected the RBC contact information (by the same means as for Start of Mission), if either no Mobile Terminal is registered to a Radio Network or no valid RBC ID/phone number is available.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 5.10.3.15.2.b	    

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, parent.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_XOR, firstChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_XOR, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed	    
	}
	{
	    // this is somewhat unclear in the specs itself...
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "The on-board shall consider the service brake command as available for use unless:", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 3.13.10.4.9
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "The service brake command is not implemented, OR", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.13.10.4.9.a
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "The national value inhibits its use.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 3.13.10.4.9.b	    

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY, parent.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_XOR, firstChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.MANDATORY_LIST_XOR, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed	    
	}
    }

    /**
     * A collection of different examples of optional sublists which are connected by exclusive OR (aka XOR)
     */
    @Test
    public void testOptionalXORSublist() {
	{
	    final HierarchyMock hierarchy = new HierarchyMock();
	    final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "For the train to be able to enter the new area, the old area must possess information about at least the first section of the new area. The information may be transmitted to the train either", Kind.ORDINARY, LegalObligation.OPTIONAL); // source: 5.10.3.5.1
	    final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "as an MA and track description information into the new area, or", Kind.ORDINARY, null); // source: 5.10.3.5.1.a
	    final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "as a target speed at the border location i.e. as an LOA.", Kind.ORDINARY, null); // source: 4.10.1.3.b	    

	    hierarchy.executeTest(hierarchy.getParent());

	    assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.OPTIONAL, parent.getRequirement().getMetadata().getLegalObligation()); // not altered
	    assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.OPTIONAL_LIST_XOR, firstChild.getRequirement().getMetadata().getLegalObligation()); // changed
	    assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	    assertEquals(LegalObligation.OPTIONAL_LIST_XOR, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed
	}
    }
    
    /**
     * A collection of different examples of optional sublists which are connected by OR
     */
    @Test
    public void testOptionalORSublist() {
	final HierarchyMock hierarchy = new HierarchyMock();
	final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "The train to track message 136 (Train Position Report) and 157 (SoM Position Report) may optionally include the following packets:", Kind.ORDINARY, LegalObligation.OPTIONAL); // source: 8.4.4.4.2
	final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "Packet 4 (Error Reporting),", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 8.4.4.4.2.a
	final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "Packet 5 (Train running number),", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 8.4.4.4.2.b
	final HierarchyMock.RequirementHandle thirdChild = hierarchy.addChild(parent, "Packet 44 (Data used by applications outside the ERTMS/ETCS system).", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 8.4.4.4.2.c

	hierarchy.executeTest(hierarchy.getParent());

	assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(LegalObligation.OPTIONAL, parent.getRequirement().getMetadata().getLegalObligation()); // changed
	assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(LegalObligation.OPTIONAL, firstChild.getRequirement().getMetadata().getLegalObligation()); // changed
	assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(LegalObligation.OPTIONAL, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed
	assertEquals(Kind.ORDINARY, thirdChild.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(LegalObligation.OPTIONAL, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed
    }
    
    /**
     * A collection of different examples of optional sublists which are connected by exclusive OR (aka XOR)
     */
    @Test
    public void testMixedSublist() {
	final HierarchyMock hierarchy = new HierarchyMock();
	final HierarchyMock.RequirementHandle parent = hierarchy.addChild(null, "A message (Euroradio/Euroloop) or telegram (Eurobalise) shall be composed of", Kind.ORDINARY, LegalObligation.MANDATORY); // source: 8.4.1.1
	final HierarchyMock.RequirementHandle firstChild = hierarchy.addChild(parent, "One Header,", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 8.4.1.1.1
	final HierarchyMock.RequirementHandle secondChild = hierarchy.addChild(parent, "When needed, a predefined set of variables (only for Radio),", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 8.4.1.1.2
	final HierarchyMock.RequirementHandle thirdChild = hierarchy.addChild(parent, "When needed, a predefined set of Packets (only for Radio),", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 8.4.1.1.3
	final HierarchyMock.RequirementHandle fourthChild = hierarchy.addChild(parent, "Optional Packets as needed by application.", Kind.ORDINARY, LegalObligation.UNKNOWN); // source: 8.4.1.1.4

	hierarchy.executeTest(hierarchy.getParent());

	assertEquals(Kind.ORDINARY, parent.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(LegalObligation.MANDATORY, parent.getRequirement().getMetadata().getLegalObligation()); // changed
	assertEquals(Kind.ORDINARY, firstChild.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(LegalObligation.MANDATORY, firstChild.getRequirement().getMetadata().getLegalObligation()); // changed
	assertEquals(Kind.ORDINARY, secondChild.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(LegalObligation.OPTIONAL, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed
	assertEquals(Kind.ORDINARY, thirdChild.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(LegalObligation.OPTIONAL, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed
	assertEquals(Kind.ORDINARY, fourthChild.getRequirement().getMetadata().getKind()); // not altered
	assertEquals(LegalObligation.OPTIONAL, secondChild.getRequirement().getMetadata().getLegalObligation()); // changed
    }

}
