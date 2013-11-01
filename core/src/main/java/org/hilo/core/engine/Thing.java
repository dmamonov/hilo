package org.hilo.core.engine;

/**
 * @author dmitry.mamonov
 *         Created: 10/31/13 11:40 PM
 */
public abstract class Thing extends GameMap.MapUnit implements GameMap.MapUnit.Movable {
    @Override
    public boolean isAllowCrossing() {
        return true;
    }

    @Override
    public boolean isFall() {
        return true;
    }

    public static class Key extends Thing {
        @Override
        public View render() {
            return new View(null, null, 'k', false);
        }
    }
}
