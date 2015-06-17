package helper.subset26.tables;

import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
class PacketFieldsTable extends GenericTable {

    @Override
    public String getName() {
	return "PacketFieldsTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        -------------------------------------------------------------
	// Row 0: | Description     | TEXT                                    |
	// Row 1: | Transmitted by  | TEXT                                    |
	// Row 2: | Content         | Variable  | Length   |     Comment      |
	// Row 3: |                 | TEXT      | NUMBER   | Comment          |	
	// *Row 3 is repeated until the end*
	this.columns.setExpected(4);
	
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Description"));	
	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Transmitted.*"));
	addData(2, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Content"));	
	addData(2, 1, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.INCONSISTENT, "Variable"));
	addData(2, 2, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.INCONSISTENT, "Length"));
	addData(2, 3, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.INCONSISTENT, "Comment"));
	addData(3, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "(?U)\\s*[A-Z].*"));
	addData(3, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.INCONSISTENT, ".*"));
	setRepeatingRowMatchingData(3);

	addData(0, 3, TracingData.newTracingDataFixedColumnId("Description", false));
	addData(1, 3, TracingData.newTracingDataFixedColumnId("Medium", false));	
	addData(3, 1, TracingData.newTracingDataFixedColumnId("Variable", false));
	addData(3, 2, TracingData.newTracingDataFixedColumnIdConditional("Length", ".*[0-9]+\\s*")); // this may contain field data; hence we can only assume it ends with a number
	addData(3, 3, TracingData.newTracingDataFixedColumnIdConditional("Comment", "[A-Za-z0-9].*"));	
	setRepeatingRowTracingData(3);
    }
}