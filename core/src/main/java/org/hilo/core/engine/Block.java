package org.hilo.core.engine;

import com.google.common.collect.Iterables;

import java.util.List;

/**
 * http://unicode-table.com/ru/#geometric-shapes
 *
 * @author dmitry.mamonov
 *         Created: 10/31/13 10:56 PM
 */
public abstract class Block extends GameMap.MapUnit {

    @Override
    public View render() {
        return null;  //TODO [DM]
    }

    public static class Rock extends Block {
        @Override
        public View render() {
            return new View(null, null, '\u2588', false);
        }
    }

    public static class Sand extends Block implements Damageable {
        private int health = 50;

        @Override
        public void damage(final int damage) {
            health -= damage;
            if (health <= 0) {
                map.remove(this);
            }
        }

        @Override
        public View render() {
            return new View(null, null, '░', false);
        }
    }

    public static class Box extends Block implements Movable, Damageable {
        private int health = 500;

        @Override
        public void damage(final int damage) {
            health -= damage;
            if (health <= 0) {
                map.remove(this);
            }
        }

        @Override
        public boolean isFall() {
            return true;
        }

        @Override
        public boolean isCollideBack() {
            return true;
        }

        @Override
        public void onCollide(final GameMap.Direction direction, final List<GameMap.MapUnit> collisions, final boolean allowCrossing) {
            if (!Iterables.isEmpty(Iterables.filter(collisions, Actor.class))) {
                map.move(this, direction);
            }
        }

        @Override
        public View render() {
            return new View(null, null, 'X', true);
        }
    }
}
