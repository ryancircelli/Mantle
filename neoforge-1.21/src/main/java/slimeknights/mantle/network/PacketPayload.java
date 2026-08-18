package slimeknights.mantle.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Wire form of a packet on a {@link NetworkWrapper} channel: the loader hands out packets as
 * {@link CustomPacketPayload}, which requires every instance to name its own {@link Type}. Mantle's packets
 * deliberately do not know their own identity, so the channel wraps them on the way out and unwraps them on the way in.
 * <p>
 * <b>Why the packet does not implement {@link CustomPacketPayload} itself.</b> It would have to carry its type as an
 * inherited instance method, and an inherited identity is exactly the bug the registration-site identifier exists to
 * prevent: a packet class extending another (which downstream channels do) would silently answer with its parent's
 * type, claim its parent's payload registration, and be decoded as the wrong class. Keeping the identity on the
 * registration means the duplicate check fires at mod load instead.
 * <p>
 * The wrapper costs one small object per packet sent, and never reaches the buffer: the payload's own bytes are
 * whatever the packet's encoder writes, with the type identifier written by vanilla ahead of them.
 * @param registration  Registration the packet belongs to, which supplies the type and the codec
 * @param packet        Packet being carried
 * @param <P>  Packet type
 */
public record PacketPayload<P>(PacketRegistration<P> registration, P packet) implements CustomPacketPayload {
  @Override
  public Type<PacketPayload<P>> type() {
    return registration.payloadType();
  }
}
