package slimeknights.mantle.data.loadable.array;

import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectFunction;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.DefaultingField;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** Loadable for a byte array */
public record ByteArrayLoadable<T extends Number>(Loadable<T> base, int minSize, int maxSize, Byte2ObjectFunction<T> mapper) implements ArrayLoadable.SizeRange<byte[]> {
  @Override
  public int getLength(byte[] array) {
    return array.length;
  }

  @Override
  public <O> byte[] convertCompact(DynamicOps<O> ops, O value, String key, TypedMap context) {
    return new byte[] { base.convert(ops, value, key, context).byteValue() };
  }

  @Override
  public <O> byte[] convertArray(DynamicOps<O> ops, List<O> list, String key, TypedMap context) {
    byte[] result = new byte[list.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = base.convert(ops, list.get(i), key + '[' + i + ']', context).byteValue();
    }
    return result;
  }

  @Override
  public <O> O serializeFirst(DynamicOps<O> ops, byte[] object) {
    return base.serialize(ops, mapper.get(object[0]));
  }

  @Override
  public <O> void serializeAll(DynamicOps<O> ops, List<O> list, byte[] object) {
    for (byte element : object) {
      list.add(base.serialize(ops, mapper.get(element)));
    }
  }

  @Override
  public byte[] decode(FriendlyByteBuf buffer, TypedMap context) {
    int max = buffer.readVarInt();
    byte[] array = new byte[max];
    for (int i = 0; i < max; i++) {
      array[i] = base.decode(buffer, context).byteValue();
    }
    return array;
  }

  @Override
  public void encode(FriendlyByteBuf buffer, byte[] array) {
    buffer.writeVarInt(array.length);
    for (byte element : array) {
      base.encode(buffer, mapper.get(element));
    }
  }

  @Override
  public <P> LoadableField<byte[],P> defaultField(String key, byte[] defaultValue, boolean serializeDefault, Function<P,byte[]> getter) {
    //noinspection Convert2Diamond  I think the method overloading stops type inferrence here
    return new DefaultingField<byte[],P>(this, key, defaultValue, serializeDefault ? null : Arrays::equals, getter);
  }

  @Override
  public <P> LoadableField<byte[],P> emptyField(String key, boolean serializeEmpty, Function<P,byte[]> getter) {
    return defaultField(key, new byte[0], serializeEmpty, getter);
  }
}
