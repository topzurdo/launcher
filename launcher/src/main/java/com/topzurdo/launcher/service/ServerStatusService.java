package com.topzurdo.launcher.service;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    public void shutdown() {
        executor.shutdown();
    }

    public void checkStatusAsync(String host, int port, Consumer<ServerStatus> callback) {
        executor.execute(() -> {
            try {
                ServerStatus status = pingServer(host, port);
                Platform.runLater(() -> callback.accept(status));
            } catch (Exception e) {
                LOG.debug("Server check failed: {}", e.getMessage());
                Platform.runLater(() -> callback.accept(new ServerStatus(false, 0, 0, -1, host, null, false)));
            }
        });
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
                Platform.runLater(() -> statusCallback.accept(new ServerStatus(false, 0, 0, -1, serverAddress, null, false)));
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

            // Read response: VarInt length, then packet (VarInt id, VarInt jsonLen, JSON)
            int packetLength = readVarInt(in);
            if (packetLength <= 0 || packetLength > 0x100000) {
                return new ServerStatus(true, 0, 0, (int) ping, address, null, false);
            }
            byte[] packet = new byte[packetLength];
            in.readFully(packet);
            String motd = parseMotdFromPacket(packet);
            int players = 0, maxPlayers = 0;
            try {
                JsonObject root = JsonParser.parseString(motd).getAsJsonObject();
                if (root.has("players")) {
                    JsonObject p = root.getAsJsonObject("players");
                    if (p.has("online")) players = p.get("online").getAsInt();
                    if (p.has("max")) maxPlayers = p.get("max").getAsInt();
                }
            } catch (Exception ignored) { }
            boolean recentWipe = isRecentWipeInMotd(motd);
            return new ServerStatus(true, players, maxPlayers, (int) ping, address, motd, recentWipe);
        }
    }

    private String parseMotdFromPacket(byte[] packet) {
        int[] offset = { 0 };
        readVarIntFromBytes(packet, offset); // packet id
        int jsonLen = readVarIntFromBytes(packet, offset);
        if (offset[0] + jsonLen > packet.length) return "{}";
        return new String(packet, offset[0], jsonLen, StandardCharsets.UTF_8);
    }

    private int readVarIntFromBytes(byte[] data, int[] offset) {
        int value = 0, shift = 0;
        byte b;
        do {
            if (offset[0] >= data.length) return 0;
            b = data[offset[0]++];
            value |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return value;
    }

    /** Определяет по MOTD, что был недавний вайп (Wipe: / Вайп / wipe). */
    private static boolean isRecentWipeInMotd(String motd) {
        if (motd == null || motd.isEmpty()) return false;
        String lower = motd.toLowerCase();
        return lower.contains("wipe") || lower.contains("вайп") || lower.contains("вайп:");
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
        private final String host;
        private final String motdRaw;
        private final boolean recentWipe;

        public ServerStatus(boolean online, int players, int maxPlayers, int ping) {
            this(online, players, maxPlayers, ping, null, null, false);
        }

        public ServerStatus(boolean online, int players, int maxPlayers, int ping, String host) {
            this(online, players, maxPlayers, ping, host, null, false);
        }

        public ServerStatus(boolean online, int players, int maxPlayers, int ping, String host, String motdRaw, boolean recentWipe) {
            this.online = online;
            this.players = players;
            this.maxPlayers = maxPlayers;
            this.ping = ping;
            this.host = host;
            this.motdRaw = motdRaw;
            this.recentWipe = recentWipe;
        }

        public boolean hasError() { return !online; }
        public boolean isOnline() { return online; }
        public String getHost() { return host; }
        public String getMotdRaw() { return motdRaw; }
        public boolean isRecentWipe() { return recentWipe; }
    }
}
