package xyz.acrylicstyle.packetListener;

import io.netty.channel.Channel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import xyz.acrylicstyle.packetListener.handler.ChannelHandler;
import xyz.acrylicstyle.packetListener.packet.ReceivedPacketHandler;
import xyz.acrylicstyle.packetListener.packet.SentPacketHandler;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimplePacketListenerPlugin extends JavaPlugin implements SimplePacketListenerAPI, Listener {
    private static SimplePacketListenerPlugin plugin;
    static final Map<SentPacketHandler, Plugin> sentPacketHandlerOwnerMap = new HashMap<>();
    static final Map<ReceivedPacketHandler, Plugin> receivedPacketHandlerOwnerMap = new HashMap<>();
    public static final List<SentPacketHandler> globalSentPacketHandlers = new ArrayList<>();
    public static final List<ReceivedPacketHandler> globalReceivedPacketHandlers = new ArrayList<>();
    public static final Map<UUID, List<SentPacketHandler>> sentPacketHandlers = new HashMap<>();
    public static final Map<UUID, List<ReceivedPacketHandler>> receivedPacketHandlers = new HashMap<>();

    @Deprecated
    public SimplePacketListenerPlugin() { SimplePacketListenerPlugin.plugin = this; }

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

    @Deprecated
    @EventHandler
    public void onPluginDisable(PluginDisableEvent e) {
        //getLogger().info("[SimplePacketListenerAPI] Unregistering packet handlers for " + e.getPlugin().getName());
        Plugin p = e.getPlugin();
        List<SentPacketHandler> sentPacketHandlersToRemove = new ArrayList<>();
        sentPacketHandlerOwnerMap.forEach((handler, plugin) -> {
            if (plugin.equals(p)) {
                sentPacketHandlers.forEach((uuid, handlers) -> handlers.forEach(h -> {
                    if (h.equals(handler)) handlers.remove(h);
                }));
                globalSentPacketHandlers.remove(handler);
            }
            sentPacketHandlersToRemove.add(handler);
        });
        sentPacketHandlersToRemove.forEach(sentPacketHandlerOwnerMap::remove);
        List<ReceivedPacketHandler> receivedPacketHandlersToRemove = new ArrayList<>();
        receivedPacketHandlerOwnerMap.forEach((handler, plugin) -> {
            if (plugin.equals(p)) {
                receivedPacketHandlers.forEach((uuid, handlers) -> handlers.forEach(h -> {
                    if (h.equals(handler)) handlers.remove(h);
                }));
                globalReceivedPacketHandlers.remove(handler);
            }
            receivedPacketHandlersToRemove.add(handler);
        });
        receivedPacketHandlersToRemove.forEach(receivedPacketHandlerOwnerMap::remove);
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
        sentPacketHandlers.put(player.getUniqueId(), new ArrayList<>());
        receivedPacketHandlers.put(player.getUniqueId(), new ArrayList<>());
        Channel channel = getChannel(player);
        ChannelHandler handler = new ChannelHandler(player);
        try {
            channel.pipeline().addBefore("packet_handler", "SimplePacketListenerAPI", handler);
        } catch (NoSuchElementException ex) {
            Bukkit.getScheduler().runTaskLater(this, () -> channel.pipeline().addBefore("packet_handler", "SimplePacketListenerAPI", handler), 1);
        }
    }

    @Override
    public void eject(@NotNull Player player) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            executorService.submit(() -> {
                Channel channel = getChannel(player);
                if (channel.pipeline().get(ChannelHandler.class) != null) {
                    channel.pipeline().remove(ChannelHandler.class);
                }
            }).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            executorService.shutdownNow();
            throw new RuntimeException("Could not remove channel handler within 3 seconds for player " + player.getName(), e);
        }
    }

    @Contract(pure = true)
    @NotNull
    private static Channel getChannel(@NotNull Player player) {
        try {
            Object ep = player.getClass().getDeclaredMethod("getHandle").invoke(player);
            Object pc = getField(ep, "playerConnection");
            Object nm = getField(pc, "networkManager");
            return (Channel) getField(nm, "channel");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Contract(pure = true)
    private static Object getField(Object obj, String f) throws ReflectiveOperationException {
        Field field = obj.getClass().getDeclaredField(f);
        field.setAccessible(true);
        return field.get(obj);
    }
}
