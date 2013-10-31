package org.hilo.core.engine;

import com.google.common.collect.Iterables;
import org.fusesource.jansi.Ansi;

import java.util.Collection;

import static org.fusesource.jansi.Ansi.ansi;

/**
* @author dmitry.mamonov
*         Created: 10/31/13 10:57 PM
*/
public abstract class Door extends GameMap.MapUnit {
    protected boolean locked = true;

    @Override
    public boolean isAllowCrossing() {
        return !locked;
    }

    public static class Locked extends Door implements Usable {


        @Override
        public Ansi renderBackground() {
            return ansi().bgBright(Ansi.Color.BLACK);
        }

        @Override
        public Ansi render() {
            final Ansi ansi = ansi();
            if (locked) {
                ansi.fgBright(Ansi.Color.BLUE);
            } else {
                ansi.fg(Ansi.Color.BLACK);

            }
            return ansi.bold().a('D');
        }

        @Override
        public boolean use(final Collection<Thing> things) {
            if (locked){
                final Thing.Key key = Iterables.getFirst(Iterables.filter(things, Thing.Key.class), null);
                if (key!=null) {
                    things.remove(key);
                    locked =false;
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
