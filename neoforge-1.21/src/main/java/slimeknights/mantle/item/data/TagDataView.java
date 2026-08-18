package slimeknights.mantle.item.data;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/** View reading a tag directly, for a caller holding a stack's tag instead of the stack */
record TagDataView(CompoundTag tag) implements DataView {
  @Nullable
  @Override
  public <T> T get(DataKey<T> key) {
    return key.read(tag);
  }

  @Nullable
  @Override
  public <T> T getStrict(DataKey<T> key) {
    return key.readStrict(tag);
  }

  @Override
  public boolean has(DataKey<?> key) {
    return key.contains(tag);
  }
}
