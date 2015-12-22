import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import test.helper.ITGenericListReader;


/**
 * Test certain hierarchical structures in the subset-026
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class ITListReaderStructures extends ITGenericListReader {

    /**
     * Test for max indent functionality in
     * {@link docreader.ListReader.SublistManager.ParagraphPropertiesDeterminer#compareTo(ParagraphPropertiesDeterminer)}
     * 
     * <p>This contains the same table as {@link ITTableReaderKnownTables#testLevelInformationTable()}</p>
     */
    @SuppressWarnings("javadoc")
    @Test
    public void testBlankLine() {
	final String filename = getResourcesDir() + "4_8_3.doc"; 
	runIndividualTest("List Reader Zero Start", filename);
	
	final String[] actualTree = getTree();
	final String[] expectedTreeBeginning = {
		"",
		" 4", // placeholder
		"  4.1", // placeholder
		"   4.1.1",
		"    4.1.1.1",
		"     4.1.1.1.1",
		"     4.1.1.1.1[2]",
		"     4.1.1.1.1[3]",
		"      4.1.1.1.1[3].[t]*"
	};
	assertArrayEquals(expectedTreeBeginning, Arrays.copyOfRange(actualTree, 0, expectedTreeBeginning.length));
    }
    
    /**
     * test for: {@link docreader.ListReader.SublistManager.LevelStore.findCorrectInsertionPoint(ParagraphPropertiesDeterminer, String)}
     */
    @SuppressWarnings("javadoc")
    @Test
    public void testBulletHierarchy() {
	final String filename = getResourcesDir() + "2_6_5_2_3.doc"; 
	runIndividualTest("List Reader Test Bullet Hierarchy", filename, 1);	
	
	final String[] actualTree = getTree();	
	final String expectedTree[] = {
		"",
		" 2", // placeholder
		"  2.1", // placeholder
		"   2.1.1", // placeholder
		"    2.1.1.1", // placeholder
		"     2.1.1.1.1",
		"      2.1.1.1.1.*[1]",
		"      2.1.1.1.1.*[2]",
		"      2.1.1.1.1.*[3]",
		"     2.1.1.1.2",
		"      2.1.1.1.2.*[1]"
	};
	assertArrayEquals(expectedTree, actualTree);
    }
    
    /**
     * test for: {@link docreader.ListReader.SublistManager.LevelStore.findCorrectInsertionPoint(ParagraphPropertiesDeterminer, String)}
     */
    @SuppressWarnings("javadoc")
    @Test
    public void testBulletHierarchyTwoLevels() {
	final String filename = getResourcesDir() + "3_6_1_3.doc"; 
	runIndividualTest("List Reader Test Bullet Hierarchy Two Levels", filename, 1);	
	
	final String[] actualTree = getTree();	
	final String expectedTree[] = {
		"",
		" 3", // placeholder
		"  3.1", // placeholder
		"   3.1.1", // placeholder
		"    3.1.1.1",
		"     3.1.1.1.*[1]",
		"     3.1.1.1.*[2]",
		"     3.1.1.1.*[3]",
		"      3.1.1.1.*[3].*[1]",
		"      3.1.1.1.*[3].*[2]",
		"      3.1.1.1.*[3].*[3]",
		"     3.1.1.1.*[3][2]",		
		"     3.1.1.1.*[4]"
	};
	assertArrayEquals(expectedTree, actualTree);
    }
    
    /**
     * Test method for {@link docreader.ListReader.SublistManager.LevelStore.LevelTuple.removeAllChildren()}
     */
    @SuppressWarnings("javadoc")
    @Test
    public void testBulletReset() {
	final String filename = getResourcesDir() + "2_6_6_2.doc"; 
	runIndividualTest("List Reader Test Bullet Reset", filename, 1);	
	
	final String[] actualTree = getTree();	
	final String expectedTree[] = {
		"",
		" 2", // placeholder
		"  2.1", // placeholder
		"   2.1.1", // placeholder
		"    2.1.1.1",
		"     2.1.1.1.1",
		"      2.1.1.1.1.*[1]",
		"      2.1.1.1.1.*[2]",
		"      2.1.1.1.1.*[3]",
		"     2.1.1.1.2",
		"      2.1.1.1.2.*[1]",
		"      2.1.1.1.2.*[2]",
		"      2.1.1.1.2.*[3]",
		"      2.1.1.1.2.*[4]",
		"      2.1.1.1.2.*[5]",
	};
	assertArrayEquals(expectedTree, actualTree);
    }
    
    /**
     * Test method for {@link docreader.ListReader.SublistManager.LevelStore#isOnSameListLevel(String, LevelTuple)} and
     * {@link docreader.range.paragraph.ParagraphReader.CharacterRunReaderRaw#read(CharacterRun)} in conjunction with {@link docreader.range.paragraph.ParagraphReader#absoluteStartOffsetForRawReader()}
     */
    @SuppressWarnings("javadoc")
    @Test
    public void testFakeItem() {
	final String filename = getResourcesDir() + "s23_3.doc"; 
	runIndividualTest("List Reader Test Fake Item", filename);	
	
	final String[] actualTree = getTree();	
	final String[] expectedTree = {
		"",
		" 0",
		"  0.3",
		"   0.3.1",
		"   0.3.1[2]",
		"   0.3.2",
		"  0.4"
	};
	assertArrayEquals(expectedTree, actualTree);
    }
    
    /**
     * Test method for {@link docreader.ListReader.SublistManager.LevelStore#isOnSameListLevel(String, LevelTuple)} and
     * {@link docreader.range.paragraph.ParagraphReader.CharacterRunReaderRaw#read(CharacterRun)} in conjunction with {@link docreader.range.paragraph.ParagraphReader#absoluteStartOffsetForRawReader()}
     */
    @SuppressWarnings("javadoc")
    @Test
    public void testFakeItemMultilevel() {
	final String filename = getResourcesDir() + "7_5.doc"; 
	runIndividualTest("List Reader Test Fake Item Multilevel", filename);	
	
	final String[] actualTree = getTree();
	final String[] expectedTree = {
		"",
		" 7", // placeholder
		"  7.5",
		"   7.5.0", // placeholder
		"    7.5.0.1",
		"    7.5.0.2",
		"    7.5.0.3",
		"    7.5.0.4",
		"    7.5.0.5",
		"   7.5.1", // placeholder
		"    7.5.1.1"		
	};
	assertArrayEquals(expectedTree, actualTree);
    }
    
    /**
     * Tests if overridden list properties for a specific paragraph are read out correctly
     * <p>Test method for: {@link docreader.range.paragraph.ParagraphListAware}</p>
     */    
    @Test
    public void testListAwarenessPresence() {
	final String filename = getResourcesDir() + "6_5.doc"; 
	runIndividualTest("List Reader Test List Awareness Presence", filename, 1);	
	
	final String[] actualTree = getTree();
	final String[] expectedTree = {
		"",
		" 6", // placeholder
		"  6.1",
		"   6.1.1",
		"    6.1.1.1", // Paragraph's list properties only detectable by ParagraphListAware
		"     6.1.1.1.1"		
	};
	assertArrayEquals(expectedTree, actualTree);
    }
    
    /**
     * Tests if overridden list properties for a specific paragraph are read out correctly
     * <p>Test method for: {@link docreader.range.paragraph.ParagraphListAware}</p>
     */    
    @Test
    public void testListAwarenessAbsence() {
	final String filename = getResourcesDir() + "6_5_1_5_22.doc"; 
	runIndividualTest("List Reader Test List Awareness Absence", filename);	
	
	final String[] actualTree = getTree();	
	final String[] expectedTree = {
		"",
		" 6", // placeholder
		"  6.1", // placeholder
		"   6.1.1", // placeholder
		"    6.1.1.1", // placeholder
		"     6.1.1.1.1",
		"     6.1.1.1.1[2]" // the fact that this is *not* a true list paragraph is only detectable by ParagraphListAware 
	};	
	assertArrayEquals(expectedTree, actualTree);
    }
    
    
    /**
     * Test if we can extract a meaningful hierarchy even without indentation information
     */
    @Test
    public void testNoIndentation() {
	final String filename = getResourcesDir() + "3_13_9_5_8.doc"; 
	runIndividualTest("List Reader Test No Indentation", filename);	
	
	final String[] actualTree = getTree();
	final String[] expectedTree = {
		"",
		" 3",
		"  3.1",
		"   3.1.1",
		"    3.1.1.1",
		"     3.1.1.1.1",
		"     3.1.1.1.2",
		"     3.1.1.1.2[2]",
		"     3.1.1.1.2[3]",
		"     3.1.1.1.2[4]",
		"     3.1.1.1.2[5]",
		"     3.1.1.1.3"		
	};	
	assertArrayEquals(expectedTree, actualTree);
	
	// check if all equations have been written correctly
	final String[] expectedFilenames = {
		"List_Reader_Test_No_Indentation-3_1_1_1_2[2]_E.wmf",
		"List_Reader_Test_No_Indentation-3_1_1_1_2[3]_E.wmf",
		"List_Reader_Test_No_Indentation-3_1_1_1_2[4]_E.wmf",
		"List_Reader_Test_No_Indentation-3_1_1_1_2[5]_E.wmf"
	};	
	assertArrayEquals(expectedFilenames, getWrittenFilenames());
    }
    
    /**
     * Generic test for handling of pseudo list paragraphs (i.e. non-list paragraphs which follow a list paragraph)
     */
    @Test
    public void testPseudoListParagraphs() {
	final String filename = getResourcesDir() + "pseudo_list_hierarchy.doc"; 
	runIndividualTest("List Reader Test Pseudo List Paragraphs", filename);	
	
	final String[] actualTree = getTree();
	final String[] expectedTree = {
		"",
		" 1",
		" 1[2]",
		" 1[3]",
		" 1[4]",
		" 1[5]",
		" 2",
		"  2.a",
		"  2.a[2]",
		"  2.a[3]"			
	};	
	assertArrayEquals(expectedTree, actualTree);
    }
    
    /**
     * This is a weird testfile since it contains two childlists which are interwoven and share the same left indent.
     * <p>
     * So we have: <ul>
     * <li>[List 1] a)</li>
     * <li>[List 2] *</li>
     * <li>[List 2] *</li>
     * <li>[List 1] b)</li>
     * <li>[List 2] *</li>
     * <li>[List 2] *</li>
     * </ul>
     * </p>
     * 
     * This tests if we can guess the right hierarchy from this mess.
     */
    @Test
    public void testInterwovenLists() {
	final String filename = getResourcesDir() + "3_13_4_3_2.doc";
	// we have to skip the last two paragraphs in order to prevent naming collisions
	runIndividualTest("List Reader Test Interwoven Lists", filename, 2);
	
	final String[] actualTree = getTree();
	final String expectedTree[] = {
		"",
		" 3", // placeholder
		"  3.1", // placeholder
		"   3.1.1", // placeholder
		"    3.1.1.1", // placeholder
		"     3.1.1.1.1", // placeholder		
		"      3.1.1.1.1.1",
		"     3.1.1.1.2",
		"      3.1.1.1.2.a",
		"       3.1.1.1.2.a.*[1]",
		"       3.1.1.1.2.a.*[2]",
		"      3.1.1.1.2.b",
		"       3.1.1.1.2.b.*[1]",
		"       3.1.1.1.2.b.*[2]",
		"     3.1.1.1.2[2]",
		"     3.1.1.1.2[3]",
		"     3.1.1.1.2[4]",
		"     3.1.1.1.2[5]",
		"     3.1.1.1.2[6]",
		"     3.1.1.1.2[7]",
		"     3.1.1.1.2[8]",
	};
	assertArrayEquals(expectedTree, actualTree);		
    }
    
    
    /**
     * Test method for {@link docreader.list.ListReaderPlain#getLevelTuple(int, int, ListFormatOverrideLevel, ListLevel, ListLevel)}
     * and {@link docreader.list.ListReaderPlain.ListStore.LevelStore#getTuple(int, int, ListLevel, int, int, int)}
     */
    @SuppressWarnings("javadoc")
    @Test
    public void testZeroStart() {
	final String filename = getResourcesDir() + "7_4_2.doc"; 
	runIndividualTest("List Reader Zero Start", filename);
	
	final String[] actualTree = getTree();
	final String[] expectedTree = {
		"",
		" 7", // placeholder
		"  7.1", // placeholder
		"   7.1.1",
		"    7.1.1.0",
		"    7.1.1.1"		
	};
	assertArrayEquals(expectedTree, actualTree);
    }
    
    /**
     * Test if bullet / figure hierarchy is correctly extracted
     */    
    @Test
    public void testCorrectTracestringWhenNoBullet() {
	final String filename = getResourcesDir() + "3_6_2_3_1_1.doc"; 
	runIndividualTest("List Reader Tracestring Bullet Issue", filename);
	
	final String[] actualTree = getTree();	
	
	final String[] expectedTree = {
		"",
		" 3", //placeholder
		"  3.1", //placeholder
		"   3.1.1", //placeholder
		"    3.1.1.1", //placeholder
		"     3.1.1.1.1", //placeholder
		"      3.1.1.1.1.1",
		"       3.1.1.1.1.1.*[1]",
		"       3.1.1.1.1.1.*[1][2]",
		"        3.1.1.1.1.1.*[1][2].[f]9",
		"         3.1.1.1.1.1.*[1][2].[f]9.C",
		"       3.1.1.1.1.1.*[2]",
		"       3.1.1.1.1.1.*[2][2]",
		"        3.1.1.1.1.1.*[2][2].[f]10",
		"         3.1.1.1.1.1.*[2][2].[f]10.C"		
	};
	assertArrayEquals(expectedTree, actualTree);
    }
}
