package org.hilo.core.engine;

import com.google.inject.Inject;
import org.hilo.core.utils.Rnd;

import static org.hilo.core.utils.ListProxy.allVoid;

/**
 * @author dmitry.mamonov
 *         Created: 11/2/13 5:06 PM
 */
public abstract class Fluid extends GameMap.MapUnit {
    @Inject
    protected Rnd rnd;
    protected GameMap.Direction lastDirection = GameMap.Direction.Right;

    @Override
    public boolean isAllowCrossing() {
       return true;
    }

    @Override
    public void onTick() {
        boolean moved = false;
        for (final GameMap.Direction direction : new GameMap.Direction[]{GameMap.Direction.Down, lastDirection}) {
            final GameMap.Position targetPosition = getPosition().translate(direction);
            if (map.isAllowCrossing(targetPosition) && map.list(targetPosition, Fluid.class).isEmpty()) {
                map.move(this, direction);
                moved = true;
                break;
            }
        }
        if (!moved) {
            lastDirection = lastDirection.inverse();
        }
        allVoid(Damageable.class, map.list(getPosition())).damage(1);
    }

    public static class Water extends Fluid {
        @Override
        public View render() {
            return new View(Paint.BLUE_BRIGHT, null, null, false);
        }
    }

    public static class Gasoline extends Fluid implements Damageable {
        private int flaming = Integer.MIN_VALUE;
        @Inject
        private GameTime time;

        @Override
        public void damage(final int damage) {
            if (flaming == Integer.MIN_VALUE && damage > 1) { //TODO [DM] fluids make damage on themselfs
                flaming = 300;
            }
        }

        @Override
        public void onTick() {
            super.onTick();
            if (flaming >= 0) {
                flaming--;
                if (flaming <= 0) {
                    allVoid(Damageable.class, map.list(getPosition().translate(GameMap.Direction.Down))).damage(10);
                    map.remove(this);
                } else {
                    if (time.getClock() % 10 == 0) {
                        for (final GameMap.Direction direction : GameMap.Direction.horizontalAndUpDirections) {
                            allVoid(Damageable.class, map.list(getPosition().translate(direction))).damage(10);
                        }
                    }
                }
            }
        }

        @Override
        public View render() {
            return new View(flaming > 0 ? (rnd.nextInt(2) % 2 == 0 ? Paint.YELLOW_BRIGHT : Paint.RED_BRIGHT) : Paint.BLACK_BRIGHT, null, null, false);
        }
    }
}


