package slimeknights.mantle.recipe.container;

import lombok.AllArgsConstructor;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Implementation of {@link ISingleStackContainer} to wrap a single slot of a {@link Container}
 */
@AllArgsConstructor
public class InventorySlotWrapper implements ISingleStackContainer {
  private final Container parent;
  private final int index;

  @Override
  public ItemStack getStack() {
    return parent.getItem(index);
  }
}
