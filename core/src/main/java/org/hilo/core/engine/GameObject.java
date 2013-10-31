package org.hilo.core.engine;

import com.google.inject.Singleton;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
* @author dmitry.mamonov
*         Created: 10/31/13 10:54 PM
*/
public abstract class GameObject {
    private static final AtomicInteger sequence = new AtomicInteger();
    private final int hash = sequence.incrementAndGet();
    public interface Usable {
        void use(final Collection<Thing> things);
    }

    public interface Damageable {
        void damage(final int damage);
    }

    @Override
    public String toString() {
        final Class<? extends GameObject> clazz = getClass();
        return clazz.getSimpleName() + (clazz.getAnnotation(Singleton.class)!=null ? "" : "@" + hashCode());
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
