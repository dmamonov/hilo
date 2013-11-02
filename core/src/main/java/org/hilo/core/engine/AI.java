package org.hilo.core.engine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.hilo.core.utils.Rnd;

/**
 * @author dmitry.mamonov
 *         Created: 11/1/13 1:26 AM
 */

public abstract class AI extends GameObject {
    public abstract void think(Character mind);

    @Singleton
    public static class Randomized extends AI {
        @Inject
        protected GameMap map;
        @Inject
        protected GameTime time;
        @Inject
        protected Rnd rnd;

        @Override
        public void think(final Character mind) {
            for (final Actor.Enemy enemy : map.list(Actor.Enemy.class)) {
                if (time.getClock()%7==0) {
                    if (enemy.isMoved()) {
                        enemy.step();
                    } else {
                        if (!enemy.act()) {
                            enemy.rotate();
                        }
                        enemy.step();
                    }
                }
            }

        }
    }

    public static class Human extends AI {
        @Override
        public void think(final Character mind) {
            //TODO [DM]
        }
    }
}
