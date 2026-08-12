package slimeknights.mantle.data.loadable.primitive;

import com.google.gson.JsonSyntaxException;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Helper for the common case of making a string loadable that uses resource locations.
 * @param <T>
 * @see Loadables#RESOURCE_LOCATION
 */
@SuppressWarnings("unused")  // API
public interface ResourceLocationLoadable<T> extends StringLoadable<T> {
  /** Standard implementation of a resource location loadable for raw resource locations. */
  StringLoadable<ResourceLocation> DEFAULT = new ResourceLocationLoadable<>() {
    @Override
    public ResourceLocation fromKey(ResourceLocation name, String key, TypedMap context) {
      return name;
    }

    @Override
    public ResourceLocation getKey(ResourceLocation object) {
      return object;
    }

    @Override
    public ResourceLocation decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return buffer.readResourceLocation();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, ResourceLocation value) {
      buffer.writeResourceLocation(value);
    }
  };

  /**
   * Parses a resource location, throwing a JSON exception naming the key instead of returning null.
   * @param text  Text to parse
   * @param key   Key that contained the text, used for exceptions only
   * @return  Parsed resource location
   * @throws com.google.gson.JsonSyntaxException  If the text is not a valid resource location
   * @apiNote  This is {@link slimeknights.mantle.util.JsonHelper#parseResourceLocation(String, String)}, which cannot
   *           be called from here as that class still depends on the unported network package.
   */
  static ResourceLocation parse(String text, String key) {
    // basically the inside of ResourceLocation#tryParse, but with a JSON exception instead of being nullable
    try {
      return ResourceLocation.parse(text);
    } catch (ResourceLocationException ex) {
      throw new JsonSyntaxException("Expected " + key + " to be a resource location, was '" + text + "'", ex);
    }
  }

  /**
   * Converts this value from a resource location.
   * @param name   Location to parse
   * @param key    Json key containing the value used for exceptions only.
   * @param context  Additional parsing context, used notably by recipe serializers to store the ID and serializer.
   * @return  Converted value.'
   * @throws com.google.gson.JsonSyntaxException  If no value exists for that key
   */
  T fromKey(ResourceLocation name, String key, TypedMap context);

  /** Same as {@link #fromKey(ResourceLocation, String, TypedMap)} but passes {@link TypedMap#EMPTY} for context. */
  default T fromKey(ResourceLocation name, String key) {
    return fromKey(name, key, TypedMap.EMPTY);
  }

  @Override
  default T parseString(String value, String key, TypedMap context) {
    return fromKey(parse(value, key), key, context);
  }

  /**
   * Converts this object to its serialized representation.
   * @param object  Object to serialize
   * @return  String representation of the object.
   * @throws RuntimeException  if unable to serialize this to a string
   */
  ResourceLocation getKey(T object);

  @Override
  default String getString(T object) {
    return getKey(object).toString();
  }
}
