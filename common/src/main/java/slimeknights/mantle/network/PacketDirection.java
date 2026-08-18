package slimeknights.mantle.network;

/**
 * Direction a packet is allowed to travel.
 * <p>
 * Mantle owns this enum because neither target's spelling is available on the other: 1.20.1 names the direction with
 * the loader's own type, which also encodes the connection phase, and 1.21.1 names it with a vanilla type that did not
 * exist in 1.20.1. Both reduce to the same two values for a play packet, which is all Mantle's channels carry.
 */
public enum PacketDirection {
  /** Sent by the server, received by a client */
  CLIENTBOUND,
  /** Sent by a client, received by the server */
  SERVERBOUND,
}
