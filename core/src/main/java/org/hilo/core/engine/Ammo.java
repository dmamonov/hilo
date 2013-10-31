package org.hilo.core.engine;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.fusesource.jansi.Ansi;

import java.util.List;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;

/**
* @author dmitry.mamonov
*         Created: 10/31/13 10:57 PM
*/
public abstract class Ammo extends GameMap.MapUnit {
    @Inject
    protected GameMap.Direction direction;

    @Override
    public boolean isCollideBack() {
        return true;
    }

    protected int getDamage(){
        return 10;
    }

    protected void terminate(){
        map.set(position).create(Effect.Explosion.class);
    }

    @Override
    public final void onCollide(final GameMap.Position position, final List<GameMap.MapUnit> collisions, final boolean allowCrossing) {
        for (final Damageable damageable : Iterables.filter(collisions, Damageable.class)) {
            damageable.damage(getDamage());
        }
        if (!allowCrossing){
            map.remove(this);
            terminate();
        }
    }

    public static class Blade extends Ammo {
        private int lifetime;

        @Override
        public void onTick() {
            lifetime--;
            if (lifetime<0){
                map.remove(this);
            }
        }

        @Override
        protected int getDamage() {
            return 1000;
        }

        @Override
        protected void terminate() {
            //silent.
        }

        @Override
        public Ansi render() {
            return ansi().fgBright(WHITE).a('-');
        }
    }

    public static class Bullet extends Ammo {
        private int lifetime;

        @Override
        public void onTick() {
            if ((++lifetime)%3==0) {
                map.move(this, direction);
            }
        }

        @Override
        public Ansi render() {
            return ansi().fgBright(RED).a('·');
        }
    }

    public static class Rocket extends Ammo {
        @Override
        public Ansi render() {
            return ansi().fg(YELLOW).a(direction == GameMap.Direction.Left ? '⤛' : '⤜');
        }
    }

    public static class Bomb extends Ammo {

        @Override
        public Ansi render() {
            return ansi().fgBright(YELLOW).a('*');
        }
    }

    public static class Mine extends Ammo {

        @Override
        public Ansi render() {
            return ansi().fg(RED).a('_');
        }
    }

    public static class Dynamite extends Ammo {

        @Override
        public Ansi render() {
            return ansi().fg(RED).a('i');
        }
    }
}
