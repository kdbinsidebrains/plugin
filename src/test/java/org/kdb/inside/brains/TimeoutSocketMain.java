package org.kdb.inside.brains;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TimeoutSocketMain {
    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket ss = new ServerSocket(9000);
        System.out.println("Started: " + ss);
        while (!Thread.currentThread().isInterrupted()) {
            final Socket accept = ss.accept();
            System.out.println("Accepted: " + accept);
        }
    }
}
