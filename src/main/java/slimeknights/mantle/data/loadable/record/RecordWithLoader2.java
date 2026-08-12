package slimeknights.mantle.data.loadable.record;

import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.datafixers.util.Function3;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.util.typed.TypedMap;

/** Record loadable with 2 fields plus the loader itself */
record RecordWithLoader2<A,B,R>(
  RecordField<A,? super R> fieldA,
  RecordField<B,? super R> fieldB,
  Function3<A,B,RecordLoadable<R>,R> constructor
) implements RecordLoadable<R> {
  @Override
  public R deserialize(JsonObject json, TypedMap context) {
    return constructor.apply(
      fieldA.get(json, context),
      fieldB.get(json, context),
      this
    );
  }

  @Override
  public <O> R deserialize(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    return constructor.apply(
      fieldA.get(ops, map, context),
      fieldB.get(ops, map, context),
      this
    );
  }

  @Override
  public void serialize(R object, JsonObject json) {
    fieldA.serialize(object, json);
    fieldB.serialize(object, json);
  }

  @Override
  public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, R object, RecordBuilder<O> builder) {
    builder = fieldA.serialize(ops, object, builder);
    builder = fieldB.serialize(ops, object, builder);
    return builder;
  }

  @Override
  public R decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return constructor.apply(
      fieldA.decode(buffer, context),
      fieldB.decode(buffer, context),
      this
    );
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, R object) {
    fieldA.encode(buffer, object);
    fieldB.encode(buffer, object);
  }
}
