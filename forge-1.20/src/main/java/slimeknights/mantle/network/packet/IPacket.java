package slimeknights.mantle.network.packet;

import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet which handles itself through {@link PacketContext} rather than the network event context.
 * <p>
 * This is the interface new packets should implement. It extends {@link ISimplePacket} so an existing packet can be
 * converted one class at a time: the registration call, the sending helpers, and
 * {@link slimeknights.mantle.util.JsonHelper#syncPackets} all keep working untouched, and the inherited
 * {@link #handle(Supplier)} bridges the network event context back to a {@link PacketContext} for any caller still on
 * the old entry point.
 */
public interface IPacket extends ISimplePacket {
  /**
   * Handles receiving the packet.
   * @param context  Packet context
   */
  void handle(PacketContext context);

  @Override
  default void handle(Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    handle(new ForgePacketContext(context));
    context.setPacketHandled(true);
  }

  /**
   * Packet which automatically wraps its logic in {@link PacketContext#enqueueWork(Runnable)} for thread safety.
   * The {@link PacketContext} counterpart of {@link IThreadsafePacket}.
   */
  interface Threadsafe extends IPacket {
    @Override
    default void handle(PacketContext context) {
      context.enqueueWork(() -> handleThreadsafe(context));
    }

    /**
     * Handles receiving the packet on the correct thread.
     * @param context  Packet context
     */
    void handleThreadsafe(PacketContext context);
  }
}
