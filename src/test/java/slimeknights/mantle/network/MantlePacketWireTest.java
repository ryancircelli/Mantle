package slimeknights.mantle.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferPacket;
import slimeknights.mantle.network.packet.DropLecternBookPacket;
import slimeknights.mantle.network.packet.IPacket;
import slimeknights.mantle.network.packet.OpenLecternBookPacket;
import slimeknights.mantle.network.packet.OpenNamedBookPacket;
import slimeknights.mantle.network.packet.SwingArmPacket;
import slimeknights.mantle.network.packet.UpdateHeldPagePacket;
import slimeknights.mantle.network.packet.UpdateInventoryPagePacket;
import slimeknights.mantle.network.packet.UpdateLecternPagePacket;
import slimeknights.mantle.test.BaseMcTest;
import slimeknights.mantle.test.LoadableTest;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks each of Mantle's packets reads exactly what it writes, and that its identity is what its registration says.
 * <p>
 * There is no byte parity with an older Mantle to hold to: payloads replaced the indexed channel and item stacks
 * became data components, so this channel's bytes changed on both counts. What is worth holding is the pairing - a
 * decoder that reads a field the encoder did not write, or leaves one behind, is a bug that only shows up on a real
 * connection.
 */
class MantlePacketWireTest extends BaseMcTest {
  /** Every packet Mantle registers today */
  private static final List<Class<? extends IPacket>> PACKETS = List.of(
    OpenLecternBookPacket.class, UpdateHeldPagePacket.class, UpdateInventoryPagePacket.class,
    UpdateLecternPagePacket.class, DropLecternBookPacket.class, SwingArmPacket.class,
    OpenNamedBookPacket.class, FluidContainerTransferPacket.class);

  /** Snapshots the readable bytes of a buffer without consuming them */
  private static byte[] readable(RegistryFriendlyByteBuf buffer) {
    byte[] bytes = new byte[buffer.readableBytes()];
    buffer.getBytes(buffer.readerIndex(), bytes);
    return bytes;
  }

  /**
   * Writes the given bytes, decodes a packet from them, then asserts the packet writes them back unchanged.
   * @param decoder  Packet decoder
   * @param writer   Writes the wire form the decoder is expected to read
   */
  private static void assertWireRoundTrip(Function<RegistryFriendlyByteBuf,? extends IPacket> decoder, Consumer<RegistryFriendlyByteBuf> writer) {
    RegistryFriendlyByteBuf input = LoadableTest.buffer();
    writer.accept(input);
    byte[] expected = readable(input);

    IPacket packet = decoder.apply(input);
    assertThat(input.readableBytes()).as("decoder left bytes unread").isZero();

    RegistryFriendlyByteBuf output = LoadableTest.buffer();
    packet.encode(output);
    assertThat(readable(output)).isEqualTo(expected);

    // and again through the payload the channel actually sends, which is where an encoder reaches the wire
    assertPayloadRoundTrip(decoder, packet, expected);
  }

  /** Runs the packet through the codec its registration hands the loader */
  private static <P extends IPacket> void assertPayloadRoundTrip(Function<RegistryFriendlyByteBuf,P> decoder, IPacket packet, byte[] expected) {
    @SuppressWarnings("unchecked")
    Class<P> clazz = (Class<P>)packet.getClass();
    PacketRegistration<P> registration = new PacketRegistration<>(
      ResourceLocation.fromNamespaceAndPath("mantle", "test"), clazz, IPacket::encode, decoder, IPacket::handle, PacketFlow.SERVERBOUND);
    StreamCodec<RegistryFriendlyByteBuf,PacketPayload<P>> codec = registration.codec();

    RegistryFriendlyByteBuf buffer = LoadableTest.buffer();
    codec.encode(buffer, registration.wrap(packet));
    // the payload carries the packet's bytes and nothing else; the identifier is written by vanilla ahead of them
    assertThat(readable(buffer)).isEqualTo(expected);
    assertThat(codec.decode(buffer).packet()).isInstanceOf(clazz);
    assertThat(buffer.readableBytes()).as("payload codec left bytes unread").isZero();
  }


  /* Identity */

