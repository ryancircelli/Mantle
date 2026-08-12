package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

/**
 * Field that tries to save the object directly, but falls back to saving it in a key if unable
 * @param <T>  Field type
 * @param <P>  Parent type
 */
public record TryDirectField<T,P>(Loadable<T> loadable, String key, Function<P,T> getter, String... conflicts) implements AlwaysPresentLoadableField<T,P> {
  @Override
  public T get(JsonObject json, String key, TypedMap context) {
    // if we have the nested key, read from that
    if (json.has(key)) {
      return loadable.convert(json.get(key), key, context);
    }
    // try reading from the current object, assumes the loadable supports JSON objects
    return loadable.convert(json, key, context);
  }

  @Override
  public <O> T get(DynamicOps<O> ops, MapLike<O> map, String key, TypedMap context) {
    // if we have the nested key, read from that
    O value = map.get(key);
    if (value != null) {
      return loadable.convert(ops, value, key, context);
    }
    // try reading from the current object, assumes the loadable supports objects
    return loadable.convert(ops, OpsHelper.toValue(ops, map), key, context);
  }

  /** Checks if the serialized object conflicts with a key we know about */
  private <O> boolean hasConflict(DynamicOps<O> ops, RecordBuilder<O> builder, MapLike<O> serialized) {
    if (serialized.get(key) != null) {
      return true;
    }
    // check all the keys written by the fields before us, if any of them exist then this conflicts
    for (Pair<O,O> entry : serialized.entries().toList()) {
      if (OpsHelper.getWritten(builder, OpsHelper.getString(ops, entry.getFirst(), key)) != null) {
        return true;
      }
    }
    // check additional conflicts passed into the field, for the sake of optional fields mostly
    for (String conflict : conflicts) {
      if (serialized.get(conflict) != null) {
        return true;
      }
    }
    return false;
  }

  @Override
  public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, P parent, RecordBuilder<O> builder) {
    O element = loadable.serialize(ops, getter.apply(parent));
    Optional<MapLike<O>> serialized = ops.getMap(element).result();
    if (serialized.isPresent() && !hasConflict(ops, builder, serialized.get())) {
      for (Pair<O,O> entry : serialized.get().entries().toList()) {
        builder = builder.add(entry.getFirst(), entry.getSecond());
      }
      return builder;
    }
    return builder.add(key, element);
  }

  /** Checks if the JSON has any conflicting keys */
  private boolean hasConflict(JsonObject parent, JsonObject serialized) {
    if (serialized.has(key)) {
      return true;
    }
    // check all the keys in the parent so far, if any of them exist then this conflicts
    for (String conflict : parent.keySet()) {
      if (serialized.has(conflict)) {
        return true;
      }
    }
    // check additional conflicts passed into the field, for the sake of optional fields mostly
    for (String conflict : conflicts) {
      if (serialized.has(conflict)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    JsonElement element = loadable.serialize(getter.apply(parent));
    if (element.isJsonObject()) {
      JsonObject serialized = element.getAsJsonObject();
      // if the serialized element contains the key, we cannot store it directly as that will confuse deserializing
      if (!hasConflict(json, serialized)) {
        for (Entry<String,JsonElement> entry : serialized.entrySet()) {
          json.add(entry.getKey(), entry.getValue());
        }
        return;
      }
    }
    json.add(key, element);
  }
}
