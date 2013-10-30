package ldrs.core;

import java.io.IOException;

/**
 * @author dmitry.mamonov
 *         Created: 10/27/13 5:19 PM
 */
public class MainLauncher {
    public static void main(String[] args) throws IOException {
        Runtime.getRuntime().exec("cmd.exe /C start mvn -Dexec.mainClass=ldrs.core.Main exec:java");
    }
}
