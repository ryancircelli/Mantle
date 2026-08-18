package slimeknights.mantle.data.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.gson.GenericRegisteredSerializer;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.data.loadable.mapping.ConditionalLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

/**
 * Generic registry for an object that can both be sent over a friendly byte buffer and serialized into JSON.
 * @param <T>  Type of the serializable object
 * @see GenericRegisteredSerializer GenericRegisteredSerializer for an alternative that does not need to handle network syncing
 * @see DefaultingLoaderRegistry
 */
public class GenericLoaderRegistry<T extends IHaveLoader> implements RecordLoadable<T> {
  /** Empty object instance for compact deserialization */
  protected static final JsonObject EMPTY_OBJECT = new JsonObject();

  /** Display name for this registry */
  @Getter
  private final String name;
  /** Map of all serializers for implementations */
  protected final NamedComponentRegistry<RecordLoadable<? extends T>> loaders;
  /** If true, single key serializations will not use a JSON object to serialize, ideal for loaders with many singletons */
  protected final boolean compact;
  /** Loader that chooses between two options based on a load condition. Exposed for datagen. */
  @Getter
  private final RecordLoadable<T> conditionalLoader;

  /**
   * Creates a new registry instance
   * @param name           Registry name for errors
   * @param compact        If true, objects with no fields beyond type will serialize as just a string.
   * @param falseInstance  Default value for {@code if_false} on conditional loaders. If null, then {@code if_false} becomes a required field.
   */
  public GenericLoaderRegistry(String name, @Nullable T falseInstance, boolean compact) {
    this.name = name;
    this.compact = compact;
    this.loaders = new NamedComponentRegistry<>("Unknown " + name + " loader");
    this.conditionalLoader = new ConditionalLoadable<>(this, falseInstance);
    register(Mantle.getResource("load_condition"), conditionalLoader);
  }

  /**
   * Creates a new registry instance with no false instance.
   * @param name           Registry name for errors
   * @param compact        If true, objects with no fields beyond type will serialize as just a string.
   */
  @SuppressWarnings("unused")  // API
  public GenericLoaderRegistry(String name, boolean compact) {
    this(name, null, compact);
  }

  /** Registers a deserializer by name */
  public void register(ResourceLocation name, RecordLoadable<? extends T> loader) {
    loaders.register(name, loader);
  }

  /** Returns the name of a registered loader, or null if it is unregistered */
  @Nullable
  public ResourceLocation getName(RecordLoadable<? extends T> loader) {
    return loaders.getOptionalKey(loader);
  }

  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    // first try object
    if (element.isJsonObject()) {
      JsonObject object = element.getAsJsonObject();
      return loaders.getIfPresent(object, "type", context).deserialize(object, context);
    }
    // try primitive if allowed
    if (compact && element.isJsonPrimitive()) {
      EMPTY_OBJECT.entrySet().clear();
      return loaders.convert(element, "type", context).deserialize(EMPTY_OBJECT, context);
    }
    // neither? failed to parse
    throw new JsonSyntaxException("Invalid " + name + " JSON at " + key + ", must be a JSON object" + (compact ? " or a string" : ""));
  }

  @Override
  public <O> T convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
    // first try object
    Optional<MapLike<O>> map = ops.getMap(input).result();
    if (map.isPresent()) {
      return loaders.getIfPresent(ops, map.get(), "type", context).deserialize(ops, map.get(), context);
    }
    // try string if allowed
    if (compact && ops.getStringValue(input).result().isPresent()) {
      return loaders.convert(ops, input, "type", context).deserialize(ops, OpsHelper.emptyMap(), context);
    }
    // neither? failed to parse
    throw new JsonSyntaxException("Invalid " + name + " JSON at " + key + ", must be a JSON object" + (compact ? " or a string" : ""));
  }

  @Override
  public T deserialize(JsonObject json, TypedMap context) {
    return loaders.getIfPresent(json, "type", context).deserialize(json, context);
  }

  @Override
  public <O> T deserialize(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    return loaders.getIfPresent(ops, map, "type", context).deserialize(ops, map, context);
  }

  /** Serializes the object to json, fighting generics */
  @SuppressWarnings("unchecked")
  private <L> void serialize(RecordLoadable<L> loader, T src, JsonObject json) {
    JsonElement type = new JsonPrimitive(loaders.getKey((RecordLoadable<? extends T>)loader).toString());
    json.add("type", type);
    loader.serialize((L)src, json);
    if (json.get("type") != type) {
      throw new IllegalStateException(name + " serializer " + type.getAsString() + " modified the type key, this is not allowed as it breaks deserialization");
    }
  }

  @Override
  public JsonElement serialize(T src) {
    JsonObject json = new JsonObject();
    serialize(src.getLoader(), src, json);
    // nothing to serialize? use type directly
    if (compact && json.entrySet().size() == 1) {
      return json.get("type");
    }
    return json;
  }

  @Override
  public void serialize(T object, JsonObject json) {
    serialize(object.getLoader(), object, json);
  }

  /** Serializes the object into the builder, fighting generics */
  @SuppressWarnings("unchecked")
  private <L,O> RecordBuilder<O> serialize(RecordLoadable<L> loader, DynamicOps<O> ops, T src, RecordBuilder<O> builder) {
    ResourceLocation type = loaders.getKey((RecordLoadable<? extends T>)loader);
    O typeValue = ops.createString(type.toString());
    RecordBuilder<O> result = loader.serialize(ops, (L)src, builder.add("type", typeValue));
    // a builder able to read itself back can run the same guard as the gson variant; one which cannot returns null
    O written = OpsHelper.getWritten(result, "type");
    if (written != null && !written.equals(typeValue)) {
      throw new IllegalStateException(name + " serializer " + type + " modified the type key, this is not allowed as it breaks deserialization");
    }
    return result;
  }

  @Override
  public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, T object, RecordBuilder<O> builder) {
    return serialize(object.getLoader(), ops, object, builder);
  }

  @Override
  public <O> O serialize(DynamicOps<O> ops, T src) {
    O result = RecordLoadable.super.serialize(ops, src);
    // nothing to serialize? use type directly
    if (compact) {
      Optional<MapLike<O>> map = ops.getMap(result).result();
      if (map.isPresent() && map.get().entries().count() == 1) {
        O type = map.get().get("type");
        if (type != null) {
          return type;
        }
      }
    }
    return result;
  }

  /** Writes the object to the network, fighting generics */
  @SuppressWarnings("unchecked")
  protected  <L> void encode(RecordLoadable<L> loader, RegistryFriendlyByteBuf buffer, T src) {
    loader.encode(buffer, (L)src);
  }

  @SuppressWarnings("unchecked")  // the cast is safe here as its just doing a map lookup, shouldn't cause harm if it fails. Besides, the loader has to extend T to work
  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T src) {
    RecordLoadable<? extends IHaveLoader> loader = src.getLoader();
    loaders.encode(buffer, (RecordLoadable<? extends T>)loader);
    encode(loader, buffer, src);
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return loaders.decode(buffer, context).decode(buffer, context);
  }

  /** Creates a field that loads this object directly into the parent JSON object by mapping the type key */
  public <P> RecordField<T,P> directField(String typeKey, Function<P,T> getter) {
    return new MergingRegistryField<>(this, typeKey, getter);
  }

  @Override
  public String toString() {
    return getClass().getName() + "('" + name + "')";
  }

  /**
   * Interface for an object with a loader.
   */
  public interface IHaveLoader {
    /** Gets the loader for the object. */
    RecordLoadable<? extends IHaveLoader> getLoader();
  }
}
