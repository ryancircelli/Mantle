package slimeknights.mantle.data.loadable.array;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Helpers for all array loadables.
 * Implementations work in terms of an arbitrary {@link DynamicOps} format, which notably means a list of numbers
 * written through {@link net.minecraft.nbt.NbtOps} lands in the matching NBT array tag.
 */
public interface ArrayLoadable<A> extends Loadable<A> {
  /** Special size representing compact where empty is allowed */
  int COMPACT_OR_EMPTY = -2;
  /** Special size representing comapct where empty is disallowed */
  int COMPACT = -1;

  /** If true, this loadable allows compact */
  boolean allowCompact();

  /** Validates the size is correct */
  void checkSize(String key, int size, ErrorFactory error);

  /** Gets the length of the array */
  int getLength(A array);


  /** Converts the given value into a length 1 array  */
  <O> A convertCompact(DynamicOps<O> ops, O value, String key, TypedMap context);

  /** Converts the given list of values into an array */
  <O> A convertArray(DynamicOps<O> ops, List<O> list, String key, TypedMap context);


  /** Serializes the first element into a value */
  <O> O serializeFirst(DynamicOps<O> ops, A object);

  /** Serializes all elements into the passed list */
  <O> void serializeAll(DynamicOps<O> ops, List<O> list, A object);


  /* Implementation */

  @Override
  default A convert(JsonElement element, String key, TypedMap context) {
    return convert(JsonOps.INSTANCE, element, key, context);
  }

  @Override
  default <O> A convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
    // if we allow compact, parse from compact
    if (allowCompact() && !OpsHelper.isList(ops, input)) {
      return convertCompact(ops, input, key, context);
    }
    List<O> list = OpsHelper.getList(ops, input, key);
    checkSize(key, list.size(), ErrorFactory.JSON_SYNTAX_ERROR);
    return convertArray(ops, list, key, context);
  }


  @Override
  default JsonElement serialize(A object) {
    return serialize(JsonOps.INSTANCE, object);
  }

  @Override
  default <O> O serialize(DynamicOps<O> ops, A object) {
    // if we support compact, serialize compact
    int length = getLength(object);
    if (allowCompact() && length == 1) {
      O element = serializeFirst(ops, object);
      // only return if its not a list; lists means a conflict with deserializing
      // there is a small waste of work here in the case of list, but you shouldn't be using compact with list serializing elements anyway
      if (!OpsHelper.isList(ops, element)) {
        return element;
      }
    }
    checkSize("Collection", length, ErrorFactory.RUNTIME);
    List<O> list = new ArrayList<>(length);
    serializeAll(ops, list, object);
    return ops.createList(list.stream());
  }


  /* Fields */

  /** Creates a field that defaults to empty */
  <P> LoadableField<A,P> emptyField(String key, boolean serializeEmpty, Function<P,A> getter);

  /** Creates a field that defaults to empty */
  default <P> LoadableField<A,P> emptyField(String key, Function<P,A> getter) {
    return emptyField(key, false, getter);
  }

  /** Standard implementation of array loadable using a single size parameter */
  interface SizeRange<A> extends ArrayLoadable<A> {
    /** Gets the minimum size */
    int minSize();

    /** Gets the maximum size */
    int maxSize();

    @Override
    default boolean allowCompact() {
      return minSize() < 0;
    }

    @Override
    default void checkSize(String key, int size, ErrorFactory error) {
      // ensure compact min size is displayed as the proper value
      int minSize = minSize();
      if (minSize == COMPACT) {
        minSize = 1;
      } else {
        minSize = 0;
      }
      int maxSize = maxSize();
      if (size < minSize || maxSize < size) {
        if (maxSize == Integer.MAX_VALUE) {
          throw error.create(key + " must have at least " + minSize + " elements");
        } else {
          throw error.create(key + " must have between " + minSize + " and " + maxSize + " elements");
        }
      }
    }
  }
}
