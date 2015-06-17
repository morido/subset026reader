package helper.subset26.tables;

import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
class PacketReferenceTable extends GenericTable {
    
    @Override
    public String getName() {
	return "PacketReferenceTable";
    }

    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        -----------------------------------------
	// Row 0: | Packet Number | Packet Name | Page N° |
	//        -----------------------------------------	
	// Row 1: | <NUMBER>      | TEXT        |         |
	// *Row 1 repeats until the end*

	this.columns.setExpected(3);
	
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "Packet\\sNumber"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "Packet Name"));
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "Page N°"));
	
	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "[0-9]+"));
	setRepeatingRowMatchingData(1);
	
	// uncomment the following lines to trace this table
//	addData(1, 0, TracingData.newTracingDataFixedColumnId("Number", false));
//	addData(1, 1, TracingData.newTracingDataFixedColumnId("Name", false));
//	addData(1, 2, TracingData.newTracingDataFixedColumnId("Page", false));
//	setRepeatingRowTracingData(1);
    }
}
