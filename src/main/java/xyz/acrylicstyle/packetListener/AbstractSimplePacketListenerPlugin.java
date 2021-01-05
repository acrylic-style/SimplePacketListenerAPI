package xyz.acrylicstyle.packetListener;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.acrylicstyle.packetListener.netty.NettyHAProxyChannelInitializer;
import xyz.acrylicstyle.packetListener.netty.NettyVanillaChannelInitializer;
import xyz.acrylicstyle.packetListener.util.LazyInitVar;
import xyz.acrylicstyle.packetListener.util.ReflectionUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

// Adds HAProxy related methods
public abstract class AbstractSimplePacketListenerPlugin extends JavaPlugin {
    public static final Class<?> MinecraftServer;
    public static final Class<?> ServerConnection;
    public static final Class<?> NetworkManager;
    public static final Method MinecraftServer_getServer;
    public static final Method MinecraftServer_getServerConnection;

    static {
        Class<?> class1 = null;
        Class<?> class2 = null;
        Class<?> class3 = null;
        Method method1 = null;
        Method method2 = null;
        try {
            class1 = Class.forName(ReflectionUtil.nms("MinecraftServer"));
            class2 = Class.forName(ReflectionUtil.nms("ServerConnection"));
            class3 = Class.forName(ReflectionUtil.nms("NetworkManager"));
            method1 = class1.getDeclaredMethod("getServer");
            method2 = class1.getDeclaredMethod("getServerConnection");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        MinecraftServer = class1;
        ServerConnection = class2;
        NetworkManager = class3;
        MinecraftServer_getServer = method1;
        MinecraftServer_getServerConnection = method2;
    }

    public final HAProxyConfiguration haProxyConfiguration;

    {
        HAProxyConfiguration haProxyConfiguration1;
        File haproxyConfigurationFile = new File("./plugins/SimplePacketListenerAPI/haproxy.yml");
        getLogger().info("Loading HAProxy configuration file " + haproxyConfigurationFile.getAbsolutePath());
        try {
            haProxyConfiguration1 = new HAProxyConfiguration(getLogger(), YamlConfiguration.loadConfiguration(new FileReader(haproxyConfigurationFile)));
        } catch (FileNotFoundException e) {
            haProxyConfiguration1 = new HAProxyConfiguration(getLogger(), new YamlConfiguration());
        }
        haProxyConfiguration = haProxyConfiguration1;
        if (haProxyConfiguration.proxyProtocol) {
            boolean alreadyLoaded = false;
            if (Bukkit.getOnlinePlayers().size() > 1) alreadyLoaded = true;
            // SimplePacketListenerAPI loads BEFORE ViaVersion, so ViaVersion cannot be enabled before us.
            if (Bukkit.getPluginManager().isPluginEnabled("ViaVersion")) alreadyLoaded = true;
            // If the reload count is > 0, then it is absolutely a reload or the server is already loaded.
            if (ReflectionUtil.getReloadCount() > 0) alreadyLoaded = true;
            if (alreadyLoaded) {
                getLogger().severe("Server is already loaded! Disabling HAProxy feature.");
                getLogger().severe("Please reboot the server to fix this.");
            } else {
                setupHAProxy();
            }
        }
    }

    void setupHAProxy() {
        if (haProxyConfiguration.port == -1) {
            injectHAProxyHandler();
        } else {
            addHAProxyListener();
        }
    }

    protected Channel channel;

    @SuppressWarnings("unchecked")
    void addHAProxyListener() {
        getLogger().info("Initializing HAProxy listener on port " + haProxyConfiguration.port);
        try {
            Object minecraftServer = MinecraftServer_getServer.invoke(null);
            Object serverConnection = MinecraftServer_getServerConnection.invoke(minecraftServer);
            Field field = ReflectionUtil.getListeningChannelField();
            field.setAccessible(true);
            List<ChannelFuture> futures = (List<ChannelFuture>) field.get(serverConnection);
            Class<? extends ServerSocketChannel> clazz;
            LazyInitVar<? extends EventLoopGroup> lazyInitVar;
            if (Epoll.isAvailable() && haProxyConfiguration.epoll) {
                clazz = EpollServerSocketChannel.class;
                lazyInitVar = ReflectionUtil.getEpollEventLoopGroup();
                getLogger().info("Using epoll channel type for " + haProxyConfiguration.port + ": " + Objects.requireNonNull(lazyInitVar).get());
            } else {
                clazz = NioServerSocketChannel.class;
                lazyInitVar = ReflectionUtil.getNioEventLoopGroup();
                getLogger().info("Using default channel type for " + haProxyConfiguration.port + ": " + lazyInitVar.get());
            }
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (futures) {
                ChannelInitializer<SocketChannel> vanillaInitializer;
                if (haProxyConfiguration.experimental_useReflectionChannelInitializer) {
                    vanillaInitializer = new NettyVanillaChannelInitializer(minecraftServer, serverConnection);
                } else {
                    Channel channel = futures.get(0).channel();
                    ChannelPipeline pipeline = channel.pipeline();
                    io.netty.channel.ChannelHandler handler = pipeline.first();
                    vanillaInitializer = (ChannelInitializer<SocketChannel>) ReflectionUtil.getFieldWithoutException(handler.getClass(), handler, "childHandler");
                }
                ChannelFuture future = new ServerBootstrap().channel(clazz)
                        .childHandler(new NettyHAProxyChannelInitializer(vanillaInitializer))
                        .group(lazyInitVar.get())
                        .localAddress(haProxyConfiguration.serverIp, haProxyConfiguration.port)
                        .option(ChannelOption.AUTO_READ, true)
                        .bind()
                        .syncUninterruptibly();
                futures.add(future);
                this.channel = future.channel();
            }
            getLogger().info("Initialized HAProxy listener " + this.channel + ". Please ensure this listener is properly firewalled.");
        } catch (ReflectiveOperationException ex) {
            getLogger().warning("Failed to initialize HAProxy listener");
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    void injectHAProxyHandler() {
        try {
            Object minecraftServer = MinecraftServer_getServer.invoke(null);
            Object serverConnection = MinecraftServer_getServerConnection.invoke(minecraftServer);
            Field field = ReflectionUtil.getListeningChannelField();
            field.setAccessible(true);
            List<ChannelFuture> futures = (List<ChannelFuture>) field.get(serverConnection);
            futures.forEach(future -> {
                Channel channel = future.channel();
                ChannelPipeline pipeline = channel.pipeline();
                io.netty.channel.ChannelHandler handler = pipeline.first();
                ChannelInitializer<SocketChannel> vanillaInitializer = (ChannelInitializer<SocketChannel>) ReflectionUtil.getFieldWithoutException(handler.getClass(), handler, "childHandler");
                ReflectionUtil.setFieldWithoutException(handler.getClass(), handler, "childHandler", new NettyHAProxyChannelInitializer(vanillaInitializer));
                getLogger().info("Initialized HAProxy support for " + channel + ". Please ensure this port is properly firewalled.");
            });
        } catch (ReflectiveOperationException ex) {
            getLogger().warning("Failed to initialize HAProxy support");
            ex.printStackTrace();
        }
    }
}
