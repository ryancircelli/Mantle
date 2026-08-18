package slimeknights.mantle.network.packet;

import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.platform.MantlePlatform;
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

  public OpenLecternBookPacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.book = MantlePlatform.INSTANCE.readItem(buffer);
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    // the stack may be empty, which the platform's writeItem accepts on both targets
    MantlePlatform.INSTANCE.writeItem(buffer, book);
  }

  @Override
  public void handleThreadsafe(PacketContext context) {
    if (book.getItem() instanceof ILecternBookItem) {
      ((ILecternBookItem)book.getItem()).openLecternScreenClient(pos, book);
    }
  }
}
