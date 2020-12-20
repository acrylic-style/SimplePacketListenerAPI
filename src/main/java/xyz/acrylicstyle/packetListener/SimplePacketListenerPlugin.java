package xyz.acrylicstyle.packetListener;

import io.netty.channel.Channel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import util.Collection;
import util.CollectionList;
import util.reflector.Reflector;
import xyz.acrylicstyle.nmsapi.NMSAPI;
import xyz.acrylicstyle.packetListener.handler.ChannelHandler;
import xyz.acrylicstyle.packetListener.packet.ReceivedPacketHandler;
import xyz.acrylicstyle.packetListener.packet.SentPacketHandler;

import java.util.UUID;

public class SimplePacketListenerPlugin extends JavaPlugin implements SimplePacketListenerAPI, Listener {
    private static SimplePacketListenerPlugin plugin;
    public static final CollectionList<SentPacketHandler> globalSentPacketHandlers = new CollectionList<>();
    public static final CollectionList<ReceivedPacketHandler> globalReceivedPacketHandlers = new CollectionList<>();
    public static final Collection<UUID, CollectionList<SentPacketHandler>> sentPacketHandlers = new Collection<>();
    public static final Collection<UUID, CollectionList<ReceivedPacketHandler>> receivedPacketHandlers = new Collection<>();

    @Deprecated
    public SimplePacketListenerPlugin() { SimplePacketListenerPlugin.plugin = this; }

    @Deprecated
    @Override
    public void onLoad() {
        Reflector.classLoader = this.getClassLoader();
    }

    @Deprecated
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getOnlinePlayers().forEach(this::inject);
    }

    @Deprecated
    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(this::eject);
    }

    @Contract(pure = true)
    @NotNull
    public static SimplePacketListenerPlugin getPlugin() { return plugin; }

    @Deprecated
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        inject(e.getPlayer());
    }

    @Deprecated
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        eject(e.getPlayer());
    }

    @Override
    public void inject(@NotNull Player player) {
        sentPacketHandlers.add(player.getUniqueId(), new CollectionList<>());
        receivedPacketHandlers.add(player.getUniqueId(), new CollectionList<>());
        Channel channel = NMSAPI.getCraftPlayer(player).getHandle().getPlayerConnection().getNetworkManager().getChannel();
        ChannelHandler handler = new ChannelHandler(player);
        channel.pipeline().addBefore("packet_handler", "SimplePacketListenerAPI", handler);
    }

    @Override
    public void eject(@NotNull Player player) {
        Channel channel = NMSAPI.getCraftPlayer(player).getHandle().getPlayerConnection().getNetworkManager().getChannel();
        if (channel.pipeline().get(ChannelHandler.class) != null) {
            channel.pipeline().remove(ChannelHandler.class);
        }
    }
}
