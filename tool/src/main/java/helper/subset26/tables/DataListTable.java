/**
 * 
 */
package helper.subset26.tables;

import helper.annotations.DomainSpecific;


/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class DataListTable extends GenericTable {
    
    @Override
    public String getName() {
	return "DataListTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        -------------------------------------------------------------
	// Row 0: | BLABLA Data   | BLABLA Value    | BLABLA Name BLABLA      |
	//        -------------------------------------------------------------
	// Row 1: | TEXT *trace*  | TEXT *trace*    | NO_SPACES *trace*       |
	// *Row 1 is repeated until the end*		

	this.columns.setExpected(3);

	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "^.*Data$"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "^.*Value$"));
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "^.*Name.*$"));

	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, ".*"));
	addData(1, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, ".*"));
	addData(1, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, ".*"));
	setRepeatingRowMatchingData(1);

	addData(1, 0, TracingData.newTracingDataFixedColumnId("Data", false));
	addData(1, 1, TracingData.newTracingDataFixedColumnId("Value", false));
	addData(1, 2, TracingData.newTracingDataFixedColumnId("Name", false));
	setRepeatingRowTracingData(1);
	
	addData(1, 0, SplitupData.SplitupForbidden());
	addData(1, 1, SplitupData.SplitupForbidden());
	addData(1, 2, SplitupData.SplitupForbidden());
	setRepeatingRowSplitupData(1);
    }

}
