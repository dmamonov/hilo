package org.hilo.core.engine;

import com.google.inject.Inject;
import org.hilo.core.utils.ListProxy;
import org.hilo.core.utils.Rnd;

/**
 * @author dmitry.mamonov
 *         Created: 11/2/13 5:06 PM
 */
public abstract class Fluid extends GameMap.MapUnit {
    @Inject
    protected Rnd rnd;

    @Override
    public void onTick() {
        for(final GameMap.Direction direction:new GameMap.Direction[]{GameMap.Direction.Down, rnd.nextItem(GameMap.Direction.horizontalDirections)}) {
            final GameMap.Position targetPosition = getPosition().translate(direction);
            if (map.isAllowCrossing(targetPosition) && map.list(targetPosition, Fluid.class).isEmpty()) {
                map.move(this, direction);
                break;
            }
        }
        ListProxy.allVoid(Damageable.class, map.list(getPosition())).damage(1);
    }

    public static class Water extends Fluid {
        @Override
        public View render() {
            return new View(Paint.BLUE_BRIGHT, null, null, false);
        }
    }
}
