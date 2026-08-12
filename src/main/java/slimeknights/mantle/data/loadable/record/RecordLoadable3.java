package slimeknights.mantle.data.loadable.record;

import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.datafixers.util.Function3;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.util.typed.TypedMap;

/** Record loadable with 3 fields */
record RecordLoadable3<A,B,C,R>(
  RecordField<A,? super R> fieldA,
  RecordField<B,? super R> fieldB,
  RecordField<C,? super R> fieldC,
  Function3<A,B,C,R> constructor
) implements RecordLoadable<R> {
  @Override
  public R deserialize(JsonObject json, TypedMap context) {
    return constructor.apply(
      fieldA.get(json, context),
      fieldB.get(json, context),
      fieldC.get(json, context)
    );
  }

  @Override
  public <O> R deserialize(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    return constructor.apply(
      fieldA.get(ops, map, context),
      fieldB.get(ops, map, context),
      fieldC.get(ops, map, context)
    );
  }

  @Override
  public void serialize(R object, JsonObject json) {
    fieldA.serialize(object, json);
    fieldB.serialize(object, json);
    fieldC.serialize(object, json);
  }

  @Override
  public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, R object, RecordBuilder<O> builder) {
    builder = fieldA.serialize(ops, object, builder);
    builder = fieldB.serialize(ops, object, builder);
    builder = fieldC.serialize(ops, object, builder);
    return builder;
  }

  @Override
  public R decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return constructor.apply(
      fieldA.decode(buffer, context),
      fieldB.decode(buffer, context),
      fieldC.decode(buffer, context)
    );
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, R object) {
    fieldA.encode(buffer, object);
    fieldB.encode(buffer, object);
    fieldC.encode(buffer, object);
  }
}
