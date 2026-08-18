package slimeknights.mantle.data.loadable.record;

import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.datafixers.util.Function5;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.util.typed.TypedMap;

/** Record loadable with 4 fields plus the loader itself */
record RecordWithLoader4<A,B,C,D,R>(
  RecordField<A,? super R> fieldA,
  RecordField<B,? super R> fieldB,
  RecordField<C,? super R> fieldC,
  RecordField<D,? super R> fieldD,
  Function5<A,B,C,D,RecordLoadable<R>,R> constructor
) implements RecordLoadable<R> {
  @Override
  public R deserialize(JsonObject json, TypedMap context) {
    return constructor.apply(
      fieldA.get(json, context),
      fieldB.get(json, context),
      fieldC.get(json, context),
      fieldD.get(json, context),
      this
    );
  }

  @Override
  public <O> R deserialize(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    return constructor.apply(
      fieldA.get(ops, map, context),
      fieldB.get(ops, map, context),
      fieldC.get(ops, map, context),
      fieldD.get(ops, map, context),
      this
    );
  }

  @Override
  public void serialize(R object, JsonObject json) {
    fieldA.serialize(object, json);
    fieldB.serialize(object, json);
    fieldC.serialize(object, json);
    fieldD.serialize(object, json);
  }

  @Override
  public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, R object, RecordBuilder<O> builder) {
    builder = OpsHelper.sharedBuilder(ops, builder);
    builder = fieldA.serialize(ops, object, builder);
    builder = fieldB.serialize(ops, object, builder);
    builder = fieldC.serialize(ops, object, builder);
    builder = fieldD.serialize(ops, object, builder);
    return builder;
  }

  @Override
  public R decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return constructor.apply(
      fieldA.decode(buffer, context),
      fieldB.decode(buffer, context),
      fieldC.decode(buffer, context),
      fieldD.decode(buffer, context),
      this
    );
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, R object) {
    fieldA.encode(buffer, object);
    fieldB.encode(buffer, object);
    fieldC.encode(buffer, object);
    fieldD.encode(buffer, object);
  }
}
