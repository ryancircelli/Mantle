package slimeknights.mantle.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.packet.IPacket;
import slimeknights.mantle.network.packet.PacketContext;
import slimeknights.mantle.network.packet.PayloadPacketContext;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A small network implementation/wrapper letting a mod register packet classes instead of payloads.
 * Instantiate in your mod class, register your packets, then hand the wrapper
 * {@link RegisterPayloadHandlersEvent} so it can build the channel.
 * <p>
 * Every packet registered here gets a {@link ResourceLocation} identity, checked for uniqueness as the channel is
 * built, and that identity is what the packet is called on the wire. See {@link PacketRegistry} for what that does and
 * does not guarantee, and {@link PacketPayload} for why the identity lives on the registration rather than the packet.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class NetworkWrapper {
  /** Packets registered to this channel, keyed by identity */
  private final PacketRegistry registry;
  /**
   * Version this channel's payloads are registered under. Two sides of a connection that both have the mod must agree
   * on it or the connection is refused, so bump it whenever a packet's encoding or handling changes incompatibly.
   */
  private final String version;
  /** Set once the payloads have been handed to the loader, after which nothing more can be registered */
  private boolean built = false;

  /**
   * Creates a new network wrapper
   * @param channelName  Unique packet channel name
   * @deprecated Give your channel a version number.
   */
  @Deprecated
  public NetworkWrapper(ResourceLocation channelName) {
    this(channelName, "1");
  }

  public NetworkWrapper(ResourceLocation channelName, String version) {
    this.registry = new PacketRegistry(channelName);
    this.version = version;
  }

  /** Gets the packets registered to this channel */
  public PacketRegistry getRegistry() {
    return registry;
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
  public <P extends IPacket> void registerPacket(ResourceLocation id, Class<P> clazz, Function<RegistryFriendlyByteBuf,P> decoder, @Nullable PacketFlow direction) {
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
  public <P> void registerPacket(ResourceLocation id, Class<P> clazz, BiConsumer<P,RegistryFriendlyByteBuf> encoder, Function<RegistryFriendlyByteBuf,P> decoder, BiConsumer<P,PacketContext> handler, @Nullable PacketFlow direction) {
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
  public <P> void registerPacketNoLogger(ResourceLocation id, Class<P> clazz, BiConsumer<P,RegistryFriendlyByteBuf> encoder, Function<RegistryFriendlyByteBuf,P> decoder, BiConsumer<P,PacketContext> handler, @Nullable PacketFlow direction) {
    if (built) {
      throw new IllegalStateException("Cannot register " + id + " to channel " + registry.channel() + " after its payloads were built; register every packet before RegisterPayloadHandlersEvent");
    }
    // the registry rejects a duplicate here, so a clash fails while the mod is loading rather than at a connection
    registry.register(new PacketRegistration<>(id, clazz, encoder, decoder, handler, direction));
  }

  /**
   * Registers a new {@link IPacket}, deriving its identifier from the class name
   * @param clazz      Packet class
   * @param decoder    Packet decoder, typically the constructor
   * @param direction  Direction this packet may travel, for validation. Pass null to allow both
   * @param <P>  Packet class type
   */
  public <P extends IPacket> void registerPacket(Class<P> clazz, Function<RegistryFriendlyByteBuf,P> decoder, @Nullable PacketFlow direction) {
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
  public <P> void registerPacket(Class<P> clazz, BiConsumer<P,RegistryFriendlyByteBuf> encoder, Function<RegistryFriendlyByteBuf,P> decoder, BiConsumer<P,PacketContext> handler, @Nullable PacketFlow direction) {
    registerPacket(registry.deriveId(clazz), clazz, encoder, decoder, handler, direction);
  }

  /**
   * Registers a new packet without the automatic logging if the codec fails, deriving its identifier from the class name
   * @param clazz      Packet class
   * @param encoder    Encodes a packet to the buffer
   * @param decoder    Packet decoder, typically the constructor
   * @param handler    Logic to handle a packet
   * @param direction  Direction this packet may travel, for validation. Pass null to allow both
   * @param <P>  Packet class type
   */
  public <P> void registerPacketNoLogger(Class<P> clazz, BiConsumer<P,RegistryFriendlyByteBuf> encoder, Function<RegistryFriendlyByteBuf,P> decoder, BiConsumer<P,PacketContext> handler, @Nullable PacketFlow direction) {
    registerPacketNoLogger(registry.deriveId(clazz), clazz, encoder, decoder, handler, direction);
  }

  /* Building the channel */

  /**
   * Hands every packet registered to this channel to the loader as a payload. Add this to your mod event bus as a
   * listener for {@link RegisterPayloadHandlersEvent}, which fires after all the setup events, so anything registering
   * packets in common setup has already run.
   * @param event  Payload registration event
   */
  public void registerPayloads(RegisterPayloadHandlersEvent event) {
    if (built) {
      throw new IllegalStateException("Channel " + registry.channel() + " already built its payloads");
    }
    built = true;
    PayloadRegistrar registrar = event.registrar(version);
    for (PacketRegistration<?> registration : registry.registrations()) {
      registerPayload(registrar, registration);
    }
  }

  /** Registers a single packet's payload, in its own method so the payload type parameter is inferred from it */
  private static <P> void registerPayload(PayloadRegistrar registrar, PacketRegistration<P> registration) {
    CustomPacketPayload.Type<PacketPayload<P>> type = registration.payloadType();
    StreamCodec<RegistryFriendlyByteBuf,PacketPayload<P>> codec = registration.codec();
    // the registrar runs handlers on the main thread by default, so IPacket.Threadsafe's enqueueWork runs inline
    IPayloadHandler<PacketPayload<P>> handler = (payload, context) -> registration.handle(payload.packet(), new PayloadPacketContext(context));
    PacketFlow direction = registration.direction();
    if (direction == null) {
      registrar.playBidirectional(type, codec, handler);
    } else if (direction == PacketFlow.CLIENTBOUND) {
      registrar.playToClient(type, codec, handler);
    } else {
      registrar.playToServer(type, codec, handler);
    }
  }

  /** Wraps the given encoder function */
  private static <P> BiConsumer<P,RegistryFriendlyByteBuf> wrapLogger(Class<P> clazz, BiConsumer<P,RegistryFriendlyByteBuf> encoder) {
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
  private static <P> Function<RegistryFriendlyByteBuf,P> wrapLogger(Class<P> clazz, Function<RegistryFriendlyByteBuf,P> decoder) {
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

  /**
   * Wraps a packet in the payload its registration says it is, which is what every sending helper below does first.
   * Public so a packet can be handed to any of the loader's own distributors that this class does not wrap.
   * @param packet  Packet to send
   * @return  Payload to send
   * @throws IllegalArgumentException  If the packet's class was never registered to this channel
   */
  public CustomPacketPayload toPayload(Object packet) {
    PacketRegistration<?> registration = registry.get(packet.getClass());
    if (registration == null) {
      throw new IllegalArgumentException("Packet " + packet.getClass().getName() + " is not registered to channel " + registry.channel());
    }
    return registration.wrap(packet);
  }

  /**
   * Sends a packet to the server
   * @param msg  Packet to send
   */
  public void sendToServer(Object msg) {
    PacketDistributor.sendToServer(toPayload(msg));
  }

  /**
   * Sends a vanilla packet to the given entity
   * @param player  Player receiving the packet
   * @param packet  Packet
   */
  public void sendVanillaPacket(Packet<?> packet, Entity player) {
    if (player instanceof ServerPlayer sPlayer) {
      sPlayer.connection.send(packet);
    }
  }

  /**
   * Sends a packet to a player
   * @param msg     Packet
   * @param player  Player to send
   */
  public void sendTo(Object msg, Player player) {
    if (player instanceof ServerPlayer) {
      sendTo(msg, (ServerPlayer) player);
    }
  }

  /**
   * Sends a packet to a player
   * @param msg     Packet
   * @param player  Player to send
   */
  public void sendTo(Object msg, ServerPlayer player) {
    if (!(player instanceof FakePlayer)) {
      PacketDistributor.sendToPlayer(player, toPayload(msg));
    }
  }

  /**
   * Sends a packet to players near a location
   * @param msg          Packet to send
   * @param serverWorld  World instance
   * @param position     Position within range
   */
  public void sendToClientsAround(Object msg, ServerLevel serverWorld, BlockPos position) {
    PacketDistributor.sendToPlayersTrackingChunk(serverWorld, new ChunkPos(position), toPayload(msg));
  }

  /**
   * Sends a packet to all entities tracking the given entity, and the entity itself if it is a player
   * @param msg     Packet
   * @param entity  Entity to check
   */
  public void sendToTrackingAndSelf(Object msg, Entity entity) {
    PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, toPayload(msg));
  }

  /**
   * Sends a packet to all entities tracking the given entity
   * @param msg     Packet
   * @param entity  Entity to check
   */
  public void sendToTracking(Object msg, Entity entity) {
    PacketDistributor.sendToPlayersTrackingEntity(entity, toPayload(msg));
  }
}
