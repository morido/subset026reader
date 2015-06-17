package helper.subset26.tables;

import helper.TraceabilityManagerHumanReadable.RowLevelPrefix;
import helper.annotations.DomainSpecific;

class Procedures2ColumnTable extends GenericTable {

    @Override
    public String getName() {
	return "ProceduresTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        -------------------------------------------------------
	// Row 0: | Id #          | Requirements                          
	//        -------------------------------------------------------
	// Row 1: | <LETTER><NUM> | TEXT with Lists                       
	// *Row 1 repeats until the end*

	this.columns.setExpected(2);
	final String idRegex = "[A-Z][0-9]+";
	
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "ID #"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "Requirements"));	
	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, idRegex));
	addData(1, 1, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.LEFTORJUSTIFY, "\\S.+"));	
	setRepeatingRowMatchingData(1);
	
	addData(1, 1, TracingData.newTracingDataRowIdFromCell(-1, '(' + idRegex + ')', RowLevelPrefix.ID, "Content", false));	
	setRepeatingRowTracingData(1);
    }
}
