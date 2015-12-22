import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import test.helper.ITGenericTableReader;

/**
 * Test for a table without special tracing information
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class ITTableReader extends ITGenericTableReader {	
    private static String filename = getResourcesDir() + "3_13_2_2_6_1.doc";
    private int paragraphsRead;

    /**
     * Generic setup routine
     */
    @Before
    public void setUp() {
	this.paragraphsRead = runIndividualTest("ordinary table", filename);
    }

    /**
     * Test method for {@link docreader.range.table.TableReader#read()}. 
     */
    @Test
    public void testRichText() {

	//Check for number of paragraphs
	assertEquals(39, this.paragraphsRead);

	//Check dimensions
	assertEquals(5, numOfChildren()); //includes caption

	//Check if text has been read out correctly
	String expectedResult = "<table style=\"border-collapse:collapse; border-spacing:0;\"><tr><td style=\"background-color:#D9D9D9; border-color:transparent; border-style:none; border-width:0px; padding:0px 0px 4px; vertical-align:top; width:35pt;\"></td><td style=\"background-color:#D9D9D9; border-color:transparent Black transparent transparent; border-style:none double none none; border-width:0px 1px 0px 0px; padding:0px 0px 4px; vertical-align:top; width:97pt;\"></td><td colspan=\"4\" style=\"background-color:#D9D9D9; border-color:Black; border-style:solid solid double double; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:325pt;\"><p style=\"display:block; text-align:center;\"><i><span style=\"font-family:Arial; font-size:11pt;\">configuration possibilities</span></i></p></td></tr><tr><td style=\"background-color:#D9D9D9; border-color:transparent transparent Black; border-style:none none double; border-width:0px 0px 1px; padding:0px 0px 4px; vertical-align:top; width:35pt;\"></td><td style=\"background-color:#D9D9D9; border-color:transparent Black Black transparent; border-style:none double double none; border-width:0px 1px 1px 0px; padding:0px 0px 4px; vertical-align:top; width:97pt;\"></td><td style=\"background-color:#D9D9D9; border-color:Black; border-style:solid solid double double; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:79pt;\"><i><span style=\"font-family:Arial; font-size:11pt;\">No interface exists</span></i></td><td style=\"background-color:#D9D9D9; border-color:Black; border-style:solid solid double; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:80pt;\"><i><span style=\"font-family:Arial; font-size:11pt;\">Interface exists and status affects the emergency brake model only</span></i></td><td style=\"background-color:#D9D9D9; border-color:Black; border-style:solid solid double; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:81pt;\"><i><span style=\"font-family:Arial; font-size:11pt;\">Interface exists and status affects the service brake model only</span></i></td><td style=\"background-color:#D9D9D9; border-color:Black; border-style:solid solid double; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:84pt;\"><i><span style=\"font-family:Arial; font-size:11pt;\">Interface exists and status affects both emergency and service brake models</span></i></td></tr><tr><td rowspan=\"4\" style=\"border-color:Black; border-style:double double solid solid; border-width:1px; padding:0px 8px 4px; transform:rotate(-90deg); vertical-align:top; width:35pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][3].[c][1]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">Special brake</span></p></td><td style=\"border-color:Black; border-style:double double solid solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:97pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][3].[c][2]</div><div style=\"clear:right;\"></div></div><span style=\"font-family:Arial; font-size:11pt;\">regenerative brake</span></td><td style=\"border-color:Black; border-style:double solid solid double; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:79pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][3].[c][3]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">x</span></p></td><td style=\"border-color:Black; border-style:double solid solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:80pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][3].[c][4]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">x</span></p></td><td style=\"border-color:Black; border-style:double solid solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:81pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][3].[c][5]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">x</span></p></td><td style=\"border-color:Black; border-style:double solid solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:84pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][3].[c][6]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">x</span></p></td></tr><tr><td style=\"border-color:Black; border-style:solid double solid solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:97pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][4].[c][2]</div><div style=\"clear:right;\"></div></div><span style=\"font-family:Arial; font-size:11pt;\">eddy current brake</span></td><td style=\"border-color:Black; border-style:solid solid solid double; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:79pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][4].[c][3]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">x</span></p></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:80pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][4].[c][4]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">x</span></p></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:81pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][4].[c][5]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">x</span></p></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:84pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][4].[c][6]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">x</span></p></td></tr><tr><td style=\"border-color:Black; border-style:solid double solid solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:97pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][5].[c][2]</div><div style=\"clear:right;\"></div></div><span style=\"font-family:Arial; font-size:11pt;\">magnetic shoe brake</span></td><td style=\"border-color:Black; border-style:solid solid solid double; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:79pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][5].[c][3]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">x</span></p></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:80pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][5].[c][4]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">x</span></p></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:81pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][5].[c][5]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"></p></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:84pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][5].[c][6]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"></p></td></tr><tr><td style=\"border-color:Black; border-style:solid double solid solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:97pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][6].[c][2]</div><div style=\"clear:right;\"></div></div><span style=\"font-family:Arial; font-size:11pt;\">Ep brake</span></td><td style=\"border-color:Black; border-style:solid solid solid double; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:79pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][6].[c][3]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">x</span></p></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:80pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][6].[c][4]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"></p></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:81pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][6].[c][5]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">x</span></p></td><td style=\"border-color:Black; border-style:solid; border-width:1px; padding:0px 0px 4px; vertical-align:top; width:84pt;\"><div class=\"hrMetadata\"><div style=\"background-color:rgb(173,216,230); display:inline-block; float:right; font-family:courier; font-size:smaller; font-weight:lighter;\">[r][6].[c][6]</div><div style=\"clear:right;\"></div></div><p style=\"display:block; text-align:center;\"><span style=\"font-family:Arial; font-size:11pt;\">x</span></p></td></tr></table>";
	assertEquals(expectedResult, getRichText());
    }

    /**
     * Test if requirement structure of the table is correct
     */
    @Test
    public void testStructure() {					
	final String expectedResult[] = {
		"",
		" [t]3",
		"  [t]3.C",
		"  [t]3.[r][3]",
		"   [t]3.[r][3].[c][1]",
		"   [t]3.[r][3].[c][2]",
		"   [t]3.[r][3].[c][3]",
		"   [t]3.[r][3].[c][4]",
		"   [t]3.[r][3].[c][5]",
		"   [t]3.[r][3].[c][6]",
		"  [t]3.[r][4]",
		"   [t]3.[r][4].[c][2]",
		"   [t]3.[r][4].[c][3]",
		"   [t]3.[r][4].[c][4]",
		"   [t]3.[r][4].[c][5]",
		"   [t]3.[r][4].[c][6]",
		"  [t]3.[r][5]",
		"   [t]3.[r][5].[c][2]",
		"   [t]3.[r][5].[c][3]",
		"   [t]3.[r][5].[c][4]",
		"   [t]3.[r][5].[c][5]",
		"   [t]3.[r][5].[c][6]",
		"  [t]3.[r][6]",
		"   [t]3.[r][6].[c][2]",
		"   [t]3.[r][6].[c][3]",
		"   [t]3.[r][6].[c][4]",
		"   [t]3.[r][6].[c][5]",
		"   [t]3.[r][6].[c][6]"
	};

	checkStructure(expectedResult);
    }
}