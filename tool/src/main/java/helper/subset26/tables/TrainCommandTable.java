package helper.subset26.tables;

import helper.TraceabilityManagerHumanReadable.RowLevelPrefix;
import helper.annotations.DomainSpecific;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class TrainCommandTable extends GenericTable {
    
    @Override
    public String getName() {
	return "TrainCommandTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        ---------------------------------------------------------------------------------------------------------
	// Row 0: | *. condition | Estimated speed | Train front end position .* | TI Command .* | Supervision command .* |
	//        ---------------------------------------------------------------------------------------------------------
	// Row 1: | [tr][0-9]+   | Formula         | Formula                     | TEXT          | TEXT                   |
	// * Row 1 repeats until the end *
	// there may be vertically merged cells	
	this.columns.setExpected(5);
	
	// matches stuff like "r1" and "t15"
	final String regexConditionID = "^[rt]([0-9]+)";
	
	addData(0, 0, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "(Triggering|Revocation)\\s?condition\\s?#"));
	addData(0, 1, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "Estimated speed"));
	addData(0, 2, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "(Location|Train front end position \\(estimated and max safe\\))"));
	addData(0, 3, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "TI Command (triggered|revoked)\\s?"));
	addData(0, 4, MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, "Supervision status (triggered|revoked)\\s?"));
	addData(1, 0,  MatchingData.newMatchingData(ContentFormatting.NORMAL, ContentAlignment.LEFTORJUSTIFY, regexConditionID));
	setRepeatingRowMatchingData(1);
	
	// add conditionID for tracing
	addData(1, 1, TracingData.newTracingDataRowIdFromCell(-1, regexConditionID, RowLevelPrefix.CONDITION, "Speed", false));
	addData(1, 2, TracingData.newTracingDataRowIdFromCell(-2, regexConditionID, RowLevelPrefix.CONDITION, "Position", false));
	addData(1, 3, TracingData.newTracingDataRowIdFromCell(-3, regexConditionID, RowLevelPrefix.CONDITION, "TICommand", false));
	addData(1, 4, TracingData.newTracingDataRowIdFromCell(-4, regexConditionID, RowLevelPrefix.CONDITION, "Supervision", false));
	setRepeatingRowTracingData(1);
    }

}
