package org.hilo.core.engine;

import com.google.common.collect.Iterables;
import org.fusesource.jansi.Ansi;

import java.util.List;

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

    public static class Sand extends Block implements Damageable {
        private int health = 50;

        @Override
        public void damage(final int damage) {
            health-=damage;
            if (health<=0){
                map.remove(this);
            }
        }

        @Override
        public Ansi render() {
            return ansi().a('░');
        }
    }

    public static class Box extends Block implements Movable{
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
            if (!Iterables.isEmpty(Iterables.filter(collisions, Actor.class))){
                map.move(this, direction);
            }
        }

        @Override
        public Ansi render() {
            return ansi().a('\u25A8');
        }
    }
}
