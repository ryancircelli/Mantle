package slimeknights.mantle.network.packet;

import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.Mantle;

/**
 * Packet to drop the book as item from lectern
 */
@AllArgsConstructor
public class DropLecternBookPacket implements IPacket.Threadsafe {
  /** Identifier of this packet on Mantle's channel */
  public static final ResourceLocation ID = Mantle.getResource("drop_lectern_book");

  private final BlockPos pos;

  public DropLecternBookPacket(RegistryFriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void handleThreadsafe(PacketContext context) {
    ServerPlayer player = context.getSender();
    if(player == null) {
      return;
    }

    ServerLevel world = player.serverLevel();
    if(!world.hasChunkAt(pos)) {
      return;
    }

    BlockState state = world.getBlockState(pos);

    if(state.getBlock() instanceof LecternBlock && state.getValue(LecternBlock.HAS_BOOK)) {
      BlockEntity te = world.getBlockEntity(pos);
      if(te instanceof LecternBlockEntity lecternTe) {
        ItemStack book = lecternTe.getBook().copy();
        if(!book.isEmpty()) {
          if(!player.addItem(book)) {
            player.drop(book, false, false);
          }

          lecternTe.clearContent();

          // fix lectern state
          world.setBlock(pos, state.setValue(LecternBlock.POWERED, false).setValue(LecternBlock.HAS_BOOK, false), 3);
          world.updateNeighborsAt(pos.below(), state.getBlock());
        }
      }
    }
  }
}
