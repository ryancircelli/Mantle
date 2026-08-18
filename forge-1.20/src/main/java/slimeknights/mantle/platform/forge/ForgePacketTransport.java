package slimeknights.mantle.platform.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import slimeknights.mantle.network.PacketDirection;
import slimeknights.mantle.network.PacketRegistration;
import slimeknights.mantle.network.packet.ForgePacketContext;
import slimeknights.mantle.network.packet.PacketContext;
import slimeknights.mantle.platform.PacketTransport;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * {@link PacketTransport} over a Forge {@link SimpleChannel}.
 * <p>
 * The channel is built in the constructor and each packet is handed to it as it is registered, because that is the
 * only point at which Forge accepts a message. The wire index is the registration order, which is why
 * {@link #onPacketRegistered} takes one.
 */
public class ForgePacketTransport implements PacketTransport {
  private final SimpleChannel network;

  public ForgePacketTransport(ResourceLocation channel, String version) {
    this.network = NetworkRegistry.ChannelBuilder
      .named(channel)
      .clientAcceptedVersions(version::equals)
      .serverAcceptedVersions(version::equals)
      .networkProtocolVersion(() -> version)
      .simpleChannel();
  }

  /** Gets the channel, for a caller needing a Forge specific distributor */
  public SimpleChannel getChannel() {
    return network;
  }

  @Override
  public <P> void onPacketRegistered(PacketRegistration<P> registration, int index) {
    network.registerMessage(index, registration.type(), registration.encoder(), registration.decoder(),
                            forgeHandler(registration.handler()), Optional.ofNullable(direction(registration.direction())));
  }

  /** Maps Mantle's direction onto Forge's, which also encodes the connection phase */
  @Nullable
  private static NetworkDirection direction(@Nullable PacketDirection direction) {
    if (direction == null) {
      return null;
    }
    return switch (direction) {
      case CLIENTBOUND -> NetworkDirection.PLAY_TO_CLIENT;
      case SERVERBOUND -> NetworkDirection.PLAY_TO_SERVER;
    };
  }

  /** Adapts a context based handler back to what the channel expects, marking the packet handled once it returns */
  private static <P> BiConsumer<P,java.util.function.Supplier<NetworkEvent.Context>> forgeHandler(BiConsumer<P,PacketContext> handler) {
    return (packet, supplier) -> {
      NetworkEvent.Context context = supplier.get();
      handler.accept(packet, new ForgePacketContext(context));
      context.setPacketHandled(true);
    };
  }

  @Override
  public void sendToServer(Object packet) {
    network.sendToServer(packet);
  }

  @Override
  public void sendTo(Object packet, ServerPlayer player) {
    if (!(player instanceof FakePlayer)) {
      network.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
  }

  @Override
  public void sendToClientsAround(Object packet, ServerLevel level, BlockPos position) {
    LevelChunk chunk = level.getChunkAt(position);
    network.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), packet);
  }

  @Override
  public void sendToTracking(Object packet, Entity entity) {
    network.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
  }

  @Override
  public void sendToTrackingAndSelf(Object packet, Entity entity) {
    network.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
  }

  @Override
  public void sendVanillaPacket(Packet<?> packet, Entity player) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.connection.send(packet);
    }
  }
}
