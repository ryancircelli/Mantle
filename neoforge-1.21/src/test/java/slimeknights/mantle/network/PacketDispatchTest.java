package slimeknights.mantle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.network.packet.IPacket;
import slimeknights.mantle.network.packet.PacketContext;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests handling a packet through the context abstraction, with nothing connected behind it */
class PacketDispatchTest {
  /** Packet recording the context it was handed */
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

  /** Packet that expects to run on the main thread */
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

  private static <P> PacketRegistration<P> registrationOf(ResourceLocation id, Class<P> clazz, java.util.function.BiConsumer<P,PacketContext> handler) {
    return new PacketRegistration<>(id, clazz, (packet, buffer) -> {}, buffer -> null, handler, PacketDirection.CLIENTBOUND);
  }


  /* Handling */

  @Test
  void handle_receivesTheContext() {
    ContextPacket packet = new ContextPacket();
    TestPacketContext context = new TestPacketContext();
    packet.handle(context);
    assertThat(packet.received).isSameAs(context);
  }

  @Test
  void handle_threadsafeDefersToTheMainThread() {
    ThreadsafePacket packet = new ThreadsafePacket();
    TestPacketContext context = new TestPacketContext();
    packet.handle(context);
    // nothing ran yet, the handler is queued
    assertThat(packet.received).isNull();
    assertThat(context.enqueued).hasSize(1);
    context.runEnqueued();
    assertThat(packet.received).isSameAs(context);
  }

  @Test
  void handle_readsTheContextProperties() {
    TestPacketContext context = new TestPacketContext(null, PacketDirection.CLIENTBOUND);
    assertThat(context.getSender()).isNull();
    assertThat(context.getDirection()).isEqualTo(PacketDirection.CLIENTBOUND);
  }


  /* Dispatch through a registration, which is what the channel does */

  @Test
  void registration_dispatchesToThePacket() {
    PacketRegistration<ContextPacket> registration = registrationOf(
      ResourceLocation.fromNamespaceAndPath("mantle", "context"), ContextPacket.class, IPacket::handle);
    ContextPacket packet = new ContextPacket();
    TestPacketContext context = new TestPacketContext();
    registration.handle(packet, context);
    assertThat(packet.received).isSameAs(context);
  }

  @Test
  void registration_dispatchesToAGenericHandler() {
    // a packet class Mantle does not own is handled by the function passed at registration instead
    boolean[] called = new boolean[1];
    PacketRegistration<String> registration = registrationOf(
      ResourceLocation.fromNamespaceAndPath("mantle", "plain"), String.class, (packet, context) -> called[0] = true);
    registration.handle("packet", new TestPacketContext());
    assertThat(called[0]).isTrue();
  }

  @Test
  void registration_matchesBetweenPaths() {
    // the derived identifier is what an unmigrated registration gets, and it must equal the one a packet declares
    PacketRegistry registry = new PacketRegistry(ResourceLocation.fromNamespaceAndPath("mantle", "network"));
    PacketRegistration<ContextPacket> registration = registrationOf(
      registry.deriveId(ContextPacket.class), ContextPacket.class, IPacket::handle);
    assertThat(registration.id()).isEqualTo(ResourceLocation.fromNamespaceAndPath("mantle", "context"));
  }

  @Test
  void registration_rejectsTheWrongPacketType() {
    PacketRegistration<ContextPacket> registration = registrationOf(
      ResourceLocation.fromNamespaceAndPath("mantle", "context"), ContextPacket.class, IPacket::handle);
    assertThatThrownBy(() -> registration.handle(new ThreadsafePacket(), new TestPacketContext()))
      .isInstanceOf(ClassCastException.class);
    assertThatThrownBy(() -> registration.wrap(new ThreadsafePacket()))
      .isInstanceOf(ClassCastException.class);
  }
}
