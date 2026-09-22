package br.imd.ufrn.lab.gateway;

import br.imd.ufrn.lab.model.InstanceInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class UdpForwarder {

    private final int timeoutMillis;

    public UdpForwarder(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public String forward(InstanceInfo target, String line) throws IOException {
        byte[] requestBytes = line.getBytes(StandardCharsets.UTF_8);
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeoutMillis);

            InetAddress address = InetAddress.getByName(target.getHost());
            DatagramPacket request = new DatagramPacket(
                    requestBytes, requestBytes.length, address, target.getPort());
            socket.send(request);

            byte[] buffer = new byte[2048];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(response);
            } catch (SocketTimeoutException e) {
                throw new IOException("UDP timeout waiting for " + target, e);
            }

            return new String(
                    response.getData(), response.getOffset(), response.getLength(), StandardCharsets.UTF_8)
                    .trim();
        }
    }
}
