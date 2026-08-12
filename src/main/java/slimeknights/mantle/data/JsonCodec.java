package slimeknights.mantle.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.OpsHelper;

/**
 * Simple implementation of a codec mapping to a JSON serializer.
 * @param <T> Object type
 */
public interface JsonCodec<T> extends Codec<T> {
  /**
   * Deserializes the object from JSON
   */
  T deserialize(JsonElement element, DynamicOps<?> ops);

  /**
   * Serializes the element to json
   */
  JsonElement serialize(T object, DynamicOps<?> ops);

  /** Gets the name of the type this parses for display in codec errors */
  default String codecError() {
    return toString();
  }

  /**
   * {@inheritDoc}
   * @implNote  The whole input is consumed, so the remainder in the returned pair is {@link DynamicOps#empty()}.
   */
  @Override
  default <O> DataResult<Pair<T,O>> decode(DynamicOps<O> ops, O input) {
    return ErrorFactory
      .catching(() -> deserialize(OpsHelper.toJson(ops, input), ops), e -> Mantle.logger.warn("Unable to decode {}", codecError(), e))
      .map(value -> Pair.of(value, ops.empty()));
  }

  /**
   * {@inheritDoc}
   * @implNote  The result is a whole value rather than something appendable, so a non-empty prefix is an error.
   */
  @Override
  default <O> DataResult<O> encode(T input, DynamicOps<O> ops, O prefix) {
    return ErrorFactory
      .catching(() -> OpsHelper.fromJson(ops, serialize(input, ops)), e -> Mantle.logger.warn("Unable to encode {}", codecError(), e))
      .flatMap(value -> ops.mergeToPrimitive(prefix, value));
  }

  /** Creates a codec for a GSON element with an existing GSON serializer */
  record GsonCodec<T>(String name, Gson gson, Class<T> classType) implements JsonCodec<T> {
    @Override
    public T deserialize(JsonElement element, DynamicOps<?> ops) {
      return gson.fromJson(element, classType);
    }

    @Override
    public JsonElement serialize(T object, DynamicOps<?> ops) {
      return gson.toJsonTree(object, classType);
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
