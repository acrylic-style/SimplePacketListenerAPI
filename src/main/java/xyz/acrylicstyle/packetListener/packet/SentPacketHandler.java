package xyz.acrylicstyle.packetListener.packet;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface SentPacketHandler {
    /**
     * Handles the sent packet.
     * @param packet the packet being sent
     */
    void handle(@NotNull SentPacket packet);
}
