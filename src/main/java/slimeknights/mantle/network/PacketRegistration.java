package slimeknights.mantle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import slimeknights.mantle.network.packet.PacketContext;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Everything a channel knows about one packet: its identity, the pair of functions that move it across the wire, and
 * the logic that runs when it arrives.
 * <p>
 * Bundling these means a packet cannot be half registered. The {@link #id()} is what makes a registration checkable at
 * mod init; it is not written to the wire, see {@link PacketRegistry}.
 * @param id         Unique identifier of this packet within its channel
 * @param type       Packet class, used to look up the encoder when sending
 * @param encoder    Writes a packet to the buffer
 * @param decoder    Reads a packet from the buffer, typically the constructor
 * @param handler    Logic to run when the packet is received
 * @param direction  Direction this packet is allowed to travel, or null to allow both
 * @param <P>  Packet type
 */
public record PacketRegistration<P>(ResourceLocation id, Class<P> type,
                                    BiConsumer<P,FriendlyByteBuf> encoder,
                                    Function<FriendlyByteBuf,P> decoder,
                                    BiConsumer<P,PacketContext> handler,
                                    @Nullable NetworkDirection direction) {
  /**
   * Encodes the given packet, checking it against this registration's type.
   * @param packet  Packet to encode
   * @param buffer  Buffer to write to
   */
  public void encode(Object packet, FriendlyByteBuf buffer) {
    encoder.accept(type.cast(packet), buffer);
  }

  /**
   * Handles the given packet, checking it against this registration's type.
   * @param packet   Packet to handle
   * @param context  Context of the connection that delivered it
   */
  public void handle(Object packet, PacketContext context) {
    handler.accept(type.cast(packet), context);
  }
}
