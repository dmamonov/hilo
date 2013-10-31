package org.hilo.core.engine;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.apache.sshd.SshServer;

/**
 * @author dmitry.mamonov
 *         Created: 10/27/13 5:59 PM
 */
public class HiloModule implements Module {

    @Override
    public void configure(final Binder binder) {
        binder.bind(SshServer.class).toProvider(SshServerProvider.class);
        binder.bind(GameMap.Position.class).toProvider(GameMap.PositionProvider.class);
        binder.bind(GameMap.Direction.class).toProvider(GameMap.DirectionProvider.class);
    }


}
