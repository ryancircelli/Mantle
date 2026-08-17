package slimeknights.mantle.data.loadable.array;

import com.mojang.serialization.DynamicOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.DefaultingField;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** Loadable for an integer array */
public record IntArrayLoadable(Loadable<Integer> base, int minSize, int maxSize) implements ArrayLoadable.SizeRange<int[]> {
  @Override
  public int getLength(int[] array) {
    return array.length;
  }

  @Override
  public <O> int[] convertCompact(DynamicOps<O> ops, O value, String key, TypedMap context) {
    return new int[] { base.convert(ops, value, key, context) };
  }

  @Override
  public <O> int[] convertArray(DynamicOps<O> ops, List<O> list, String key, TypedMap context) {
    int[] result = new int[list.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = base.convert(ops, list.get(i), key + '[' + i + ']', context);
    }
    return result;
  }

  @Override
  public <O> O serializeFirst(DynamicOps<O> ops, int[] object) {
    return base.serialize(ops, object[0]);
  }

  @Override
  public <O> void serializeAll(DynamicOps<O> ops, List<O> list, int[] object) {
    for (int element : object) {
      list.add(base.serialize(ops, element));
    }
  }

  @Override
  public int[] decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    int max = buffer.readVarInt();
    int[] array = new int[max];
    for (int i = 0; i < max; i++) {
      array[i] = base.decode(buffer, context);
    }
    return array;
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, int[] array) {
    buffer.writeVarInt(array.length);
    for (int element : array) {
      base.encode(buffer, element);
    }
  }

  @Override
  public <P> LoadableField<int[],P> defaultField(String key, int[] defaultValue, boolean serializeDefault, Function<P,int[]> getter) {
    //noinspection Convert2Diamond  I think the method overloading stops type inferrence here
    return new DefaultingField<int[],P>(this, key, defaultValue, serializeDefault ? null : Arrays::equals, getter);
  }

  @Override
  public <P> LoadableField<int[],P> emptyField(String key, boolean serializeEmpty, Function<P,int[]> getter) {
    return defaultField(key, new int[0], serializeEmpty, getter);
  }
}
