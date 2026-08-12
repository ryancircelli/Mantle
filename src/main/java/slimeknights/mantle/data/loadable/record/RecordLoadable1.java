package slimeknights.mantle.data.loadable.record;

import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/** Record loadable with a single field */
record RecordLoadable1<A,R>(
  RecordField<A,? super R> fieldA,
  Function<A,R> constructor
) implements RecordLoadable<R> {
  @Override
  public R deserialize(JsonObject json, TypedMap context) {
    return constructor.apply(fieldA.get(json, context));
  }

  @Override
  public <O> R deserialize(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    return constructor.apply(fieldA.get(ops, map, context));
  }

  @Override
  public void serialize(R object, JsonObject json) {
    fieldA.serialize(object, json);
  }

  @Override
  public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, R object, RecordBuilder<O> builder) {
    builder = fieldA.serialize(ops, object, builder);
    return builder;
  }

  @Override
  public R decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return constructor.apply(fieldA.decode(buffer, context));
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, R object) {
    fieldA.encode(buffer, object);
  }
}
