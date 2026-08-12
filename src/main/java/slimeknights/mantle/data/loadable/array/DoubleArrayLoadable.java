package slimeknights.mantle.data.loadable.array;

import com.mojang.serialization.DynamicOps;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.DefaultingField;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** Loadable for a double array */
public record DoubleArrayLoadable(Loadable<Double> base, int minSize, int maxSize) implements ArrayLoadable.SizeRange<double[]> {
  @Override
  public int getLength(double[] array) {
    return array.length;
  }

  @Override
  public <O> double[] convertCompact(DynamicOps<O> ops, O value, String key, TypedMap context) {
    return new double[] { base.convert(ops, value, key, context) };
  }

  @Override
  public <O> double[] convertArray(DynamicOps<O> ops, List<O> list, String key, TypedMap context) {
    double[] result = new double[list.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = base.convert(ops, list.get(i), key + '[' + i + ']', context);
    }
    return result;
  }

  @Override
  public <O> O serializeFirst(DynamicOps<O> ops, double[] object) {
    return base.serialize(ops, object[0]);
  }

  @Override
  public <O> void serializeAll(DynamicOps<O> ops, List<O> list, double[] object) {
    for (double element : object) {
      list.add(base.serialize(ops, element));
    }
  }

  @Override
  public double[] decode(FriendlyByteBuf buffer, TypedMap context) {
    int max = buffer.readVarInt();
    double[] array = new double[max];
    for (int i = 0; i < max; i++) {
      array[i] = base.decode(buffer, context);
    }
    return array;
  }

  @Override
  public void encode(FriendlyByteBuf buffer, double[] array) {
    buffer.writeVarInt(array.length);
    for (double element : array) {
      base.encode(buffer, element);
    }
  }

  @Override
  public <P> LoadableField<double[],P> defaultField(String key, double[] defaultValue, boolean serializeDefault, Function<P,double[]> getter) {
    //noinspection Convert2Diamond  I think the method overloading stops type inferrence here
    return new DefaultingField<double[],P>(this, key, defaultValue, serializeDefault ? null : Arrays::equals, getter);
  }

  @Override
  public <P> LoadableField<double[],P> emptyField(String key, boolean serializeEmpty, Function<P,double[]> getter) {
    return defaultField(key, new double[0], serializeEmpty, getter);
  }
}
