package docreader;

/**
 * Interface to some class which can read data
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 *
 * @param <T> return type of {@link #read()}
 */
public interface GenericReader<T> {
    /**
     * Main method. Starts reading / processing the given data
     * 
     * @return T either an Integer describing how much has been read or nothing ({@code Void})
     * @throws Exception
     */
    T read() throws Exception;
}