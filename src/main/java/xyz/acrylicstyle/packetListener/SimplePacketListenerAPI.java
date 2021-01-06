package xyz.acrylicstyle.packetListener;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import xyz.acrylicstyle.packetListener.packet.ReceivedPacketHandler;
import xyz.acrylicstyle.packetListener.packet.SentPacketHandler;

public interface SimplePacketListenerAPI {
    @Contract(pure = true)
    static @NotNull SimplePacketListenerAPI getInstance() { return SimplePacketListenerPlugin.getPlugin(); }

    /**
     * Injects our packet handler to player.
     * @param player the player
     */
    void inject(@NotNull Player player);

    /**
     * Removes our packet handler from player.
     * @param player the player
     */
    void eject(@NotNull Player player);

    /**
     * Adds packet handler that fires when the server receives the packet. The packet handler will be removed after
     * [plugin] is unloaded.
     * @param plugin the plugin
     * @param handler the packet handler
     */
    static void addReceivedPacketHandler(@NotNull Plugin plugin, @NotNull ReceivedPacketHandler handler) {
        Validate.notNull(plugin, "plugin cannot be null");
        Validate.notNull(handler, "handler cannot be null");
        SimplePacketListenerPlugin.receivedPacketHandlerOwnerMap.put(handler, plugin);
        SimplePacketListenerPlugin.globalReceivedPacketHandlers.add(handler);
    }

    /**
     * Removes packet handler, your packet handler would normally be unregistered when your plugin gets disabled,
     * so use this only if you want to remove packet handler manually.
     * @param handler packet handler to remove
     */
    static void removeReceivedPacketHandler(@NotNull ReceivedPacketHandler handler) {
        SimplePacketListenerPlugin.globalReceivedPacketHandlers.remove(handler);
    }

    /**
     * Adds packet handler that fires when the server is going to send the packet. The packet handler will be removed
     * after [plugin] is unloaded.
     * @param plugin the plugin
     * @param handler the packet handler
     */
    static void addSentPacketHandler(@NotNull Plugin plugin, @NotNull SentPacketHandler handler) {
        Validate.notNull(plugin, "plugin cannot be null");
        Validate.notNull(handler, "handler cannot be null");
        SimplePacketListenerPlugin.sentPacketHandlerOwnerMap.put(handler, plugin);
        SimplePacketListenerPlugin.globalSentPacketHandlers.add(handler);
    }

    /**
     * Removes packet handler, your packet handler would normally be unregistered when your plugin gets disabled,
     * so use this only if you want to remove packet handler manually.
     * @param handler packet handler to remove
     */
    static void removeSentPacketHandler(@NotNull SentPacketHandler handler) {
        SimplePacketListenerPlugin.globalSentPacketHandlers.remove(handler);
    }

    /**
     * Adds packet handler that fires for the only a specific player when the server receives the packet. The packet
     * handler will be removed after [plugin] is unloaded.
     * @param player the player
     * @param plugin the plugin
     * @param handler the packet handler
     */
    static void addReceivedPacketHandler(@NotNull Player player, @NotNull Plugin plugin, @NotNull ReceivedPacketHandler handler) {
        Validate.notNull(plugin, "plugin cannot be null");
        Validate.notNull(handler, "handler cannot be null");
        SimplePacketListenerPlugin.receivedPacketHandlerOwnerMap.put(handler, plugin);
        SimplePacketListenerPlugin.receivedPacketHandlers.get(player.getUniqueId()).add(handler);
    }

    /**
     * Removes packet handler, your packet handler would normally be unregistered when your plugin gets disabled,
     * so use this only if you want to remove packet handler manually.
     * @param player the player
     * @param handler packet handler to remove
     */
    static void removeReceivedPacketHandler(@NotNull Player player, @NotNull ReceivedPacketHandler handler) {
        SimplePacketListenerPlugin.receivedPacketHandlers.get(player.getUniqueId()).remove(handler);
    }

    /**
     * Adds packet handler that fires for the only a specific player when the server is going to send the packet.
     * The packet handler will be removed after [plugin] is unloaded.
     * @param player the player
     * @param plugin the plugin
     * @param handler the packet handler
     */
    static void addSentPacketHandler(@NotNull Player player, @NotNull Plugin plugin, @NotNull SentPacketHandler handler) {
        Validate.notNull(plugin, "plugin cannot be null");
        Validate.notNull(handler, "handler cannot be null");
        SimplePacketListenerPlugin.sentPacketHandlerOwnerMap.put(handler, plugin);
        SimplePacketListenerPlugin.sentPacketHandlers.get(player.getUniqueId()).add(handler);
    }

    /**
     * Removes packet handler, your packet handler would normally be unregistered when your plugin gets disabled,
     * so use this only if you want to remove packet handler manually.
     * @param player the player
     * @param handler packet handler to remove
     */
    static void removeSentPacketHandler(@NotNull Player player, @NotNull SentPacketHandler handler) {
        SimplePacketListenerPlugin.sentPacketHandlers.get(player.getUniqueId()).remove(handler);
    }
}
