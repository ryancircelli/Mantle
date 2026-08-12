package slimeknights.mantle.data.loadable.record;

import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.datafixers.util.Function11;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.util.typed.TypedMap;

/** Record loadable with 11 fields */
@SuppressWarnings("DuplicatedCode")
record RecordLoadable11<A,B,C,D,E,F,G,H,I,J,K,R>(
  RecordField<A,? super R> fieldA,
  RecordField<B,? super R> fieldB,
  RecordField<C,? super R> fieldC,
  RecordField<D,? super R> fieldD,
  RecordField<E,? super R> fieldE,
  RecordField<F,? super R> fieldF,
  RecordField<G,? super R> fieldG,
  RecordField<H,? super R> fieldH,
  RecordField<I,? super R> fieldI,
  RecordField<J,? super R> fieldJ,
  RecordField<K,? super R> fieldK,
  Function11<A,B,C,D,E,F,G,H,I,J,K,R> constructor
) implements RecordLoadable<R> {
  @Override
  public R deserialize(JsonObject json, TypedMap context) {
    return constructor.apply(
      fieldA.get(json, context),
      fieldB.get(json, context),
      fieldC.get(json, context),
      fieldD.get(json, context),
      fieldE.get(json, context),
      fieldF.get(json, context),
      fieldG.get(json, context),
      fieldH.get(json, context),
      fieldI.get(json, context),
      fieldJ.get(json, context),
      fieldK.get(json, context)
    );
  }

  @Override
  public <O> R deserialize(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    return constructor.apply(
      fieldA.get(ops, map, context),
      fieldB.get(ops, map, context),
      fieldC.get(ops, map, context),
      fieldD.get(ops, map, context),
      fieldE.get(ops, map, context),
      fieldF.get(ops, map, context),
      fieldG.get(ops, map, context),
      fieldH.get(ops, map, context),
      fieldI.get(ops, map, context),
      fieldJ.get(ops, map, context),
      fieldK.get(ops, map, context)
    );
  }

  @Override
  public void serialize(R object, JsonObject json) {
    fieldA.serialize(object, json);
    fieldB.serialize(object, json);
    fieldC.serialize(object, json);
    fieldD.serialize(object, json);
    fieldE.serialize(object, json);
    fieldF.serialize(object, json);
    fieldG.serialize(object, json);
    fieldH.serialize(object, json);
    fieldI.serialize(object, json);
    fieldJ.serialize(object, json);
    fieldK.serialize(object, json);
  }

  @Override
  public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, R object, RecordBuilder<O> builder) {
    builder = fieldA.serialize(ops, object, builder);
    builder = fieldB.serialize(ops, object, builder);
    builder = fieldC.serialize(ops, object, builder);
    builder = fieldD.serialize(ops, object, builder);
    builder = fieldE.serialize(ops, object, builder);
    builder = fieldF.serialize(ops, object, builder);
    builder = fieldG.serialize(ops, object, builder);
    builder = fieldH.serialize(ops, object, builder);
    builder = fieldI.serialize(ops, object, builder);
    builder = fieldJ.serialize(ops, object, builder);
    builder = fieldK.serialize(ops, object, builder);
    return builder;
  }

  @Override
  public R decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return constructor.apply(
      fieldA.decode(buffer, context),
      fieldB.decode(buffer, context),
      fieldC.decode(buffer, context),
      fieldD.decode(buffer, context),
      fieldE.decode(buffer, context),
      fieldF.decode(buffer, context),
      fieldG.decode(buffer, context),
      fieldH.decode(buffer, context),
      fieldI.decode(buffer, context),
      fieldJ.decode(buffer, context),
      fieldK.decode(buffer, context)
    );
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, R object) {
    fieldA.encode(buffer, object);
    fieldB.encode(buffer, object);
    fieldC.encode(buffer, object);
    fieldD.encode(buffer, object);
    fieldE.encode(buffer, object);
    fieldF.encode(buffer, object);
    fieldG.encode(buffer, object);
    fieldH.encode(buffer, object);
    fieldI.encode(buffer, object);
    fieldJ.encode(buffer, object);
    fieldK.encode(buffer, object);
  }
}
