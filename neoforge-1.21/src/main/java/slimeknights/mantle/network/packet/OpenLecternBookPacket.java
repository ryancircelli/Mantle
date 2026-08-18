package slimeknights.mantle.network.packet;

import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.item.ILecternBookItem;

/**
 * Packet to open a book on a lectern
 */
@AllArgsConstructor
public class OpenLecternBookPacket implements IPacket.Threadsafe {
  /** Identifier of this packet on Mantle's channel */
  public static final ResourceLocation ID = Mantle.getResource("open_lectern_book");

  private final BlockPos pos;
  private final ItemStack book;

  public OpenLecternBookPacket(RegistryFriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.book = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    // the stack may be empty, and the empty aware codec is the one that matches the old writeItem
    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, book);
  }

  @Override
  public void handleThreadsafe(PacketContext context) {
    if (book.getItem() instanceof ILecternBookItem) {
      ((ILecternBookItem)book.getItem()).openLecternScreenClient(pos, book);
    }
  }
}
