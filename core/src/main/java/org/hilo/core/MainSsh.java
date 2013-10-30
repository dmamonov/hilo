package ldrs.core;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
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
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author dmitry.mamonov
 *         Created: 10/30/13 1:23 PM
 */
public class MainSsh {
    public static void main(final String[] args) throws IOException, InterruptedException {
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
                    @Override
                    public void setInputStream(final InputStream inputStream) {
                        new Thread(){
                            @Override
                            public void run() {
                                try {
                                    final InputStream in = inputStream;
                                    while (true) {
                                        final int ch = in.read();
                                        if (ch<0){
                                            break;
                                        }
                                        System.out.println("Command: " + (char) ch+" @ "+
                                        environment.getEnv());
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }

                    @Override
                    public void setOutputStream(final OutputStream outputStream) {
                        new Thread(){
                            @Override
                            public void run() {
                                final Ansi ansi = Ansi.ansi().eraseScreen(Ansi.Erase.ALL)
                                        .cursor(0,0)
                                        .bg(Ansi.Color.BLUE).a("Hello\r\n").bg(Ansi.Color.BLACK).a(new Date().toString());
                                try {
                                    final OutputStream out = outputStream;
                                    while (true) {
                                        out.write(ansi.toString().getBytes(Charsets.UTF_8));
                                        out.flush();
                                        Thread.sleep(3000L);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }

                    @Override
                    public void setErrorStream(final OutputStream outputStream) {
                        new Thread(){
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
        sshd.start();
        Thread.sleep(TimeUnit.HOURS.toMillis(1));
    }
}
