package org.hilo.core.engine;

import org.fusesource.jansi.Ansi;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * http://unicode-table.com/ru/#geometric-shapes
* @author dmitry.mamonov
*         Created: 10/31/13 10:56 PM
*/
public abstract class Block extends GameMap.MapUnit {

    @Override
    public Ansi render() {
        return null;  //TODO [DM]
    }

    public static class Rock extends Block {
        @Override
        public Ansi render() {
            return ansi().a('\u2588');
        }
    }

    public static class Sand extends Block {
        @Override
        public Ansi render() {
            return ansi().a('░');
        }
    }

    public static class Box extends Block {
        @Override
        public Ansi render() {
            return ansi().a('\u25A8');
        }
    }
}
