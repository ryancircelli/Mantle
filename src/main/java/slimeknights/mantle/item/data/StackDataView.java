package slimeknights.mantle.item.data;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * View reading the tag of a stack. The tag is fetched per read rather than stored, so the view keeps working when the
 * stack gains or loses its tag.
 */
record StackDataView(ItemStack stack) implements DataView {
  @Nullable
  @Override
  public <T> T get(DataKey<T> key) {
    return key.read(stack.getTag());
  }

  @Nullable
  @Override
  public <T> T getStrict(DataKey<T> key) {
    return key.readStrict(stack.getTag());
  }

  @Override
  public boolean has(DataKey<?> key) {
    return key.contains(stack.getTag());
  }
}
