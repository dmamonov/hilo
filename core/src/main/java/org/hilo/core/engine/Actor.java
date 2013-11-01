package org.hilo.core.engine;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;
import static org.hilo.core.utils.ListProxy.allVoid;
import static org.hilo.core.utils.ListProxy.firstSuccess;

/**
 * @author dmitry.mamonov
 *         Created: 10/31/13 10:56 PM
 */
public abstract class Actor extends GameMap.MapUnit implements GameObject.Damageable, GameMap.MapUnit.Movable {
    protected GameMap.Direction direction = GameMap.Direction.Right;

    protected List<Thing> things = new ArrayList<>();
    protected int health = 100;
    protected String status = "";

    public List<Thing> getThings() {
        return things;
    }

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
    public void onCollide(final GameMap.Direction direction, final List<GameMap.MapUnit> collisions, final boolean allowCrossing) {
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
        status = "Damage " + damage + " hits";
        this.health -= damage;
        if (health <= 0) {
            status = "Dead";
            map.remove(this);
            for (final Thing thing : things) {
                map.set(getPosition()).put(thing);
            }

        } else {
            map.set(getPosition()).create(Effect.Blood.class);
        }
    }

    public void step() {
        status = "Step "+direction;
        map.move(this, direction);
    }

    public void rotate() {
        this.direction = this.direction.inverse();
        status="Rotate to "+direction;
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
        status = "Climb";
        if (map.isHold(getPosition().translate(GameMap.Direction.Up))) {
            status += " OK";
            map.move(this, GameMap.Direction.Up);
        } else {
            status += " nope";
        }
    }

    public void descend() {
        status = "Descend";
        map.move(this, GameMap.Direction.Down);
    }

    public boolean act() {
        status = "Act ";
        if (!things.isEmpty()) {
            if (firstSuccess(Usable.class, map.list(getPosition().translate(direction))).use(this)) {
                status += "thing used!";
                return true;
            }
        }
        if (firstSuccess(Usable.class,map.list(getPosition())).use(this)) {
            status += "pressed";
            return true;
        }
        status += "nope";
        return false;
    }


    public static class Player extends Actor {
        @Inject
        protected Weapon.Pistol pistol;
        @Inject
        //protected Weapon.Knife knife;
        protected Weapon.Bazooka knife;
        protected Weapon tool;
        protected Weapon otherTool;
        protected int jumping = 0;

        @Override
        public View render() {
            return new View(null, Paint.GREEN_BRIGHT, '@',true);
        }

        @Override
        public void onTick() {
            if (jumping > 0) {
                jumping--;
            }
        }

        @Override
        public boolean isFall() {
            return jumping <= 0;
        }

        public void changeTool() {
            final Weapon switchTool = otherTool;
            otherTool = tool;
            tool = switchTool;
            status = "Switched to "+tool;
        }

        public void useTool() {
            if (tool != null) {
                tool.use(this);
                status = "Used: "+tool;
            } else {
                tool = pistol;
                otherTool = knife;
            }
        }


        public void jump() {
            status = "Jump";
            if (map.isHold(getPosition())) {
                climb();
            } else if (!map.isAllowCrossing(getPosition().translate(GameMap.Direction.Down))) {
                if (jumping <= 0) {
                    jumping = 3;
                    map.move(this, GameMap.Direction.Up);
                    status += " OK";
                }
            } else {
                status += " denied";
            }

        }

        @Override
        public String toString() {
            return ansi().a("Player" + hashCode() + ": " + status).newline()
                    .a("xy" + getPosition()).newline()
                    .a("Health: ").fgBright(RED).a(health).reset().newline()
                    .a("Weapons Primary: ").fgBright(WHITE).a(tool).reset().newline()
                    .a("      Secondary: ").a(otherTool).newline()
                    .a("Bag: ").fg(BLUE).a(things).reset().newline().toString();
        }
    }

    public static class Enemy extends Actor {
        protected boolean moved = false;

        @Override
        public void onTick() {
        }

        @Override
        public void onMove() {
            this.moved = true;
        }

        @Override
        public void step() {
            super.step();
            this.moved = false;
        }

        public boolean isMoved() {
            return moved;
        }

        @Override
        public void onCollide(final GameMap.Direction direction, final List<GameMap.MapUnit> collisions, final boolean allowCrossing) {
            super.onCollide(direction, collisions, allowCrossing);
            allVoid(Damageable.class, collisions).damage(25);
        }

        @Override
        public View render() {
            return new View(null, Paint.MAGENTA_BRIGHT, '$',true);
        }
    }

    public static class SmallEnemy extends Actor {
        @Override
        public View render() {
            return new View(null, Paint.MAGENTA_BRIGHT, 's',true);
        }
    }

}
