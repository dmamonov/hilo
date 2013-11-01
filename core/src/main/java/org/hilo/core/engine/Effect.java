package org.hilo.core.engine;

/**
 * @author dmitry.mamonov
 *         Created: 10/31/13 11:21 PM
 */
public abstract class Effect extends GameMap.MapUnit {
    protected int countdown = 15;

    @Override
    public void onTick() {
        if (--countdown <= 0) {
            map.remove(this);
        }
    }

    @Override
    public final boolean isAllowCrossing() {
        return true;
    }

    public static class Blood extends Effect {
        @Override
        public View render() {
            return new View(countdown % 2 == 0 ? Paint.RED : Paint.RED_BRIGHT, null, null, false);
        }

    }

    public static class Explosion extends Effect {
        @Override
        public View render() {
            return new View(
                    countdown % 2 == 0 ? Paint.YELLOW_BRIGHT : Paint.RED_BRIGHT,
                    countdown % 2 == 0 ? Paint.RED : Paint.YELLOW,
                    null, false);
        }
    }

    public static class Appear extends Effect {
        {
            countdown = 25;
        }

        @Override
        public View render() {
            return new View(countdown % 2 == 0 ? Paint.WHITE_BRIGHT : Paint.BLACK_BRIGHT, null, null, false);
        }

    }

}
