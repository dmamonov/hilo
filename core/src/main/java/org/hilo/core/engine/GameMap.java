package org.hilo.core.engine;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.fusesource.jansi.Ansi;
import org.hilo.core.utils.LinkedHashMapList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.*;

/**
 * @author dmitry.mamonov
 *         Created: 10/31/13 10:55 PM
 */
@Singleton
public class GameMap {
    private final Logger log = LoggerFactory.getLogger(GameMap.class);

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

        public static final List<Direction> horizontalDirections = ImmutableList.of(Left, Right);
        public static final List<Direction> verticalDirections = ImmutableList.of(Up, Down);
        public static final List<Direction> allDirections = ImmutableList.of(Right, Down, Left, Up);

    }

    private static class MapOperation {
        private final MapUnit unit;
        private final boolean fall;
        private final boolean create;
        private final Direction moveTo;

        private MapOperation(final MapUnit unit, final Direction moveTo, final boolean create, final boolean fall) {
            this.unit = unit;
            this.moveTo = moveTo;
            this.fall = fall;
            this.create = create;
        }

        public static MapOperation move(final MapUnit unit, final Direction moveTo) {
            return new MapOperation(unit, moveTo, false, false);
        }

        public static MapOperation fall(final MapUnit unit) {
            return new MapOperation(unit, Direction.Down, false, true);
        }

        public static MapOperation create(final MapUnit unit) {
            return new MapOperation(unit, Direction.Center, true, false);
        }

        private boolean isCreate() {
            return create;
        }

        private MapUnit getUnit() {
            return unit;
        }

        private Direction getMoveTo() {
            return moveTo;
        }

        private boolean isFall() {
            return fall;
        }
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<Position, List<MapUnit>> map = new LinkedHashMapList<>();
    private final Set<MapUnit> lastFalls = new HashSet<>();

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
    private NameProvider nameProvider;
    @Inject
    protected GameTime time;
    private int width;
    private int height;

    public GameMap setName(final String name) {
        nameProvider.set(name);
        return this;
    }

    public <T extends MapUnit> T create(final Class<T> type) {
        final T mapObject = injector.getInstance(type);
        operations.add(MapOperation.create(mapObject));
        return mapObject;
    }

    public void put(final MapUnit unit) {
        unit.setPosition(positionProvider.get());
        operations.add(MapOperation.create(unit));
    }

    public GameMap move(final MapUnit obj, final Direction moveTo) {
        checkNotNull(obj);
        checkNotNull(moveTo);
        operations.add(MapOperation.move(obj, moveTo));
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

    public <T extends MapUnit> Iterable<T> list(final Class<T> filter, final String name) {
        //noinspection unchecked
        return (Iterable<T>) unmodifiableIterable(filter(filter(concat(map.values()), instanceOf(filter)), new Predicate<MapUnit>() {
            @Override
            public boolean apply(final MapUnit unit) {
                return Objects.equal(unit.getName(), name);
            }
        }));
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
            final MapUnit unit = operation.getUnit();
            final boolean create = operation.isCreate();
            final Direction moveTo = operation.getMoveTo();
            final List<MapUnit> sourceUnitList = map.get(unit.getPosition());
            final Position targetPosition = unit.getPosition().translate(moveTo);
            final List<MapUnit> targetUnitList = map.get(targetPosition);
            if (create || sourceUnitList.contains(unit)) {
                boolean allowCrossing = true;
                if (!targetUnitList.isEmpty()) {
                    for (final MapUnit targetUnit : targetUnitList) {
                        if (targetUnit.isCollideBack()) {
                            targetUnit.onCollide(moveTo, ImmutableList.of(unit), unit.isAllowCrossing());
                        }
                        if (!targetUnit.isAllowCrossing()) {
                            allowCrossing = false;
                            break;
                        }
                    }
                    unit.onCollide(moveTo.inverse(), targetUnitList, allowCrossing);
                }
                if (allowCrossing || create) {
                    if (!create) {
                        sourceUnitList.remove(unit);
                    }
                    targetUnitList.add(unit);
                    unit.setPosition(targetPosition);
                    unit.onMove();
                    if (operation.isFall()) {
                        lastFalls.add(unit);
                    }
                }
            }
        }
        final boolean doFalls = time.getClock() % 3 == 0;
        for (final MapUnit unit : ImmutableList.copyOf(Iterables.concat(map.values()))) {
            unit.onTick();
            if (doFalls) {
                if (unit.isFall()) {
                    final boolean holdStill = isHold(unit.getPosition()) && unit.isHoldOn();
                    if (!holdStill) {
                        final boolean doFall = isAllowCrossing(unit.getPosition().translate(Direction.Down));
                        if (doFall || lastFalls.contains(unit)) {
                            operations.add(MapOperation.fall(unit));
                        }
                    }
                }
            }
        }
        if (doFalls) {
            lastFalls.clear();
        }
    }

    public boolean isAllowCrossing(final Position position) {
        for (final MapUnit unit : map.get(position)) {
            if (!unit.isAllowCrossing()) {
                return false;
            }
        }
        return true;
    }

    public boolean isHold(final Position position) {
        for (final MapUnit unit : map.get(position)) {
            if (unit.isHold()) {
                return true;
            }
        }
        return false;
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
        final Ansi ansi = Ansi.ansi();
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                final MapUnit.View unitView = new MapUnit.View(null, null, null, false);
                final List<MapUnit> unitList = map.get(new Position(x, y));
                try {
                    for (final MapUnit unit : unitList) {
                        unitView.join(unit.render());
                    }
                } catch (RuntimeException re) {
                    re.printStackTrace();
                }
                unitView.render(ansi);
            }
            ansi.a(GameRenderer.LINE_BREAK);
        }
        return ansi.toString();
    }

    public void init(final int width, final List<String> lines) {
        this.width = width;
        this.height = lines.size();
        final Map<Character, Class<? extends MapUnit>> mapping = ImmutableMap.<Character, Class<? extends MapUnit>>builder()
                .put('P', Actor.Player.class)
                .put('E', Actor.Enemy.class)
                .put('e', Actor.SmallEnemy.class)
                .put('W', Block.Rock.class)
                .put('D', Door.Locked.class)
                .put('K', Thing.Key.class)
                .put('_', Ammo.Mine.class)
                .put('*', Ammo.Grenade.class)
                .put('>', Transport.TravelatorRight.class)
                .put('<', Transport.TravelatorLeft.class)
                .put('T', Transport.Teleport.class)
                .put('H', Transport.Ladder.class)
                .put('-', Transport.Rope.class)
                .put('^', Transport.Elevator.class)
                .put('}', Transport.PushRight.class)
                .put('{', Transport.PushLeft.class)
                .put('X', Block.Box.class)
                .put('S', Block.Sand.class)
                .put('~', Fluid.Water.class)
                .build();
        int y = 0;
        for (final String line : Lists.reverse(ImmutableList.copyOf(lines))) {
            if (y >= height) {
                break;
            } else {
                int x = 0;
                final String namingPart = (line.length() > width ? line.substring(width) : "").trim();
                @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
                final Map<Character, List<String>> knownNames = new LinkedHashMapList<>();
                if (!namingPart.isEmpty()) {
                    for (final String name : namingPart.split("\\s+")) {
                        if (name.length() >= 3 && name.substring(1, 2).equals("=")) {
                            final Character key = name.charAt(0);
                            final String named = name.substring(2);
                            knownNames.get(key).add(named);
                        } else {
                            log.error("Bad name '" + name + "' at line: " + line);
                        }
                    }
                }
                for (final char ch : line.toCharArray()) {
                    if (x >= width) {
                        break;
                    } else {
                        if (mapping.containsKey(ch)) {
                            final List<String> nameList = knownNames.get(ch);
                            if (nameList.size() > 0) {
                                final String useName = nameList.remove(0);
                                setName(useName);
                            }
                            set(new Position(x, y)).create(mapping.get(ch));
                        }
                        x++;
                    }
                }
                y++;
            }
        }
        applyOperations();
    }

    public void debug() throws IOException {
        final StringBuilder html = new StringBuilder("<html>\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "</head>\n" +
                "<body>\n" +
                "<pre>\n");
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                final List<MapUnit> unitList = map.get(new Position(x, y));
                final MapUnit.View unitView = new MapUnit.View(null, null, null, false);
                for (final MapUnit unit : unitList) {
                    unitView.join(unit.render());
                }
                final String tipSeparator = "\n - ";
                unitView.render(html, new Position(x, y) + tipSeparator + Joiner.on(tipSeparator).join(Iterables.transform(unitList, new Function<MapUnit, Object>() {
                    @Override
                    public Object apply(final MapUnit unit) {
                        return unit.getClass().getName() + "#" + fromNullable(unit.getName()).or("" + unit.hashCode());
                    }
                })));
            }
            html.append("\n");
        }
        html.append("</pre>\n" +
                "</body>\n" +
                "</html>\n");
        final File debugFile = new File("debug.html");
        Files.write(html.toString(), debugFile, Charsets.UTF_8);
        Desktop.getDesktop().open(debugFile);

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
        public boolean equals(final Object o) {
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
        private Direction direction = Direction.Right;

        @Override
        public Direction get() {
            return direction;
        }

        public void set(final Direction direction) {
            this.direction = checkNotNull(direction);
        }
    }

    @Singleton
    protected static class NameProvider implements Provider<String> {
        private String name;

        @Override
        public String get() {
            final String result = name;
            name = null;
            return result;
        }

        public void set(final String name) {
            this.name = name;
        }
    }

    public static abstract class MapUnit extends GameObject {
        @Inject
        protected GameMap map;
        @Inject
        private Position position;

        @Inject
        @Nullable
        @SuppressWarnings("NullableProblems")
        @Named("name")
        private String name;

        @Nullable
        public String getName() {
            return name;
        }

        public Position getPosition() {
            return position;
        }

        void setPosition(final Position position) {
            this.position = position;
        }

        public boolean isAllowCrossing() {
            return false;
        }

        public boolean isHold() {
            return false;
        }

        public boolean isHoldOn() {
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

        public void onCollide(final Direction direction, final List<MapUnit> collisions, final boolean allowCrossing) {

        }

        public void onMove() {
        }

        public abstract View render();

        public interface Movable {

        }

        public enum Paint {
            BLACK(Ansi.Color.BLACK, false),
            BLACK_BRIGHT(Ansi.Color.BLACK, true),
            RED(Ansi.Color.RED, false),
            RED_BRIGHT(Ansi.Color.RED, true),
            GREEN(Ansi.Color.GREEN, false),
            GREEN_BRIGHT(Ansi.Color.GREEN, true),
            YELLOW(Ansi.Color.YELLOW, false),
            YELLOW_BRIGHT(Ansi.Color.YELLOW, true),
            BLUE(Ansi.Color.BLUE, false),
            BLUE_BRIGHT(Ansi.Color.BLUE, true),
            MAGENTA(Ansi.Color.MAGENTA, false),
            MAGENTA_BRIGHT(Ansi.Color.MAGENTA, true),
            CYAN(Ansi.Color.CYAN, false),
            CYAN_BRIGHT(Ansi.Color.CYAN, true),
            WHITE(Ansi.Color.WHITE, false),
            WHITE_BRIGHT(Ansi.Color.WHITE, true),
            DEFAULT(Ansi.Color.DEFAULT, false),
            DEFAULT_BRIGHT(Ansi.Color.DEFAULT, true);
            private final Ansi.Color color;
            private final boolean bright;

            private Paint(final Ansi.Color color, final boolean bright) {
                this.color = color;
                this.bright = bright;
            }

            public void bg(final Ansi ansi) {
                if (bright) {
                    ansi.bgBright(color);
                } else {
                    ansi.bg(color);
                }
            }

            public void fg(final Ansi ansi) {
                if (bright) {
                    ansi.fgBright(color);
                } else {
                    ansi.fg(color);
                }
            }
        }

        public static class View {
            protected Paint bg;
            protected Paint fg;
            protected Character ch;
            protected boolean bold;

            public View(final Paint bg, final Paint fg, final Character ch, final boolean bold) {
                this.bg = bg;
                this.fg = fg;
                this.ch = ch;
                this.bold = bold;
            }

            public void join(final View other) {
                if (other.bg != null) {
                    this.bg = other.bg;
                }
                if (other.fg != null) {
                    this.fg = other.fg;
                }
                if (other.ch != null) {
                    this.ch = other.ch;
                }
                if (other.bold) {
                    this.bold = true;
                }
            }

            public void render(final Ansi ansi) {
                if (bg != null) {
                    bg.bg(ansi);
                }
                if (fg != null) {
                    fg.fg(ansi);
                }
                if (bold) {
                    ansi.bold();
                }
                if (ch != null) {
                    ansi.a(ch);
                } else {
                    ansi.a(' ');
                }
                ansi.reset();
            }

            public void render(final StringBuilder html, final String hint) {
                final Paint fg = fromNullable(this.fg).or(Paint.WHITE);
                final Paint bg = fromNullable(this.bg).or(Paint.BLACK);
                html.append(String.format("<span style='color: %s;background: %s;' title='%s'>%s</span>",
                        (!fg.bright ? "DARK" : "") + fg.color.name(),
                        (!bg.bright ? "DARK" : "") + bg.color.name(),
                        hint,
                        fromNullable(ch).or(' ')));
            }
        }
    }
}
