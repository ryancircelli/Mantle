package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * A record loadable that loads directly into the parent instead of nesting.
 * @param <P>  Parent object
 * @param <T>  Loadable type
 */
public record DirectField<T,P>(RecordLoadable<T> loadable, Function<P,T> getter) implements AlwaysPresentRecordField<T,P> {
  @Override
  public T get(JsonObject json, TypedMap context) {
    return loadable.deserialize(json, context);
  }

  @Override
  public <O> T get(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    return loadable.deserialize(ops, map, context);
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    loadable.serialize(getter.apply(parent), json);
  }

  @Override
  public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, P parent, RecordBuilder<O> builder) {
    return loadable.serialize(ops, getter.apply(parent), builder);
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return loadable.decode(buffer, context);
  }
}
