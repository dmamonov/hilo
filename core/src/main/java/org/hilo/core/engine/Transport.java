package org.hilo.core.engine;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        public View render() {
            return new View(null, null, 'H', false);
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
        public View render() {
            return new View(null, null, '-', false);
        }
    }

    public static class Elevator extends Transport {
        @Inject
        protected GameTime time;

        @Override
        public boolean isAllowCrossing() {
            return true;
        }

        @Override
        public boolean isHold() {
            return true;
        }

        @Override
        public void onTick() {
            if (time.getClock() % 3 == 0) {
                for (final Movable movable : map.list(getPosition(), Movable.class)) {
                    map.move((GameMap.MapUnit) movable, GameMap.Direction.Up);
                }
            }
        }

        @Override
        public View render() {
            return new View(null, Paint.BLUE, '^', false);
        }
    }

    public static abstract class AbstractPush extends Transport {
        @Inject
        protected GameTime time;

        @Override
        public boolean isAllowCrossing() {
            return true;
        }

        @Override
        public boolean isHold() {
            return true;
        }

        @Override
        public void onTick() {
            if (time.getClock() % 3 == 0) {
                for (final Movable movable : map.list(getPosition(), Movable.class)) {
                    map.move((GameMap.MapUnit) movable, getDirection());
                }
            }
        }


        protected abstract GameMap.Direction getDirection();
    }

    public static class PushRight extends AbstractPush {
        @Override
        protected GameMap.Direction getDirection() {
            return GameMap.Direction.Right;
        }

        @Override
        public View render() {
            return new View(null, Paint.GREEN, '>', false);
        }

    }

    public static class PushLeft extends AbstractPush {
        @Override
        protected GameMap.Direction getDirection() {
            return GameMap.Direction.Left;
        }

        @Override
        public View render() {
            return new View(null, Paint.GREEN, '<', false);
        }

    }


    static abstract class AbstractTravelator extends Transport {
        @Inject
        protected GameTime time;

        protected abstract GameMap.Direction getDirection();

        @Override
        public void onTick() {
            if (time.getClock() % 3 == 0) {
                for (final Movable movable : map.list(getPosition().translate(GameMap.Direction.Up), Movable.class)) {
                    map.move((GameMap.MapUnit) movable, getDirection());
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
        public View render() {
            return new View(null, Paint.BLUE, '>', false);
        }
    }

    public static class TravelatorLeft extends AbstractTravelator {
        @Override
        protected GameMap.Direction getDirection() {
            return GameMap.Direction.Left;
        }

        @Override
        public View render() {
            return new View(null, Paint.BLUE, '<', false);
        }
    }

    public static class Lift extends Transport {

        @Override
        public View render() {
            return new View(null, null, '_', false);
        }
    }

    public static class Teleport extends Transport implements Usable {
        @Inject
        protected GameTime time;

        @Override
        public boolean isAllowCrossing() {
            return true;
        }

        @Override
        public View render() {
            return new View(null, null, 'T', true);
        }


        @Override
        public boolean use(final Actor actor) {
            final List<Teleport> gates = new ArrayList<>(Sets.difference(ImmutableSet.copyOf(map.list(Teleport.class)), ImmutableSet.of(this)));
            if (gates.size() > 0) {
                Collections.shuffle(gates);
                final Teleport gate = gates.get(0);
                map.remove(actor);
                map.set(getPosition()).create(Effect.Appear.class);
                time.scheduled(150, new Runnable() {
                    @Override
                    public void run() {
                        map.set(gate.getPosition());
                        map.put(actor);
                        map.create(Effect.Appear.class);
                    }
                });
                return true;
            }
            return false;
        }
    }
}
