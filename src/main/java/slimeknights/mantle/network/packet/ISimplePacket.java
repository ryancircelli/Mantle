package slimeknights.mantle.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * A packet that knows how to write itself to a buffer.
 * <p>
 * This is the half of the packet API that sending needs, which is why
 * {@link slimeknights.mantle.util.JsonHelper#syncPackets} and the sending helpers on
 * {@link slimeknights.mantle.network.NetworkWrapper} take it. {@link IPacket} adds the receiving half and is what a
 * packet class should implement; this interface only exists separately so a caller that merely sends a packet does not
 * have to name the handling contract.
 * <p>
 * The decoding half is not on this interface: the registry needs a decoder before an instance exists, so it is passed
 * at registration instead.
 */
public interface ISimplePacket {
  /**
   * Encodes a packet for the buffer.
   * @param buf  Buffer instance, carrying the connection's registries as every play payload does
   */
  void encode(RegistryFriendlyByteBuf buf);
}
