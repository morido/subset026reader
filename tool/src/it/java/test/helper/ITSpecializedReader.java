package test.helper;

/**
 * Abstract version of {@link ITGenericReader} intended for specialized subclasses
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
abstract class ITSpecializedReader extends ITGenericReader {

    /**
     * Wraps common functionality of all tests derived from this class
     * 
     * @param testcaseName human readable name of the testcase
     * @param filename Name of the file to read
     * @return number of paragraphs which were read
     */
    protected abstract int runIndividualTest(final String testcaseName, final String filename);
}
