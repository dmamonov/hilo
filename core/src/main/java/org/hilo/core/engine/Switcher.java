package org.hilo.core.engine;

/**
 * @author dmitry.mamonov
 *         Created: 10/31/13 10:57 PM
 */
public abstract class Switcher extends GameMap.MapUnit {

    public static class Permanent extends Switcher {

        @Override
        public View render() {
            return new View(null, null, 'o', false);
        }
    }

    public static class Timed extends Switcher {

        @Override
        public View render() {
            return new View(null, null, '0', false);
        }
    }
}
