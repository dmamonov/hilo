package org.hilo.core.engine;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
* @author dmitry.mamonov
*         Created: 10/31/13 10:53 PM
*/
@Singleton
public final class GameTime {
    public int getClock() {
        return clock;
    }

    protected static class Schedule {
        private final int when;
        private final Runnable callback;

        public Schedule(final int when, final Runnable callback) {
            this.when = when;
            this.callback = callback;
        }

        public int getWhen() {
            return when;
        }

        public Runnable getCallback() {
            return callback;
        }
    }

    private final List<Schedule> schedules = new ArrayList<>();
    private final List<Runnable> subscribers = new ArrayList<>();
    private int clock = 0;

    public void tick() {
        clock = getClock() + 1;
        Iterables.removeIf(schedules, new Predicate<Schedule>() {
            @Override
            public boolean apply(final Schedule schedule) {
                if (schedule.getWhen() >= getClock()) {
                    schedule.getCallback().run();
                    return true;
                }
                return false;
            }
        });
        for(final Runnable sub:subscribers){
            sub.run();
        }
    }

    public void scheduled(final int ticks, final Runnable callback) {
        schedules.add(new Schedule(getClock() + ticks, callback));
    }

    public void subscribe(final Runnable subscriber) {
        this.subscribers.add(subscriber);
    }
}
