package slimeknights.mantle.network.packet;

import slimeknights.mantle.network.PacketDirection;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * Everything a packet handler is allowed to know about the connection that delivered it.
 * <p>
 * This exists so a packet's handling logic is written against Mantle's own type instead of the loader's context type,
 * which cannot be created outside a live connection and has no shared spelling. A packet implementing {@link IPacket}
 * can therefore be handled by a test, by either target, or by any future transport, without the handler changing.
 * <p>
 * The surface is deliberately tiny: a survey of every packet in Mantle and in the mods that build on it found only
 * {@link #enqueueWork(Runnable)} and {@link #getSender()} in use. {@link #getDirection()} is included because the
 * direction passed at registration is optional, so a packet registered in both directions has no other way to learn
 * which side received it.
 */
public interface PacketContext {
  /**
   * Runs the given work on the receiving side's main thread. A handler that is already on the main thread runs the work
   * immediately, so this is safe to call unconditionally and is what {@link IPacket.Threadsafe} does for you.
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
   * Gets the direction this packet travelled: {@link PacketDirection#CLIENTBOUND} means it was received by a client.
   * @return  Packet direction
   */
  PacketDirection getDirection();
}
