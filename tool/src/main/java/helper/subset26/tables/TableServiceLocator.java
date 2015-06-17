package helper.subset26.tables;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.poi.hwpf.usermodel.Table;

/**
 * Collection of all known abstract tables
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
final class TableServiceLocator {
    private final transient Set<GenericTable> handlers = new LinkedHashSet<>();    
    private final transient Table concreteTable;

    /**
     * @param concreteTable concrete table to match against
     */
    public TableServiceLocator(final Table concreteTable) {
	assert concreteTable != null;	
	this.concreteTable = concreteTable;

	// Note: constructors are intentionally empty to allow short (user-modifiable) classes
	// actual data will be added via setContext() below
	// hence we cannot easily make this array static
	final GenericTable[] tablePatterns = {
		new AbbreviationsTable(),
		new AcknowledgementTable(),
		new ConditionTable(),
		new ConditionTableWArrows(),
		new DataListTable(),
		new FieldTable(),
		new FieldTableWSpecialValues(),
		new FieldListTable(),
		new FunctionsTable(),
		new GeneralFormatTable(),
		new LevelInformationTable(),
		new LevelInformationTwoNTCTable(),
		new MessageActionTable(),
		new MessageListTable(),
		new ModesDataTable(),
		new ModesTable(),
		new ModesTable2(),
		new TermTable(),
		new TrainCommandTable(),
		new TransitionTable(),
		new TransitionTableWArrows(),
		new TransitionTableWArrowsNarrow(),
		new PacketReferenceTable(), 
		new PacketFieldsTable(),
		new Procedures2ColumnTable(),
		new Procedures3ColumnTable(),
		new Procedures3Column1BlankTable(),
		new VersionHistoryTableNarrow(),
		new VersionHistoryTableWide()		
	};
	addTableDescriptions(tablePatterns);
    }

    /**
     * @return a Collection of all known handlers
     */
    public Collection<GenericTable> getHandlers() {
	return this.handlers;
    }
    

    private void addTableDescriptions(final GenericTable[] tables) {
	for (final GenericTable table : tables) {
	    table.setContext(this.concreteTable);
	    this.handlers.add(table);
	}		
    }
}
