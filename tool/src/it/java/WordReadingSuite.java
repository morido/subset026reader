import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


/**
 * All tests which deal with reading in Word-files
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
@RunWith(Suite.class)
@SuiteClasses({
    ITCharacterReader.class,
    ITFigureReader.class,
    ITFootnoteReader.class,
    ITImageReader.class,
    ITListReaderPlain.class,
    ITListReader.class,
    ITListReaderStructures.class,
    ITDocumentBeginning.class,
    ITDocumentAppendix.class,
    ITTableReader.class,
    ITTableReaderKnownTables.class,
    ITTableReaderKnownTablesWArrows.class,
    ITNestedStructures.class,
    ITMinispec.class,
})
public class WordReadingSuite {
    // intentionally empty
}