package slimeknights.mantle.fluid.transfer;

import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
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

  public FluidContainerTransferPacket(FriendlyByteBuf buffer) {
    RegistryFriendlyByteBuf registryBuffer = registryBuffer(buffer);
    int size = buffer.readVarInt();
    List<Item> builder = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      builder.add(Loadables.ITEM.decode(registryBuffer));
    }
    this.items = Set.copyOf(builder);
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    RegistryFriendlyByteBuf registryBuffer = registryBuffer(buffer);
    buffer.writeVarInt(items.size());
    for (Item item : items) {
      Loadables.ITEM.encode(registryBuffer, item);
    }
  }

  /**
   * Narrows the shared buffer type to the one a loadable's stream half demands.
   * <p>
   * This packet cannot be shared source, and this cast is why: {@link slimeknights.mantle.data.loadable.Streamable}
   * extends {@code StreamCodec<RegistryFriendlyByteBuf,T>} on 1.21, a supertype with no 1.20 counterpart, so Mantle's
   * own serialization framework cannot be named from a signature that compiles on both.
   */
  private static RegistryFriendlyByteBuf registryBuffer(FriendlyByteBuf buffer) {
    if (buffer instanceof RegistryFriendlyByteBuf registry) {
      return registry;
    }
    throw new IllegalStateException("Buffer " + buffer.getClass().getName() + " does not carry the connection's registries");
  }

  @Override
  public void handleThreadsafe(PacketContext context) {
    FluidContainerTransferManager.INSTANCE.setContainerItems(items);
  }
}
