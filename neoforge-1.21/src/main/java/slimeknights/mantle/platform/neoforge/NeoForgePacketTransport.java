package slimeknights.mantle.platform.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import slimeknights.mantle.network.PacketDirection;
import slimeknights.mantle.network.PacketPayload;
import slimeknights.mantle.network.PacketRegistration;
import slimeknights.mantle.network.packet.PayloadPacketContext;
import slimeknights.mantle.platform.PacketTransport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link PacketTransport} over NeoForge's payload registry.
 * <p>
 * Registrations are collected as they arrive and handed to the loader in one go when it asks, which is the opposite of
 * Forge's eager channel. That difference is not expressible on the shared interface, so the moment the channel goes
 * live is a target concern: this class exposes {@link #registerPayloads} for the mod entrypoint to hook up.
 */
public class NeoForgePacketTransport implements PacketTransport {
  private final ResourceLocation channel;
  private final String version;
  /** Registrations awaiting the loader, in registration order */
  private final List<PacketRegistration<?>> registrations = new ArrayList<>();
  /** Payload type of each packet class, derived here because the shared registration cannot name one */
  private final Map<Class<?>,CustomPacketPayload.Type<?>> types = new HashMap<>();
  private boolean built = false;

  public NeoForgePacketTransport(ResourceLocation channel, String version) {
    this.channel = channel;
    this.version = version;
  }

  @Override
  public <P> void onPacketRegistered(PacketRegistration<P> registration, int index) {
    if (built) {
      throw new IllegalStateException("Cannot register " + registration.id() + " to channel " + channel + " after its payloads were built; register every packet before RegisterPayloadHandlersEvent");
    }
    registrations.add(registration);
    types.put(registration.type(), new CustomPacketPayload.Type<PacketPayload<P>>(registration.id()));
  }

  /**
   * Hands every packet registered to this channel to the loader as a payload. Add this to your mod event bus as a
   * listener for {@link RegisterPayloadHandlersEvent}, which fires after all the setup events, so anything registering
   * packets in common setup has already run.
   * @param event  Payload registration event
   */
  public void registerPayloads(RegisterPayloadHandlersEvent event) {
    if (built) {
      throw new IllegalStateException("Channel " + channel + " already built its payloads");
    }
    built = true;
    PayloadRegistrar registrar = event.registrar(version);
    for (PacketRegistration<?> registration : registrations) {
      registerPayload(registrar, registration);
    }
  }

  /** Registers a single packet's payload, in its own method so the payload type parameter is inferred from it */
  private <P> void registerPayload(PayloadRegistrar registrar, PacketRegistration<P> registration) {
    CustomPacketPayload.Type<PacketPayload<P>> type = type(registration.type());
    StreamCodec<RegistryFriendlyByteBuf,PacketPayload<P>> codec = StreamCodec.of(
      (buffer, payload) -> registration.encoder().accept(payload.packet(), buffer),
      buffer -> new PacketPayload<>(type, registration.decoder().apply(buffer)));
    // the registrar runs handlers on the main thread by default, so IPacket.Threadsafe's enqueueWork runs inline
    IPayloadHandler<PacketPayload<P>> handler = (payload, context) -> registration.handle(payload.packet(), new PayloadPacketContext(context));
    PacketDirection direction = registration.direction();
    if (direction == null) {
      registrar.playBidirectional(type, codec, handler);
    } else if (direction == PacketDirection.CLIENTBOUND) {
      registrar.playToClient(type, codec, handler);
    } else {
      registrar.playToServer(type, codec, handler);
    }
  }

  @SuppressWarnings("unchecked")
  private <P> CustomPacketPayload.Type<PacketPayload<P>> type(Class<P> clazz) {
    CustomPacketPayload.Type<?> type = types.get(clazz);
    if (type == null) {
      throw new IllegalArgumentException("Packet " + clazz.getName() + " is not registered to channel " + channel);
    }
    return (CustomPacketPayload.Type<PacketPayload<P>>) type;
  }

  /**
   * Wraps a packet in the payload its registration says it is, which is what every sending helper below does first.
   * @param packet  Packet to send
   * @return  Payload to send
   * @throws IllegalArgumentException  If the packet's class was never registered to this channel
   */
  @SuppressWarnings({"unchecked","rawtypes"})
  public CustomPacketPayload toPayload(Object packet) {
    CustomPacketPayload.Type<?> type = types.get(packet.getClass());
    if (type == null) {
      throw new IllegalArgumentException("Packet " + packet.getClass().getName() + " is not registered to channel " + channel);
    }
    return new PacketPayload((CustomPacketPayload.Type) type, packet);
  }

  @Override
  public void sendToServer(Object packet) {
    PacketDistributor.sendToServer(toPayload(packet));
  }

  @Override
  public void sendTo(Object packet, ServerPlayer player) {
    if (!(player instanceof FakePlayer)) {
      PacketDistributor.sendToPlayer(player, toPayload(packet));
    }
  }

  @Override
  public void sendToClientsAround(Object packet, ServerLevel level, BlockPos position) {
    PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(position), toPayload(packet));
  }

  @Override
  public void sendToTracking(Object packet, Entity entity) {
    PacketDistributor.sendToPlayersTrackingEntity(entity, toPayload(packet));
  }

  @Override
  public void sendToTrackingAndSelf(Object packet, Entity entity) {
    PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, toPayload(packet));
  }

  @Override
  public void sendVanillaPacket(Packet<?> packet, Entity player) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.connection.send(packet);
    }
  }
}
