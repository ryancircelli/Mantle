package slimeknights.mantle.data.loadable.array;

import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.shorts.Short2ObjectFunction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.DefaultingField;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** Loadable for a short array */
public record ShortArrayLoadable<T extends Number>(Loadable<T> base, int minSize, int maxSize, Short2ObjectFunction<T> mapper) implements ArrayLoadable.SizeRange<short[]> {
  @Override
  public int getLength(short[] array) {
    return array.length;
  }

  @Override
  public <O> short[] convertCompact(DynamicOps<O> ops, O value, String key, TypedMap context) {
    return new short[] { base.convert(ops, value, key, context).shortValue() };
  }

  @Override
  public <O> short[] convertArray(DynamicOps<O> ops, List<O> list, String key, TypedMap context) {
    short[] result = new short[list.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = base.convert(ops, list.get(i), key + '[' + i + ']', context).shortValue();
    }
    return result;
  }

  @Override
  public <O> O serializeFirst(DynamicOps<O> ops, short[] object) {
    return base.serialize(ops, mapper.get(object[0]));
  }

  @Override
  public <O> void serializeAll(DynamicOps<O> ops, List<O> list, short[] object) {
    for (short element : object) {
      list.add(base.serialize(ops, mapper.get(element)));
    }
  }

  @Override
  public short[] decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    int max = buffer.readVarInt();
    short[] array = new short[max];
    for (int i = 0; i < max; i++) {
      array[i] = base.decode(buffer, context).shortValue();
    }
    return array;
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, short[] array) {
    buffer.writeVarInt(array.length);
    for (short element : array) {
      base.encode(buffer, mapper.get(element));
    }
  }

  @Override
  public <P> LoadableField<short[],P> defaultField(String key, short[] defaultValue, boolean serializeDefault, Function<P,short[]> getter) {
    //noinspection Convert2Diamond  I think the method overloading stops type inferrence here
    return new DefaultingField<short[],P>(this, key, defaultValue, serializeDefault ? null : Arrays::equals, getter);
  }

  @Override
  public <P> LoadableField<short[],P> emptyField(String key, boolean serializeEmpty, Function<P,short[]> getter) {
    return defaultField(key, new short[0], serializeEmpty, getter);
  }
}
