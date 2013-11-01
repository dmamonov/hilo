package org.hilo.core.engine;

import com.google.common.collect.Iterables;
import org.fusesource.jansi.Ansi;

import static org.fusesource.jansi.Ansi.ansi;

/**
* @author dmitry.mamonov
*         Created: 10/31/13 10:57 PM
*/
public abstract class Door extends GameMap.MapUnit {


    public static class Locked extends Door implements Usable {
        private Thing.Key key = null;
        @Override
        public Ansi renderBackground() {
            return ansi().bgBright(Ansi.Color.BLACK);
        }

        @Override
        public boolean isAllowCrossing() {
            return key!=null;
        }


        @Override
        public Ansi render() {
            if (!isAllowCrossing()) {
                return ansi().fgBright(Ansi.Color.BLUE).bold().a('D');
            } else {
                return ansi().fg(Ansi.Color.BLACK).a('k');

            }
        }

        @Override
        public boolean use(final Actor actor) {
            if (key!=null) {
                actor.getThings().add(key);
                key = null;
                return true;
            } else {
                final Thing.Key key = Iterables.getFirst(Iterables.filter(actor.getThings(), Thing.Key.class), null);
                if (key != null) {
                    actor.getThings().remove(key);
                    this.key = key;
                    return true;
                }
            }
            return false;
        }
    }

    public static class Automatic extends Door {

        @Override
        public Ansi render() {
            return ansi().a("\uD83D\uDEAA");
        }
    }
}
