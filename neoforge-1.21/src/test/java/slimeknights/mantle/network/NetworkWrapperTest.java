package slimeknights.mantle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.network.packet.IPacket;
import slimeknights.mantle.network.packet.PacketContext;
import slimeknights.mantle.platform.neoforge.NeoForgePacketTransport;
import slimeknights.mantle.test.BaseMcTest;
import slimeknights.mantle.test.LoadableTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the channel wrapper without a connection. Building the channel no longer needs one: a wrapper holds its packets
 * until it is handed the payload registration event, so everything up to that point is plain Java.
 */
class NetworkWrapperTest extends BaseMcTest {
  private static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath("mantle", "test_network");

  /** Packet with no state, only ever used to fill a registration */
  private static class EmptyPacket implements IPacket {
    @Override
    public void encode(FriendlyByteBuf buffer) {}

    @Override
    public void handle(PacketContext context) {}
  }

  private static class OtherPacket extends EmptyPacket {}

  private static NetworkWrapper wrapper() {
    return new NetworkWrapper(CHANNEL, "1");
  }

  /**
   * The target half of a wrapper's channel. Reaching for it is what a caller has to do for anything the shared
   * interface cannot name, which on this target is everything payload shaped.
   */
  private static NeoForgePacketTransport transport(NetworkWrapper network) {
    return (NeoForgePacketTransport)network.getTransport();
  }


  /* Registration */

  @Test
  void registerPacket_derivesTheIdFromTheClass() {
    NetworkWrapper network = wrapper();
    network.registerPacket(EmptyPacket.class, buffer -> new EmptyPacket(), PacketDirection.CLIENTBOUND);
    assertThat(network.getRegistry().ids()).containsExactly(ResourceLocation.fromNamespaceAndPath("mantle", "empty"));
  }

  @Test
  void registerPacket_duplicateIdFailsWhileRegistering() {
    // the whole point of the identifier: a clash is a mod load failure, not a decoder reading the wrong bytes later
    NetworkWrapper network = wrapper();
    ResourceLocation id = ResourceLocation.fromNamespaceAndPath("mantle", "clash");
    network.registerPacket(id, EmptyPacket.class, buffer -> new EmptyPacket(), PacketDirection.CLIENTBOUND);
    assertThatThrownBy(() -> network.registerPacket(id, OtherPacket.class, buffer -> new OtherPacket(), PacketDirection.CLIENTBOUND))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Duplicate packet ID mantle:clash");
  }


  /* Sending */

  @Test
  void toPayload_wrapsThePacketInItsDeclaredType() {
    NetworkWrapper network = wrapper();
    ResourceLocation id = ResourceLocation.fromNamespaceAndPath("mantle", "wrapped");
    network.registerPacket(id, EmptyPacket.class, buffer -> new EmptyPacket(), PacketDirection.CLIENTBOUND);

    EmptyPacket packet = new EmptyPacket();
    CustomPacketPayload payload = transport(network).toPayload(packet);
    assertThat(payload.type().id()).isEqualTo(id);
    assertThat(payload).isInstanceOf(PacketPayload.class);
    assertThat(((PacketPayload<?>)payload).packet()).isSameAs(packet);
  }

  @Test
  void toPayload_unregisteredPacketFails() {
    // sending a packet nobody registered used to be a silent no-op on the channel
    NetworkWrapper network = wrapper();
    assertThatThrownBy(() -> transport(network).toPayload(new EmptyPacket()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(EmptyPacket.class.getName())
      .hasMessageContaining(CHANNEL.toString());
  }

  @Test
  void toPayload_usesTheExactClass() {
    // a subclass is its own packet, and must not borrow its parent's registration to reach the wire
    NetworkWrapper network = wrapper();
    network.registerPacket(ResourceLocation.fromNamespaceAndPath("mantle", "parent"), EmptyPacket.class, buffer -> new EmptyPacket(), PacketDirection.CLIENTBOUND);
    assertThatThrownBy(() -> transport(network).toPayload(new OtherPacket()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(OtherPacket.class.getName());
  }


  /* Codec, which is what the loader is handed for each packet */

  @Test
  void codec_roundTripsThroughThePayload() {
    PacketRegistration<EmptyPacket> registration = new PacketRegistration<>(
      ResourceLocation.fromNamespaceAndPath("mantle", "coded"), EmptyPacket.class,
      (packet, buffer) -> buffer.writeVarInt(7), buffer -> {
        assertThat(buffer.readVarInt()).isEqualTo(7);
        return new EmptyPacket();
      }, IPacket::handle, PacketDirection.CLIENTBOUND);

    NeoForgePacketTransport transport = new NeoForgePacketTransport(CHANNEL, "1");
    transport.onPacketRegistered(registration, 0);
    StreamCodec<RegistryFriendlyByteBuf,PacketPayload<EmptyPacket>> codec = transport.codec(registration);
    RegistryFriendlyByteBuf buffer = LoadableTest.buffer();
    codec.encode(buffer, new PacketPayload<>(transport.type(EmptyPacket.class), new EmptyPacket()));
    PacketPayload<EmptyPacket> decoded = codec.decode(buffer);
    assertThat(buffer.readableBytes()).as("codec left bytes unread").isZero();
    assertThat(decoded.type()).isEqualTo(transport.type(EmptyPacket.class));
  }

  /*
   * Not covered: NeoForgePacketTransport#registerPayloads. The loader's payload registry is locked once mod loading finishes,
   * which a JUnit run has already done by the time the first test executes, so calling it here can only assert that
   * NeoForge refuses. The two pieces it feeds the registrar - the identity and the codec - are covered above.
   */
}
