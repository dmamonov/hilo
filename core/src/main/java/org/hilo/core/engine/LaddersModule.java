package org.hilo.core.engine;

import com.google.common.collect.Iterables;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.fusesource.jansi.Ansi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author dmitry.mamonov
 *         Created: 10/27/13 5:59 PM
 */
public class LaddersModule implements Module {

    @Override
    public void configure(final Binder binder) {
        binder.bind(Position.class).toProvider(GameMap.class);
    }

    public static final class GameTime {
        private int clock=0;

        public void tick(){
            clock++;
        }

        public void scheduled(final int ticks,final Runnable callback){

        }
    }

    public static abstract class GameObject {

    }

    public enum Direction {
        Left(-1, 0) {
            @Override
            public Direction inverse() {
                return Right;
            }
        },
        Right(1, 0) {
            @Override
            public Direction inverse() {
                return Left;
            }
        },
        Up(0, 1) {
            @Override
            public Direction inverse() {
                return Down;
            }
        },
        Down(0, -1) {
            @Override
            public Direction inverse() {
                return Up;
            }
        },
        Center(0, 0) {
            @Override
            public Direction inverse() {
                return Center;
            }
        };
        private final int x;
        private final int y;

        private Direction(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public abstract Direction inverse();
    }

    public static class Position {
        private final int x;
        private final int y;

        public Position(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        public Position translate(final Direction direction) {
            return new Position(x + direction.getX(), y + direction.getY());
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            final Position that = (Position) o;

            if (x != that.x) {
                return false;
            }
            if (y != that.y) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + x;
            result = 31 * result + y;
            return result;
        }
    }

    @Singleton
    public static class GameMap implements Provider<Position> {
        private static class MapOperation {
            private final MapObject object;
            private final Direction moveTo;

            private MapOperation(final MapObject object, final Direction moveTo) {
                this.object = object;
                this.moveTo = moveTo;
            }

            private MapObject getObject() {
                return object;
            }

            private Direction getMoveTo() {
                return moveTo;
            }
        }
        private final Map<Position, List<MapObject>> map = new HashMap<Position, List<MapObject>>(){
            @Override
            public List<MapObject> get(final Object o) {
                final List<MapObject> existingResult = super.get(o);
                if (existingResult!=null){
                    return existingResult;
                } else {
                    final List<MapObject> createdResult = new ArrayList<>();
                    put((Position) o, createdResult);
                    return createdResult;
                }
            }
        };
        private final List<MapOperation> operations = new ArrayList<>();
        private Position cursor = new Position(10,10);
        @Inject
        private Injector injector;

        public <T extends MapObject> T create(final Class<T> type) {
            final T mapObject = injector.getInstance(type);
            map.get(cursor).add(mapObject);
            return mapObject;
        }

        @Override
        public Position get() {
            return cursor;
        }

        public GameMap set(final Position position){
            this.cursor = checkNotNull(position);
            return this;
        }

        public GameMap move(final MapObject obj, final Direction moveTo) {
            operations.add(new MapOperation(obj, moveTo));
            return this;
        }

        public Iterable<MapObject> list() {
            return Iterables.unmodifiableIterable(map.get(cursor));
        }
    }

    public static abstract class ToolObject extends GameObject {
        public abstract void use();
    }


    public static abstract class MapObject extends GameObject {
        protected Ansi.Color color = null;
        protected char symbol;

        @Inject
        protected GameMap map;
        @Inject
        protected Position position;
        protected int momentum = 0;

        public Position getPosition() {
            return position;
        }

        public int getMomentum() {
            return momentum;
        }
    }

    public static abstract class AbstractActor extends MapObject {
        protected Direction direction;
        protected ToolObject tool;
        protected ToolObject otherTool;


        public void jump() {
            if (0==momentum){
                momentum=3;
                map.move(this, Direction.Up);
            }
        }

        public void step() {
            map.move(this,direction);
        }

        public void rotate() {
            this.direction=this.direction.inverse();
        }

        public void climb() {
            map.move(this, Direction.Up);
        }

        public void change() {
            final ToolObject switchTool = otherTool;
            otherTool = tool;
            tool = switchTool;
        }

        public void act() {
            if (tool!=null){
                tool.use();
            }
        }

        public void press() {
            map.set(position).list();
        }
    }

    public static class Player extends AbstractActor {
        public void ok() {
            System.out.println("map: " + map + ", " + position);
        }
    }

    public static class Enemy extends AbstractActor {

    }

    public static abstract class AbstractBlock extends MapObject {

    }

    public static class BlockRock extends AbstractBlock {

    }

    public static class BlockSand extends AbstractBlock {

    }

    public static class BlockBox extends AbstractBlock {

    }

    public static abstract class AbstractTransport extends MapObject {

    }

    public static class TransportElevator extends AbstractTransport {

    }

    public static class TransportTravelator extends AbstractTransport {

    }

    public static class TransportLift extends AbstractTransport {

    }

    public static class TransportTeleport extends AbstractTransport {

    }

    public static abstract class AbstractDoor extends MapObject {

    }

    public static class DoorKey extends AbstractDoor {

    }

    public static class DoorSwitcher extends AbstractDoor {

    }

    public static abstract class AbstractSwitcher extends MapObject {

    }

    public static class Switcher extends AbstractSwitcher {

    }

    public static abstract class AbstractWeapon extends MapObject {

    }

    public static class WeaponKnife extends AbstractWeapon {

    }

    public static class WeaponBullet extends AbstractWeapon {

    }

    public static class WeaponRocket extends AbstractWeapon {

    }

    public static class WeaponBomb extends AbstractWeapon {

    }

    public static class WeaponMine extends AbstractWeapon {

    }

    public static class WeaponDynamite extends AbstractWeapon {

    }


}
