package slimeknights.mantle.item.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Editor collecting its changes as written entries, keeping them off the stack until they are applied.
 * <p>
 * Buffering is what makes {@link #apply(ItemStack)} the only visible change, and it is needed even though a stack's tag
 * can be written directly: writing as the caller goes would leave a stack half changed if the caller stopped partway,
 * and would make a change visible to anything else holding the stack before the caller was done with it.
 */
class BufferedDataEditor implements DataEditor {
  /** Values to read for any key not set here */
  private final DataView base;
  /**
   * Entries to write, by key name, in the order they were first given. A null value means the entry is to be removed,
   * which is why this cannot be a map of values: the two cases have to be told apart, and reusing the map for both is
   * what gives a later set or remove of a key the last word over an earlier one.
   */
  private final Map<String,Tag> pending = new LinkedHashMap<>();

  BufferedDataEditor(DataView base) {
    this.base = base;
  }

  @Nullable
  @Override
  public <T> T get(DataKey<T> key) {
    Tag value = pending.get(key.getName());
    if (value != null) {
      return key.parseOrNull(value);
    }
    return pending.containsKey(key.getName()) ? null : base.get(key);
  }

  @Nullable
  @Override
  public <T> T getStrict(DataKey<T> key) {
    Tag value = pending.get(key.getName());
    if (value != null) {
      return key.parse(value);
    }
    return pending.containsKey(key.getName()) ? null : base.getStrict(key);
  }

  @Override
  public boolean has(DataKey<?> key) {
    Tag value = pending.get(key.getName());
    if (value != null) {
      return true;
    }
    return !pending.containsKey(key.getName()) && base.has(key);
  }

  @Override
  public <T> DataEditor set(DataKey<T> key, T value) {
    // write now rather than at apply: the caller who supplied a bad value is still on the stack trace here,
    // and it leaves apply with nothing left that can fail partway through
    pending.put(key.getName(), key.write(value));
    return this;
  }

  @Override
  public DataEditor remove(DataKey<?> key) {
    pending.put(key.getName(), null);
    return this;
  }

  @Override
  public ItemStack apply(ItemStack stack) {
    // the empty stack is shared by everything holding nothing, so writing to it would give every one of them the data
    if (pending.isEmpty() || stack.isEmpty()) {
      return stack;
    }
    // work on a copy of the component's compound, then hand the whole thing back: CustomData is immutable to everyone
    // outside its own package, and this is the one place the editor is allowed to be visible
    CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    for (Entry<String,Tag> entry : pending.entrySet()) {
      Tag value = entry.getValue();
      if (value == null) {
        tag.remove(entry.getKey());
      } else {
        // copy so applying this editor again does not give two stacks the same entry to change out from under each other
        tag.put(entry.getKey(), value.copy());
      }
    }
    // an empty component is not the same as no component, it stops the stack from stacking with a plain one.
    // CustomData#set drops it in exactly that case, which is also why a pure removal never creates one
    CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    return stack;
  }

  @Override
  public String toString() {
    return "BufferedDataEditor" + pending.keySet();
  }
}
