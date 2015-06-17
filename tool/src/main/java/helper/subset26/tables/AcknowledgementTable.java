package helper.subset26.tables;

import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class AcknowledgementTable extends GenericTable {
    
    @Override
    public String getName() {
	return "AcknowledgementTable";
    }

    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        --------------------------------------------------------------
	// Row 0: |                |        |   Acknowledgement when entering  |
	//        --------------------------------------------------------------
	// Row 1: |                |        | L 0 | L 1 | ...                  |
	//        --------------------------------------------------------------
	// Row 2: | Coming from... |   L 0  | YES / NO
	//        --------------------------------------------------------------
	
	this.columns.setExpected(7);
	this.rows.setExpected(7);
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Acknowledgement when entering"));
	addData(2, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Coming from.*"));
	addData(1, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "L 0"));
	addData(1, 3, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "L 1"));
	addData(1, 4, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "L 2"));
	addData(1, 5, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "L 3"));
	addData(1, 6, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "L NTC"));
	addData(2, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "L 0"));
	addData(3, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "L 1"));
	addData(4, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "L 2"));
	addData(5, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "L 3"));
	addData(6, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "L NTC"));
	
	final String[] levels = { "L0", "L1", "L2", "L3", "LNTC"};
	for (int row = 2; row <= 6; row++) {
	    for (int column = 2; column <= 6; column++) {
		final String columnId = levels[row-2] + '_' + levels[column-2];
		addData(row, column, TracingData.newTracingDataFixedColumnIdConditional(columnId, "Yes"));
	    }
	}
    }

}
