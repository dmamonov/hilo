package org.hilo.core.engine;

import com.google.inject.Inject;

import java.util.List;

import static org.hilo.core.utils.ListProxy.allVoid;

/**
 * @author dmitry.mamonov
 *         Created: 10/31/13 10:57 PM
 */
public abstract class Ammo extends GameMap.MapUnit {
    @Inject
    protected GameMap.Direction direction;

    @Override
    public final boolean isCollideBack() {
        return true;
    }

    @Override
    public final boolean isAllowCrossing() {
        return true;
    }

    protected void terminate() {
        map.set(getPosition()).create(Effect.Explosion.class);
    }

    protected abstract int getDamage();

    @Override
    public void onCollide(final GameMap.Direction direction, final List<GameMap.MapUnit> collisions, final boolean allowCrossing) {
        allVoid(Damageable.class, collisions).damage(getDamage());
        if (!allowCrossing) {
            map.remove(this);
            terminate();
        }
    }

    public static class Blade extends Ammo {
        private int lifetime;

        @Override
        protected int getDamage() {
            return 150;
        }

        @Override
        public void onTick() {
            lifetime--;
            if (lifetime < 0) {
                map.remove(this);
            }
        }

        @Override
        protected void terminate() {
            //silent.
        }

        @Override
        public View render() {
            return new View(null, Paint.WHITE_BRIGHT, '-', false);
        }
    }

    public static class Bullet extends Ammo {
        private int lifetime;

        @Override
        protected int getDamage() {
            return 25;
        }

        @Override
        public void onTick() {
            if ((++lifetime) % 3 == 0) {
                map.move(this, direction);
            }
        }

        @Override
        public View render() {
            return new View(null, Paint.RED_BRIGHT, '·', false);
        }
    }

    public static class Rocket extends Ammo {
        @Override
        protected int getDamage() {
            return 75;
        }

        @Override
        public View render() {
            return new View(null, Paint.YELLOW, direction == GameMap.Direction.Left ? '⤛' : '⤜', false);
        }
    }

    public static class Grenade extends Ammo {
        @Override
        protected int getDamage() {
            return 50;
        }

        @Override
        protected void terminate() {
            for (final GameMap.Direction around : GameMap.Direction.values()) {
                map.set(getPosition().translate(around)).create(Effect.Explosion.class);
            }
        }

        @Override
        public void onCollide(final GameMap.Direction direction, final List<GameMap.MapUnit> collisions, final boolean allowCrossing) {
            if (!allowCrossing) {
                for (final GameMap.Direction around : GameMap.Direction.values()) {
                    allVoid(Damageable.class, map.list(getPosition().translate(around))).damage(50);
                }
                map.remove(this);
                terminate();
            }
        }

        @Override
        public boolean isFall() {
            return true;
        }


        @Override
        public View render() {
            return new View(null, Paint.YELLOW_BRIGHT, '*', false);
        }
    }

    public static class Mine extends Ammo {
        @Override
        protected int getDamage() {
            return 99;
        }

        @Override
        protected void terminate() {
            for (final GameMap.Direction around : GameMap.Direction.values()) {
                map.set(getPosition().translate(around)).create(Effect.Explosion.class);
            }
        }

        @Override
        public void onCollide(final GameMap.Direction direction, final List<GameMap.MapUnit> collisions, final boolean allowCrossing) {
            for (final GameMap.Direction around : GameMap.Direction.values()) {
                allVoid(Damageable.class, map.list(getPosition().translate(around))).damage(getDamage());
            }
            map.remove(this);
            terminate();
        }

        @Override
        public View render() {
            return new View(null, Paint.RED_BRIGHT, '_', true);
        }
    }

    public static class Dynamite extends Ammo {

        @Override
        protected int getDamage() {
            return 500;
        }

        @Override
        public View render() {
            return new View(null, Paint.RED, 'i', false);
        }
    }
}
