package helper.subset26.tables;

import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
class FieldTable extends GenericTable {
    
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

	// Note:
	// helper.subset26.tables.FieldTableWSpecialValues is equal to this except that it has more rows
	// so we set the number of rows explicitly here
	this.columns.setExpected(4);
	this.rows.setExpected(4); 
	
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Name"));
	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Description"));
	addData(2, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Length of variable"));
	addData(2, 1, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Minimum Value"));
	addData(2, 2, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Maximum Value"));
	addData(2, 3, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Resolution\\s?/\\s?formula"));	
	
	addData(0, 3, TracingData.newTracingDataFixedColumnId("Name", false));
	addData(1, 3, TracingData.newTracingDataFixedColumnId("Description", false));
	addData(3, 0, TracingData.newTracingDataFixedColumnId("Length", false));
	addData(3, 1, TracingData.newTracingDataFixedColumnId("Min", false));
	addData(3, 2, TracingData.newTracingDataFixedColumnId("Max", false));
	addData(3, 3, TracingData.newTracingDataFixedColumnId("Data", false));	
    }
}
