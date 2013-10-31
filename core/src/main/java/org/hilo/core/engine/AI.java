package org.hilo.core.engine;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Random;

/**
 * @author dmitry.mamonov
 *         Created: 11/1/13 1:26 AM
 */

public abstract class AI extends GameObject {
    public abstract void think();

    @Singleton
    public static class Randomized extends AI {
        @Inject
        protected GameMap map;
        protected final Random random = new Random();
        @Override
        public void think() {
            for (final Actor.Enemy enemy : map.list(Actor.Enemy.class)) {
                switch (random.nextInt(10)){
                    case 0:
                        enemy.left();
                        break;
                    case 1:
                        enemy.right();
                        break;
                }
            }

        }
    }
}
