package org.hilo.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.hilo.core.engine.LaddersModule;

/**
 * @author dmitry.mamonov
 *         Created: 10/27/13 5:40 PM
 */
public class Main {
    public static void main(final String[] args) {
        System.out.println("NEW MAIN");
        final Injector injector = Guice.createInjector(new LaddersModule());
        final LaddersModule.GameMap map = injector.getInstance(LaddersModule.GameMap.class);
        final LaddersModule.Player p = map.create(LaddersModule.Player.class);
        p.ok();
        System.out.println(map.list());
    }
}
