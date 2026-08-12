package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Interface for fields in a {@link RecordLoadable}.
 * Unlike {@link LoadableField}, this interface is not designed for use outside of loadables..
 * <p>
 * As with {@link slimeknights.mantle.data.loadable.Loadable}, the gson methods are the abstract pair and the ops
 * methods default to them, so fields able to read or write a format directly should override the ops pair.
 * @param <P>  Parent object
 * @param <T>  Loadable type
 */
public interface RecordField<T,P> {
  /**
   * Gets the loadable from the given JSON
   * @param json     JSON object
   * @param context  Additional parsing context, used notably by recipe serializers to store the ID and serializer.
   *                 Will be {@link TypedMap#EMPTY} in nested usages unless {@link DirectField} is used.
   * @return  Parsed loadable value
   * @throws com.google.gson.JsonSyntaxException  If unable to read from JSON
   */
  T get(JsonObject json, TypedMap context);

  /**
   * Gets the loadable from the given map of an arbitrary serialization format.
   * @param ops      Ops representing the format of the map
   * @param map      Map of fields to read
   * @param context  Additional parsing context, used notably by recipe serializers to store the ID and serializer.
   * @param <O>      Format of the map
   * @return  Parsed loadable value
   * @throws RuntimeException  If unable to read the map. See {@link ErrorFactory}.
   * @implNote  The default implementation converts the map into a {@link JsonObject} then reads that, losing anything
   *            gson cannot represent. Fields able to read a format directly should override this method.
   */
  default <O> T get(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    return get(OpsHelper.toJson(ops, map), context);
  }

  /**
   * Serializes the passed object into the JSON instance
   * @param json    JSON instance
   * @param parent  Object
   * @throws RuntimeException  If unable to save the element
   */
  void serialize(P parent, JsonObject json);

  /**
   * Serializes the passed object into the passed record builder.
   * @param ops      Ops representing the format of the builder
   * @param parent   Object to read the field value from
   * @param builder  Builder receiving the field
   * @param <O>      Format of the builder
   * @return  Builder containing the field, for chaining. Never assume the builder was modified in place, record builders are free to be immutable.
   * @throws RuntimeException  If unable to save the field. See {@link ErrorFactory}.
   * @implNote  The default implementation serializes to a {@link JsonObject} then copies that into the builder, losing
   *            anything gson cannot represent. Fields able to write a format directly should override this method.
   */
  default <O> RecordBuilder<O> serialize(DynamicOps<O> ops, P parent, RecordBuilder<O> builder) {
    JsonObject json = new JsonObject();
    serialize(parent, json);
    return OpsHelper.addAll(ops, builder, json);
  }

  /**
   * Parses this loadable from the network
   * @param buffer  Buffer instance
   * @param context  Additional parsing context, used notably by recipe serializers to store the ID and serializer.
   *                 Will be {@link TypedMap#EMPTY} in nested usages unless {@link DirectField} is used.
   * @return  Parsed field value
   * @throws io.netty.handler.codec.DecoderException  If unable to decode a value from network
   */
  T decode(RegistryFriendlyByteBuf buffer, TypedMap context);

  /**
   * Writes this field to the buffer
   * @param buffer  Buffer instance
   * @param parent  Parent to read values from
   * @throws io.netty.handler.codec.EncoderException  If unable to encode a value to network
   */
  void encode(RegistryFriendlyByteBuf buffer, P parent);
}
