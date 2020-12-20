package xyz.acrylicstyle.packetListener.packet;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface Packet {
    /**
     * @return the player
     */
    @NotNull
    Player getPlayer();

    /**
     * @return the packet, it can be cast to nms classes.
     */
    @NotNull
    Object getPacket();

    /**
     * @return the packet name
     */
    @NotNull
    String getPacketName();
}
