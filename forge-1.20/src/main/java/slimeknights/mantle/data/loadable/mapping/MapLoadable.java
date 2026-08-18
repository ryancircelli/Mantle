package slimeknights.mantle.data.loadable.mapping;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Loadable for a map type.
 * @param <K>  Key type
 * @param <V>  Value type
 */
public class MapLoadable<K, V> implements Loadable<Map<K,V>> {
  protected final StringLoadable<K> keyLoadable;
  protected final Loadable<V> valueLoadable;
  protected final int minSize;

  /**
   * Creates a new map loadable
   * @param keyLoadable    Loadable for the map keys, parsed from strings
   * @param valueLoadable  Loadable for map values, parsed from elements
   * @param minSize        Minimum size for the map to be valid
   */
  public MapLoadable(StringLoadable<K> keyLoadable, Loadable<V> valueLoadable, int minSize) {
    this.keyLoadable = keyLoadable;
    this.valueLoadable = valueLoadable;
    this.minSize = minSize;
  }

  /** Builds the final map, given the passed mutable map. */
  protected Map<K,V> build(Map<K,V> builder) {
    return Map.copyOf(builder);
  }

  /** Creates a new builder instance for the given expected size */
  protected Map<K,V> createBuilder(int size) {
    return new HashMap<>(size);
  }

  @Override
  public Map<K,V> convert(JsonElement element, String key, TypedMap context) {
    return convert(JsonOps.INSTANCE, element, key, context);
  }

  @Override
  public <O> Map<K,V> convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
    List<Pair<O,O>> entries = OpsHelper.getMap(ops, input, key).entries().toList();
    if (entries.size() < minSize) {
      throw new JsonSyntaxException(key + " must have at least " + minSize + " elements");
    }
    Map<K,V> builder = createBuilder(entries.size());
    String mapKey = key + "'s key";
    for (Pair<O,O> entry : entries) {
      String entryKey = OpsHelper.getString(ops, entry.getFirst(), mapKey);
      builder.put(
        keyLoadable.parseString(entryKey, mapKey),
        valueLoadable.convert(ops, entry.getSecond(), entryKey, context));
    }
    return build(builder);
  }

  @Override
  public JsonElement serialize(Map<K,V> map) {
    return serialize(JsonOps.INSTANCE, map);
  }

  @Override
  public <O> O serialize(DynamicOps<O> ops, Map<K,V> map) {
    if (map.size() < minSize) {
      throw new RuntimeException("Collection must have at least " + minSize + " elements");
    }
    return ops.createMap(map.entrySet().stream().map(entry -> Pair.of(
      ops.createString(keyLoadable.getString(entry.getKey())),
      valueLoadable.serialize(ops, entry.getValue()))));
  }

  @Override
  public Map<K,V> decode(FriendlyByteBuf buffer, TypedMap context) {
    int size = buffer.readVarInt();
    Map<K,V> builder = createBuilder(size);
    for (int i = 0; i < size; i++) {
      builder.put(
        keyLoadable.decode(buffer, context),
        valueLoadable.decode(buffer, context));
    }
    return build(builder);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, Map<K,V> map) {
    buffer.writeVarInt(map.size());
    for (Entry<K,V> entry : map.entrySet()) {
      keyLoadable.encode(buffer, entry.getKey());
      valueLoadable.encode(buffer, entry.getValue());
    }
  }

  /** Creates a field that defaults to empty */
  public <P> LoadableField<Map<K,V>,P> emptyField(String key, boolean serializeEmpty, Function<P,Map<K,V>> getter) {
    return defaultField(key, Map.of(), serializeEmpty, getter);
  }

  /** Creates a field that defaults to empty */
  public <P> LoadableField<Map<K,V>,P> emptyField(String key, Function<P,Map<K,V>> getter) {
    return emptyField(key, false, getter);
  }

  @Override
  public String toString() {
    return "MapLoadable[keyLoadable=" + keyLoadable + ", " +
           "valueLoadable=" + valueLoadable + ", " +
           "minSize=" + minSize + ']';
  }
}
