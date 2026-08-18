package slimeknights.mantle.network.packet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;

import javax.annotation.Nullable;

/**
 * Everything a packet handler is allowed to know about the connection that delivered it.
 * <p>
 * This exists so a packet's handling logic is written against Mantle's own type instead of the network event context,
 * which cannot be created outside a live connection. A packet implementing {@link IPacket} can therefore be handled by
 * a test, or by any future transport, without the handler changing.
 * <p>
 * The surface is deliberately tiny: a survey of every packet in Mantle and in the mods that build on it found only
 * {@link #enqueueWork(Runnable)} and {@link #getSender()} in use. {@link #getDirection()} is included because the
 * direction passed at registration is optional, so a packet registered with a null direction has no other way to learn
 * which side received it.
 */
public interface PacketContext {
  /**
   * Runs the given work on the receiving side's main thread. The packet is decoded on the network thread, so anything
   * touching the world, an entity, or the client must go through here.
   * @param work  Work to run
   */
  void enqueueWork(Runnable work);

  /**
   * Gets the player that sent this packet, or null if it was sent by the server.
   * @return  Sending player
   */
  @Nullable
  ServerPlayer getSender();

  /**
   * Gets the direction this packet travelled.
   * @return  Network direction
   */
  @Nullable
  NetworkDirection getDirection();
}
