package slimeknights.mantle.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.junit.jupiter.api.Test;
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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks Mantle's packets still read and write the bytes they always did.
 * <p>
 * Moving a packet onto the identified API changes registration only, so a packet that reads a buffer written by the old
 * encoder and writes the same bytes back is the guarantee that nothing on the wire moved.
 */
class MantlePacketWireTest extends BaseMcTest {
  /** Every packet Mantle registers, paired with its declared identifier */
  private static final List<Class<? extends IPacket>> PACKETS = List.of(
    OpenLecternBookPacket.class, UpdateHeldPagePacket.class, UpdateInventoryPagePacket.class,
    UpdateLecternPagePacket.class, DropLecternBookPacket.class, SwingArmPacket.class,
    OpenNamedBookPacket.class, FluidContainerTransferPacket.class);

  private static FriendlyByteBuf buffer() {
    return new FriendlyByteBuf(Unpooled.buffer());
  }

  /** Snapshots the readable bytes of a buffer without consuming them */
  private static byte[] readable(FriendlyByteBuf buffer) {
    byte[] bytes = new byte[buffer.readableBytes()];
    buffer.getBytes(buffer.readerIndex(), bytes);
    return bytes;
  }

  /**
   * Writes the given bytes, decodes a packet from them, then asserts the packet writes them back unchanged.
   * @param decoder  Packet decoder
   * @param writer   Writes the wire form the decoder is expected to read
   */
  private static void assertWireRoundTrip(Function<FriendlyByteBuf,? extends IPacket> decoder, Consumer<FriendlyByteBuf> writer) {
    FriendlyByteBuf input = buffer();
    writer.accept(input);
    byte[] expected = readable(input);

    IPacket packet = decoder.apply(input);
    assertThat(input.readableBytes()).as("decoder left bytes unread").isZero();

    FriendlyByteBuf output = buffer();
    packet.encode(output);
    assertThat(readable(output)).isEqualTo(expected);
  }


  /* Identity */

  @Test
  void packetIds_areUnique() {
    PacketRegistry registry = new PacketRegistry(new ResourceLocation("mantle", "network"));
    for (Class<? extends IPacket> packet : PACKETS) {
      registry.register(new PacketRegistration<>(registry.deriveId(packet), packet, IPacket::encode, buffer -> null, IPacket::handle, null));
    }
    assertThat(registry.size()).isEqualTo(PACKETS.size());
  }

  @Test
  void packetIds_matchTheDerivedIds() {
    // a packet's declared ID is what the old registration would have derived, so moving one over does not rename it
    PacketRegistry registry = new PacketRegistry(new ResourceLocation("mantle", "network"));
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
      buffer.writeItem(new ItemStack(Items.WRITTEN_BOOK));
    });
  }

  @Test
  void openLecternBook_roundTripsAnEmptyStack() {
    assertWireRoundTrip(OpenLecternBookPacket::new, buffer -> {
      buffer.writeBlockPos(new BlockPos(-4, 5, -6));
      buffer.writeItem(ItemStack.EMPTY);
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
    assertWireRoundTrip(OpenNamedBookPacket::new, buffer -> buffer.writeResourceLocation(new ResourceLocation("mantle", "test_book")));
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
      buffer.writeRegistryIdUnsafe(ForgeRegistries.ITEMS, Items.BUCKET);
    });
  }
}
