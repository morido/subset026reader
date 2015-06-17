package helper.subset26.tables;

import helper.TraceabilityManagerHumanReadable.RowLevelPrefix;
import helper.annotations.DomainSpecific;

class Procedures3Column1BlankTable extends GenericTable {

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
	// Row 0: | Id #          | Requirements                         | 
	//        --------------------------------------------------------------
	// Row 1: | <LETTER><NUM> | TEXT with Lists                      | 
	// *Row 1 repeats until the end*

	this.columns.setExpected(3);
	final String idRegex = "[A-Z][0-9]+";
	
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "ID #"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.LEFTORJUSTIFY, "Requirements"));
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.INCONSISTENT, "\\s*"));
	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, idRegex));
	addData(1, 1, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.LEFTORJUSTIFY, "\\S.+"));
	addData(1, 2, MatchingData.newMatchingData(ContentFormatting.INCONSISTENT, ContentAlignment.INCONSISTENT, "\\s*"));
	setRepeatingRowMatchingData(1);
		
	addData(1, 1, TracingData.newTracingDataRowIdFromCell(-1, '(' + idRegex + ')', RowLevelPrefix.ID, "Content", false));	
	setRepeatingRowTracingData(1);
    }
}
