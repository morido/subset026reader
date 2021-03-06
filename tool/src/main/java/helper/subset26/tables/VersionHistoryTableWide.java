package helper.subset26.tables;

import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class VersionHistoryTableWide extends GenericTable {
    
    @Override
    public String getName() {
	return "VersionHistoryTable";
    }

    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        -----------------------------------------------------------------------------------
	// Row 0: | Issue Number Date | Section Number | Modification / Description | Author/Editor |
	//        -----------------------------------------------------------------------------------
	// Row 1: *we dont match any of the subsequent rows; the header itself should be quite unique already*
	this.columns.setExpected(4);

	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Issue Number\\s?Date"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Section [Nn]umber"));

	// sometimes we have spaces in the header text, sometimes we dont
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Modification\\s*/\\s*Description"));
	addData(0, 3, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "Author.*"));


	// no traceworthy cells here at all
    }

}
