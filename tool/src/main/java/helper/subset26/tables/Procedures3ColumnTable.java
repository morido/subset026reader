package helper.subset26.tables;

import helper.TraceabilityManagerHumanReadable.RowLevelPrefix;
import helper.annotations.DomainSpecific;

class Procedures3ColumnTable extends GenericTable {

    @Override
    public String getName() {
	return "ProceduresTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        --------------------------------------------------------------
	// Row 0: | Id #          | Requirements                         | Level
	//        --------------------------------------------------------------
	// Row 1: | <LETTER><NUM> | TEXT with Lists                      | TEXT?
	// *Row 1 repeats until the end*

	this.columns.setExpected(3);
	final String idRegex = "[A-Z][0-9]+";
	
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "ID #"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Requirements"));
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Level"));
	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, idRegex));
	addData(1, 1, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.LEFTORJUSTIFY, "\\S.+"));
	setRepeatingRowMatchingData(1);
		
	addData(1, 1, TracingData.newTracingDataRowIdFromCell(-1, '(' + idRegex + ')', RowLevelPrefix.ID, "Content", false));
	addData(1, 2, TracingData.newTracingDataRowIdFromCell(-2, '(' + idRegex + ')', RowLevelPrefix.ID, "Level", false));
	setRepeatingRowTracingData(1);
    }
}
