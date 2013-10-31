package org.hilo.core.engine;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * @author dmitry.mamonov
 *         Created: 10/27/13 5:59 PM
 */
public class HiloModule implements Module {

    @Override
    public void configure(final Binder binder) {
        binder.bind(SshServer.class).toProvider(SshServerProvider.class);
        binder.bind(Position.class).toProvider(PositionProvider.class);
        binder.bind(Direction.class).toProvider(DirectionProvider.class);
    }

    @Singleton
    public static final class GameTime {
        public int getClock() {
            return clock;
        }

        protected static class Schedule {
            private final int when;
            private final Runnable callback;

            public Schedule(final int when, final Runnable callback) {
                this.when = when;
                this.callback = callback;
            }

            public int getWhen() {
                return when;
            }

            public Runnable getCallback() {
                return callback;
            }
        }

        private final List<Schedule> schedules = new ArrayList<>();
        private int clock = 0;

        public void tick() {
            clock = getClock() + 1;
            Iterables.removeIf(schedules, new Predicate<Schedule>() {
                @Override
                public boolean apply(final GameTime.Schedule schedule) {
                    if (schedule.getWhen() >= getClock()) {
                        schedule.getCallback().run();
                        return true;
                    }
                    return false;
                }
            });
        }

        public void scheduled(final int ticks, final Runnable callback) {
            schedules.add(new Schedule(getClock() + ticks, callback));
        }
    }

    public static abstract class GameObject {
        //TODO [?]
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

    @Singleton
    public static class GameMap {
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

        private final Map<Position, List<MapObject>> map = new HashMap<Position, List<MapObject>>() {
            @Override
            public List<MapObject> get(final Object key) {
                final List<MapObject> existingResult = super.get(key);
                if (existingResult != null) {
                    return existingResult;
                } else {
                    final List<MapObject> createdResult = new ArrayList<>();
                    put((Position) key, createdResult);
                    return createdResult;
                }
            }
        };
        private final List<MapOperation> operations = new ArrayList<>();

        @Inject
        private Injector injector;
        @Inject
        private PositionProvider positionProvider;
        @Inject
        private DirectionProvider directionProvider;
        private int width;
        private int height;

        public <T extends MapObject> T create(final Class<T> type) {
            final T mapObject = injector.getInstance(type);
            operations.add(new MapOperation(mapObject, null));
            return mapObject;
        }

        public GameMap move(final MapObject obj, final Direction moveTo) {
            checkNotNull(obj);
            checkNotNull(moveTo);
            operations.add(new MapOperation(obj, moveTo));
            return this;
        }

        public Iterable<MapObject> list(final Position position) {
            return Iterables.unmodifiableIterable(map.get(position));
        }

        public void applyOperations() {
            for (final MapOperation operation : operations) {
                final List<MapObject> objctesInPosition = map.get(operation.getObject().getPosition());
                if (operation.getMoveTo() == null) {
                    objctesInPosition.add(operation.getObject());
                } else {
                    objctesInPosition.remove(operation.getObject());
                    map.get(operation.getObject().getPosition().translate(operation.getMoveTo())).add(operation.getObject());
                }
            }
            operations.clear();
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
                    final List<MapObject> objs = map.get(new Position(x, y));
                    if (objs.isEmpty()) {
                        result.append('.');
                    } else {
                        result.append(objs.get(0).render().reset());
                    }
                }
                result.append("\r\n");
            }
            return result.toString();
        }

        public void init(final int width, final int height, final Iterable<String> lines) {
            this.width = width;
            this.height = height;
            final Map<Character, Class<? extends MapObject>> mapping = ImmutableMap.<Character, Class<? extends MapObject>>builder()
                    .put('P', AbstractActor.Player.class)
                    .put('E', AbstractActor.Enemy.class)
                    .put('W', AbstractBlock.BlockRock.class)
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
    }

