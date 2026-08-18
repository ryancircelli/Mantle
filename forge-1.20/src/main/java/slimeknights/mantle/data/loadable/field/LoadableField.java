package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Interface for a field in a JSON object loaded from a single field, typically used in {@link RecordLoadable} but also usable statically.
 * @param <P>  Parent object
 * @param <T>  Loadable type
 */
public interface LoadableField<T,P> extends RecordField<T,P> {
  /** Key this field is loaded from. Other fields may be used for fallback */
  String key();

  /**
   * Gets the loadable from the given JSON, overriding the key
   * @param json     JSON object
   * @param key      Key to use instead of {@link #key()}. Used for legacy fallback fields.
   * @param context  Additional parsing context, used notably by recipe serializers to store the ID and serializer.
   *                 Will be {@link TypedMap#EMPTY} in nested usages unless {@link DirectField} is used.
   * @return  Parsed loadable value
   * @throws com.google.gson.JsonSyntaxException  If unable to read from JSON
   */
  T get(JsonObject json, String key, TypedMap context);

  @NonExtendable
  @Override
  default T get(JsonObject json, TypedMap context) {
    return get(json, key(), context);
  }

  /**
   * Gets the loadable from the given map of an arbitrary serialization format, overriding the key.
   * @param ops      Ops representing the format of the map
   * @param map      Map of fields to read
   * @param key      Key to use instead of {@link #key()}. Used for legacy fallback fields.
   * @param context  Additional parsing context, used notably by recipe serializers to store the ID and serializer.
   * @param <O>      Format of the map
   * @return  Parsed loadable value
   * @throws RuntimeException  If unable to read the map. See {@link slimeknights.mantle.data.loadable.ErrorFactory}.
   * @implNote  The default implementation converts the map into a {@link JsonObject} then reads that, losing anything
   *            gson cannot represent. Fields able to read a format directly should override this method.
   */
  default <O> T get(DynamicOps<O> ops, MapLike<O> map, String key, TypedMap context) {
    return get(OpsHelper.toJson(ops, map), key, context);
  }

  @NonExtendable
  @Override
  default <O> T get(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    return get(ops, map, key(), context);
  }

  /** Same as {@link #get(JsonObject, TypedMap)} but passes {@link TypedMap#EMPTY} for context. */
  @NonExtendable
  default T get(JsonObject json) {
    return get(json, TypedMap.EMPTY);
  }

  /** Same as {@link #decode(FriendlyByteBuf, TypedMap)} but passes {@link TypedMap#EMPTY} for context. */
  @NonExtendable
  default T decode(FriendlyByteBuf buffer) {
    return decode(buffer, TypedMap.EMPTY);
  }
}
