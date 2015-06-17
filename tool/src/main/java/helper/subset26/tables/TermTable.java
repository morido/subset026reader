package helper.subset26.tables;

import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class TermTable extends GenericTable {

    @Override
    public String getName() {
	return "TermTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        --------------------------------------------------
	// Row n: | TEXT | TEXT                                    |
	//	  --------------------------------------------------
	this.columns.setExpected(2);
	
	final String termRegex = "[A-Za-z].*[0-9]?";
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "^(" + termRegex + "|)$"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, ".*"));
	setRepeatingRowMatchingData(0);
	
	addData(0, 0, TracingData.newTracingDataFixedColumnIdConditional("Term", termRegex));
	addData(0, 1, TracingData.newTracingDataFixedColumnIdConditional("Definition", "[A-Za-z].*"));
	setRepeatingRowTracingData(0);
    }

}
