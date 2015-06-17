package helper.subset26.tables;

import helper.annotations.DomainSpecific;


/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class LevelInformationTable extends GenericTable {

    @Override
    public String getName() {
	return "LevelInformationTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        --------------------------------------------------------------
	// Row 0: |             | From |         Onboard operating level       |
	// Row 1: | Information | RBC  |   0   |   NTC   |   1   |   2   |  3  |
	//	  --------------------------------------------------------------
	// Row 2: PLACEHOLDER
	// Row 3: |    TEXT?    | Yes/No | TEXT| TEXT    |  TEXT | TEXT  | TEXT|
	// *Row 3 is repeated until the end.*
	
	this.columns.setExpected(7);
	
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Information"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "From RBC"));
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Onboard operating level"));
	addData(1, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "0"));
	addData(1, 3, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "NTC"));
	addData(1, 4, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "1"));
	addData(1, 5, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "2"));
	addData(1, 6, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "3"));
	
	addData(3, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "(Yes|No)"));	
	setRepeatingRowMatchingData(3);
		
	addData(3, 0, TracingData.newTracingDataFixedColumnIdConditional("Info", "^\\S.+"));
	addData(3, 1, TracingData.newTracingDataFixedColumnId("RBC", false));
	addData(3, 2, TracingData.newTracingDataFixedColumnId("L0", false));
	addData(3, 3, TracingData.newTracingDataFixedColumnId("LNTC", false));	
	addData(3, 4, TracingData.newTracingDataFixedColumnId("L1", false));
	addData(3, 5, TracingData.newTracingDataFixedColumnId("L2", false));
	addData(3, 6, TracingData.newTracingDataFixedColumnId("L3", false));
	setRepeatingRowTracingData(3);
    }

}
