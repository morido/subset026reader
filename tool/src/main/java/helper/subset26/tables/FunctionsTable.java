package helper.subset26.tables;

import org.apache.poi.hwpf.usermodel.TableCell;

import docreader.range.paragraph.characterRun.FakeFieldHandler;
import helper.annotations.DomainSpecific;
import helper.word.DataConverter;


/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
class FunctionsTable extends GenericTable {
    
    @Override
    public String getName() {
	return "FunctionsTable";
    }

    @Override
    @DomainSpecific
    protected void setTableData() {	
	// Pattern of the table to match:
	//
	//        --------------------------------------------------------------------------------------------------------------------------
	// Row 0: | ONBOARD-FUNCTIONS | RELATED SRS § | NP | SB | PS | SH | FS | LS | SR | OS | SL | NL | UN | TR | PT | SF | IS | SN | RV |
	//        --------------------------------------------------------------------------------------------------------------------------
	// Row 1: | TEXT?
	// *Row 1 is repeated until the end.*

	final int expectedColumns = 19;
	this.columns.setExpected(expectedColumns);

	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "ONBOARD[- ]FUNCTIONS"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "RELATED SRS.*"));
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "NP"));
	addData(0, 3, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "SB"));
	addData(0, 4, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "PS"));
	addData(0, 5, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "SH"));
	addData(0, 6, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "FS"));
	addData(0, 7, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "LS"));
	addData(0, 8, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "SR"));
	addData(0, 9, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "OS"));
	addData(0, 10, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "SL"));
	addData(0, 11, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "NL"));
	addData(0, 12, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "UN"));
	addData(0, 13, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "TR"));
	addData(0, 14, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "PT"));
	addData(0, 15, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "SF"));
	addData(0, 16, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "IS"));
	addData(0, 17, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "SN"));
	addData(0, 18, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "RV"));
	
	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.LEFTORJUSTIFY, "\\S.*"));
	setRepeatingRowMatchingData(1);
		
	// tracing depends on two conditions here; we do this "manually"
	for (int i = 1; i<this.concreteTable.numRows(); i++) {
	    final int upperCellBound = this.concreteTable.getRow(i).numCells();
	    if (upperCellBound != expectedColumns) break; // shortcut; we wont match anyways 
	    addData(i, 0, TracingData.newTracingDataFixedColumnId("Function", false));
	    for (int j = 1; j<upperCellBound; j++) {
		if (DataConverter.cleanupText(this.concreteTable.getRow(i).getCell(j).text()).matches("\\S.*")) {
		    // at least one cell contains data
		    addData(i, 1, TracingData.newTracingDataFixedColumnIdConditional("SRSRef", "\\S.*", 0));
		    addData(i, 1, referenceCellContentCheck(this.concreteTable.getRow(i).getCell(1)));
		    addData(i, 2, TracingData.newTracingDataFixedColumnId("NP", false));
		    addData(i, 3, TracingData.newTracingDataFixedColumnId("SB", false));
		    addData(i, 4, TracingData.newTracingDataFixedColumnId("PS", false));
		    addData(i, 5, TracingData.newTracingDataFixedColumnId("SH", false));
		    addData(i, 6, TracingData.newTracingDataFixedColumnId("FS", false));
		    addData(i, 7, TracingData.newTracingDataFixedColumnId("LS", false));
		    addData(i, 8, TracingData.newTracingDataFixedColumnId("SR", false));
		    addData(i, 9, TracingData.newTracingDataFixedColumnId("OS", false));
		    addData(i, 10, TracingData.newTracingDataFixedColumnId("SL", false));
		    addData(i, 11, TracingData.newTracingDataFixedColumnId("NL", false));
		    addData(i, 12, TracingData.newTracingDataFixedColumnId("UN", false));
		    addData(i, 13, TracingData.newTracingDataFixedColumnId("TR", false));
		    addData(i, 14, TracingData.newTracingDataFixedColumnId("PT", false));
		    addData(i, 15, TracingData.newTracingDataFixedColumnId("SF", false));
		    addData(i, 16, TracingData.newTracingDataFixedColumnId("IS", false));
		    addData(i, 17, TracingData.newTracingDataFixedColumnId("SN", false));
		    addData(i, 18, TracingData.newTracingDataFixedColumnId("RV", false));
		    break;
		}		
	    }	    
	}
    }

    
    /**
     * (Primitive and highly-domain specific) check to ensure links split across different paragraphs are kept together
     * 
     * @param cell cell of the "SRSRef"-Column
     * @return a SplitupData qualifier specifying if this cell may be split into several requirements or not
     */
    @DomainSpecific
    private static SplitupData referenceCellContentCheck(final TableCell cell) {
	SplitupData output = SplitupData.SplitupAllowed();
	if (cell.numParagraphs() == 2) {
	    final String paragraph1 = cell.getParagraph(0).text().trim();
	    final String paragraph2 = cell.getParagraph(1).text().trim();
	    if (paragraph1.matches(FakeFieldHandler.getRequirementRegex())
		    && paragraph2.matches("^[^0-9].*")) {
		// paragraph1 contains a link; paragraph 2 contains no link (or child links)
		output = SplitupData.SplitupForbidden();
	    }
	}
	return output;
    }
}
