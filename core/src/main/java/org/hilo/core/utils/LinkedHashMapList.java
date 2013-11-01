package org.hilo.core.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author dmitry.mamonov
 *         Created: 11/2/13 12:43 AM
 */
public class LinkedHashMapList<K,V> extends LinkedHashMap<K,List<V>> {
    @Override
    public List<V> get(final Object key) {
        final List<V> existingResult = super.get(key);
        if (existingResult != null) {
            return existingResult;
        } else {
            final List<V> createdResult = new ArrayList<>();
            //noinspection unchecked
            put((K) key, createdResult);
            return createdResult;
        }
    }
}
