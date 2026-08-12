package slimeknights.mantle.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.packet.ForgePacketContext;
import slimeknights.mantle.network.packet.IPacket;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.mantle.network.packet.PacketContext;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A small network implementation/wrapper using AbstractPackets instead of IMessages.
 * Instantiate in your mod class and register your packets accordingly.
 * <p>
 * Every packet registered here gets a {@link ResourceLocation} identity, which is checked for uniqueness as the channel
 * is built. The identity is registration metadata only; the wire still carries the registration index, see
 * {@link PacketRegistry}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class NetworkWrapper {
  /** Network instance */
  public final SimpleChannel network;
  /** Packets registered to this channel, keyed by identity */
  private final PacketRegistry registry;

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
    this.network = NetworkRegistry.ChannelBuilder
      .named(channelName)
      .clientAcceptedVersions(version::equals)
      .serverAcceptedVersions(version::equals)
      .networkProtocolVersion(() -> version)
      .simpleChannel();
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
   * @param direction  Network direction for validation. Pass null for no direction
   * @param <P>  Packet class type
   */
  public <P extends IPacket> void registerPacket(ResourceLocation id, Class<P> clazz, Function<FriendlyByteBuf,P> decoder, @Nullable NetworkDirection direction) {
    registerPacket(id, clazz, IPacket::encode, decoder, IPacket::handle, direction);
  }

  /**
   * Registers a new generic packet under an explicit identifier
   * @param id         Unique identifier for this packet within the channel
   * @param clazz      Packet class
   * @param encoder    Encodes a packet to the buffer
   * @param decoder    Packet decoder, typically the constructor
   * @param handler    Logic to handle a packet
   * @param direction  Network direction for validation. Pass null for no direction
   * @param <P>  Packet class type
   */
  public <P> void registerPacket(ResourceLocation id, Class<P> clazz, BiConsumer<P,FriendlyByteBuf> encoder, Function<FriendlyByteBuf,P> decoder, BiConsumer<P,PacketContext> handler, @Nullable NetworkDirection direction) {
    registerPacketNoLogger(id, clazz, wrapLogger(clazz, encoder), wrapLogger(clazz, decoder), handler, direction);
  }

  /**
   * Registers a new packet under an explicit identifier, without the automatic logging if the codec fails
   * @param id         Unique identifier for this packet within the channel
   * @param clazz      Packet class
   * @param encoder    Encodes a packet to the buffer
   * @param decoder    Packet decoder, typically the constructor
   * @param handler    Logic to handle a packet
   * @param direction  Network direction for validation. Pass null for no direction
   * @param <P>  Packet class type
   */
  public <P> void registerPacketNoLogger(ResourceLocation id, Class<P> clazz, BiConsumer<P,FriendlyByteBuf> encoder, Function<FriendlyByteBuf,P> decoder, BiConsumer<P,PacketContext> handler, @Nullable NetworkDirection direction) {
    // the registry rejects a duplicate before the channel sees it, so a clash fails while the mod is loading
    int index = registry.register(new PacketRegistration<>(id, clazz, encoder, decoder, handler, direction));
    this.network.registerMessage(index, clazz, encoder, decoder, forgeHandler(handler), Optional.ofNullable(direction));
  }

  /**
   * Registers a new {@link ISimplePacket}, deriving its identifier from the class name
   * @param clazz    Packet class
   * @param decoder  Packet decoder, typically the constructor
   * @param <MSG>  Packet class type
   */
  public <MSG extends ISimplePacket> void registerPacket(Class<MSG> clazz, Function<FriendlyByteBuf, MSG> decoder, @Nullable NetworkDirection direction) {
    registerPacket(registry.deriveId(clazz), clazz, ISimplePacket::encode, decoder, simpleHandler(clazz), direction);
  }

  /**
   * Registers a new generic packet, deriving its identifier from the class name
   * @param clazz      Packet class
   * @param encoder    Encodes a packet to the buffer
   * @param decoder    Packet decoder, typically the constructor
   * @param consumer   Logic to handle a packet
   * @param direction  Network direction for validation. Pass null for no direction
   * @param <MSG>  Packet class type
   */
  public <MSG> void registerPacket(Class<MSG> clazz, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG,Supplier<NetworkEvent.Context>> consumer, @Nullable NetworkDirection direction) {
    registerPacket(registry.deriveId(clazz), clazz, encoder, decoder, contextHandler(consumer), direction);
  }

  /**
   * Registers a new packet without the automatic logging if the decoder fails, deriving its identifier from the class name
   * @param clazz      Packet class
   * @param encoder    Encodes a packet to the buffer
   * @param decoder    Packet decoder, typically the constructor
   * @param consumer   Logic to handle a packet
   * @param direction  Network direction for validation. Pass null for no direction
   * @param <MSG>  Packet class type
   */
  public <MSG> void registerPacketNoLogger(Class<MSG> clazz, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG,Supplier<NetworkEvent.Context>> consumer, @Nullable NetworkDirection direction) {
    registerPacketNoLogger(registry.deriveId(clazz), clazz, encoder, decoder, contextHandler(consumer), direction);
  }

  /**
   * Adapts an {@link ISimplePacket} to the context based handler.
   * An {@link IPacket} is called directly so registering it the old way still keeps it off the network event context.
   * Package private so the adapter can be checked without building a channel.
   */
  static <MSG extends ISimplePacket> BiConsumer<MSG,PacketContext> simpleHandler(Class<MSG> clazz) {
    if (IPacket.class.isAssignableFrom(clazz)) {
      return (packet, context) -> ((IPacket)packet).handle(context);
    }
    return (packet, context) -> packet.handle(ForgePacketContext.unwrap(context));
  }

  /** Adapts a handler written against the network event context to the context based handler */
  static <MSG> BiConsumer<MSG,PacketContext> contextHandler(BiConsumer<MSG,Supplier<NetworkEvent.Context>> consumer) {
    return (packet, context) -> consumer.accept(packet, ForgePacketContext.unwrap(context));
  }

  /** Adapts a context based handler back to what the channel expects, marking the packet handled once it returns */
  private static <P> BiConsumer<P,Supplier<NetworkEvent.Context>> forgeHandler(BiConsumer<P,PacketContext> handler) {
    return (packet, supplier) -> {
      NetworkEvent.Context context = supplier.get();
      handler.accept(packet, new ForgePacketContext(context));
      context.setPacketHandled(true);
    };
  }

  /** Wraps the given encoder function */
  private static <MSG> BiConsumer<MSG, FriendlyByteBuf> wrapLogger(Class<MSG> clazz, BiConsumer<MSG, FriendlyByteBuf> encoder) {
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
  private static <MSG> Function<FriendlyByteBuf,MSG> wrapLogger(Class<MSG> clazz, Function<FriendlyByteBuf,MSG> decoder) {
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
   * Sends a packet to the server
   * @param msg  Packet to send
   */
  public void sendToServer(Object msg) {
    this.network.sendToServer(msg);
  }

  /**
   * Sends a packet to the given packet distributor
   * @param target   Packet target
   * @param message  Packet to send
   */
  public void send(PacketDistributor.PacketTarget target, Object message) {
    network.send(target, message);
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
      network.sendTo(msg, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
  }

  /**
   * Sends a packet to players near a location
   * @param msg          Packet to send
   * @param serverWorld  World instance
   * @param position     Position within range
   */
  public void sendToClientsAround(Object msg, ServerLevel serverWorld, BlockPos position) {
    LevelChunk chunk = serverWorld.getChunkAt(position);
    network.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), msg);
  }

  /**
   * Sends a packet to all entities tracking the given entity
   * @param msg     Packet
   * @param entity  Entity to check
   */
  public void sendToTrackingAndSelf(Object msg, Entity entity) {
    this.network.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
  }

  /**
   * Sends a packet to all entities tracking the given entity
   * @param msg     Packet
   * @param entity  Entity to check
   */
  public void sendToTracking(Object msg, Entity entity) {
    this.network.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
  }
}
