package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.ResourceLocationLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Optional;

/**
 * Loadable for an entry of a datapack registry, such as {@link net.minecraft.core.registries.Registries#ENCHANTMENT}.
 * <p>
 * A datapack registry does not exist until a world is loaded and is rebuilt on every reload, so unlike
 * {@link RegistryLoadable} there is no registry instance to capture. The registry has to come from the caller, and the
 * three ways it does are:
 * <ol>
 *   <li>the ops, when they are a {@link RegistryOps}. This is the path every datapack load takes, as vanilla builds a
 *       registry ops for the whole reload;</li>
 *   <li>{@link ContextKey#REGISTRY_ACCESS} in the loadable context, for a caller which holds the registries but was
 *       handed plain ops. {@link OpsHelper#withRegistries(DynamicOps, TypedMap)} is what turns that into (1);</li>
 *   <li>{@link RegistryFriendlyByteBuf#registryAccess()} on the network, which is why every loadable takes that buffer.</li>
 * </ol>
 * Values are read as a {@link Holder} rather than the value itself, both because that is what 1.21 gameplay code takes
 * and because a holder keeps working across a datapack reload that replaces the registry contents.
 * <p>
 * If none of the three supply the registry, reading fails with a message naming the registry and listing those routes;
 * see {@link #noRegistries(String, String)}. Writing never needs the registry, as a holder already knows its own key.
 * @param <T>  Registry value type
 * @see RegistryLoadable RegistryLoadable for the static registry equivalent
 */
@RequiredArgsConstructor
public class DynamicRegistryLoadable<T> implements ResourceLocationLoadable<Holder<T>> {
  /** Key of the registry being read */
  private final ResourceKey<? extends Registry<T>> registryKey;
  /** Network codec, which reads the registry from the buffer rather than from us */
  private final StreamCodec<RegistryFriendlyByteBuf,Holder<T>> network;

  public DynamicRegistryLoadable(ResourceKey<? extends Registry<T>> registryKey) {
    this(registryKey, ByteBufCodecs.holderRegistry(registryKey));
  }

  /**
   * Error for the case a caller supplied no route to the registries at all. This is the failure a new caller is most
   * likely to hit, so the message names every way to fix it rather than just stating the problem.
   * @param key     Key holding the value, for the caller to locate it
   * @param source  Description of what was searched, for the caller to see which route it was on
   */
  private RuntimeException noRegistries(String key, String source) {
    return new JsonSyntaxException(
      "Unable to parse " + key + " as the datapack registry " + registryKey.location() + " is unavailable: " + source
      + ". Read through a RegistryOps, which any HolderLookup.Provider builds via #createSerializationContext and every"
      + " datapack load already supplies, or place the provider in the loadable context under ContextKey.REGISTRY_ACCESS."
      + " On the network the buffer supplies the registries, so there is nothing to pass.");
  }

  /** Locates the registry through the ops, throwing a message naming the route that failed */
  private HolderGetter<T> getter(DynamicOps<?> ops, String key) {
    if (ops instanceof RegistryOps<?> registryOps) {
      Optional<HolderGetter<T>> getter = registryOps.getter(registryKey);
      if (getter.isPresent()) {
        return getter.get();
      }
      throw noRegistries(key, "the RegistryOps in use does not provide it");
    }
    throw noRegistries(key, "neither the ops (" + ops.getClass().getSimpleName() + ") nor the loadable context has them");
  }

  /** Resolves a name against the located registry */
  private Holder<T> resolve(HolderGetter<T> getter, ResourceLocation name, String key) {
    return getter.get(ResourceKey.create(registryKey, name)).orElseThrow(
      () -> new JsonSyntaxException("Unable to parse " + key + " as registry " + registryKey.location() + " does not contain ID " + name));
  }

  /**
   * {@inheritDoc}
   * @implNote  Only reaches the registries through {@link ContextKey#REGISTRY_ACCESS}, as this method has no ops to
   *            read them from. {@link #convert(DynamicOps, Object, String, TypedMap)} is the path with both.
   */
  @Override
  public Holder<T> fromKey(ResourceLocation name, String key, TypedMap context) {
    return resolve(getter(OpsHelper.withRegistries(JsonOps.INSTANCE, context), key), name, key);
  }

  @Override
  public <O> Holder<T> convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
    String name = OpsHelper.getString(ops, input, key);
    return resolve(getter(OpsHelper.withRegistries(ops, context), key), ResourceLocationLoadable.parse(name, key), key);
  }

  @Override
  public ResourceLocation getKey(Holder<T> object) {
    return object.unwrapKey().orElseThrow(
      () -> new RuntimeException("Cannot serialize " + object + " as it is not a registered entry of " + registryKey.location())).location();
  }

  @Override
  public Holder<T> decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return network.decode(buffer);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, Holder<T> value) {
    network.encode(buffer, value);
  }

  @Override
  public String toString() {
    return "DynamicRegistryLoadable(" + registryKey.location() + ")";
  }
}
