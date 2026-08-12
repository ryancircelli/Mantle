package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.Map.Entry;

/**
 * Helpers to bridge between an arbitrary {@link DynamicOps} format and the gson methods on {@link Loadable}.
 * Used by the default implementations of the ops based loadable methods, and useful for any loadable choosing
 * to implement just one of the two sides.
 */
@SuppressWarnings("unused")  // API
public class OpsHelper {
  private OpsHelper() {}

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
      ops.getStringValue(entry.getFirst()).getOrThrow(false, ErrorFactory.JSON_SYNTAX_ERROR),
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
