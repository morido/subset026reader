package helper;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * HashMap whose <em>values</em> are weak referenced.
 * Thus, it differs from {@link java.util.WeakHashMap} where the <em>keys</em> are weak referenced.
 * <p>modified from <a href="http://stackoverflow.com/a/25634084">http://stackoverflow.com/a/25634084</a></p>
 * <p><em>Note:</em> This is a very simplistic version of a map. Hence, it does not implement the {@link java.util.Map} interface.</p>  
 *
 * @param <K> type of key to be stored
 * @param <V> type of weak referenced value to be stored
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public final class HashMapWeakValue<K, V> {
    /**
     * Internal map
     */
    private final transient Map<K,WeakReference<V>> map;
    
    
    /**
     * Constructor for a new WeakValueHashMap
     * 
     * @param initialCapacity Initial capacity of the new HashMap
     * @throws IllegalArgumentException if the initial capacity is negative
     * @see java.util.HashMap#HashMap(int)
     */
    public HashMapWeakValue(final int initialCapacity) {
	if (initialCapacity < 0) throw new IllegalArgumentException("Capacity cannot be negative");
	this.map = new HashMap<>(initialCapacity);
    }

    /**
     * Get a value
     * 
     * @param key key to look up
     * @return value if still present; or {@code null} if it has either been garbage collected or never existed in the first place
     * @see java.util.Map#get(Object)
     */
    public V get(final K key) {
	final WeakReference<V> weakReference = this.map.get(key);
	final V output;

	if (weakReference == null) output = null;
	else {
	    output = weakReference.get();
	    if (output == null) this.map.remove(key); // the GC has kicked in, clean up the map
	}
	return output;
    }

    /**
     * Put a value
     * 
     * @param key key of the value
     * @param value value to store
     * @see java.util.Map#put(Object, Object)
     */
    public void put(final K key, final V value) {
	this.map.put(key, new WeakReference<>(value));
    }
}
