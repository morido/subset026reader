import static org.junit.Assert.*;

import org.junit.Test;

import requirement.RequirementOrdinary;
import requirement.RequirementWParent;
import test.helper.ITGenericFigureReader;


/**
 * Test methods for figure extraction
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class ITFigureReader extends ITGenericFigureReader {

    /**
     * Test for generic functionality in {@link docreader.range.FigureReader}
     */
    @SuppressWarnings("javadoc")
    @Test
    public void testFigureGeneric() {
	final String filename = getResourcesDir() + "3_13_6_2_1_9_f38.doc";
	
	final String[] expectedTree = {
		"",
		" 3", // placeholder
		"  3.1", // placeholder
		"   3.1.1", // placeholder
		"    3.1.1.1", // placeholder
		"     3.1.1.1.1", // placeholder
		"      3.1.1.1.1.1",
		"      3.1.1.1.1.1[2]",
		"       3.1.1.1.1.1[2].[f]38",
		"        3.1.1.1.1.1[2].[f]38.C"
	};

	final String[] expectedFilenames = {
		"Figure_Test-3_1_1_1_1_1[2]_[f]38_I.emf"
	};

	final String[] expectedFirstRequirementText = {
		"Note: Figure 38 gives an example of the influence of the various track/train characteristics on A_safe(V,d) and consequently on the EBD curve (see 3.13.8.3).",
	"<span style=\"font-family:Arial; font-size:11pt;\">Note: <span class=\"field\" style=\"color:blue; text-decoration:underline;\">Figure 38</span> gives an example of the influence of the various track/train characteristics on A_safe(V,d) and consequently on the EBD curve (see <span class=\"field\">3.13.8.3</span>).</span>"};		
	final String expectedFigureRequirementText = "<object data=\"media/Figure_Test-3_1_1_1_1_1[2]_[f]38_I.png\" type=\"image/png\" width=\"579\" height=\"262\">Picture missing. No alternative text available.</object>";
	final String[] expectedFigureCaptionRequirementText = {
		"Figure 38: Influence of track/train characteristics on A_safe",
	"<p style=\"display:block; text-align:justify;\"><b><span style=\"font-family:Arial; font-size:11pt;\">Figure <span class=\"field\" style=\"background-color:yellow; font-family:courier; font-weight:bolder;\">{Figure 38}</span>: Influence of track/train characteristics on A_safe</span></b></p>"};

	addIterationAction(0, new CodeWrapper() {	    
	    @Override
	    public void call(final RequirementOrdinary currentRequirement) {		
		assertEquals(expectedFirstRequirementText[0], currentRequirement.getText().getRaw());
		assertEquals(expectedFirstRequirementText[1], currentRequirement.getText().getRich());        		
		assertNull(currentRequirement.getText().getRichWithTraceTags());		
	    }
	});

	addIterationAction(1, new CodeWrapper() {	    
	    @Override
	    public void call(final RequirementOrdinary currentRequirement) {
		//Figure		
		RequirementWParent requirementOfInterest = currentRequirement.getChildIterator().next();
		assertNull(requirementOfInterest.getText().getRaw());
		assertEquals(expectedFigureRequirementText, requirementOfInterest.getText().getRich());
		assertNull(requirementOfInterest.getText().getRichWithTraceTags());        		

		//Figure caption
		requirementOfInterest = requirementOfInterest.getChildIterator().next();
		assertEquals(expectedFigureCaptionRequirementText[0], requirementOfInterest.getText().getRaw());
		assertEquals(expectedFigureCaptionRequirementText[1], requirementOfInterest.getText().getRich());
		assertNull(requirementOfInterest.getText().getRichWithTraceTags());		
	    }
	});
	
	final int numParagraphs = runIndividualTest("Figure Test", filename);

	// check for number of paragraphs read
	assertEquals(4, numParagraphs);

	// check for written filenames
	final String[] writtenFilenames = getWrittenFilenames();	
	assertArrayEquals(expectedFilenames, writtenFilenames);

	// check requirement tree structure
	final String[] actualTree = getTree();
	assertArrayEquals(expectedTree, actualTree);
	
	// check that we have a missing crossreference mark in here
	// TODO
    }

    
    /**
     * Test if we can properly read out figures which share a paragraph with their caption
     */
    @Test
    public void testCombinedFigureCaptionParagraph() {
	final String filename = getResourcesDir() + "3_8_5_3_2_f21a.doc";
	
	final String[] expectedTree = {
		"",
		" 3", // placeholder
		"  3.1", // placeholder
		"   3.1.1", // placeholder
		"    3.1.1.1", // placeholder
		"     3.1.1.1.1",
		"      3.1.1.1.1.*[1]",
		"      3.1.1.1.1.*[2]",
		"      3.1.1.1.1.*[2][2]",
		"       3.1.1.1.1.*[2][2].[f]21a",
		"        3.1.1.1.1.*[2][2].[f]21a.C",
		"      3.1.1.1.1.*[2][3]",
		"       3.1.1.1.1.*[2][3].[f]21b",
		"        3.1.1.1.1.*[2][3].[f]21b.C",
		"     3.1.1.1.2",
		"      3.1.1.1.2.*[1]",
		"      3.1.1.1.2.*[2]",
		"      3.1.1.1.2.*[3]"
	};

	final String[] expectedFilenames = {
		"Figure_Test_With_Fallback-3_1_1_1_1_+[2][2]_[f]21a_I.wmf",
		"Figure_Test_With_Fallback-3_1_1_1_1_+[2][3]_[f]21b_I.wmf"
	};
	
	final String[] firstFigure = {
		"<object data=\"media/Figure_Test_With_Fallback-3_1_1_1_1_+[2][2]_[f]21a_I.png\" type=\"image/png\" width=\"559\" height=\"251\">Picture missing. No alternative text available.</object>",
		"Figure 21a: Extension of an MA in Level 1, one section in the new MA"
	};
	final String[] secondFigure = {
		"<object data=\"media/Figure_Test_With_Fallback-3_1_1_1_1_+[2][3]_[f]21b_I.png\" type=\"image/png\" width=\"559\" height=\"268\">Picture missing. No alternative text available.</object>"
		, "Figure 21b: Extension of an MA in level 1, two sections in the new MA"
	};
	
	addIterationAction(3, new CodeWrapper() {	    
	    @Override
	    public void call(final RequirementOrdinary currentRequirement) {		
		assertEquals(firstFigure[0], currentRequirement.getChildIterator().next().getText().getRich());
		assertEquals(firstFigure[1], currentRequirement.getChildIterator().next().getChildIterator().next().getText().getRaw());		
	    }
	});
	addIterationAction(5, new CodeWrapper() {	    
	    @Override
	    public void call(final RequirementOrdinary currentRequirement) {		
		assertEquals(secondFigure[0], currentRequirement.getChildIterator().next().getText().getRich());
		assertEquals(secondFigure[1], currentRequirement.getChildIterator().next().getChildIterator().next().getText().getRaw());		
	    }
	});
	
	final int numParagraphs = runIndividualTest("Figure Test With Fallback", filename);

	// check for number of paragraphs read
	assertEquals(12, numParagraphs);

	// check for written filenames
	final String[] writtenFilenames = getWrittenFilenames();	
	assertArrayEquals(expectedFilenames, writtenFilenames);

	// check requirement tree structure
	final String[] actualTree = getTree();
	assertArrayEquals(expectedTree, actualTree);	
    }
    
    /**
     * Test if we can build a proper hierarchy from non-indented figures
     */
    @Test
    public void testFigureNoIndentation() {
	final String filename = getResourcesDir() + "2_6_6_1_6.doc";
	
	final String[] expectedTree = {
		"",
		" 2", // placeholder
		"  2.1", // placeholder
		"   2.1.1", // placeholder
		"    2.1.1.1", // placeholder
		"     2.1.1.1.1",
		"     2.1.1.1.2",
		"     2.1.1.1.2[2]",
		"      2.1.1.1.2[2].[f]6",
		"       2.1.1.1.2[2].[f]6.C"
	};
	
	final String[] expectedFilenames = { "Figure_Test_No_Indentation-2_1_1_1_2[2]_[f]6_I.wmf" };	
	
	final String[] figure = {
		"<object data=\"media/Figure_Test_No_Indentation-2_1_1_1_2[2]_[f]6_I.png\" type=\"image/png\" width=\"544\" height=\"376\">Picture missing. No alternative text available.</object>",
		"Figure 6: ERTMS/ETCS Application Level 2"
	};
	
	addIterationAction(2, new CodeWrapper() {	    
	    @Override
	    public void call(final RequirementOrdinary currentRequirement) {		
		assertEquals(figure[0], currentRequirement.getChildIterator().next().getText().getRich());
		assertEquals(figure[1], currentRequirement.getChildIterator().next().getChildIterator().next().getText().getRaw());		
	    }
	});
	
	final int numParagraphs = runIndividualTest("Figure Test No Indentation", filename, 2);
	
	// check for number of paragraphs read
	assertEquals(4, numParagraphs);

	// check for written filenames
	final String[] writtenFilenames = getWrittenFilenames();	
	assertArrayEquals(expectedFilenames, writtenFilenames);

	// check requirement tree structure
	final String[] actualTree = getTree();
	assertArrayEquals(expectedTree, actualTree);
    }
    
    /**
     * Test if we can deal with OfficeDrawings (as described in [MS-ODRAW]) in figures
     */
    @Test
    public void testFigureWOfficeDrawing() {
	final String filename = getResourcesDir() + "3_4_2_3_3_6_1_f2b.doc";
	
	final String[] expectedTree = {
		"",
		" 3", // placeholder
		"  3.1", // placeholder
		"   3.1.1", // placeholder
		"    3.1.1.1", // placeholder
		"     3.1.1.1.1", // placeholder
		"      3.1.1.1.1.1", // placeholder
		"       3.1.1.1.1.1.1",
		"       3.1.1.1.1.1.1[2]",
		"        3.1.1.1.1.1.1[2].[f]2b",
		"         3.1.1.1.1.1.1[2].[f]2b.C",
		"      3.1.1.1.1.2"
	};
	
	runIndividualTest("Figure Test Office Drawing", filename);
	
	final String[] actualTree = getTree();
	assertArrayEquals(expectedTree, actualTree);
    }
    
    /**
     * Test if we can read out figures which consist of more than one image
     */
    @Test
    public void testFigureWTwoImages() {
	final String filename = getResourcesDir() + "3_6_1_5_2.doc";
	
	final String[] expectedTree = {
		"",
		" 3", // placeholder
		"  3.1", // placeholder
		"   3.1.1", // placeholder
		"    3.1.1.1", // placeholder
		"     3.1.1.1.1",
		"      3.1.1.1.1.a",
		"      3.1.1.1.1.b",
		"      3.1.1.1.1.c",
		"      3.1.1.1.1.d",
		"      3.1.1.1.1.e",
		"      3.1.1.1.1.f",
		"      3.1.1.1.1.g",
		"      3.1.1.1.1.h",
		"      3.1.1.1.1.i",
		"     3.1.1.1.1[2]",
		"      3.1.1.1.1[2].[f]14",
		"       3.1.1.1.1[2].[f]14.C",
		"     3.1.1.1.2"

	};

	final String[] expectedFilenames = {
		// first file is missing in the source
		"Figure_Test_Two_Images-3_1_1_1_1[2]_[f]14_I[2].wmf", 
	};
	
	runIndividualTest("Figure Test Two Images", filename);
	
	// check for tree structure
	final String[] actualTree = getTree();
	assertArrayEquals(expectedTree, actualTree);

	// check for written files	
	final String[] writtenFilenames = getWrittenFilenames();	
	assertEquals(1, writtenFilenames.length); // this should actually be two files; but one is apparently missing in the *.doc
	assertArrayEquals(expectedFilenames, writtenFilenames);	
    }
    
    /**
     * Test if we can extract figures which do not come wrapped in MS Word fields
     */
    @Test
    public void testFigureWithoutField() {
	final String filename = getResourcesDir() + "2_6_3_1_6.doc";	
	
	final String[] expectedTree = {
		"",
		" 2", // placeholder
		"  2.1", // placeholder
		"   2.1.1", // placeholder
		"    2.1.1.1", // placeholder
		"     2.1.1.1.1",
		"     2.1.1.1.1[2]",
		"      2.1.1.1.1[2].[f]2",
		"       2.1.1.1.1[2].[f]2.C"
	};
	
	final String[] expectedFilenames = { "Figure_Test_Without_Field-2_1_1_1_1[2]_[f]2_I.wmf" };	
	
	final String[] figure = {
		"<object data=\"media/Figure_Test_Without_Field-2_1_1_1_1[2]_[f]2_I.png\" type=\"image/png\" width=\"490\" height=\"249\">Picture missing. No alternative text available.</object>",
		"Figure 2: ERTMS/ETCS Application Level 0"
	};
	
	addIterationAction(1, new CodeWrapper() {	    
	    @Override
	    public void call(final RequirementOrdinary currentRequirement) {		
		assertEquals(figure[0], currentRequirement.getChildIterator().next().getText().getRich());
		assertEquals(figure[1], currentRequirement.getChildIterator().next().getChildIterator().next().getText().getRaw());		
	    }
	});
	
	runIndividualTest("Figure Test Without Field", filename, 2);
	
	// check for tree structure
	final String[] actualTree = getTree();
	assertArrayEquals(expectedTree, actualTree);

	// check for written files	
	final String[] writtenFilenames = getWrittenFilenames();	
	assertEquals(1, writtenFilenames.length); // this should actually be two files; but one is apparently missing in the *.doc
	assertArrayEquals(expectedFilenames, writtenFilenames);	
    }
}
