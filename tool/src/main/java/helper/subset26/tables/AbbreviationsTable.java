package helper.subset26.tables;

import helper.annotations.DomainSpecific;


/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class AbbreviationsTable extends GenericTable {

    @Override
    public String getName() {
	return "AbbreviationsTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        ---------------------------------------------------------
	// Row 0: | TEXT = TEXT | TEXT = TEXT | TEXT = TEXT | TEXT = TEXT | (may be more or less than four columns)
	//        ---------------------------------------------------------	

	this.rows.setExpected(1);

	final String regexContents = "[A-Z]+\\s?=\\s?.+";
	final String regexAbbreviation = "^([A-Z]+)\\s?=";
	for (int cn = 0; cn < this.columns.getActual(); cn++) {
	    addData(0, cn, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, regexContents));
	    addData(0, cn, TracingData.newTracingDataColumnIdFromColumn(regexAbbreviation));
	}
    }
}