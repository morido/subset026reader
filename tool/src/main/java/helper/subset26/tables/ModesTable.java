package helper.subset26.tables;

import helper.annotations.DomainSpecific;


/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class ModesTable extends GenericTable {
    
    @Override
    public String getName() {
	return "ModesTable";
    }

    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        -----------------------------------------------------------------------------------------------------
	// Row 0: | *information | NP | SB | PS | SH | FS | LS | SR | OS | SL | NL | UN | TR | PT | SF | IS | SN | RV |
	//        -----------------------------------------------------------------------------------------------------
	// Row 1: | TEXT | TEXT? x17
	// *Row 1 is repeated until the end.*	
	this.columns.setExpected(18);
	
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, ".*information"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "NP"));
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "SB"));
	addData(0, 3, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "PS"));
	addData(0, 4, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "SH"));
	addData(0, 5, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "FS"));
	addData(0, 6, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "LS"));
	addData(0, 7, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "SR"));
	addData(0, 8, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "OS"));
	addData(0, 9, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "SL"));
	addData(0, 10, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "NL"));
	addData(0, 11, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "UN"));
	addData(0, 12, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "TR"));
	addData(0, 13, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "PT"));
	addData(0, 14, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "SF"));
	addData(0, 15, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "IS"));
	addData(0, 16, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "SN"));
	addData(0, 17, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "RV"));
	
	addData(1, 0, TracingData.newTracingDataFixedColumnId("Info", false));
	addData(1, 1, TracingData.newTracingDataFixedColumnIdConditional("NP", "\\S.*"));
	addData(1, 2, TracingData.newTracingDataFixedColumnIdConditional("SB", "\\S.*"));
	addData(1, 3, TracingData.newTracingDataFixedColumnIdConditional("PS", "\\S.*"));
	addData(1, 4, TracingData.newTracingDataFixedColumnIdConditional("SH", "\\S.*"));
	addData(1, 5, TracingData.newTracingDataFixedColumnIdConditional("FS", "\\S.*"));
	addData(1, 6, TracingData.newTracingDataFixedColumnIdConditional("LS", "\\S.*"));
	addData(1, 7, TracingData.newTracingDataFixedColumnIdConditional("SR", "\\S.*"));
	addData(1, 8, TracingData.newTracingDataFixedColumnIdConditional("OS", "\\S.*"));
	addData(1, 9, TracingData.newTracingDataFixedColumnIdConditional("SL", "\\S.*"));
	addData(1, 10, TracingData.newTracingDataFixedColumnIdConditional("NL", "\\S.*"));
	addData(1, 11, TracingData.newTracingDataFixedColumnIdConditional("UN", "\\S.*"));
	addData(1, 12, TracingData.newTracingDataFixedColumnIdConditional("TR", "\\S.*"));
	addData(1, 13, TracingData.newTracingDataFixedColumnIdConditional("PT", "\\S.*"));
	addData(1, 14, TracingData.newTracingDataFixedColumnIdConditional("SF", "\\S.*"));
	addData(1, 15, TracingData.newTracingDataFixedColumnIdConditional("IS", "\\S.*"));
	addData(1, 16, TracingData.newTracingDataFixedColumnIdConditional("SN", "\\S.*"));
	addData(1, 17, TracingData.newTracingDataFixedColumnIdConditional("RV", "\\S.*"));
	setRepeatingRowTracingData(1);
	
    }
}

