package slimeknights.mantle.data.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.field.AlwaysPresentRecordField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

/** Direct field for a registry which maps the type to a different key to prevent conflict */
public record MergingRegistryField<T,P>(RecordLoadable<T> loadable, String typeKey, Function<P,T> getter) implements AlwaysPresentRecordField<T,P> {
  /** Moves the passed type key to "type" */
  public static void mapType(JsonObject json, String typeKey) {
    json.addProperty("type", GsonHelper.getAsString(json, typeKey));
    json.remove(typeKey);
  }

  /**
   * Serializes the passed object into the passed JSON
   * @param json      JSON target for serializing
   * @param typeKey   Key to use for "type" in the serialized value
   * @param element   Element to insert
   */
  public static void serializeInto(JsonObject json, String typeKey, JsonElement element) {
    // if its an object, copy all the data over
    if (element.isJsonObject()) {
      JsonObject nestedObject = element.getAsJsonObject();
      for (Entry<String, JsonElement> entry : nestedObject.entrySet()) {
        String key = entry.getKey();
        if (typeKey.equals(key)) {
          throw new JsonIOException("Unable to serialize nested object, object already has key " + typeKey);
        }
        if ("type".equals(key)) {
          key = typeKey;
        }
        json.add(key, entry.getValue());
      }
    } else if (element.isJsonPrimitive()){
      // if its a primitive, its the type ID, add just that by itself
      json.add(typeKey, element);
    } else {
      throw new JsonIOException("Unable to serialize nested object, expected string or object");
    }
  }

  @Override
  public T get(JsonObject json, TypedMap context) {
    // replace our type with the nested type, then run the nested loader
    mapType(json, typeKey);
    return loadable.deserialize(json, context);
  }

  @Override
  public <O> T get(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    // replace our type with the nested type, then run the nested loader
    O type = map.get(typeKey);
    if (type == null) {
      throw new JsonSyntaxException("Missing JSON field '" + typeKey + "'");
    }
    Map<O,O> copy = new LinkedHashMap<>();
    map.entries().forEach(entry -> copy.put(entry.getFirst(), entry.getSecond()));
    copy.remove(ops.createString(typeKey));
    copy.put(ops.createString("type"), type);
    return loadable.deserialize(ops, OpsHelper.getMap(ops, ops.createMap(copy), typeKey), context);
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    serializeInto(json, typeKey, loadable.serialize(getter.apply(parent)));
  }

  @Override
  public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, P parent, RecordBuilder<O> builder) {
    O element = loadable.serialize(ops, getter.apply(parent));
    // if its an object, copy all the data over
    Optional<MapLike<O>> map = ops.getMap(element).result();
    if (map.isPresent()) {
      for (Pair<O,O> entry : map.get().entries().toList()) {
        String key = OpsHelper.getString(ops, entry.getFirst(), typeKey);
        if (typeKey.equals(key)) {
          throw new JsonIOException("Unable to serialize nested object, object already has key " + typeKey);
        }
        if ("type".equals(key)) {
          key = typeKey;
        }
        builder = builder.add(key, entry.getSecond());
      }
      return builder;
    }
    // if its a string, its the type ID, add just that by itself
    if (ops.getStringValue(element).result().isPresent()) {
      return builder.add(typeKey, element);
    }
    throw new JsonIOException("Unable to serialize nested object, expected string or object");
  }
}
