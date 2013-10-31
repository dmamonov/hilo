package org.hilo.core.engine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.fusesource.jansi.Ansi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.*;

/**
 * @author dmitry.mamonov
 *         Created: 10/31/13 10:55 PM
 */
@Singleton
public class GameMap {
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

    private static class MapOperation {
        private final MapUnit object;
        private final Direction moveTo;

        private MapOperation(final MapUnit object, final Direction moveTo) {
            this.object = object;
            this.moveTo = moveTo;
        }

        private MapUnit getObject() {
            return object;
        }

        private Direction getMoveTo() {
            return moveTo;
        }
    }

    private final Map<Position, List<MapUnit>> map = new HashMap<Position, List<MapUnit>>() {
        @Override
        public List<MapUnit> get(final Object key) {
            final List<MapUnit> existingResult = super.get(key);
            if (existingResult != null) {
                return existingResult;
            } else {
                final List<MapUnit> createdResult = new ArrayList<>();
                put((Position) key, createdResult);
                return createdResult;
            }
        }
    };

    private final List<Runnable> actions = new ArrayList<>();
    private final List<MapOperation> operations = new ArrayList<>();
    private final List<MapUnit> removes = new ArrayList<>();

    @Inject
    private Injector injector;
    @Inject
    private PositionProvider positionProvider;
    @Inject
    private DirectionProvider directionProvider;
    @Inject
    protected GameTime time;
    private int width;
    private int height;

    public <T extends MapUnit> T create(final Class<T> type) {
        final T mapObject = injector.getInstance(type);
        operations.add(new MapOperation(mapObject, null));
        return mapObject;
    }

    public GameMap move(final MapUnit obj, final Direction moveTo) {
        checkNotNull(obj);
        checkNotNull(moveTo);
        operations.add(new MapOperation(obj, moveTo));
        return this;
    }

    public void remove(final MapUnit mapUnit) {
        removes.add(mapUnit);
    }

    public Iterable<MapUnit> list(final Position position) {
        return unmodifiableIterable(map.get(position));
    }

    public <T> List<T> list(final Position position, final Class<T> filter) {
        return ImmutableList.copyOf(Iterables.filter(map.get(position), filter));
    }

    public <T extends MapUnit> Iterable<T> list(final Class<T> filter) {
        //noinspection unchecked
        return (Iterable<T>) unmodifiableIterable(filter(concat(map.values()), instanceOf(filter)));
    }


    public void applyOperations() {
        synchronized (this) {
            for (final Runnable action : actions) {
                action.run();
            }
            actions.clear();
        }

        for (final MapUnit mapUnit : removes) {
            map.get(mapUnit.getPosition()).remove(mapUnit);
        }
        removes.clear();

        final ImmutableList<MapOperation> currentOperations = ImmutableList.copyOf(operations);
        operations.clear();
        for (final MapOperation operation : currentOperations) {
            final MapUnit unit = operation.getObject();
            final boolean create = operation.getMoveTo() == null;
            final Direction moveTo = create ? Direction.Center : operation.getMoveTo();
            final List<MapUnit> sourceUnitList = map.get(unit.getPosition());
            final Position targetPosition = unit.getPosition().translate(moveTo);
            final List<MapUnit> targetUnitList = map.get(targetPosition);
            if (create || sourceUnitList.contains(unit)) {
                boolean allowCrossing = true;
                if (!targetUnitList.isEmpty()) {
                    for (final MapUnit targetUnit : targetUnitList) {
                        if (targetUnit.isCollideBack()) {
                            targetUnit.onCollide(targetPosition, ImmutableList.of(unit), unit.isAllowCrossing());
                        }
                        if (!targetUnit.isAllowCrossing()) {
                            allowCrossing = false;
                            break;
                        }
                    }
                    unit.onCollide(targetPosition, targetUnitList, allowCrossing);
                }
                if (allowCrossing || create) {
                    if (!create) {
                        sourceUnitList.remove(unit);
                    }
                    targetUnitList.add(unit);
                    unit.setPosition(targetPosition);
                }
            }
        }
        for (final MapUnit mapUnit : ImmutableList.copyOf(Iterables.concat(map.values()))) {
            mapUnit.onTick();
            if (mapUnit.isFall()) {
                boolean doFall = true;
                for (final MapUnit underUnit : map.get(mapUnit.getPosition().translate(Direction.Down))) {
                    if (!underUnit.isAllowCrossing()) {
                        doFall = false;
                        break;
                    }
                }
                if (doFall) {
                    move(mapUnit, Direction.Down);
                }
            }
        }
    }


