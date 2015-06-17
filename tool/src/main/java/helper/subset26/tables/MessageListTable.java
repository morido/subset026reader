package helper.subset26.tables;

import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class MessageListTable extends GenericTable {

    @Override
    public String getName() {
	return "MessageListTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        ---------------------------------------------------------------
	// Row 0: | Mes.-Id. | Message Name | Type | Invariant | Transmitted to |
	//        ---------------------------------------------------------------
	// Row 1: | NUMBER   | TEXT         | [A-Z]| TEXT      | TEXT           |
	// *Row 1 is repeated until the end.*
	this.columns.setExpected(5);
	
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Mes.\\s?Id."));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Message Name"));
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Type"));
	addData(0, 3, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Invariant"));
	addData(0, 4, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Transmitted .*"));
	
	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "[0-9]+"));
	addData(1, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, ".+"));
	addData(1, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, ".+"));
	addData(1, 3, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, ".+"));
	addData(1, 4, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, ".+"));
	setRepeatingRowMatchingData(1);
	
	addData(1, 0, TracingData.newTracingDataFixedColumnId("ID", false));
	addData(1, 1, TracingData.newTracingDataFixedColumnId("Name", false));
	addData(1, 2, TracingData.newTracingDataFixedColumnId("Type", false));
	addData(1, 3, TracingData.newTracingDataFixedColumnId("Inv", false));
	addData(1, 4, TracingData.newTracingDataFixedColumnId("Medium", false));
	setRepeatingRowTracingData(1);
    }    
}
