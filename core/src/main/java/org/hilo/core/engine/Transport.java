package org.hilo.core.engine;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.fusesource.jansi.Ansi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * @author dmitry.mamonov
 *         Created: 10/31/13 10:57 PM
 */
public abstract class Transport extends GameMap.MapUnit {
    public static class Ladder extends Transport {
        @Override
        public boolean isAllowCrossing() {
            return true;
        }

        @Override
        public boolean isHold() {
            return true;
        }

        @Override
        public Ansi render() {
            return ansi().a('H');
        }
    }

    public static class Rope extends Transport {
        @Override
        public boolean isAllowCrossing() {
            return true;
        }

        @Override
        public boolean isHold() {
            return true;
        }

        @Override
        public Ansi render() {
            return ansi().a('-');
        }
    }

    public static class Elevator extends Transport {
        @Inject
        protected GameMap.Direction direction;

        @Override
        public Ansi render() {
            return ansi().a(GameMap.Direction.Up == direction ? 'u' : 'd');
        }
    }

    static abstract class AbstractTravelator extends Transport {
        @Inject
        protected GameTime time;

        protected abstract GameMap.Direction getDirection();

        @Override
        public void onTick() {
            if (time.getClock() % 3 == 0) {
                for (final Actor actor : map.list(position.translate(GameMap.Direction.Up), Actor.class)) {
                    map.move(actor, getDirection());
                }
            }
        }



    }

    public static class TravelatorRight extends AbstractTravelator {
        @Override
        protected GameMap.Direction getDirection() {
            return GameMap.Direction.Right;
        }

        @Override
        public Ansi render() {
            return ansi().fg(Ansi.Color.BLUE).a('>');
        }
    }

    public static class TravelatorLeft extends AbstractTravelator {
        @Override
        protected GameMap.Direction getDirection() {
            return GameMap.Direction.Left;
        }

        @Override
        public Ansi render() {
            return ansi().fg(Ansi.Color.BLUE).a('<');
        }
    }

    public static class Lift extends Transport {

        @Override
        public Ansi render() {
            return ansi().a('_');
        }
    }

    public static class Teleport extends Transport implements Usable{
        @Inject
        protected GameTime time;

        @Override
        public boolean isAllowCrossing() {
            return true;
        }

        @Override
        public Ansi render() {
            return ansi().a('T');
        }


        @Override
        public boolean use(final Actor actor) {
            final List<Teleport> gates = new ArrayList<>(Sets.difference(ImmutableSet.copyOf(map.list(Teleport.class)), ImmutableSet.of(this)));
            if (gates.size()>0) {
                Collections.shuffle(gates);
                final Teleport gate = gates.get(0);
                map.remove(actor);
                time.scheduled(100, new Runnable() {
                    @Override
                    public void run() {
                        map.set(gate.getPosition()).put(actor);
                    }
                });
                return true;
            }
            return false;
        }
    }
}
