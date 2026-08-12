package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;

/**
 * Helpers for reading and writing values of an arbitrary {@link DynamicOps} format, plus bridges between such a
 * format and the gson methods on {@link Loadable}.
 * <p>
 * The readers here are the format neutral counterparts of {@link net.minecraft.util.GsonHelper}; like that class they
 * take the key containing the value purely to produce a readable error. The bridges are used by the default
 * implementations of the ops based loadable methods, and are useful for any loadable choosing to implement just one of
 * the two sides.
 */
@SuppressWarnings("unused")  // API
public class OpsHelper {
  private OpsHelper() {}


  /* Registries */

  /**
   * Upgrades the passed ops to one able to read registry backed values, if the context supplies the registries.
   * <p>
   * A handful of 1.21 formats cannot be read without the registries behind them: a data component patch holding an
   * enchantment, anything referencing a datapack registry. Vanilla threads those through the ops as a
   * {@link RegistryOps}, and a loadable given one simply passes it along, which is the path every datapack load takes.
   * This exists for the caller which holds the registries but was handed a plain ops, and supplies them through
   * {@link ContextKey#REGISTRY_ACCESS} instead.
   * @param ops      Ops the loadable was called with
   * @param context  Loadable context, possibly holding {@link ContextKey#REGISTRY_ACCESS}
   * @param <O>      Format of the value
   * @return  The passed ops if it already reaches the registries or the context has none, otherwise a
   *          {@link RegistryOps} over it.
   */
  public static <O> DynamicOps<O> withRegistries(DynamicOps<O> ops, TypedMap context) {
    if (ops instanceof RegistryOps) {
      return ops;
    }
    HolderLookup.Provider registries = context.get(ContextKey.REGISTRY_ACCESS);
    if (registries != null) {
      return registries.createSerializationContext(ops);
    }
    return ops;
  }


  /* Reading */

  /** Unwraps the result, throwing a message keyed by the field name if it failed */
  private static <T> T orThrow(DataResult<T> result, String key, String expected) {
    return result.result().orElseThrow(() -> ErrorFactory.JSON_SYNTAX_ERROR.create("Expected " + key + " to be " + expected));
  }

  /**
   * {@return true if the passed value is missing or the empty value for its format}
   * Notably, this is a JSON null for {@link JsonOps} and an end tag for {@link net.minecraft.nbt.NbtOps}.
   */
  public static <O> boolean isEmpty(DynamicOps<O> ops, @Nullable O value) {
    return value == null || value.equals(ops.empty());
  }

  /**
   * Reads a number from a value of an arbitrary format.
   * @throws com.google.gson.JsonSyntaxException  If the value is not a number
   */
  public static <O> Number getNumber(DynamicOps<O> ops, O input, String key) {
    return orThrow(ops.getNumberValue(input), key, "a number");
  }

  /**
   * Reads a string from a value of an arbitrary format.
   * @throws com.google.gson.JsonSyntaxException  If the value is not a string
   */
  public static <O> String getString(DynamicOps<O> ops, O input, String key) {
    return orThrow(ops.getStringValue(input), key, "a string");
  }

  /**
   * Reads a map from a value of an arbitrary format.
   * @throws com.google.gson.JsonSyntaxException  If the value is not a map
   */
  public static <O> MapLike<O> getMap(DynamicOps<O> ops, O input, String key) {
    return orThrow(ops.getMap(input), key, "an object");
  }

  /**
   * Reads a list from a value of an arbitrary format.
   * @throws com.google.gson.JsonSyntaxException  If the value is not a list
   */
  public static <O> List<O> getList(DynamicOps<O> ops, O input, String key) {
    return orThrow(ops.getStream(input), key, "an array").toList();
  }

  /** {@return true if the passed value is a map in its format} */
  public static <O> boolean isMap(DynamicOps<O> ops, O input) {
    return ops.getMap(input).result().isPresent();
  }

  /** {@return true if the passed value is a list in its format} */
  public static <O> boolean isList(DynamicOps<O> ops, O input) {
    return ops.getStream(input).result().isPresent();
  }

  /** Converts a map into a single value of its format, used when a loadable wishes to treat a map as an element. */
  public static <O> O toValue(DynamicOps<O> ops, MapLike<O> map) {
    return ops.createMap(map.entries());
  }

  /** Shared instance of the empty map, safe to reuse as it has no state */
  private static final MapLike<?> EMPTY_MAP = new MapLike<>() {
    @Nullable
    @Override
    public Object get(Object key) {
      return null;
    }

    @Nullable
    @Override
    public Object get(String key) {
      return null;
    }

    @Override
    public Stream<Pair<Object,Object>> entries() {
      return Stream.empty();
    }

    @Override
    public String toString() {
      return "MapLike[]";
    }
  };

  /** {@return a map with no entries in the given format}, used by loadables supporting a form with no fields */
  @SuppressWarnings("unchecked")  // safe, the map has no values to be of the wrong type
  public static <O> MapLike<O> emptyMap() {
    return (MapLike<O>)EMPTY_MAP;
  }


  /* Bridging to gson */

  /**
   * Converts a value of an arbitrary format into a JSON element.
   * @param ops    Ops representing the format of the value
   * @param input  Value to convert
   * @param <O>    Format of the value
   * @return  Value as a JSON element
   */
  public static <O> JsonElement toJson(DynamicOps<O> ops, O input) {
    // if the value is already gson there is nothing to do, notably relevant as JSON is the format loadables see most
    if (input instanceof JsonElement json) {
      return json;
    }
    return ops.convertTo(JsonOps.INSTANCE, input);
  }

  /**
   * Converts a map of an arbitrary format into a JSON object.
   * @param ops  Ops representing the format of the map
   * @param map  Map to convert
   * @param <O>  Format of the map
   * @return  Map as a JSON object
   * @throws com.google.gson.JsonSyntaxException  If any key in the map is not a string
   */
  public static <O> JsonObject toJson(DynamicOps<O> ops, MapLike<O> map) {
    JsonObject json = new JsonObject();
    map.entries().forEach(entry -> json.add(
      ops.getStringValue(entry.getFirst()).getOrThrow(ErrorFactory.JSON_SYNTAX_ERROR::create),
      toJson(ops, entry.getSecond())
    ));
    return json;
  }

  /**
   * Converts a JSON element into a value of an arbitrary format.
   * @param ops   Ops representing the desired format
   * @param json  Element to convert
   * @param <O>   Format of the result
   * @return  Element in the format of the passed ops
   */
  @SuppressWarnings("unchecked")  // safe as JsonOps is a DynamicOps<JsonElement>, meaning O is JsonElement
  public static <O> O fromJson(DynamicOps<O> ops, JsonElement json) {
    // unlike the input, we cannot detect gson from the value, so the best we can do is check the common instance
    if (ops == JsonOps.INSTANCE) {
      return (O)json;
    }
    return JsonOps.INSTANCE.convertTo(ops, json);
  }

  /**
   * Copies all fields of a JSON object into the passed record builder.
   * @param ops      Ops representing the format of the builder
   * @param builder  Builder receiving the fields
   * @param json     Object to copy
   * @param <O>      Format of the builder
   * @return  Builder containing the fields, for chaining
   */
  public static <O> RecordBuilder<O> addAll(DynamicOps<O> ops, RecordBuilder<O> builder, JsonObject json) {
    for (Entry<String,JsonElement> entry : json.entrySet()) {
      builder = builder.add(entry.getKey(), fromJson(ops, entry.getValue()));
    }
    return builder;
  }
}