    public static abstract class ToolObject extends GameObject {
        @Inject
        protected GameMap map;

        public abstract void use(final AbstractActor actor);

        public static class ToolKnife extends ToolObject {
            public void use(final AbstractActor actor) {
                map.set(actor.getPosition().translate(actor.getDirection()))
                        .set(actor.getDirection())
                        .create(AbstractWeapon.WeaponKnife.class);
            }
        }

        public static class ToolPistol extends ToolObject {
            public void use(final AbstractActor actor) {
                map.set(actor.getPosition().translate(actor.getDirection()))
                        .set(actor.getDirection())
                        .create(AbstractWeapon.WeaponBullet.class);
            }
        }

        public static class ToolGrenade extends ToolObject {
            public void use(final AbstractActor actor) {
                map.set(actor.getPosition().translate(Direction.Up).translate(actor.getDirection()))
                        .set(actor.getDirection())
                        .create(AbstractWeapon.WeaponBomb.class);
            }
        }
    }

    public static abstract class MapObject extends GameObject {
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

        public abstract Ansi render();
    }

    public static abstract class AbstractActor extends MapObject {
        protected Direction direction;
        protected ToolObject tool;
        protected ToolObject otherTool;

        public Direction getDirection() {
            return direction;
        }

        public void jump() {
            if (0 == momentum) {
                momentum = 3;
                map.move(this, Direction.Up);
            }
        }

        public void step() {
            map.move(this, direction);
        }

        public void rotate() {
            this.direction = this.direction.inverse();
        }

        public void climb() {
            map.move(this, Direction.Up);
        }

        public void descend() {
            map.move(this, Direction.Down);
        }

        public void change() {
            final ToolObject switchTool = otherTool;
            otherTool = tool;
            tool = switchTool;
        }

        public void useTool() {
            if (tool != null) {
                tool.use(this);
            }
        }

        public void act() {
            map.list(position);
        }

        public static class Player extends AbstractActor {
            public void ok() {
                System.out.println("map: " + map + ", " + position);
            }

            @Override
            public Ansi render() {
                return ansi().a('@');
            }
        }

        public static class Enemy extends AbstractActor {
            @Override
            public Ansi render() {
                return ansi().fg(MAGENTA).a('^');
            }
        }

    }


    public static abstract class AbstractBlock extends MapObject {

        @Override
        public Ansi render() {
            return null;  //TODO [DM]
        }

        public static class BlockRock extends AbstractBlock {
            @Override
            public Ansi render() {
                return ansi().a('▉');
            }
        }

        public static class BlockSand extends AbstractBlock {
            @Override
            public Ansi render() {
                return ansi().a('░');
            }
        }

        public static class BlockBox extends AbstractBlock {
            @Override
            public Ansi render() {
                return ansi().a('X');
            }
        }
    }


    public static abstract class AbstractTransport extends MapObject {
        public static class TransportElevator extends AbstractTransport {
            @Inject
            protected Direction direction;

            @Override
            public Ansi render() {
                return ansi().a(Direction.Up == direction ? 'u' : 'd');
            }
        }

        public static class TransportTravelator extends AbstractTransport {
            @Inject
            protected Direction direction;

            @Override
            public Ansi render() {
                return ansi().a(Direction.Left == direction ? '<' : '>');
            }
        }

        public static class TransportLift extends AbstractTransport {

            @Override
            public Ansi render() {
                return ansi().a('_');
            }
        }

        public static class TransportTeleport extends AbstractTransport {

            @Override
            public Ansi render() {
                return ansi().a('T');
            }
        }
    }


    public static abstract class AbstractDoor extends MapObject {

        @Override
        public Ansi render() {
            return null;  //TODO [DM]
        }

        public static class DoorKey extends AbstractDoor {

            @Override
            public Ansi render() {
                return ansi().a("\uD83D\uDEAA");
            }
        }

        public static class DoorSwitcher extends AbstractDoor {

