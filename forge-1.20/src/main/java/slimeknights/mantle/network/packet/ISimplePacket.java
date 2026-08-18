package slimeknights.mantle.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet interface to add common methods for registration.
 * <p>
 * Handling a packet through this interface requires a live network event context, which means the handler cannot run
 * outside a connection. {@link IPacket} handles itself through {@link PacketContext} instead and is the preferred
 * interface for new packets; it extends this one, so nothing that consumes an {@link ISimplePacket} needs to change.
 */
public interface ISimplePacket {
  /**
   * Encodes a packet for the buffer
   * @param buf  Buffer instance
   */
  void encode(FriendlyByteBuf buf);

  /**
   * Handles receiving the packet
   * @param context  Packet context
   */
  void handle(Supplier<NetworkEvent.Context> context);
}
