package slimeknights.mantle.item.data;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.Loadable;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A typed, owned name for a single entry of an item stack's tag.
 * <p>
 * A key pairs the name of the entry with the {@link Loadable} which reads and writes its value, which is everything
 * needed to move between the tag and a value of type {@code T}. Reads go through {@link DataView} and writes through
 * {@link DataEditor}; a key never touches any entry other than its own {@link #getName() name}.
 * <p>
 * Keys are meant to be static constants, created once in the class which owns the data:
 * <pre>{@code
 * public static final DataKey<Block> TEXTURE = DataKey.of(Mantle.getResource("texture"), Loadables.BLOCK);
 * }</pre>
 * A name may be claimed exactly once per game instance; {@link #of(ResourceLocation, Loadable)} throws on a second
 * claim of the same name, which is what makes the ownership of an entry checkable rather than a convention. Since only
 * one key can exist per name, keys use identity equality, and two keys are equal exactly when they are the same key.
 *
 * @param <T>  Type of the value stored under this key
 * @see DataView
 * @see DataEditor
 */
@SuppressWarnings("unused")  // API
public final class DataKey<T> {
  /** Every claimed name, used to reject a second claim. Concurrent as mod class loading is not single threaded. */
  private static final Map<String,DataKey<?>> CLAIMED_NAMES = new ConcurrentHashMap<>();
  /**
   * Entries vanilla reads from the tag of an arbitrary stack. Rejected by {@link #ofLegacyName(String, Loadable)},
   * which is the only factory able to produce a name vanilla could also use; a namespaced name always contains a colon
   * and no vanilla entry does, so {@link #of(ResourceLocation, Loadable)} cannot collide with vanilla at all.
   * <p>
   * This is a guard against the common mistake rather than a proof: an item is free to read any entry it likes, so
   * always check the item you are extending before reusing a name it may already have written.
   */
  private static final Set<String> VANILLA_NAMES = Set.of(
    "Damage", "Unbreakable", "RepairCost", "Enchantments", "StoredEnchantments", "display", "HideFlags",
    "CanDestroy", "CanPlaceOn", "AttributeModifiers", "CustomModelData", "BlockEntityTag", "BlockStateTag", "EntityTag");

  /** Name of the entry this key reads and writes in the stack's tag */
  @Getter
  private final String name;
  /** Loadable converting between the entry and the value */
  @Getter
  private final Loadable<T> loadable;
  /**
   * Set once this key has reported a malformed value. Reads happen every frame in tooltip and model code and a given
   * stack fails the same way every time, so only the first failure is logged. A race logging twice is harmless.
   */
  private boolean loggedError = false;

  private DataKey(String name, Loadable<T> loadable) {
    this.name = name;
    this.loadable = loadable;
  }

  /** Claims the name of the passed key, throwing if something else already claimed it */
  private static <T> DataKey<T> claim(DataKey<T> key) {
    DataKey<?> existing = CLAIMED_NAMES.putIfAbsent(key.name, key);
    if (existing != null) {
      throw new IllegalArgumentException("Duplicate item data key '" + key.name + "', each name may only be claimed once");
    }
    return key;
  }

  /**
   * Creates a key storing its value under the passed namespaced name, such as {@code mantle:texture}.
   * <p>
   * This is the factory to use for new data. The name of the entry is the full namespaced string, so two mods can
   * never collide with each other, and since no entry vanilla reads contains a colon it can never collide with vanilla
   * either.
   * @param name      Name of the key, whose namespace should be the mod owning the data
   * @param loadable  Loadable reading and writing the value. It must construct a fresh value on read; see {@link DataView#get(DataKey)}.
   * @param <T>       Type of the value
   * @return  Key for the given name
   * @throws IllegalArgumentException  If the name was already claimed by another key.
   */
  public static <T> DataKey<T> of(ResourceLocation name, Loadable<T> loadable) {
    return claim(new DataKey<>(name.toString(), loadable));
  }

  /**
   * Creates a key storing its value under a name which does not follow the namespaced convention.
   * <p>
   * This exists for data which was written before it had a key, where changing the name would mean losing every value
   * already saved in a world. New data should use {@link #of(ResourceLocation, Loadable)} instead, as a bare name is
   * exactly the collision hazard keys exist to remove: nothing stops another mod, or a future version of vanilla, from
   * choosing the same one.
   * @param name      Name of the entry as it is already written in the tag
   * @param loadable  Loadable reading and writing the value. It must construct a fresh value on read; see {@link DataView#get(DataKey)}.
   * @param <T>       Type of the value
   * @return  Key for the given name
   * @throws IllegalArgumentException  If the name is empty, is read by vanilla, or was already claimed by another key.
   */
  public static <T> DataKey<T> ofLegacyName(String name, Loadable<T> loadable) {
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Item data key name must not be empty");
    }
    if (VANILLA_NAMES.contains(name)) {
      throw new IllegalArgumentException("Item data key name '" + name + "' is read by vanilla, choose another name");
    }
    return claim(new DataKey<>(name, loadable));
  }

  @Override
  public String toString() {
    return "DataKey['" + name + "']";
  }


  /* Reading and writing, used by the view and the editor */

  /**
   * Reads a value from the passed entry.
   * @throws RuntimeException  If the entry is malformed, per the loadable's error contract.
   */
  @SuppressWarnings("unchecked")  // safe, copying a tag gives a tag of the same type
  T parse(Tag value) {
    T parsed = loadable.convert(NbtOps.INSTANCE, value, name);
    // a loadable is free to return its input instead of building a value, which NBTLoadable does through NbtOps.
    // that would hand the caller the stack's live entry, so copy it; a value from this API never aliases a stack
    if (parsed instanceof Tag tag && tag == value) {
      return (T)tag.copy();
    }
    return parsed;
  }

  /** Same as {@link #parse(Tag)} but reports a malformed entry instead of throwing */
  @Nullable
  T parseOrNull(Tag value) {
    try {
      return parse(value);
    } catch (RuntimeException e) {
      if (!loggedError) {
        loggedError = true;
        Mantle.logger.warn("Ignoring malformed value for item data key '{}'", name, e);
      }
      return null;
    }
  }

  /**
   * Writes a value into the form stored in the tag.
   * @return  Entry to store, or null if the value writes nothing and the entry should be removed
   * @throws RuntimeException  If the value cannot be written, per the loadable's error contract.
   */
  @Nullable
  Tag write(T value) {
    Tag written = loadable.serialize(NbtOps.INSTANCE, value);
    // a loadable writing the empty value of the format has nothing to store; an end tag is also not a legal entry.
    // treating it as a removal matches how loadables read an empty value as an absent one
    if (written.getId() == Tag.TAG_END) {
      return null;
    }
    // as in parse, a loadable may hand back its input, here the caller's own object. copy so a later change to it
    // cannot reach the stack through this editor
    if (value instanceof Tag tag && tag == written) {
      return written.copy();
    }
    return written;
  }

  /** {@return the value of this key in the passed tag, or null if absent or malformed} */
  @Nullable
  T read(@Nullable CompoundTag parent) {
    Tag value = parent == null ? null : parent.get(name);
    return value == null ? null : parseOrNull(value);
  }

  /**
   * {@return the value of this key in the passed tag, or null if absent}
   * @throws RuntimeException  If the entry is present but malformed.
   */
  @Nullable
  T readStrict(@Nullable CompoundTag parent) {
    Tag value = parent == null ? null : parent.get(name);
    return value == null ? null : parse(value);
  }

  /** {@return true if the passed tag has an entry for this key}, without checking whether it can be read */
  boolean contains(@Nullable CompoundTag parent) {
    return parent != null && parent.contains(name);
  }
}
