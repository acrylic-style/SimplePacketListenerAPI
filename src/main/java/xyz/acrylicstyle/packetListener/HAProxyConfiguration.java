package xyz.acrylicstyle.packetListener;

import io.netty.channel.epoll.Epoll;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import xyz.acrylicstyle.packetListener.util.ReflectionUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

@SuppressWarnings("unused") // private methods are invoked by reflection
public class HAProxyConfiguration {
    private final Logger logger;
    private final FileConfiguration config;
    public final boolean proxyProtocol;

    public HAProxyConfiguration(Logger logger, FileConfiguration config) {
        this.logger = logger;
        this.config = config;
        this.proxyProtocol = this.config.getBoolean("proxy_protocol", false);
        @SuppressWarnings("StringBufferReplaceableByString")
        StringBuilder sb = new StringBuilder();
        sb.append("Welcome to SimplePacketListenerAPI *HAProxy* configuration file!\n");
        sb.append("There are some things to configure, and I'll explain what are these values and that does that do.\n");
        sb.append("\n");
        sb.append("These settings will affect whether if SimplePacketListenerAPI will try to create an HAProxy listener.\n");
        sb.append("If proxy_protocol is disabled, then SimplePacketListenerAPI will do completely nothing about HAProxy.\n");
        sb.append("Only edit these settings when you are using HAProxy or something.\n");
        sb.append("Also, enabling proxy_protocol might causes ViaVersion to stop working after /reload.\n");
        sb.append("Please restart the server if you see the error from ViaVersion in console.\n");
        sb.append("\n");
        sb.append("Important notes:\n");
        sb.append(" - You cannot load plugin (by PlugMan or something) AFTER the server is fully started.\n");
        sb.append(" - Breaks ViaVersion when you do /reload\n");
        sb.append("\n");
        sb.append("proxy_protocol:\n");
        sb.append("    Sets whether enables PROXY protocol for usage from HAProxy etc.\n");
        sb.append("    **HAProxy features will be disabled if it is set to false.**");
        sb.append("    (Default: false)\n");
        sb.append("server_ip:\n");
        sb.append("    Set specific server IP / Hostname when you have multiple NICs.\n");
        sb.append("    (Requires proxy_protocol to work.)\n");
        sb.append("    (Default: 'null')\n");
        sb.append("port:\n");
        sb.append("    Sets port to listen HAProxy.\n");
        sb.append("    Specify -1 to replace main server port with HAProxy-enabled listener.\n");
        sb.append("    If you specify the port other than -1, then the HAProxy-enabled packet\n");
        sb.append("    listener will be created.\n");
        sb.append("    (Requires proxy_protocol to work.)\n");
        sb.append("    (Default: -1)\n");
        sb.append("epoll:\n");
        sb.append("    Whether to enable epoll on linux servers.\n");
        sb.append("    Epoll enables native enhancements to listener, and it is recommend to enable it.\n");
        sb.append("    Please note that epoll is not available on Windows etc, so epoll will not be used\n");
        sb.append("    and the Netty IO will be used.\n");
        sb.append("    (Requires proxy_protocol to work.)\n");
        sb.append("    (Default: true)\n");
        sb.append("\n");
        sb.append("===== EXPERIMENTAL =====\n");
        sb.append("These settings are experimental and might not work, or may behave weird!\n");
        sb.append("Use at your own risk.\n");
        sb.append("\n");
        sb.append("experimental.useReflectionChannelInitializer:\n");
        sb.append("    Whether to use reflection-based channel initializer when creating HAProxy-enabled\n");
        sb.append("    packet listener.\n");
        sb.append("    This setting has no effect when replacing main server port with HAProxy-enabled listener.\n");
        sb.append("    (Requires proxy_protocol to work.)\n");
        sb.append("    (Default: false)\n");
        config.options().header(sb.toString());
        config.set("proxy_protocol", proxyProtocol);
        logger.info("==================================================");
        logger.info("HAProxy support: " + (proxyProtocol ? "Enabled" : "Disabled"));
        if (this.proxyProtocol) {
            try {
                for (Method method : HAProxyConfiguration.class.getDeclaredMethods()) {
                    if (method.getReturnType() != void.class) continue;
                    if (method.getParameterCount() != 0) continue;
                    if (method.isSynthetic()) continue;
                    method.invoke(this);
                }
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Failed to initialize HAProxyConfiguration", ex);
            }
        } else {
            setDefault("server_ip", "null");
            setDefault("port", -1);
            setDefault("epoll", true);
            setDefault("experimental.useReflectionChannelInitializer", false);
        }
        try {
            config.save("./plugins/SimplePacketListenerAPI/haproxy.yml");
        } catch (IOException e) {
            logger.warning("Failed to save configuration");
            e.printStackTrace();
        }
        logger.info("==================================================");
    }

    private void setDefault(String key, Object value) {
        if (config.get(key) == null) {
            config.set(key, value);
        }
    }

    public InetAddress serverIp = null;

    private void serverIp() {
        String ip = config.getString("server_ip", "null");
        if (ip != null && !ip.equals("null")) {
            try {
                serverIp = InetAddress.getByName(ip);
            } catch (UnknownHostException e) {
                logger.warning("Server IP or Hostname was invalid, using default one");
                ip = "null"; // it will be saved
            }
        }
        config.set("server_ip", ip);
        if (serverIp != null) logger.info("Listening HAProxy server on " + serverIp);
    }

    public int port;

    private void port() {
        port = config.getInt("port", -1);
        if (port != -1 && (port < 0 || port > 65535)) {
            logger.warning("Port is out of range, using default port");
            port = -1; // it will be saved
        }
        config.set("port", port);
        if (port == Bukkit.getPort()) {
            port = -1; // it will not be saved
        }
        logger.info("Port: " + (port == -1 ? "Default (" + Bukkit.getPort() + ")" : port));
    }

    public boolean epoll;

    private void epoll() {
        epoll = config.getBoolean("useEpoll", true);
        config.set("epoll", epoll);
        if (/*ReflectionUtil.getServerVersion().equals("v1_8_R1") || */!Epoll.isAvailable()) {
            String message = "(Not supported: MC 1.8)";
            if (!Epoll.isAvailable()) {
                String exMessage = Epoll.unavailabilityCause().getMessage();
                if (exMessage == null) exMessage = Epoll.unavailabilityCause().getCause().getMessage();
                message = "(Not supported: " + exMessage + ")";
            }
            logger.info("Using default channel type " + message);
            epoll = false; // it will not be saved
        } else {
            logger.info("Using " + (epoll ? "epoll" : "default") + " channel type");
        }
    }

    public boolean experimental_useReflectionChannelInitializer;

    private void experimental_useReflectionChannelInitializer() {
        experimental_useReflectionChannelInitializer = config.getBoolean("experimental.useReflectionChannelInitializer", false);
        config.set("experimental.useReflectionChannelInitializer", experimental_useReflectionChannelInitializer);
        if (experimental_useReflectionChannelInitializer) {
            logger.info("Using experimental NettyVanillaChannelInitializer channel initializer");
        } else {
            logger.info("Using vanilla channel initializer");
        }
    }
}
