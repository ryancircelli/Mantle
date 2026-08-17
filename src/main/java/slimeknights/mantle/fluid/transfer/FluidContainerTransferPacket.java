package slimeknights.mantle.fluid.transfer;

import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.network.packet.IPacket;
import slimeknights.mantle.network.packet.PacketContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Packet to sync fluid container transfer */
@RequiredArgsConstructor
public class FluidContainerTransferPacket implements IPacket.Threadsafe {
  /** Identifier of this packet on Mantle's channel */
  public static final ResourceLocation ID = Mantle.getResource("fluid_container_transfer");

  private final Set<Item> items;

  public FluidContainerTransferPacket(RegistryFriendlyByteBuf buffer) {
    int size = buffer.readVarInt();
    List<Item> builder = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      builder.add(Loadables.ITEM.decode(buffer));
    }
    this.items = Set.copyOf(builder);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer) {
    buffer.writeVarInt(items.size());
    for (Item item : items) {
      Loadables.ITEM.encode(buffer, item);
    }
  }

  @Override
  public void handleThreadsafe(PacketContext context) {
    FluidContainerTransferManager.INSTANCE.setContainerItems(items);
  }
}
