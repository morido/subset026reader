/**
 * 
 */
package helper.subset26.tables;

import helper.annotations.DomainSpecific;
import helper.word.DataConverter;

/**
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
class TransitionTable extends GenericTable {

    @Override
    public String getName() {
	return "TransitionTable";
    }
    
    @Override
    @DomainSpecific
    protected void setTableData() {
	// Pattern of the table to match:
	//
	//        -----------------------------------------------------------------
	// Row 0: | CENTERED TEXT |               |               |               | 
	//        -----------------------------------------------------------------
	// Row 1: |               | CENTERED TEXT |               |               | 
	//        -----------------------------------------------------------------
	// Row 2: |               |               | CENTERED TEXT |               | 
	//        -----------------------------------------------------------------
	// Row 2: |               |               |               | CENTERED TEXT |
	//        -----------------------------------------------------------------
	//
	// the empty cells *may* contain data; we dont check for the shading

	// the table must be rectangular (columns == rows)
	setRectangular(true);

	// this is a little redundant - the same functionality is also in #isTableMatch()
	if (this.rows.getActual() != this.columns.getActual()) {			
	    return;
	}

	// do a quick (and not very elegant) check if the formatting is correct
	// #isTableMatch() does the same thing later on, but this saves us from potentially expensive calculations below
	if (this.concreteTable.getRow(this.rows.getActual()-1).numCells() < this.columns.getActual()
		|| !this.concreteTable.getRow(this.rows.getActual()-1).getCell(this.columns.getActual()-1).getParagraph(0).getCharacterRun(0).isBold()
		|| this.concreteTable.getRow(this.rows.getActual()-1).getCell(this.columns.getActual()-1).getParagraph(0).getJustification() != 0x01)
	{
	    // force fail #isTableMatch()
	    this.forceFailingMatch = true;
	    return;
	}
	
	// Step 1: process the main diagonal
	final String[] sourceModes = new String[this.rows.getActual()];
	for (int iterator = 0; iterator < this.rows.getActual(); iterator++) {
	    // the main diagonal must have bold writing + centered alignment and may not contain tracing information
	    addData(iterator, iterator, MatchingData.newMatchingData(ContentFormatting.BOLD, ContentAlignment.CENTER, ".*"));	    
	    
	    // store away the texts of those cells
	    sourceModes[iterator] = cleanupTraceComponent(this.concreteTable.getRow(iterator).getCell(iterator).text());
	}
	
	// Step 2: process the other cells
	for (int rn = 0; rn < this.rows.getActual(); rn++) {
	    final String targetMode = cleanupTraceComponent(this.concreteTable.getRow(rn).getCell(rn).text());
	    
	    for (int cn = 0; cn < this.columns.getActual(); cn++) {		
		if (cn == rn) {
		    // we already processed those in Step 1
		    continue;
		}
		// other cells; all those that contain characters need to be centered
		addData(rn, cn, MatchingData.newMatchingDataConditional(ContentFormatting.INCONSISTENT, ContentAlignment.CENTER, "\\S.*", true));

		// add tracing information for all cells which contain "<" or ">"
		// the columId is column oriented: first part is the current element in the main diagonal derived from the column,
		// second part the same thing derived from the row 
		addData(rn, cn, TracingData.newTracingDataFixedColumnIdConditional(sourceModes[cn] + '_' + targetMode, ".*[<>].*"));
		addData(rn, cn, SplitupData.SplitupForbidden());
	    }
	}
    }
    
    /**
     * Remove certain characters from strings which are to be used as components of a resulting tracestring
     * 
     * @param input text from table cell
     * @return cleaned up version
     */
    private static String cleanupTraceComponent(final String input) {
	assert input != null;
	final String output = DataConverter.cleanupText(input);
	return output.replace(" ", "").replace("/", "");
    }
}