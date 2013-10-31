package org.hilo.core.engine;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author dmitry.mamonov
 *         Created: 11/1/13 12:25 AM
 */
@Singleton
public class GameRenderer {
    @Inject
    protected GameMap map;
    @Inject
    protected GameTime time;

    protected volatile View lastView = new View("", 0);

    public static class View {
        private final String content;
        private final int version;

        public View(final String content, final int version) {
            this.content = content;
            this.version = version;
        }

        public String getContent() {
            return content;
        }

        public byte[] getContentBytes() {
            return content.getBytes(Charsets.UTF_8);
        }

        public int getVersion() {
            return version;
        }
    }


    public View getView() {
        return lastView;
    }

    public void update() {
        lastView = new View(
                map.render() +
                        Joiner.on("\r\n").join(map.list(Actor.Player.class)) + "\r\n" +
                        "Game Time: " + time.getClock(),
                time.getClock()
        );
    }


}
