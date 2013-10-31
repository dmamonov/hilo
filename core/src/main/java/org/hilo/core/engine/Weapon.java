package org.hilo.core.engine;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
* @author dmitry.mamonov
*         Created: 10/31/13 10:56 PM
*/
public abstract class Weapon extends GameObject {
    @Inject
    protected GameMap map;

    public void use(final Actor actor){
        final GameMap.Position target = target(actor);
        if (map.list(target, getAmmoType()).isEmpty()){
            map.set(target)
                    .set(actor.getDirection())
                    .create(getAmmoType());
        }
    }

    protected GameMap.Position target(final Actor actor){
        return actor.getPosition().translate(actor.getDirection());
    }

    protected abstract Class<? extends Ammo> getAmmoType();

    @Singleton
    public static class Knife extends Weapon {
        @Override
        protected Class<? extends Ammo> getAmmoType() {
            return Ammo.Blade.class;
        }
    }

    @Singleton
    public static class Pistol extends Weapon {

        @Override
        protected Class<? extends Ammo> getAmmoType() {
            return Ammo.Bullet.class;
        }
    }

    @Singleton
    public static class Bazooka extends Weapon {
        @Override
        protected GameMap.Position target(final Actor actor) {
            return actor.getPosition().translate(GameMap.Direction.Up).translate(actor.getDirection());
        }

        @Override
        protected Class<? extends Ammo> getAmmoType() {
            return Ammo.Grenade.class;
        }
    }
}
