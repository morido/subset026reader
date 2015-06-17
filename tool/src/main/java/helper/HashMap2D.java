package helper;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple 2D version of a HashMap
 * <p>does not implement {@link java.util.Map} due to its simplicity</p>
 *
 * @param <K1> type of key of the first dimension
 * @param <K2> type of key of the second dimension
 * @param <V> type of value to be stored
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public final class HashMap2D<K1, K2, V> {		
    private final transient Map<K1, Map<K2, V>> map = new HashMap<>();

    /**
     * @param outerKey key of the first dimension
     * @param innerKey key of the second dimension
     * @return the value to which the specified combination of keys is mapped, or {@code null} if this map contains no mapping for the key
     * @see java.util.Map#get(Object)
     */
    public V get(final K1 outerKey, final K2 innerKey) {
	return (this.map.containsKey(outerKey)) ? this.map.get(outerKey).get(innerKey) : null;		
    }

    /**
     * @param outerKey key of the first dimension
     * @param innerKey key of the second dimension
     * @param value value to be stored at the given position, may be {@code null}
     * @see java.util.Map#put(Object, Object)
     */
    public void put(final K1 outerKey, final K2 innerKey, final V value) {
	if (!this.map.containsKey(outerKey)) this.map.put(outerKey, new HashMap<K2, V>());	
	final Map<K2, V> innerMap = this.map.get(outerKey);
	innerMap.put(innerKey, value);
    }

    /**
     * @param outerKey key of the first dimension
     * @return the entire map stored at the position of {@code outerKey}
     */
    public Map<K2, V> getRow(final K1 outerKey) {
	return this.map.get(outerKey);
    }

    /**
     * @param outerKey key of the first dimension
     * @param input entire map to be stored at the position of {@code outerKey}
     */
    public void putRow (final K1 outerKey, final Map<K2, V> input) {
	this.map.put(outerKey, input);
    }	
}
