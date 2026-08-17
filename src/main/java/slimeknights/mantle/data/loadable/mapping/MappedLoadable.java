package slimeknights.mantle.data.loadable.mapping;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.BiFunction;
import java.util.function.Function;

/** Represents a trivially mapped loadable that serializes/writes to network like another loadable */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MappedLoadable<F,T> implements Loadable<T> {
  private final Loadable<F> base;
  protected final BiFunction<F,ErrorFactory,T> from;
  protected final BiFunction<T,ErrorFactory,F> to;

  /** Creates a new loadable for a non-record loadable */
  public static <T,F> Loadable<T> of(Loadable<F> base, BiFunction<F,ErrorFactory,T> from, BiFunction<T,ErrorFactory,F> to) {
    return new MappedLoadable<>(base, from, to);
  }

  /** Creates a new loadable for a record loadable */
  public static <T,F> RecordLoadable<T> of(RecordLoadable<F> base, BiFunction<F,ErrorFactory,T> from, BiFunction<T,ErrorFactory,F> to) {
    return new Record<>(base, from, to);
  }

  /** Creates a new loadable for a record loadable */
  public static <T,F> StringLoadable<T> of(StringLoadable<F> base, BiFunction<F,ErrorFactory,T> from, BiFunction<T,ErrorFactory,F> to) {
    return new StringMapped<>(base, from, to);
  }

  /** Flattens the given mapping function */
  public static <T,R> BiFunction<T,ErrorFactory,R> flatten(Function<T,R> function) {
    return (value, error) -> function.apply(value);
  }

  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    return from.apply(base.convert(element, key, context), ErrorFactory.JSON_SYNTAX_ERROR);
  }

  @Override
  public <O> T convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
    return from.apply(base.convert(ops, input, key, context), ErrorFactory.JSON_SYNTAX_ERROR);
  }

  @Override
  public JsonElement serialize(T object) {
    return base.serialize(to.apply(object, ErrorFactory.RUNTIME));
  }

  @Override
  public <O> O serialize(DynamicOps<O> ops, T object) {
    return base.serialize(ops, to.apply(object, ErrorFactory.RUNTIME));
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return from.apply(base.decode(buffer, context), ErrorFactory.DECODER_EXCEPTION);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T object) {
    base.encode(buffer, to.apply(object, ErrorFactory.ENCODER_EXCEPTION));
  }

  /** Implementation for records */
  private static class Record<F,T> extends MappedLoadable<F,T> implements RecordLoadable<T> {
    private final RecordLoadable<F> base;
    protected Record(RecordLoadable<F> base, BiFunction<F,ErrorFactory,T> from, BiFunction<T,ErrorFactory,F> to) {
      super(base, from, to);
      this.base = base;
    }

    @Override
    public T deserialize(JsonObject json, TypedMap context) {
      return from.apply(base.deserialize(json, context), ErrorFactory.JSON_SYNTAX_ERROR);
    }

    @Override
    public <O> T deserialize(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
      return from.apply(base.deserialize(ops, map, context), ErrorFactory.JSON_SYNTAX_ERROR);
    }

    @Override
    public void serialize(T object, JsonObject json) {
      base.serialize(to.apply(object, ErrorFactory.RUNTIME), json);
    }

    @Override
    public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, T object, RecordBuilder<O> builder) {
      return base.serialize(ops, to.apply(object, ErrorFactory.RUNTIME), builder);
    }

    @Override
    public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return from.apply(base.decode(buffer, context), ErrorFactory.DECODER_EXCEPTION);
    }
  }

  /** Implementation for strings */
  private static class StringMapped<F,T> extends MappedLoadable<F,T> implements StringLoadable<T> {
    private final StringLoadable<F> base;
    protected StringMapped(StringLoadable<F> base, BiFunction<F,ErrorFactory,T> from, BiFunction<T,ErrorFactory,F> to) {
      super(base, from, to);
      this.base = base;
    }

    @Override
    public T parseString(String value, String key, TypedMap context) {
      return from.apply(base.parseString(value, key, context), ErrorFactory.JSON_SYNTAX_ERROR);
    }

    @Override
    public String getString(T object) {
      return base.getString(to.apply(object, ErrorFactory.RUNTIME));
    }
  }
}
