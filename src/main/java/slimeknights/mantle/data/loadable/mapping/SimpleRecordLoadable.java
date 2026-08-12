package slimeknights.mantle.data.loadable.mapping;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;

/**
 * Implements a record loadable with a single key.
 * @param loadable      Loadable for parsing
 * @param key           Key used in object form
 * @param defaultValue  If non-null, will be used as the value if the object is empty.
 * @param compact       If true, serializes using the loadable instead of in object form.
 */
public record SimpleRecordLoadable<T>(Loadable<T> loadable, String key, @Nullable T defaultValue, boolean compact) implements RecordLoadable<T> {
  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    if (!element.isJsonObject()) {
      return loadable.convert(element, key, context);
    }
    return RecordLoadable.super.convert(element, key, context);
  }

  @Override
  public <O> T convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
    if (!OpsHelper.isMap(ops, input)) {
      return loadable.convert(ops, input, key, context);
    }
    return RecordLoadable.super.convert(ops, input, key, context);
  }

  @Override
  public T deserialize(JsonObject json, TypedMap context) {
    if (defaultValue != null) {
      return loadable.getOrDefault(json, key, defaultValue, context);
    } else {
      return loadable.getIfPresent(json, key, context);
    }
  }

  @Override
  public <O> T deserialize(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    if (defaultValue != null) {
      return loadable.getOrDefault(ops, map, key, defaultValue, context);
    } else {
      return loadable.getIfPresent(ops, map, key, context);
    }
  }

  @Override
  public JsonElement serialize(T object) {
    if (compact) {
      return loadable.serialize(object);
    }
    return RecordLoadable.super.serialize(object);
  }

  @Override
  public <O> O serialize(DynamicOps<O> ops, T object) {
    if (compact) {
      return loadable.serialize(ops, object);
    }
    return RecordLoadable.super.serialize(ops, object);
  }

  @Override
  public void serialize(T object, JsonObject json) {
    json.add(key, loadable.serialize(object));
  }

  @Override
  public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, T object, RecordBuilder<O> builder) {
    return builder.add(key, loadable.serialize(ops, object));
  }

  @Override
  public void encode(FriendlyByteBuf buffer, T value) {
    loadable.encode(buffer, value);
  }

  @Override
  public T decode(FriendlyByteBuf buffer, TypedMap context) {
    return loadable.decode(buffer, context);
  }
}
