package slimeknights.mantle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.network.packet.IPacket;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.mantle.network.packet.PacketContext;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests handling a packet through the context abstraction, with nothing connected behind it */
class PacketDispatchTest {
  /** Packet on the new interface, recording the context it was handed */
  private static class ContextPacket implements IPacket {
    @Nullable
    PacketContext received;

    @Override
    public void encode(FriendlyByteBuf buffer) {}

    @Override
    public void handle(PacketContext context) {
      this.received = context;
    }
  }

  /** Packet on the new interface that expects to run on the main thread */
  private static class ThreadsafePacket implements IPacket.Threadsafe {
    @Nullable
    PacketContext received;

    @Override
    public void encode(FriendlyByteBuf buffer) {}

    @Override
    public void handleThreadsafe(PacketContext context) {
      this.received = context;
    }
  }

  /** Packet on the old interface which never reads its context */
  private static class LegacyPacket implements ISimplePacket {
    boolean handled = false;

    @Override
    public void encode(FriendlyByteBuf buffer) {}

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
      this.handled = true;
    }
  }

  /** Packet on the old interface which reads its context */
  private static class LegacySenderPacket implements ISimplePacket {
    @Override
    public void encode(FriendlyByteBuf buffer) {}

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
      context.get().getSender();
    }
  }


  /* New path */

  @Test
  void handle_receivesTheContext() {
    ContextPacket packet = new ContextPacket();
    TestPacketContext context = new TestPacketContext();
    NetworkWrapper.simpleHandler(ContextPacket.class).accept(packet, context);
    assertThat(packet.received).isSameAs(context);
  }

  @Test
  void handle_threadsafeDefersToTheMainThread() {
    ThreadsafePacket packet = new ThreadsafePacket();
    TestPacketContext context = new TestPacketContext();
    NetworkWrapper.simpleHandler(ThreadsafePacket.class).accept(packet, context);
    // nothing ran yet, the handler is queued
    assertThat(packet.received).isNull();
    assertThat(context.enqueued).hasSize(1);
    context.runEnqueued();
    assertThat(packet.received).isSameAs(context);
  }

  @Test
  void handle_readsTheContextProperties() {
    TestPacketContext context = new TestPacketContext(null, NetworkDirection.PLAY_TO_CLIENT);
    assertThat(context.getSender()).isNull();
    assertThat(context.getDirection()).isEqualTo(NetworkDirection.PLAY_TO_CLIENT);
  }


  /* Legacy path */

  @Test
  void simpleHandler_runsAPacketThatIgnoresItsContext() {
    LegacyPacket packet = new LegacyPacket();
    NetworkWrapper.simpleHandler(LegacyPacket.class).accept(packet, new TestPacketContext());
    assertThat(packet.handled).isTrue();
  }

  @Test
  void simpleHandler_readingTheNetworkContextFailsClearly() {
    LegacySenderPacket packet = new LegacySenderPacket();
    BiConsumer<LegacySenderPacket,PacketContext> handler = NetworkWrapper.simpleHandler(LegacySenderPacket.class);
    assertThatThrownBy(() -> handler.accept(packet, new TestPacketContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("is not backed by a network event context");
  }

  @Test
  void contextHandler_runsAHandlerThatIgnoresItsContext() {
    boolean[] called = new boolean[1];
    BiConsumer<String,PacketContext> handler = NetworkWrapper.contextHandler((packet, context) -> called[0] = true);
    handler.accept("packet", new TestPacketContext());
    assertThat(called[0]).isTrue();
  }


  /* Equivalence between the two registration paths */

  @Test
  void simpleHandler_routesANewPacketStraightToTheContextHandler() {
    // registering an IPacket the old way must not send it through the network event context, as it does not need one
    ContextPacket packet = new ContextPacket();
    TestPacketContext context = new TestPacketContext();
    NetworkWrapper.simpleHandler(ContextPacket.class).accept(packet, context);
    ContextPacket direct = new ContextPacket();
    direct.handle(context);
    assertThat(packet.received).isSameAs(direct.received);
  }

  @Test
  void registration_matchesBetweenPaths() {
    // the derived identifier is what an unmigrated registration gets, and it must equal the one a packet declares
    PacketRegistry registry = new PacketRegistry(new ResourceLocation("mantle", "network"));
    PacketRegistration<ContextPacket> registration = new PacketRegistration<>(
      registry.deriveId(ContextPacket.class), ContextPacket.class, IPacket::encode, buffer -> new ContextPacket(),
      NetworkWrapper.simpleHandler(ContextPacket.class), NetworkDirection.PLAY_TO_CLIENT);
    assertThat(registration.id()).isEqualTo(new ResourceLocation("mantle", "context"));

    ContextPacket packet = new ContextPacket();
    TestPacketContext context = new TestPacketContext();
    registration.handle(packet, context);
    assertThat(packet.received).isSameAs(context);
  }

  @Test
  void registration_rejectsTheWrongPacketType() {
    PacketRegistration<ContextPacket> registration = new PacketRegistration<>(
      new ResourceLocation("mantle", "context"), ContextPacket.class, IPacket::encode, buffer -> new ContextPacket(),
      IPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
    assertThatThrownBy(() -> registration.handle(new ThreadsafePacket(), new TestPacketContext()))
      .isInstanceOf(ClassCastException.class);
  }
}
