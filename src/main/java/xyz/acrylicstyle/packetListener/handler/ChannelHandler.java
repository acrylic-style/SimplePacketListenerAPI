package xyz.acrylicstyle.packetListener.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.bukkit.entity.Player;
import xyz.acrylicstyle.packetListener.SimplePacketListenerPlugin;
import xyz.acrylicstyle.packetListener.packet.ReceivedPacket;
import xyz.acrylicstyle.packetListener.packet.SentPacket;

public class ChannelHandler extends ChannelDuplexHandler {
    private final Player player;

    public ChannelHandler(Player player) { this.player = player; }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ReceivedPacket packet = new ReceivedPacket(player, msg);
        SimplePacketListenerPlugin.globalReceivedPacketHandlers.forEach(handler -> handler.handle(packet));
        SimplePacketListenerPlugin.receivedPacketHandlers.get(player.getUniqueId()).forEach(handler -> handler.handle(packet));
        if (!packet.isCancelled()) {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        SentPacket packet = new SentPacket(player, msg);
        SimplePacketListenerPlugin.globalSentPacketHandlers.forEach(handler -> handler.handle(packet));
        SimplePacketListenerPlugin.sentPacketHandlers.get(player.getUniqueId()).forEach(handler -> handler.handle(packet));
        if (!packet.isCancelled()) {
            super.write(ctx, msg, promise);
        }
    }
}
