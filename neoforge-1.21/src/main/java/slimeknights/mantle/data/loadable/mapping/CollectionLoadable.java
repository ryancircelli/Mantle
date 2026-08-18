package slimeknights.mantle.data.loadable.mapping;

import com.mojang.serialization.DynamicOps;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Shared base class for a loadable of a collection of elements */
@SuppressWarnings("unused") // API
@RequiredArgsConstructor
public abstract class CollectionLoadable<T,C extends Collection<T>> implements ArrayLoadable<C> {
  /** Loadable for an object */
  protected final Loadable<T> base;
  /** Minimum list size allowed */
  private final int minSize;

  /** Creates a new builder instance for the given expected size */
  protected Collection<T> createBuilder(int size) {
    return new ArrayList<>(size);
  }

  /** Builds the final collection, given the passed mutable collection. */
  protected abstract C build(Collection<T> builder);

  @Override
  public void checkSize(String key, int size, ErrorFactory error) {
    int minSize = this.minSize;
    if (minSize == COMPACT) {
      minSize = 1;
    }
    if (size < minSize) {
      throw error.create(key + " must have at least " + minSize + " elements");
    }
  }

  @Override
  public boolean allowCompact() {
    return minSize < 0;
  }

  @Override
  public int getLength(C array) {
    return array.size();
  }

  @Override
  public <O> C convertCompact(DynamicOps<O> ops, O value, String key, TypedMap context) {
    return build(List.of(base.convert(ops, value, key, context)));
  }

  @Override
  public <O> C convertArray(DynamicOps<O> ops, List<O> list, String key, TypedMap context) {
    Collection<T> builder = createBuilder(list.size());
    for (int i = 0; i < list.size(); i++) {
      builder.add(base.convert(ops, list.get(i), key + '[' + i + ']', context));
    }
    return build(builder);
  }

  @Override
  public <O> O serializeFirst(DynamicOps<O> ops, C collection) {
    return base.serialize(ops, collection.iterator().next());
  }

  @Override
  public <O> void serializeAll(DynamicOps<O> ops, List<O> list, C collection) {
    for (T element : collection) {
      list.add(base.serialize(ops, element));
    }
  }

  @Override
  public C decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    int max = buffer.readVarInt();
    Collection<T> builder = createBuilder(max);
    for (int i = 0; i < max; i++) {
      builder.add(base.decode(buffer, context));
    }
    return build(builder);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, C collection) {
    buffer.writeVarInt(collection.size());
    for (T element : collection) {
      base.encode(buffer, element);
    }
  }
}
