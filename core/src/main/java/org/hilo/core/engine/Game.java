package org.hilo.core.engine;

import com.google.inject.Inject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author dmitry.mamonov
 *         Created: 11/1/13 10:17 PM
 */
public abstract class Game {
    private static final Logger log = LoggerFactory.getLogger(Game.class);
    @Inject
    protected GameMap map;
    @Inject
    protected GameTime time;
    @Inject
    protected GameRenderer renderer;
    @Inject
    protected SshServerProvider face;
    @Inject
    private AI.Randomized ai;

    protected abstract void load();

    @SuppressWarnings("InfiniteLoopStatement")
    public void loop(final long delay) {
        load();
        while (true) {
            try {
                time.tick();
                ai.think(null);
                map.applyOperations();
                renderer.render();
                Thread.sleep(delay);
            } catch (Throwable th) {
                log.error("Loop failed", th);
                th.printStackTrace();
            }
        }
    }
}