  @Test
  void packetIds_areUnique() {
    PacketRegistry registry = new PacketRegistry(ResourceLocation.fromNamespaceAndPath("mantle", "network"));
    for (Class<? extends IPacket> packet : PACKETS) {
      registry.register(new PacketRegistration<>(registry.deriveId(packet), packet, IPacket::encode, buffer -> null, IPacket::handle, null));
    }
    assertThat(registry.size()).isEqualTo(PACKETS.size());
  }

  @Test
  void packetIds_matchTheDerivedIds() {
    // a packet's declared ID is what an unmigrated registration would have derived, so moving one over does not rename it
    PacketRegistry registry = new PacketRegistry(ResourceLocation.fromNamespaceAndPath("mantle", "network"));
    assertThat(OpenLecternBookPacket.ID).isEqualTo(registry.deriveId(OpenLecternBookPacket.class));
    assertThat(UpdateHeldPagePacket.ID).isEqualTo(registry.deriveId(UpdateHeldPagePacket.class));
    assertThat(UpdateInventoryPagePacket.ID).isEqualTo(registry.deriveId(UpdateInventoryPagePacket.class));
    assertThat(UpdateLecternPagePacket.ID).isEqualTo(registry.deriveId(UpdateLecternPagePacket.class));
    assertThat(DropLecternBookPacket.ID).isEqualTo(registry.deriveId(DropLecternBookPacket.class));
    assertThat(SwingArmPacket.ID).isEqualTo(registry.deriveId(SwingArmPacket.class));
    assertThat(OpenNamedBookPacket.ID).isEqualTo(registry.deriveId(OpenNamedBookPacket.class));
    assertThat(FluidContainerTransferPacket.ID).isEqualTo(registry.deriveId(FluidContainerTransferPacket.class));
  }


  /* Wire format */

  @Test
  void openLecternBook_roundTrips() {
    assertWireRoundTrip(OpenLecternBookPacket::new, buffer -> {
      buffer.writeBlockPos(new BlockPos(1, 2, 3));
      ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, new ItemStack(Items.WRITTEN_BOOK));
    });
  }

  @Test
  void openLecternBook_roundTripsAnEmptyStack() {
    assertWireRoundTrip(OpenLecternBookPacket::new, buffer -> {
      buffer.writeBlockPos(new BlockPos(-4, 5, -6));
      ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, ItemStack.EMPTY);
    });
  }

  @Test
  void updateHeldPage_roundTrips() {
    assertWireRoundTrip(UpdateHeldPagePacket::new, buffer -> {
      buffer.writeEnum(InteractionHand.OFF_HAND);
      buffer.writeUtf("chapter/page");
    });
  }

  @Test
  void updateInventoryPage_roundTrips() {
    assertWireRoundTrip(UpdateInventoryPagePacket::new, buffer -> {
      buffer.writeVarInt(17);
      buffer.writeUtf("chapter/page");
    });
  }

  @Test
  void updateLecternPage_roundTrips() {
    assertWireRoundTrip(UpdateLecternPagePacket::new, buffer -> {
      buffer.writeBlockPos(new BlockPos(7, 8, 9));
      buffer.writeUtf("chapter/page");
    });
  }

  @Test
  void dropLecternBook_roundTrips() {
    assertWireRoundTrip(DropLecternBookPacket::new, buffer -> buffer.writeBlockPos(new BlockPos(10, -11, 12)));
  }

  @Test
  void swingArm_roundTrips() {
    assertWireRoundTrip(SwingArmPacket::new, buffer -> {
      buffer.writeVarInt(4242);
      buffer.writeEnum(InteractionHand.MAIN_HAND);
    });
  }

  @Test
  void openNamedBook_roundTrips() {
    assertWireRoundTrip(OpenNamedBookPacket::new, buffer -> buffer.writeResourceLocation(ResourceLocation.fromNamespaceAndPath("mantle", "test_book")));
  }

  @Test
  void fluidContainerTransfer_roundTripsEmpty() {
    assertWireRoundTrip(FluidContainerTransferPacket::new, buffer -> buffer.writeVarInt(0));
  }

  @Test
  void fluidContainerTransfer_roundTripsAnItem() {
    // a single item keeps the set iteration order irrelevant, so the bytes must come back identical
    assertWireRoundTrip(FluidContainerTransferPacket::new, buffer -> {
      buffer.writeVarInt(1);
      Loadables.ITEM.encode(buffer, Items.BUCKET);
    });
  }
}
