package helper.subset26.tables;

import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class ModesDataTable extends GenericTable {
    
    @Override
    public String getName() {
	return "ModesDataTable";
    }

    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        -------------------------------------------------------------------------------------------------------------
	// Row 0: | Data stored on board |                                Entered Mode                                        |
	// Row 1: |                      | NP | SB | PS | SH | FS | LS | SR | OS | SL | NL | UN | TR | PT | SF | IS | SN | RV |
	//        -------------------------------------------------------------------------------------------------------------
	// * Row 2 is placeholder *
	// Row 3: | TEXT | TEXT x17
	// *Row 3 is repeated until the end.*

	this.columns.setExpected(18);

	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "Data Stored on[- ]board"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "Entered Mode"));
	
	addData(1, 1, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "NP"));
	addData(1, 2, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "SB"));
	addData(1, 3, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "PS"));
	addData(1, 4, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "SH"));
	addData(1, 5, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "FS"));
	addData(1, 6, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "LS"));
	addData(1, 7, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "SR"));
	addData(1, 8, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "OS"));
	addData(1, 9, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "SL"));
	addData(1, 10, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "NL"));
	addData(1, 11, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "UN"));
	addData(1, 12, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "TR"));
	addData(1, 13, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "PT"));
	addData(1, 14, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "SF"));
	addData(1, 15, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "IS"));
	addData(1, 16, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "SN"));
	addData(1, 17, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "RV"));

	addData(3, 0, TracingData.newTracingDataFixedColumnId("Data", false));
	addData(3, 1, TracingData.newTracingDataFixedColumnId("NP", false));
	addData(3, 2, TracingData.newTracingDataFixedColumnId("SB", false));
	addData(3, 3, TracingData.newTracingDataFixedColumnId("PS", false));
	addData(3, 4, TracingData.newTracingDataFixedColumnId("SH", false));
	addData(3, 5, TracingData.newTracingDataFixedColumnId("FS", false));
	addData(3, 6, TracingData.newTracingDataFixedColumnId("LS", false));
	addData(3, 7, TracingData.newTracingDataFixedColumnId("SR", false));
	addData(3, 8, TracingData.newTracingDataFixedColumnId("OS", false));
	addData(3, 9, TracingData.newTracingDataFixedColumnId("SL", false));
	addData(3, 10, TracingData.newTracingDataFixedColumnId("NL", false));
	addData(3, 11, TracingData.newTracingDataFixedColumnId("UN", false));
	addData(3, 12, TracingData.newTracingDataFixedColumnId("TR", false));
	addData(3, 13, TracingData.newTracingDataFixedColumnId("PT", false));
	addData(3, 14, TracingData.newTracingDataFixedColumnId("SF", false));
	addData(3, 15, TracingData.newTracingDataFixedColumnId("IS", false));
	addData(3, 16, TracingData.newTracingDataFixedColumnId("SN", false));
	addData(3, 17, TracingData.newTracingDataFixedColumnId("RV", false));
	setRepeatingRowTracingData(3);

    }

}
