package org.hilo.core.engine;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.fusesource.jansi.Ansi;

import java.util.ArrayList;
import java.util.List;

import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.MAGENTA;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * @author dmitry.mamonov
 *         Created: 10/31/13 10:56 PM
 */
public abstract class Actor extends GameMap.MapUnit implements GameObject.Damageable {
    protected GameMap.Direction direction = GameMap.Direction.Right;

    protected List<Thing> things = new ArrayList<>();
    protected int health = 100;




    public GameMap.Direction getDirection() {
        return direction;
    }

    @Override
    public boolean isAllowCrossing() {
        return true;
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
    public void onCollide(final GameMap.Position position, final List<GameMap.MapUnit> collisions, final boolean allowCrossing) {
        if (allowCrossing) {
            final Iterable<Thing> collidedThings = Iterables.filter(collisions, Thing.class);
            for (final Thing thing : collidedThings) {
                this.things.add(thing);
                map.remove(thing);
            }
        }
    }

    @Override
    public void damage(final int damage) {
        this.health -= damage;
        if (health <= 0) {
            map.remove(this);
        } else {
            map.set(position).create(Effect.Blood.class);
        }
    }

    public void step() {
        map.move(this, direction);
    }

    public void rotate(){
        this.direction = this.direction.inverse();
    }

    public void right() {
        if (direction == GameMap.Direction.Right) {
            step();
        } else {
            rotate();
        }
    }

    public void left() {
        if (direction == GameMap.Direction.Left) {
            step();
        } else {
            rotate();
        }
    }

    public void climb() {
        map.move(this, GameMap.Direction.Up);
    }

    public void descend() {
        map.move(this, GameMap.Direction.Down);
    }

    public void act() {
        if (!things.isEmpty()) {
            for (final Usable usable : Iterables.filter(map.list(position.translate(direction)), Usable.class)) {
                usable.use(things);
            }
        }
    }



    public static class Player extends Actor {
        @Inject
        protected Weapon.Pistol pistol;
        @Inject
        protected Weapon.Knife knife;
        protected Weapon tool;
        protected Weapon otherTool;
        protected int momentum = 0;

        @Override
        public Ansi render() {
            return ansi().fgBright(GREEN).a('@');
        }

        @Override
        public void onTick() {
            if (momentum > 0) {
                momentum--;
            }
        }

        @Override
        public boolean isFall() {
            return momentum <= 0;
        }

        public void changeTool() {
            final Weapon switchTool = otherTool;
            otherTool = tool;
            tool = switchTool;
        }

        public void useTool() {
            if (tool != null) {
                tool.use(this);
            } else {
                pistol.use(this);
                tool = pistol;
                otherTool = knife;
            }
        }


        public void jump() {
            if (isFall()) {
                momentum = 3;
                map.move(this, GameMap.Direction.Up);
            }
        }

        @Override
        public String toString() {
            return "Player{\r\n" +
                    "  direction=" + direction + "\r\n" +
                    "  pistol=" + pistol + "\r\n" +
                    "  tool=" + tool + "\r\n" +
                    "  otherTool=" + otherTool + "\r\n" +
                    "  things=" + things + "\r\n" +
                    "  health=" + health + "\r\n" +
                    "  momentum=" + momentum + "\r\n" +
                    "}\r\n";
        }
    }

    public static class Enemy extends Actor {
        protected boolean moved = false;

        @Override
        public void onMove() {
            this.moved = true;
        }

        public boolean isMoved() {
            return moved;
        }

        @Override
        public void onCollide(final GameMap.Position position, final List<GameMap.MapUnit> collisions, final boolean allowCrossing) {
            super.onCollide(position, collisions, allowCrossing);
            for (final Damageable damageable : Iterables.filter(collisions, Damageable.class)) {
                damageable.damage(25);
            }
        }

        @Override
        public Ansi render() {
            return ansi().fgBright(MAGENTA).a('$');
        }
    }

}
