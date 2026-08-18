package slimeknights.mantle.item.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;

/**
 * View reading the custom data of a stack. The component is fetched per read rather than stored, so the view keeps
 * working when the stack gains or loses it.
 * @apiNote  1.21 replaced a stack's free form tag with data components, and the component holding the free form data a
 *           mod owns is {@link DataComponents#CUSTOM_DATA}. Keys therefore name entries inside that component's
 *           compound rather than entries of the stack's tag, which keeps every legacy name (see
 *           {@link DataKey#ofLegacyName}) reading the same bytes it always did.
 */
record StackDataView(ItemStack stack) implements DataView {
  /**
   * {@return the stack's custom data compound, or null if it has none}
   * Uses {@link CustomData#getUnsafe()} rather than {@link CustomData#copyTag()} as this view only ever reads: a copy
   * would cost the whole compound on every read of a single key, on a path tooltips and models run every frame. The
   * value handed back to a caller is still never the stack's own object, as {@link DataKey#parse} copies anything a
   * loadable returns by identity.
   */
  @SuppressWarnings("deprecation")  // getUnsafe is only unsafe for a caller which mutates it, which this never does
  @Nullable
  private CompoundTag tag() {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    return data == null ? null : data.getUnsafe();
  }

  @Nullable
  @Override
  public <T> T get(DataKey<T> key) {
    return key.read(tag());
  }

  @Nullable
  @Override
  public <T> T getStrict(DataKey<T> key) {
    return key.readStrict(tag());
  }

  @Override
  public boolean has(DataKey<?> key) {
    return key.contains(tag());
  }
}
