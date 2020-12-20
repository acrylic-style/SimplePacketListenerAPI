package xyz.acrylicstyle.packetListener;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import xyz.acrylicstyle.packetListener.packet.ReceivedPacketHandler;
import xyz.acrylicstyle.packetListener.packet.SentPacketHandler;

public interface SimplePacketListenerAPI {
    @Contract(pure = true)
    static @NotNull SimplePacketListenerAPI getInstance() { return SimplePacketListenerPlugin.getPlugin(); }

    void inject(@NotNull Player player);

    void eject(@NotNull Player player);

    static void addReceivedPacketHandler(@NotNull ReceivedPacketHandler handler) {
        SimplePacketListenerPlugin.globalReceivedPacketHandlers.add(handler);
    }

    static void removeReceivedPacketHandler(@NotNull ReceivedPacketHandler handler) {
        SimplePacketListenerPlugin.globalReceivedPacketHandlers.remove(handler);
    }

    static void addSentPacketHandler(@NotNull SentPacketHandler handler) {
        SimplePacketListenerPlugin.globalSentPacketHandlers.add(handler);
    }

    static void removeSentPacketHandler(@NotNull SentPacketHandler handler) {
        SimplePacketListenerPlugin.globalSentPacketHandlers.remove(handler);
    }

    static void addReceivedPacketHandler(@NotNull Player player, @NotNull ReceivedPacketHandler handler) {
        SimplePacketListenerPlugin.receivedPacketHandlers.get(player.getUniqueId()).add(handler);
    }

    static void removeReceivedPacketHandler(@NotNull Player player, @NotNull ReceivedPacketHandler handler) {
        SimplePacketListenerPlugin.receivedPacketHandlers.get(player.getUniqueId()).remove(handler);
    }

    static void addSentPacketHandler(@NotNull Player player, @NotNull SentPacketHandler handler) {
        SimplePacketListenerPlugin.sentPacketHandlers.get(player.getUniqueId()).add(handler);
    }

    static void removeSentPacketHandler(@NotNull Player player, @NotNull SentPacketHandler handler) {
        SimplePacketListenerPlugin.sentPacketHandlers.get(player.getUniqueId()).remove(handler);
    }
}
