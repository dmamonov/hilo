package org.hilo.core.engine;

import com.google.common.collect.Iterables;

/**
* @author dmitry.mamonov
*         Created: 10/31/13 10:57 PM
*/
public abstract class Door extends GameMap.MapUnit {


    public static class Locked extends Door implements Usable {
        private Thing.Key key = null;

        @Override
        public boolean isAllowCrossing() {
            return key!=null;
        }


        @Override
        public View render() {
            if (isAllowCrossing()) {
                return new View(Paint.BLACK_BRIGHT,Paint.BLACK,'k',false);
            } else {
                return new View(Paint.BLACK_BRIGHT,Paint.BLUE_BRIGHT,'D',true);
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
        public View render() {
            if (isAllowCrossing()) {
                return new View(Paint.BLACK_BRIGHT,Paint.BLACK,'k',false);
            } else {
                return new View(Paint.BLACK_BRIGHT,Paint.BLUE_BRIGHT,'A',true);
            }
        }
    }
}
