package org.hilo.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.sshd.SshServer;
import org.hilo.core.engine.HiloModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.google.common.base.Charsets.US_ASCII;

/**
 * @author dmitry.mamonov
 *         Created: 10/27/13 5:40 PM
 */
public class Main {
    public static void main(final String[] args) throws IOException, InterruptedException {
        System.out.println("NEW MAIN");
        final Injector injector = Guice.createInjector(new HiloModule());
        final SshServer ssh = injector.getInstance(SshServer.class);
        ssh.start();
        final HiloModule.GameMap map = injector.getInstance(HiloModule.GameMap.class);
        map.init(73, 21, Files.readAllLines(new File("core/demo-map-01.txt").toPath(), US_ASCII));
        System.out.println(map.render());
        Thread.sleep(60*60*1000L);
    }
}
