package org.hilo.core.utils;

import java.util.List;
import java.util.Random;

/**
 * @author dmitry.mamonov
 *         Created: 11/2/13 5:16 PM
 */
public class Rnd extends Random {
    public <T> T nextItem(final List<T> items) {
        return items.get(nextInt(items.size()));
    }
}
