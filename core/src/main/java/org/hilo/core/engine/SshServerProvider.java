package org.hilo.core.engine;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
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

/**
 * @author dmitry.mamonov
 *         Created: 10/31/13 10:58 PM
 */
@Singleton
public class SshServerProvider implements Provider<SshServer> {
    @Inject
    protected GameMap map;
    @Inject
    protected GameTime time;
    @Inject
    protected GameRenderer renderer;

    @Override
    public SshServer get() {
        final SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(22);
        sshd.setUserAuthFactories(ImmutableList.<org.apache.sshd.common.NamedFactory<org.apache.sshd.server.UserAuth>>of(new UserAuthNone.Factory()));
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hilo.key"));
        sshd.setShellFactory(new Factory<Command>() {
            @Override
            public Command create() {
                return new Command() {
                    private volatile ExitCallback exitCallback;
                    private Environment environment;


                    @Override
                    public void setInputStream(final InputStream in) {
                        new Thread() {
                            private Actor.Player firstPlayer() {
                                return Iterables.getFirst(map.<Actor.Player>list(Actor.Player.class), null);
                            }

                            @Override
                            public void run() {
                                try {
                                    final Actor.Player player = firstPlayer();
                                    while (true) {
                                        final int ch = in.read();

                                        if (ch < 0) {
                                            break;
                                        } else {
                                            final char key = Character.toUpperCase((char) ch);
                                            if (player != null) {
                                                map.addAction(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        switch (key) {
                                                            case 27:
                                                                break;
                                                            case '[':
                                                                break;
                                                            case 'C': //right
                                                                player.right();
                                                                break;
                                                            case 'D': //left
                                                                player.left();
                                                                break;
                                                            case 'A': //up
                                                                player.jump();
                                                                break;
                                                            case 'B': //down
                                                                player.descend();
                                                                break;
                                                            case '\t': //switch
                                                                player.changeTool();
                                                                break;
                                                            case ' ': //act
                                                                player.act();
                                                                break;
                                                            case 'Q'://fire
                                                                player.useTool();
                                                                break;
                                                            default:
                                                                System.out.println("Command: " + key + " @ " + environment.getEnv());
                                                        }

                                                    }
                                                });
                                            }
                                        }
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
                                    GameRenderer.View lastView = null;
                                    while (true) {
                                        final GameRenderer.View newView = renderer.getView();
                                        if (newView!=lastView){
                                            out.write(prepare.toString().getBytes(Charsets.UTF_8));
                                            out.write(newView.getContentBytes());
                                            out.flush();
                                            lastView = newView;
                                        }
                                        Thread.sleep(100L);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    System.exit(0);
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
