package helper;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;

/**
 * Various helper functions to work with MS Word tables
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public enum TableHelper {
    ;
    private static final Logger logger = Logger.getLogger(TableHelper.class.getName()); // NOPMD - Reference rather than a static field

    /**
     * @param cell cell of a table under consideration
     * @return {@code true} if this cell is merged in some way and hence does not contain data; {@code false} otherwise
     * @throws IllegalArgumentException if the given cell is {@code null}
     */
    public static boolean isMerged(final TableCell cell) {
	if (cell == null) throw new IllegalArgumentException("Cell cannot be null.");
	return (cell.isMerged() && !cell.isFirstMerged()) || (cell.isVerticallyMerged() && !cell.isFirstVerticallyMerged());
    }

    /**
     * @param table table under consideration
     * @return the number of cells in the widest column
     * @throws IllegalArgumentException if the given table is {@code null}
     */
    public static int getMaxColumns(final Table table) {
	if (table == null) throw new IllegalArgumentException("Table cannot be null.");
	int output = 0;
	for (int rn=0; rn<table.numRows(); rn++) {
	    if (table.getRow(rn).numCells() > output) {
		output = table.getRow(rn).numCells();
	    }
	}
	if (output > 63) logger.log(Level.INFO, "Encountered a table with more than 63 columns. Support for these is currently limited.");
	return output;
    }
}
