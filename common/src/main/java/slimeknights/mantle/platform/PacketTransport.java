package slimeknights.mantle.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import slimeknights.mantle.network.PacketRegistration;

/**
 * The loader-facing half of a channel: everything {@link slimeknights.mantle.network.NetworkWrapper} needs a target to
 * do for it.
 * <p>
 * The sending methods are the surface a survey of Mantle and the mods built on it found in use. What is deliberately
 * absent is a way to name the loader's own distributor type, because the two targets do not agree on it; a caller
 * wanting a distributor Mantle does not wrap has to reach the target module directly.
 */
public interface PacketTransport {
  /**
   * Called as each packet is registered, before the channel is live.
   * <p>
   * 1.20.1 registers a message with the channel here; 1.21.1 has nothing to do until the loader asks for payloads.
   * The difference is why registration is a callback rather than a single build step.
   * @param registration  Packet being registered
   * @param index         Position of the packet within the channel, which 1.20.1 uses as its wire index
   * @param <P>  Packet type
   */
  <P> void onPacketRegistered(PacketRegistration<P> registration, int index);

  /** Sends a packet to the server */
  void sendToServer(Object packet);

  /** Sends a packet to a single player */
  void sendTo(Object packet, ServerPlayer player);

  /** Sends a packet to every player tracking the chunk containing the given position */
  void sendToClientsAround(Object packet, ServerLevel level, BlockPos position);

  /** Sends a packet to every player tracking the given entity */
  void sendToTracking(Object packet, Entity entity);

  /** Sends a packet to every player tracking the given entity, and to the entity itself if it is a player */
  void sendToTrackingAndSelf(Object packet, Entity entity);

  /** Sends a vanilla packet to the given entity, if it is a server player */
  void sendVanillaPacket(Packet<?> packet, Entity player);
}
