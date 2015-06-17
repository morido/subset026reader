package helper.subset26.tables;

import helper.TraceabilityManagerHumanReadable.RowLevelPrefix;
import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class ConditionTable extends GenericTable {
    
    @Override
    public String getName() {
	return "ConditionTable";
    }

    @Override
    @DomainSpecific
    protected void setTableData() {		
	// Pattern of the table to match:
	//
	//        --------------------------------------------------
	// Row 0: | Condition Id |    Content of the conditions    |
	//        --------------------------------------------------
	// Row 1: |      [1]     | (some text)                     |
	//        --------------------------------------------------
	// Row n: |      [n]     | (some other text)               |
	//        --------------------------------------------------
	this.columns.setExpected(2);

	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "Condition Id"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, "Content of the conditions"));

	// matches stuff like "[17]"
	final String regexConditionID = "^\\[([0-9]+)\\]";

	// match for a number in square brackets
	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, regexConditionID));
	// match for a non-empty cell
	addData(1, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "\\S.*"));
	setRepeatingRowMatchingData(1);

	// add conditionID for tracing
	addData(1, 1, TracingData.newTracingDataRowIdFromCell(-1, regexConditionID, RowLevelPrefix.CONDITION, "Content", false));
	setRepeatingRowTracingData(1);
    }
}