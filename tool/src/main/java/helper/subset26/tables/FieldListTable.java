package helper.subset26.tables;

import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
class FieldListTable extends GenericTable {

    @Override
    public String getName() {
	return "FieldListTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        --------------------------------------------------------------
	// Row 0: | Field No.          | VARIABLE       | Remarks              |
	// Row 1: | NUMBER             | TEXT           |                      |
	// *Row 1 is repeated until the end*
	this.columns.setExpected(3);
	
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Field\\sNo."));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "(VARIABLE|VARIABLE/\\s?PACKET)"));
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "Remarks"));
	// we have numbering inside a cell - the cell's text is empty or contains \ldots
	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, "…?"));
	addData(1, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "([^0-9].*|)"));
	setRepeatingRowMatchingData(1);
	
	addData(1, 0, TracingData.newTracingDataFixedColumnId("Field", true));
	addData(1, 1, TracingData.newTracingDataFixedColumnId("Variable", false));
	addData(1, 2, TracingData.newTracingDataFixedColumnIdConditional("Remarks", "[^\\s].*"));
	setRepeatingRowTracingData(1);
    }
}
