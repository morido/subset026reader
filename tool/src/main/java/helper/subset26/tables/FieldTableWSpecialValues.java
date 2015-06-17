package helper.subset26.tables;

import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class FieldTableWSpecialValues extends GenericTable {
    
    @Override
    public String getName() {
	return "FieldTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        ------------------------------------------------------------------------------
	// Row 0: | Name               | TEXT                                                  |
	// Row 1: | Description        | TEXT                                                  |
	// Row 2: | Length of variable | Minimum Value  | Maximum Value  | Resolution/formula  |
	// Row 3: | TEXT               | TEXT           | TEXT           | TEXT                |
	// Row 4: | Special/Reserved V | TEXT           | TEXT                                 |	
	// *Row 4 is repeated until the end*

	this.columns.setExpected(4);	
	
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.LEFTORJUSTIFY, "Name"));
	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.LEFTORJUSTIFY, "Description"));
	addData(2, 0, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.LEFTORJUSTIFY, "Length of variable"));
	addData(2, 1, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.LEFTORJUSTIFY, "Minimum Value"));
	addData(2, 2, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.LEFTORJUSTIFY, "Maximum Value"));
	addData(2, 3, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.LEFTORJUSTIFY, "Resolution\\s?/\\s?formula"));
	addData(4, 0, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.LEFTORJUSTIFY, "Special\\s?/\\s?Reserved Values"));	
	
	addData(0, 3, TracingData.newTracingDataFixedColumnId("Name", false));
	addData(1, 3, TracingData.newTracingDataFixedColumnId("Description", false));
	addData(3, 0, TracingData.newTracingDataFixedColumnId("Length", false));
	addData(3, 1, TracingData.newTracingDataFixedColumnId("Min", false));
	addData(3, 2, TracingData.newTracingDataFixedColumnId("Max", false));
	addData(3, 3, TracingData.newTracingDataFixedColumnId("Data", false));
	addData(4, 1, TracingData.newTracingDataFixedColumnId("Value", false));
	addData(4, 3, TracingData.newTracingDataFixedColumnId("Meaning", false));
	setRepeatingRowTracingData(4);
    }

}
