package org.hilo.core.engine;

import org.fusesource.jansi.Ansi;

import static org.fusesource.jansi.Ansi.ansi;

/**
* @author dmitry.mamonov
*         Created: 10/31/13 10:57 PM
*/
public abstract class Switcher extends GameMap.MapUnit {

    @Override
    public Ansi render() {
        return null;  //TODO [DM]
    }

    public static class Permanent extends Switcher {

        @Override
        public Ansi render() {
            return ansi().a('o');
        }
    }

    public static class Timed extends Switcher {

        @Override
        public Ansi render() {
            return ansi().a('0');
        }
    }
}
