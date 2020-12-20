package xyz.acrylicstyle.packetListener.packet;

public interface CancellablePacket extends Packet {
    /**
     * Returns whether the packet was cancelled for handling.
     * @return whether cancelled or not
     */
    boolean isCancelled();

    /**
     * Sets whether the packet will be cancelled for handling.
     * @param cancelled whether to cancel or not
     */
    void setCancelled(boolean cancelled);
}
