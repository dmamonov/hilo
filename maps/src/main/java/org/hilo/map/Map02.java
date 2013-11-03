package org.hilo.map;

import com.google.common.collect.DiscreteDomains;
import com.google.common.collect.Ranges;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.sshd.SshServer;
import org.hilo.core.Main;
import org.hilo.core.engine.Game;
import org.hilo.core.engine.HiloModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dmitry.mamonov
 *         Created: 11/2/13 10:18 PM
 */
public class Map02 {
    public static void main(final String[] args) throws IOException {
        final Injector injector = Guice.createInjector(new HiloModule(), new Module() {
            @Override
            public void configure(final Binder binder) {
                binder.bind(Game.class).to(ThisGame.class).asEagerSingleton();
            }
        });
        final Game game = injector.getInstance(Game.class);
        final SshServer ssh = injector.getInstance(SshServer.class);
        ssh.start();
        Main.debugWithPutty();
        game.loop(70L);
    }

    private static class ThisGame extends Game {
        @Override
        protected void load() {
            final List<String> mapLines = new ArrayList<>();
            int width = 0;
            int height = 0;
            try {
                final HSSFWorkbook workbook = new HSSFWorkbook(ThisGame.class.getResourceAsStream("/com/hilo/map/map-template.xls"));
                final HSSFSheet sheet = workbook.getSheetAt(0);
                boolean first = true;
                for (final Row row : sheet) {
                    if (first) {
                        width = (int) row.getCell(0).getNumericCellValue();
                        height = (int) row.getCell(1).getNumericCellValue();
                        first = false;
                    } else {
                        final StringBuilder mapRow = new StringBuilder();
                        for (final Integer cellNumber: Ranges.closedOpen(0,width).asSet(DiscreteDomains.integers())) {
                            final Cell cell = row.getCell(cellNumber);
                            final String cellValue = cell!=null?cell.getStringCellValue():" ";
                            if (cellValue.length()>0) {
                                mapRow.append(cellValue.substring(0, 1));
                            } else {
                                mapRow.append(" ");
                            }
                        }
                        mapLines.add(mapRow.toString());
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            map.init(width, mapLines);
        }
    }

}
