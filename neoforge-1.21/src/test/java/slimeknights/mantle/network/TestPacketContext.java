package slimeknights.mantle.network;

import net.minecraft.server.level.ServerPlayer;
import slimeknights.mantle.network.packet.PacketContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link PacketContext} with no connection behind it, used to run packet handlers in a test.
 * Work is collected rather than run so a test can assert a handler deferred to the main thread.
 */
public class TestPacketContext implements PacketContext {
  /** Work handed to {@link #enqueueWork(Runnable)}, in order */
  public final List<Runnable> enqueued = new ArrayList<>();
  @Nullable
  private final ServerPlayer sender;
  private final PacketDirection direction;

  public TestPacketContext() {
    this(null, PacketDirection.SERVERBOUND);
  }

  public TestPacketContext(@Nullable ServerPlayer sender, PacketDirection direction) {
    this.sender = sender;
    this.direction = direction;
  }

  @Override
  public void enqueueWork(Runnable work) {
    enqueued.add(work);
  }

  @Nullable
  @Override
  public ServerPlayer getSender() {
    return sender;
  }

  @Override
  public PacketDirection getDirection() {
    return direction;
  }

  /** Runs everything that was enqueued, clearing the queue */
  public void runEnqueued() {
    List<Runnable> work = List.copyOf(enqueued);
    enqueued.clear();
    work.forEach(Runnable::run);
  }
}
