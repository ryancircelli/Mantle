package slimeknights.mantle.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Wire form of a packet on a {@link NetworkWrapper} channel: the loader hands out packets as
 * {@link CustomPacketPayload}, which requires every instance to name its own {@link Type}. Mantle's packets
 * deliberately do not know their own identity, so the transport wraps them on the way out and unwraps them on the way
 * in.
 * <p>
 * <b>Why the packet does not implement {@link CustomPacketPayload} itself.</b> It would have to carry its type as an
 * inherited instance method, and an inherited identity is exactly the bug the registration-site identifier exists to
 * prevent: a packet class extending another (which downstream channels do) would silently answer with its parent's
 * type, claim its parent's payload registration, and be decoded as the wrong class. Keeping the identity on the
 * registration means the duplicate check fires at mod load instead.
 * <p>
 * <b>Why this class is not shared.</b> {@link CustomPacketPayload} does not exist on 1.20.1, so neither this record nor
 * anything naming it can be. It is the reason the shared {@link PacketRegistration} carries no payload type and the
 * target's transport keeps a map of them instead.
 * @param type    Payload type this packet is sent as
 * @param packet  Packet being carried
 * @param <P>  Packet type
 */
public record PacketPayload<P>(Type<PacketPayload<P>> type, P packet) implements CustomPacketPayload {}
