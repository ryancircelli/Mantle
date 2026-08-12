package slimeknights.mantle.item.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Read only view of the values an item stack stores under {@link DataKey}s.
 * <p>
 * A view is a live window on the stack rather than a copy of it: creating one reads nothing, and each read parses only
 * the single entry its key names. A stack with no tag reads as a view with no values rather than as an error, so a
 * caller never has to check for a tag first.
 * <p>
 * Values from a view are plain deserialized objects with no link back to the stack. Changing one has no effect on the
 * stack, and two reads of the same key give two equal but separate objects; committing a change means handing the new
 * value to a {@link DataEditor}.
 *
 * @see DataKey
 * @see DataEditor
 */
@SuppressWarnings("unused")  // API
public interface DataView {
  /** View holding no values, which every key reads as absent. Also what a stack with no tag behaves like. */
  DataView EMPTY = new DataView() {
    @Nullable
    @Override
    public <T> T get(DataKey<T> key) {
      return null;
    }

    @Nullable
    @Override
    public <T> T getStrict(DataKey<T> key) {
      return null;
    }

    @Override
    public boolean has(DataKey<?> key) {
      return false;
    }

    @Override
    public String toString() {
      return "DataView.EMPTY";
    }
  };

  /**
   * Reads the value of the passed key.
   * <p>
   * This read never throws. An item stack is the least trustworthy data in the game: it arrives from a client over the
   * network, from a command with hand written tags, from another mod's recipe and from a world saved by an older
   * version of the mod that owns the key, and it is read on the render thread every frame by tooltips and models.
   * A malformed value is therefore reported as no value, logged once per key, on the grounds that the player whose game
   * would have crashed usually cannot fix the data that crashed it. Use {@link #getStrict(DataKey)} where the caller
   * can act on the failure, and {@link #has(DataKey)} to tell a malformed value apart from a missing one.
   * <p>
   * The returned value is freshly built by the key's loadable, so changing it does not change the stack. A loadable
   * which returns a value it was given rather than building one would break that, so the view copies an entry it is
   * handed straight back; a loadable burying the entry inside a larger value is beyond the view's reach and must copy
   * it itself.
   * @param key  Key to read
   * @param <T>  Type of the value
   * @return  Value of the key, or null if it is absent or malformed
   */
  @Nullable
  <T> T get(DataKey<T> key);

  /**
   * Same as {@link #get(DataKey)} but reports a malformed value instead of ignoring it.
   * @param key  Key to read
   * @param <T>  Type of the value
   * @return  Value of the key, or null if it is absent
   * @throws RuntimeException  If the value is present but malformed, per the key's loadable. See {@link slimeknights.mantle.data.loadable.ErrorFactory}.
   */
  @Nullable
  <T> T getStrict(DataKey<T> key);

  /**
   * Reads the value of the passed key, falling back when it is absent or malformed.
   * @param key       Key to read
   * @param fallback  Value to use when the key has none
   * @param <T>       Type of the value
   * @return  Value of the key, or the fallback
   */
  default <T> T getOrDefault(DataKey<T> key, T fallback) {
    T value = get(key);
    return value != null ? value : fallback;
  }

  /**
   * {@return true if the passed key has a value here}
   * This reports whether the entry exists, not whether it can be read, so a key whose value is malformed is present
   * here while {@link #get(DataKey)} gives null for it.
   * @param key  Key to check
   */
  boolean has(DataKey<?> key);


  /* Factories */

  /**
   * Creates a view of the data on the passed stack. The view follows the stack, so it sees any later change to it.
   * @param stack  Stack to read
   * @return  View of the stack
   */
  static DataView of(ItemStack stack) {
    return new StackDataView(stack);
  }

  /**
   * Creates a view of the data in the passed tag, for the rare caller holding a stack's tag rather than the stack.
   * There is no editor counterpart, as committing a change needs the stack to decide whether it should have a tag.
   * @param tag  Tag to read, in the shape of a stack's tag
   * @return  View of the tag
   */
  static DataView of(@Nullable CompoundTag tag) {
    return tag == null ? EMPTY : new TagDataView(tag);
  }
}
