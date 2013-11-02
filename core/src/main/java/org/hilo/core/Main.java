package org.hilo.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.sshd.SshServer;
import org.hilo.core.engine.AI;
import org.hilo.core.engine.GameMap;
import org.hilo.core.engine.GameRenderer;
import org.hilo.core.engine.GameTime;
import org.hilo.core.engine.HiloModule;

import java.io.IOException;

/**
 * @author dmitry.mamonov
 *         Created: 10/27/13 5:40 PM
 */
public class Main {
    public static void main(final String[] args) throws IOException, InterruptedException {
        final Injector injector = Guice.createInjector(new HiloModule());
        final SshServer ssh = injector.getInstance(SshServer.class);
        ssh.start();
        final GameMap map = injector.getInstance(GameMap.class);
        final GameRenderer renderer = injector.getInstance(GameRenderer.class);
        //map.init(73, Files.readAllLines(new File("../core/demo-map-01.txt").getAbsoluteFile().toPath(), US_ASCII));
        debugWithPutty();
        final GameTime time = injector.getInstance(GameTime.class);
        final AI ai = injector.getInstance(AI.Randomized.class);
        //noinspection InfiniteLoopStatement
        while (true){
            Thread.sleep(100L);
            time.tick();
            ai.think(null);
            map.applyOperations();
            renderer.render();
        }
    }

    public static void debugWithPutty() throws IOException {
        if (System.getProperty("os.name").toLowerCase().contains("windows")){
            Runtime.getRuntime().exec("putty.exe -load 1  me@localhost");
        }
    }
}
