package slimeknights.mantle.network.packet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * {@link PacketContext} backed by a live network event context. This is the implementation every packet sees at
 * runtime; it is the single place where the packet API touches the network event types.
 */
public record ForgePacketContext(NetworkEvent.Context context) implements PacketContext {
  @Override
  public void enqueueWork(Runnable work) {
    context.enqueueWork(work);
  }

  @Nullable
  @Override
  public ServerPlayer getSender() {
    return context.getSender();
  }

  @Override
  public NetworkDirection getDirection() {
    return context.getDirection();
  }

  /**
   * Recovers the network event context for the sake of {@link ISimplePacket} and the other handlers still written
   * against it.
   * <p>
   * The supplier is lazy on purpose: a handler that never reads its context works against any {@link PacketContext},
   * and only a handler that actually reaches for the network event context is limited to a live connection.
   * @param context  Packet context
   * @return  Supplier of the backing network event context
   * @throws IllegalStateException  If the supplier is called on a context that is not backed by a connection
   */
  public static Supplier<NetworkEvent.Context> unwrap(PacketContext context) {
    return () -> {
      if (context instanceof ForgePacketContext forge) {
        return forge.context();
      }
      throw new IllegalStateException("Packet context " + context.getClass().getName() + " is not backed by a network event context");
    };
  }
}
