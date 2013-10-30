package org.hilo.core;

import com.google.common.base.Charsets;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author dmitry.mamonov
 *         Created: 10/30/13 1:37 PM
 */
public class MainSsh2 {
    public static void main(String[] args) throws IOException {
        final ServerSocket server = new ServerSocket(22);
        System.out.println("Server started, listening..");
        while (true) {
            final Socket socket = server.accept();
            System.out.println("Connection accepted");
            new Thread() {
                @Override
                public void run() {
                    try {
                        final InputStream in = socket.getInputStream();
                        while (true) {
                            final int ch = in.read();
                            if (ch<0){
                                break;
                            }
                            System.out.println("Command: " + (char) ch);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
            new Thread() {
                @Override
                public void run() {
                    try {
                        final OutputStream out = socket.getOutputStream();
                        while (true) {
                            out.write((Ansi.Color.CYAN + "Hello\n").getBytes(Charsets.UTF_8));
                            Thread.sleep(3000L);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }
}
