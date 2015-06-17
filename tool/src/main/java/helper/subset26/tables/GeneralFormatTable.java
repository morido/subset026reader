package helper.subset26.tables;

import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class GeneralFormatTable extends GenericTable {

    @Override
    public String getName() {
	return "GeneralFormatTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        --------------------------------------------------------------
	// Row 0: | General Format of .*                                       |
	//	--------------------------------------------------------------
	// Row 1: | Field No. | VARIABLE | Length ( bits) | Invariant | Remarks|	
	// Row 2: | NUMBER?   | TEXT     | TEXT           | Remarks            |
	// *Row 2 is repeated until the end.*
	
	this.columns.setExpected(4);

	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "General Format of.*"));
	
	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Field No."));
	addData(1, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "VARIABLE"));
	addData(1, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Length\\s?\\(\\s?bits\\)"));
	addData(1, 3, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "Remarks"));
	
	addData(2, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "[0-9 ]*"));
	addData(2, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, ".*"));
	addData(2, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, ".*"));
	addData(2, 3, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, ".*"));
	setRepeatingRowMatchingData(2);
	
	addData(2, 0, TracingData.newTracingDataFixedColumnId("Field", false));
	addData(2, 1, TracingData.newTracingDataFixedColumnId("Variable", false));
	addData(2, 2, TracingData.newTracingDataFixedColumnId("Length", false));
	addData(2, 4, TracingData.newTracingDataFixedColumnId("Remarks", false)); // there is some strange colspan here	
	setRepeatingRowTracingData(2);
    }

}
