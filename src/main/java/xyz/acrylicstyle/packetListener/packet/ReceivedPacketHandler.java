package xyz.acrylicstyle.packetListener.packet;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ReceivedPacketHandler {
    /**
     * Handles the received packet.
     * @param packet the packet being received
     */
    void handle(@NotNull ReceivedPacket packet);
}
