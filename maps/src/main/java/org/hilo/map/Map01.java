package org.hilo.map;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.sshd.SshServer;
import org.hilo.core.Main;
import org.hilo.core.engine.Game;
import org.hilo.core.engine.HiloModule;

import java.io.IOException;

/**
 * @author dmitry.mamonov
 *         Created: 11/1/13 10:17 PM
 */
public class Map01 {
    public static void main(final String[] args) throws IOException {
        final Injector injector = Guice.createInjector(new HiloModule(), new Module() {
            @Override
            public void configure(final Binder binder) {
                binder.bind(Game.class).to(ThisGame.class).asEagerSingleton();
            }
        });
        final Game game = injector.getInstance(Game.class);
        final SshServer ssh = injector.getInstance(SshServer.class);
        ssh.start();
        if (true) Main.debugWithPutty();
        game.loop(70L);
    }

    private static class ThisGame extends Game {
        @Override
        protected void load() {
            map.init(73, ImmutableList.of(
                    "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
                    "W                                                               }      W",
                    "W   *    *                                                      ^W     W",
                    "W   T  e      X                                          X      ^      W T=TopTeleport",
                    "W   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>               >>>>>>>W      W",
                    "W      *        K                                                      W",
                    "W             K K --W   W  ><<<<<<<<<<<<<<<<<<<<                       W",
                    "W               W  ^W   W        T                                     W",
                    "W                  ^W   W      D      E           K   _S D             W",
                    "W        *         ^W   W       WWWWWWWWWWWWWWWWWWWWWWWWWWWW           W",
                    "W          X       ^W~~~W                                              W",
                    "W                  ^W~~~W   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>      W",
                    "W                  ^W~~~W                                              W",
                    "W             H    ^ SSS *H           <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<W",
                    "W         ----H----^----- H                                            W",
                    "W             H    ^      H             *         *                    W",
                    "W             H    ^      H                                            W",
                    "W             H    ^      H-----------------------------------T        W",
                    "W        W    H  W--------HW                                  W        W",
                    "W   E    D  K PT D       E D         __SSS    SSSSSS                  TW",
                    "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"
            ));
            try {
                if (false){
                    map.debug();
                    System.exit(0);
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }
}
