package slimeknights.mantle.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.packet.IPacket;
import slimeknights.mantle.network.packet.PacketContext;
import slimeknights.mantle.platform.MantlePlatform;
import slimeknights.mantle.platform.PacketTransport;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A small network implementation/wrapper letting a mod register packet classes instead of the loader's own message
 * types. Instantiate in your mod class and register your packets.
 * <p>
 * Every packet registered here gets a {@link ResourceLocation} identity, checked for uniqueness as the channel is
 * built. What the identity means on the wire is the target's business: see {@link PacketTransport}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class NetworkWrapper {
  /** Packets registered to this channel, keyed by identity */
  private final PacketRegistry registry;
  /** The target's half of this channel */
  private final PacketTransport transport;

  public NetworkWrapper(ResourceLocation channelName, String version) {
    this.registry = new PacketRegistry(channelName);
    this.transport = MantlePlatform.INSTANCE.createTransport(channelName, version, registry);
  }

  /** Gets the packets registered to this channel */
  public PacketRegistry getRegistry() {
    return registry;
  }

  /**
   * Gets the target's half of this channel, for a caller needing something Mantle does not wrap.
   * <p>
   * Reaching for this is reaching past the shared API: the returned object's concrete type differs per target, so a
   * caller that casts it is a caller that no longer compiles on both.
   */
  public PacketTransport getTransport() {
    return transport;
  }


  /* Registration */

  /**
   * Registers a new {@link IPacket} under an explicit identifier
   * @param id         Unique identifier for this packet within the channel
   * @param clazz      Packet class
   * @param decoder    Packet decoder, typically the constructor
   * @param direction  Direction this packet may travel, for validation. Pass null to allow both
   * @param <P>  Packet class type
   */
  public <P extends IPacket> void registerPacket(ResourceLocation id, Class<P> clazz, Function<FriendlyByteBuf,P> decoder, @Nullable PacketDirection direction) {
    registerPacket(id, clazz, IPacket::encode, decoder, IPacket::handle, direction);
  }

  /**
   * Registers a new generic packet under an explicit identifier
   * @param id         Unique identifier for this packet within the channel
   * @param clazz      Packet class
   * @param encoder    Encodes a packet to the buffer
   * @param decoder    Packet decoder, typically the constructor
   * @param handler    Logic to handle a packet
   * @param direction  Direction this packet may travel, for validation. Pass null to allow both
   * @param <P>  Packet class type
   */
  public <P> void registerPacket(ResourceLocation id, Class<P> clazz, BiConsumer<P,FriendlyByteBuf> encoder, Function<FriendlyByteBuf,P> decoder, BiConsumer<P,PacketContext> handler, @Nullable PacketDirection direction) {
    registerPacketNoLogger(id, clazz, wrapLogger(clazz, encoder), wrapLogger(clazz, decoder), handler, direction);
  }

  /**
   * Registers a new packet under an explicit identifier, without the automatic logging if the codec fails
   * @param id         Unique identifier for this packet within the channel
   * @param clazz      Packet class
   * @param encoder    Encodes a packet to the buffer
   * @param decoder    Packet decoder, typically the constructor
   * @param handler    Logic to handle a packet
   * @param direction  Direction this packet may travel, for validation. Pass null to allow both
   * @param <P>  Packet class type
   */
  public <P> void registerPacketNoLogger(ResourceLocation id, Class<P> clazz, BiConsumer<P,FriendlyByteBuf> encoder, Function<FriendlyByteBuf,P> decoder, BiConsumer<P,PacketContext> handler, @Nullable PacketDirection direction) {
    // the registry rejects a duplicate here, so a clash fails while the mod is loading rather than at a connection
    PacketRegistration<P> registration = new PacketRegistration<>(id, clazz, encoder, decoder, handler, direction);
    int index = registry.size();
    registry.register(registration);
    transport.onPacketRegistered(registration, index);
  }

  /**
   * Registers a new {@link IPacket}, deriving its identifier from the class name
   * @param clazz      Packet class
   * @param decoder    Packet decoder, typically the constructor
   * @param direction  Direction this packet may travel, for validation. Pass null to allow both
   * @param <P>  Packet class type
   */
  public <P extends IPacket> void registerPacket(Class<P> clazz, Function<FriendlyByteBuf,P> decoder, @Nullable PacketDirection direction) {
    registerPacket(registry.deriveId(clazz), clazz, decoder, direction);
  }

  /**
   * Registers a new generic packet, deriving its identifier from the class name
   * @param clazz      Packet class
   * @param encoder    Encodes a packet to the buffer
   * @param decoder    Packet decoder, typically the constructor
   * @param handler    Logic to handle a packet
   * @param direction  Direction this packet may travel, for validation. Pass null to allow both
   * @param <P>  Packet class type
   */
  public <P> void registerPacket(Class<P> clazz, BiConsumer<P,FriendlyByteBuf> encoder, Function<FriendlyByteBuf,P> decoder, BiConsumer<P,PacketContext> handler, @Nullable PacketDirection direction) {
    registerPacket(registry.deriveId(clazz), clazz, encoder, decoder, handler, direction);
  }

  /** Wraps the given encoder function */
  private static <P> BiConsumer<P,FriendlyByteBuf> wrapLogger(Class<P> clazz, BiConsumer<P,FriendlyByteBuf> encoder) {
    return (message, buffer) -> {
      try {
        encoder.accept(message, buffer);
      } catch (Exception e) {
        Mantle.logger.error("Exception while encoding packet of class {}", clazz.getName(), e);
        throw e;
      }
    };
  }

  /** Wraps the given decoder function */
  private static <P> Function<FriendlyByteBuf,P> wrapLogger(Class<P> clazz, Function<FriendlyByteBuf,P> decoder) {
    return buffer -> {
      try {
        return decoder.apply(buffer);
      } catch (Exception e) {
        Mantle.logger.error("Exception while decoding packet of class {}", clazz.getName(), e);
        throw e;
      }
    };
  }


  /* Sending packets */

  /** Sends a packet to the server */
  public void sendToServer(Object packet) {
    transport.sendToServer(packet);
  }

  /** Sends a vanilla packet to the given entity */
  public void sendVanillaPacket(Packet<?> packet, Entity player) {
    transport.sendVanillaPacket(packet, player);
  }

  /** Sends a packet to a player, doing nothing if the player is not on a server */
  public void sendTo(Object packet, Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      sendTo(packet, serverPlayer);
    }
  }

  /** Sends a packet to a player */
  public void sendTo(Object packet, ServerPlayer player) {
    transport.sendTo(packet, player);
  }

  /** Sends a packet to players tracking the chunk containing the given position */
  public void sendToClientsAround(Object packet, ServerLevel serverWorld, BlockPos position) {
    transport.sendToClientsAround(packet, serverWorld, position);
  }

  /** Sends a packet to all entities tracking the given entity, and the entity itself if it is a player */
  public void sendToTrackingAndSelf(Object packet, Entity entity) {
    transport.sendToTrackingAndSelf(packet, entity);
  }

  /** Sends a packet to all entities tracking the given entity */
  public void sendToTracking(Object packet, Entity entity) {
    transport.sendToTracking(packet, entity);
  }
}
