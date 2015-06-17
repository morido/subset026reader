import helper.CSSManagerTest;
import helper.nlp.NLPJobTest;
import helper.subset26.MetadataDeterminerSecondPassTest;
import helper.subset26.MetadataDeterminerTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import requirement.metadata.TextAnnotatorTest;
import docreader.range.paragraph.characterRun.FakeFieldHandlerTest;


/**
 * All tests which deal with internal processing
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
@SuppressWarnings("unused")
@RunWith(Suite.class)
@SuiteClasses({
    FakeFieldHandlerTest.class,
    MetadataDeterminerTest.class,
    MetadataDeterminerSecondPassTest.class,
    CSSManagerTest.class,
    TextAnnotatorTest.class,
    // NLPJobTest.class, // call this separately; for some reason it gets stuck when executing through this suite
})
public class AllTests {
    // intentionally empty
}
