package helper.subset26.tables;

import helper.annotations.DomainSpecific;

/**
 * Matches a table such as that in 5.4.3.3
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class TransitionTableWArrows extends GenericTable {

    @Override
    public String getName() {
	return "TransitionTable";
    }
    
    /* (non-Javadoc)
     * @see helper.subset26.tables.GenericTable#setTableData()
     */
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match (rough):
	//
	//        -----------------------------------------------------------------
	// Row 0: |               |         State of On board Variables           | 
	//        -----------------------------------------------------------------
	// Row 1: |               |   TEXT        |               |               | 
	//        -----------------------------------------------------------------
	// Row 2: | Transition ...|   MORE COLUMNS THAN ROW 1                     | 
	//        -----------------------------------------------------------------
	// Row 3: | TEXT          |   AREA WITH ARROWS                            |
	//        -----------------------------------------------------------------
	// *Row 3 continues to the end*
	
	this.columns.setExpected(19);
	
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "State of On-board Variables"));
	
	addData(1, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "ERTMS[ ]?/[ ]?ETCS Level"));
	addData(1, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "RBC ID[ ]?/[ ]?Phone Number"));
	addData(1, 3, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Train position[ ]?data"));
	addData(1, 4, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Driver ID"));
	addData(1, 5, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Train Data"));
	addData(1, 6, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Train Running Number"));
	addData(2, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.INCONSISTENT, "Transition conditions"));
	addData(2, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Un-.?known"));
	addData(2, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Invalid"));
	addData(2, 3, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Valid"));
	addData(2, 4, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Un-.?known"));
	addData(2, 5, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Invalid"));
	addData(2, 6, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Valid"));
	addData(2, 7, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Un-.?known"));
	addData(2, 8, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Invalid"));
	addData(2, 9, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Valid"));
	addData(2, 10, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Un-.?known"));
	addData(2, 11, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Invalid"));
	addData(2, 12, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Valid"));
	addData(2, 13, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Un-.?known"));
	addData(2, 14, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Invalid"));
	addData(2, 15, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Valid"));
	addData(2, 16, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Un-.?known"));
	addData(2, 17, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Invalid"));
	addData(2, 18, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "Valid"));
	
	addData(3, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFT, ".+"));
	setRepeatingRowMatchingData(3);
	
	addData(3, 0, TracingData.newTracingDataFixedColumnId("Condition", false));
	addData(3, 1, TracingData.newTracingDataFixedColumnId("LevelUnknown", true));
	addData(3, 2, TracingData.newTracingDataFixedColumnId("LevelInvalid", true));
	addData(3, 3, TracingData.newTracingDataFixedColumnId("LevelValid", true));
	addData(3, 4, TracingData.newTracingDataFixedColumnId("RBCUnknown", true));
	addData(3, 5, TracingData.newTracingDataFixedColumnId("RBCInvalid", true));
	addData(3, 6, TracingData.newTracingDataFixedColumnId("RBCValid", true));
	addData(3, 7, TracingData.newTracingDataFixedColumnId("TPosUnknown", true));
	addData(3, 8, TracingData.newTracingDataFixedColumnId("TPosInvalid", true));
	addData(3, 9, TracingData.newTracingDataFixedColumnId("TPosValid", true));
	addData(3, 10, TracingData.newTracingDataFixedColumnId("DriverUnknown", true));
	addData(3, 11, TracingData.newTracingDataFixedColumnId("DriverInvalid", true));
	addData(3, 12, TracingData.newTracingDataFixedColumnId("DriverValid", true));
	addData(3, 13, TracingData.newTracingDataFixedColumnId("TDataUnknown", true));
	addData(3, 14, TracingData.newTracingDataFixedColumnId("TDataInvalid", true));
	addData(3, 15, TracingData.newTracingDataFixedColumnId("TDataValid", true));
	addData(3, 16, TracingData.newTracingDataFixedColumnId("TNumUnknown", true));
	addData(3, 17, TracingData.newTracingDataFixedColumnId("TNumInvalid", true));
	addData(3, 18, TracingData.newTracingDataFixedColumnId("TNumValid", true));
	setRepeatingRowTracingData(3);
    }
}
