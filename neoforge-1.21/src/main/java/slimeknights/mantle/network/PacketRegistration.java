package slimeknights.mantle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.network.packet.PacketContext;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Everything a channel knows about one packet: its identity, the pair of functions that move it across the wire, and
 * the logic that runs when it arrives.
 * <p>
 * Bundling these means a packet cannot be half registered. The identity is stored as the {@link CustomPacketPayload}
 * type it becomes on the wire, so {@link #id()} and the payload identifier are the same value by construction rather
 * than by two pieces of code agreeing.
 * @param payloadType  Payload type this packet is sent as, wrapping its unique identifier within the channel
 * @param type         Packet class, used to look up the registration when sending
 * @param encoder      Writes a packet to the buffer
 * @param decoder      Reads a packet from the buffer, typically the constructor
 * @param handler      Logic to run when the packet is received
 * @param direction    Direction this packet is allowed to travel, or null to allow both
 * @param <P>  Packet type
 */
public record PacketRegistration<P>(CustomPacketPayload.Type<PacketPayload<P>> payloadType, Class<P> type,
                                    BiConsumer<P,RegistryFriendlyByteBuf> encoder,
                                    Function<RegistryFriendlyByteBuf,P> decoder,
                                    BiConsumer<P,PacketContext> handler,
                                    @Nullable PacketFlow direction) {
  /**
   * Creates a registration from a plain identifier, which is what a registration call site passes.
   * @param id  Unique identifier of this packet within its channel
   */
  public PacketRegistration(ResourceLocation id, Class<P> type,
                            BiConsumer<P,RegistryFriendlyByteBuf> encoder,
                            Function<RegistryFriendlyByteBuf,P> decoder,
                            BiConsumer<P,PacketContext> handler,
                            @Nullable PacketFlow direction) {
    this(new CustomPacketPayload.Type<>(id), type, encoder, decoder, handler, direction);
  }

  /** Gets the unique identifier of this packet within its channel, which is also its identifier on the wire */
  public ResourceLocation id() {
    return payloadType.id();
  }

  /**
   * Wraps the given packet in its payload, checking it against this registration's type.
   * @param packet  Packet to send
   * @return  Payload to hand the loader
   */
  public PacketPayload<P> wrap(Object packet) {
    return new PacketPayload<>(this, type.cast(packet));
  }

  /** Builds the codec the loader uses to move this packet's payload across the wire */
  public StreamCodec<RegistryFriendlyByteBuf,PacketPayload<P>> codec() {
    return StreamCodec.of(
      (buffer, payload) -> encoder.accept(payload.packet(), buffer),
      buffer -> new PacketPayload<>(this, decoder.apply(buffer)));
  }

  /**
   * Encodes the given packet, checking it against this registration's type.
   * @param packet  Packet to encode
   * @param buffer  Buffer to write to
   */
  public void encode(Object packet, RegistryFriendlyByteBuf buffer) {
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
