package helper.subset26.tables;

import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
class MessageActionTable extends GenericTable {
    
    @Override
    public String getName() {
	return "MessageActionTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        --------------------------------------------------------------
	// Row 0: | Message Number     | Message Name   | Action               |
	// Row 1: | PLACEHOLDER ROW
	// Row 2: | NUMBER             | TEXT           | TEXT                 |	
	// *Row 2 is repeated until the end*

	this.columns.setExpected(3);
	
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.CENTER, "Message\\s?Number"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.CENTER, "Message Name"));
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.CENTER, "Action"));	
	addData(2, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "[0-9]+"));
	addData(2, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, ".+"));
	addData(2, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, ".+"));
	setRepeatingRowMatchingData(2);
	
	addData(2, 0, TracingData.newTracingDataFixedColumnId("Number", false));
	addData(2, 1, TracingData.newTracingDataFixedColumnId("Name", false));
	addData(2, 2, TracingData.newTracingDataFixedColumnId("Action", false));
	setRepeatingRowTracingData(2);
    }

}