    public GameMap set(final Position position) {
        positionProvider.set(position);
        return this;
    }


    public GameMap set(final Direction direction) {
        directionProvider.set(direction);
        return this;
    }

    public String render() {
        final StringBuilder result = new StringBuilder();
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                final List<MapUnit> units = map.get(new Position(x, y));
                if (units.isEmpty()) {
                    result.append(' ');
                } else {
                    for (final MapUnit unit : units) {
                        final Ansi background = unit.renderBackground();
                        if (background != null) {
                            result.append(background);
                            break;
                        }
                    }
                    boolean drawBlank = true;
                    for (final MapUnit unit : Lists.reverse(units)) {
                        final Ansi rendered = unit.render();
                        if (rendered != null) {
                            result.append(rendered.reset());
                            drawBlank = false;
                            break;
                        }
                    }
                    if (drawBlank) {
                        result.append(Ansi.ansi().a(' ').reset());
                    }
                }
            }
            result.append("\r\n");
        }
        return result.toString();
    }

    public void init(final int width, final int height, final Iterable<String> lines) {
        this.width = width;
        this.height = height;
        final Map<Character, Class<? extends MapUnit>> mapping = ImmutableMap.<Character, Class<? extends MapUnit>>builder()
                .put('P', Actor.Player.class)
                .put('E', Actor.Enemy.class)
                .put('W', Block.Rock.class)
                .put('D', Door.Locked.class)
                .put('K', Thing.Key.class)
                .build();
        if (lines != null) {
            int y = 0;
            for (final String line : Lists.reverse(ImmutableList.copyOf(lines))) {
                if (y >= height) {
                    break;
                } else {
                    int x = 0;
                    for (final char ch : line.toCharArray()) {
                        if (x >= width) {
                            break;
                        } else {
                            if (mapping.containsKey(ch)) {
                                set(new Position(x, y)).create(mapping.get(ch));
                            }
                            x++;
                        }
                    }
                    y++;
                }
            }
        }
        applyOperations();
    }

    public synchronized void addAction(final Runnable action) {
        this.actions.add(action);
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

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Position)) {
                return false;
            }

            final Position position = (Position) o;

            if (x != position.x) {
                return false;
            }
            if (y != position.y) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return result;
        }

        @Override
        public String toString() {
            return String.format("(%02d,%02d)", x, y);
        }
    }

    @Singleton
    protected static class PositionProvider implements Provider<Position> {
        private Position cursor = new Position(10, 10);

        @Override
        public Position get() {
            return cursor;
        }

        public void set(final Position position) {
            this.cursor = checkNotNull(position);
        }
    }

    @Singleton
    protected static class DirectionProvider implements Provider<Direction> {
        private Direction direction;

        @Override
        public Direction get() {
            return direction;
        }

        public void set(final Direction direction) {
            this.direction = checkNotNull(direction);
        }
    }

    public static abstract class MapUnit extends GameObject {
        @Inject
        protected GameMap map;
        @Inject
        protected Position position;


        public Position getPosition() {
            return position;
        }

        void setPosition(final Position position) {
            this.position = position;
        }

        public boolean isAllowCrossing() {
            return false;
        }

        public boolean isFall() {
            return false;
        }


        public void onTick() {

        }

        public boolean isCollideBack() {
            return false;
        }

        public void onCollide(final Position position, final List<MapUnit> collisions, final boolean allowCrossing) {

        }

        public void onMove() {
        }

        public Ansi renderBackground() {
            return null;
        }

        public abstract Ansi render();
    }
}
