package slimeknights.mantle.network.packet;

/**
 * Packet which handles itself through {@link PacketContext}, the interface every packet class should implement.
 * <p>
 * It extends {@link ISimplePacket} so a packet is accepted anywhere only the write half is needed, notably
 * {@link slimeknights.mantle.util.JsonHelper#syncPackets} and the sending helpers on
 * {@link slimeknights.mantle.network.NetworkWrapper}.
 * <p>
 * A packet does not name its own identity: the identifier passed at registration is what becomes the payload type on
 * the wire. See {@link slimeknights.mantle.network.PacketPayload} for why that is deliberate.
 */
public interface IPacket extends ISimplePacket {
  /**
   * Handles receiving the packet.
   * @param context  Packet context
   */
  void handle(PacketContext context);

  /**
   * Packet which wraps its logic in {@link PacketContext#enqueueWork(Runnable)} so it always runs on the receiving
   * side's main thread, whichever thread the channel chose to decode on.
   */
  interface Threadsafe extends IPacket {
    @Override
    default void handle(PacketContext context) {
      context.enqueueWork(() -> handleThreadsafe(context));
    }

    /**
     * Handles receiving the packet on the main thread.
     * @param context  Packet context
     */
    void handleThreadsafe(PacketContext context);
  }
}
