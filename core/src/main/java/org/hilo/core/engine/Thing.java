package org.hilo.core.engine;

import org.fusesource.jansi.Ansi;

/**
 * @author dmitry.mamonov
 *         Created: 10/31/13 11:40 PM
 */
public abstract class Thing extends GameMap.MapUnit {
    @Override
    public boolean isAllowCrossing() {
        return true;
    }

    @Override
    public boolean isFall() {
        return true;
    }

    public static class Key extends Thing{
        @Override
        public Ansi render() {
            return Ansi.ansi().a('k');
        }
    }
}
