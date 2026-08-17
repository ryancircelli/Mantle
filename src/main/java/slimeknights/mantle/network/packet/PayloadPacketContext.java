package slimeknights.mantle.network.packet;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;

/**
 * {@link PacketContext} backed by a live payload context. This is the implementation every packet sees at runtime; it
 * is the single place where the packet API touches the loader's networking types.
 */
public record PayloadPacketContext(IPayloadContext context) implements PacketContext {
  @Override
  public void enqueueWork(Runnable work) {
    context.enqueueWork(work);
  }

  /**
   * {@inheritDoc}
   * <p>
   * The payload context's player is the receiving player on either side, so this narrows it to the server's: a
   * clientbound packet gets null, as it always did. Players only exist in the play phase, and asking outside it throws,
   * hence the protocol check.
   */
  @Nullable
  @Override
  public ServerPlayer getSender() {
    if (context.protocol() != ConnectionProtocol.PLAY) {
      return null;
    }
    return context.player() instanceof ServerPlayer player ? player : null;
  }

  @Override
  public PacketFlow getDirection() {
    return context.flow();
  }
}
