package slimeknights.mantle.recipe.container;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/**
 * {@link Recipe} extension for an input wrapper containing a single item. Primarily used for furnace like recipes.
 * @apiNote  1.21 added {@link SingleRecipeInput}, which is this interface for a stack you already hold. This one
 *           remains for the common case of a view over something else, such as one slot of an inventory.
 */
public interface ISingleStackContainer extends RecipeInput {
  /**
   * Gets the relevant item in this inventory
   * @return  Contained item
   */
  ItemStack getStack();

  /* Multistack methods, redundant now */

  /** @deprecated use {{@link #getStack()}} */
  @Deprecated
  @Override
  default ItemStack getItem(int index) {
    return index == 0 ? getStack() : ItemStack.EMPTY;
  }

  @Override
  default boolean isEmpty() {
    return getStack().isEmpty();
  }

  /** @deprecated always 1, not useful */
  @Deprecated
  @Override
  default int size() {
    return 1;
  }
}
