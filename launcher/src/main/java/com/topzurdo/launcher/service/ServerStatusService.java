package com.topzurdo.launcher.service;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service for checking Minecraft server status
 */
public class ServerStatusService {

    private static final Logger LOG = LoggerFactory.getLogger(ServerStatusService.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ServerStatusService");
        t.setDaemon(true);
        return t;
    });

    private String serverAddress = "funtime.su";
    private int serverPort = 25565;
    private Consumer<ServerStatus> statusCallback;

    public void setServer(String address, int port) {
        this.serverAddress = address;
        this.serverPort = port;
    }

    public void setStatusCallback(Consumer<ServerStatus> callback) {
        this.statusCallback = callback;
    }

    public void start() {
        executor.scheduleAtFixedRate(this::checkStatus, 0, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdown();
    }

    private void checkStatus() {
        try {
            ServerStatus status = pingServer(serverAddress, serverPort);
            if (statusCallback != null) {
                Platform.runLater(() -> statusCallback.accept(status));
            }
        } catch (Exception e) {
            LOG.debug("Server ping failed: {}", e.getMessage());
            if (statusCallback != null) {
                Platform.runLater(() -> statusCallback.accept(new ServerStatus(false, 0, 0, -1)));
            }
        }
    }

    private ServerStatus pingServer(String address, int port) throws IOException {
        try (Socket socket = new Socket()) {
            long start = System.currentTimeMillis();
            socket.connect(new InetSocketAddress(address, port), 5000);
            long ping = System.currentTimeMillis() - start;

            // Basic handshake
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Send handshake
            writeVarInt(out, 0x00);
            writeVarInt(out, 47); // Protocol version
            writeString(out, address);
            out.writeShort(port);
            writeVarInt(out, 1); // Next state: status

            // Send status request
            writeVarInt(out, 0x00);

            // Read response (simplified)
            int length = readVarInt(in);
            if (length > 0) {
                return new ServerStatus(true, 0, 100, (int) ping);
            }

            return new ServerStatus(true, 0, 0, (int) ping);
        }
    }

    private void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private void writeString(DataOutputStream out, String str) throws IOException {
        byte[] bytes = str.getBytes();
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int shift = 0;
        byte b;
        do {
            b = in.readByte();
            value |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return value;
    }

    public static class ServerStatus {
        public final boolean online;
        public final int players;
        public final int maxPlayers;
        public final int ping;

        public ServerStatus(boolean online, int players, int maxPlayers, int ping) {
            this.online = online;
            this.players = players;
            this.maxPlayers = maxPlayers;
            this.ping = ping;
        }
    }
}
