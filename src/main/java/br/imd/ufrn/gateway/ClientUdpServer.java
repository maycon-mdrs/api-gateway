package br.imd.ufrn.gateway;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class ClientUdpServer implements Runnable {

    private final int port;
    private final TimeRequestHandler handler;

    public ClientUdpServer(int port, TimeRequestHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("[lab-udp] ouvindo UDP " + port);
            byte[] buffer = new byte[2048];
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                String line = new String(
                        request.getData(), request.getOffset(), request.getLength(), StandardCharsets.UTF_8);
                InetAddress clientAddress = request.getAddress();
                int clientPort = request.getPort();

                String response = handler.handleLine(line, TransportProtocol.UDP);
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                DatagramPacket responsePacket = new DatagramPacket(
                        bytes, bytes.length, clientAddress, clientPort);
                socket.send(responsePacket);
            }
        } catch (IOException e) {
            System.err.println("[lab-udp] erro: " + e.getMessage());
        }
    }
}
