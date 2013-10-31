package org.hilo.core.engine;

import com.google.inject.Inject;
import org.fusesource.jansi.Ansi;

import static org.fusesource.jansi.Ansi.ansi;

/**
* @author dmitry.mamonov
*         Created: 10/31/13 10:57 PM
*/
public abstract class Transport extends GameMap.MapUnit {
    public static class Elevator extends Transport {
        @Inject
        protected GameMap.Direction direction;

        @Override
        public Ansi render() {
            return ansi().a(GameMap.Direction.Up == direction ? 'u' : 'd');
        }
    }

    public static class Travelator extends Transport {
        @Inject
        protected GameMap.Direction direction;

        @Override
        public Ansi render() {
            return ansi().a(GameMap.Direction.Left == direction ? '<' : '>');
        }
    }

    public static class Lift extends Transport {

        @Override
        public Ansi render() {
            return ansi().a('_');
        }
    }

    public static class Teleport extends Transport {

        @Override
        public Ansi render() {
            return ansi().a('T');
        }
    }
}
