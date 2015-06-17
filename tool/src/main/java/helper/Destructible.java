package helper;

/**
 * Interface for all classes which come with a fake destuctor (i.e. which need to do some cleanup) 
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public interface Destructible {
    /**
     * Cleanup method
     */
    void close();
}