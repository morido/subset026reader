package helper.subset26.tables;

import helper.TraceabilityManagerHumanReadable.RowLevelPrefix;
import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class ConditionTableWArrows extends GenericTable {
    
    @Override
    public String getName() {
	return "ConditionTable";
    }

    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        ----------------------------------------------------------
	// Row 0: | Condition Id | Transition condition  | CSM | TSM | RSM |
	//        ----------------------------------------------------------
	// Row 1: |      [1]     | (some text)           |     |     |     |
	//        ----------------------------------------------------------
	// Row n: |      [n]     | (some other text)     |     |     |     |
	//        ----------------------------------------------------------
	// Note: there may be arrows in columns CSM, TSM and RSM
	this.columns.setExpected(5);

	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFT, "Condition id"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFT, "Transition condition"));
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "CSM"));
	addData(0, 3, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "TSM"));
	addData(0, 4, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.CENTER, "RSM"));

	// matches stuff like "[17]"
	final String regexConditionID = "^\\[([0-9]+)\\]";

	// match for a number in square brackets
	addData(1, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFT, regexConditionID));
	// match for an arbitrary, multiline string in parentheses
	addData(1, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFT, "(?s)\\(.+\\)"));
	setRepeatingRowMatchingData(1);		

	// add conditionID for tracing
	addData(1, 1, TracingData.newTracingDataRowIdFromCell(-1, regexConditionID, RowLevelPrefix.CONDITION, "Content", false));
	addData(1, 2, TracingData.newTracingDataRowIdFromCell(-2, regexConditionID, RowLevelPrefix.CONDITION, "CSM", true));
	addData(1, 3, TracingData.newTracingDataRowIdFromCell(-3, regexConditionID, RowLevelPrefix.CONDITION, "TSM", true));
	addData(1, 4, TracingData.newTracingDataRowIdFromCell(-4, regexConditionID, RowLevelPrefix.CONDITION, "RSM", true));
	setRepeatingRowTracingData(1);
    }
}