            @Override
            public Ansi render() {
                return ansi().a("\uD83D\uDEAA");
            }
        }
    }


    public static abstract class AbstractSwitcher extends MapObject {

        @Override
        public Ansi render() {
            return null;  //TODO [DM]
        }

        public static class SwitcherPermanent extends AbstractSwitcher {

            @Override
            public Ansi render() {
                return ansi().a('o');
            }
        }

        public static class SwitcherTimed extends AbstractSwitcher {

            @Override
            public Ansi render() {
                return ansi().a('0');
            }
        }
    }

    public static abstract class AbstractWeapon extends MapObject {
        protected Direction direction;

        public Direction getDirection() {
            return direction;
        }

        public static class WeaponKnife extends AbstractWeapon {

            @Override
            public Ansi render() {
                return ansi().a(direction == Direction.Left ? '⤙' : '⤚');
            }
        }

        public static class WeaponBullet extends AbstractWeapon {

            @Override
            public Ansi render() {
                return ansi().fg(BLUE).a('·');
            }
        }

        public static class WeaponRocket extends AbstractWeapon {
            @Override
            public Ansi render() {
                return ansi().fg(YELLOW).a(direction == Direction.Left ? '⤛' : '⤜');
            }
        }

        public static class WeaponBomb extends AbstractWeapon {

            @Override
            public Ansi render() {
                return ansi().fg(YELLOW).a('*');
            }
        }

        public static class WeaponMine extends AbstractWeapon {

            @Override
            public Ansi render() {
                return ansi().fg(RED).a('_');
            }
        }

        public static class WeaponDynamite extends AbstractWeapon {

            @Override
            public Ansi render() {
                return ansi().fg(RED).a('i');
            }
        }
    }

    @Singleton
    public static class SshServerProvider implements Provider<SshServer> {
        @Inject
        protected GameMap map;

        @Override
        public SshServer get() {
            final SshServer sshd = SshServer.setUpDefaultServer();
            sshd.setPort(22);
            sshd.setUserAuthFactories(ImmutableList.<org.apache.sshd.common.NamedFactory<org.apache.sshd.server.UserAuth>>of(new UserAuthNone.Factory()));
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
            sshd.setShellFactory(new Factory<Command>() {
                @Override
                public Command create() {
                    return new Command() {
                        private volatile ExitCallback exitCallback;
                        private Environment environment;
                        private boolean displayUpdate = true;

                        @Override
                        public void setInputStream(final InputStream in) {
                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        while (true) {
                                            final int ch = in.read();
                                            if (ch < 0) {
                                                break;
                                            } else {
                                                displayUpdate = true;
                                            }
                                            System.out.println("Command: " + (char) ch + " @ " +
                                                    environment.getEnv());
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }.start();
                        }

                        @Override
                        public void setOutputStream(final OutputStream out) {
                            new Thread() {
                                @Override
                                public void run() {
                                    final Ansi prepare = Ansi.ansi().eraseScreen(Ansi.Erase.ALL).cursor(0, 0);
                                    try {
                                        while (true) {
                                            if (displayUpdate) {
                                                out.write(prepare.toString().getBytes(Charsets.UTF_8));
                                                out.write(map.render().getBytes(Charsets.UTF_8));
                                                out.flush();
                                            } else {
                                                Thread.sleep(100L);
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }.start();
                        }

                        @Override
                        public void setErrorStream(final OutputStream outputStream) {
                            new Thread() {
                                @Override
                                public void run() {

                                }
                            }.start();
                        }

                        @Override
                        public void setExitCallback(final ExitCallback exitCallback) {
                            this.exitCallback = exitCallback;
                        }

                        @Override
                        public void start(final Environment environment) throws IOException {
                            this.environment = environment;
                        }

                        @Override
                        public void destroy() {
                            exitCallback.onExit(0);
                        }
                    };
                }
            });
            return sshd;
        }
    }

}
