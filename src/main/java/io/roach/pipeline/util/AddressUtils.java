package io.roach.pipeline.util;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

public abstract class AddressUtils {
    private AddressUtils() {
    }

    public static String getLocalIP() {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (IOException e) {
            return "localhost";
        }
    }
}
