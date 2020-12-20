package xyz.acrylicstyle.packetListener.packet;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import util.Validate;

public class ReceivedPacket implements CancellablePacket {
    private boolean cancelled = false;
    private final Player player;
    private final Object packet;
    private final String packetName;

    public ReceivedPacket(@NotNull Player player, @NotNull Object packet) {
        Validate.notNull(player, "player cannot be null");
        Validate.notNull(packet, "packet cannot be null");
        this.player = player;
        this.packet = packet;
        this.packetName = packet.getClass().getSimpleName();
    }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public @NotNull Player getPlayer() { return player; }

    @Override
    public @NotNull Object getPacket() {
        return packet;
    }

    @Override
    public @NotNull String getPacketName() {
        return packetName;
    }
}
