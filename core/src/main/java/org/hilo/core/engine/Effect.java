package org.hilo.core.engine;

import org.fusesource.jansi.Ansi;

/**
 * @author dmitry.mamonov
 *         Created: 10/31/13 11:21 PM
 */
public abstract class Effect extends GameMap.MapUnit {
    protected int countdown = 5;

    @Override
    public void onTick() {
        if (--countdown <= 0) {
            map.remove(this);
        }
    }


    @Override
    public final Ansi render() {
        return null;
    }

    public static class Blood extends Effect {
        @Override
        public Ansi renderBackground() {
            if (countdown%2==0) {
                return Ansi.ansi().bg(Ansi.Color.RED);
            } else {
                return Ansi.ansi().bgBright(Ansi.Color.RED);
            }
        }
    }

    public static class Explosion extends Effect {
        @Override
        public Ansi renderBackground() {
            return Ansi.ansi().bgBright(countdown % 2 == 0 ? Ansi.Color.YELLOW : Ansi.Color.RED);
        }
    }


}
