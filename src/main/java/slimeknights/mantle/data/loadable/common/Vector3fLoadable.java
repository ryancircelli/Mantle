package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import org.joml.Vector3f;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.List;
import java.util.stream.Stream;

/** Loadable for {@link Vector3f}. Supports reading as an array or a JSON object. */
public enum Vector3fLoadable implements RecordLoadable<Vector3f> {
  INSTANCE;

  @Override
  public Vector3f convert(JsonElement element, String key, TypedMap context) {
    return convert(JsonOps.INSTANCE, element, key, context);
  }

  @Override
  public <O> Vector3f convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
    if (OpsHelper.isList(ops, input)) {
      List<O> list = OpsHelper.getList(ops, input, key);
      if (list.size() != 3) {
        throw new JsonParseException("Expected " + key + " to be size 3, found " + list.size());
      }
      return new Vector3f(
        OpsHelper.getNumber(ops, list.get(0), key + "[0]").floatValue(),
        OpsHelper.getNumber(ops, list.get(1), key + "[1]").floatValue(),
        OpsHelper.getNumber(ops, list.get(2), key + "[2]").floatValue()
      );
    }
    return RecordLoadable.super.convert(ops, input, key, context);
  }

  @Override
  public Vector3f deserialize(JsonObject json, TypedMap context) {
    return new Vector3f(
      GsonHelper.getAsFloat(json, "x"),
      GsonHelper.getAsFloat(json, "y"),
      GsonHelper.getAsFloat(json, "z")
    );
  }

  /** Reads a required float field from a map */
  private static <O> float getFloat(DynamicOps<O> ops, MapLike<O> map, String key) {
    O value = map.get(key);
    if (value == null) {
      throw new JsonParseException("Missing JSON field '" + key + "'");
    }
    return OpsHelper.getNumber(ops, value, key).floatValue();
  }

  @Override
  public <O> Vector3f deserialize(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    return new Vector3f(getFloat(ops, map, "x"), getFloat(ops, map, "y"), getFloat(ops, map, "z"));
  }

  @Override
  public JsonElement serialize(Vector3f vector) {
    return serialize(JsonOps.INSTANCE, vector);
  }

  @Override
  public <O> O serialize(DynamicOps<O> ops, Vector3f vector) {
    return ops.createList(Stream.of(ops.createFloat(vector.x()), ops.createFloat(vector.y()), ops.createFloat(vector.z())));
  }

  @Override
  public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, Vector3f vector, RecordBuilder<O> builder) {
    if (vector.x != 0) builder = builder.add("x", ops.createFloat(vector.x));
    if (vector.y != 0) builder = builder.add("y", ops.createFloat(vector.y));
    if (vector.z != 0) builder = builder.add("z", ops.createFloat(vector.z));
    return builder;
  }

  @Override
  public void serialize(Vector3f vector, JsonObject json) {
    if (vector.x != 0) json.addProperty("x", vector.x);
    if (vector.y != 0) json.addProperty("y", vector.y);
    if (vector.z != 0) json.addProperty("z", vector.z);
  }

  @Override
  public Vector3f decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return buffer.readVector3f();
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, Vector3f value) {
    buffer.writeVector3f(value);
  }
}
