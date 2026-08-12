package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Loadable for the set of data components which differ from an item or fluid's defaults.
 * <p>
 * This is the 1.21 replacement for the {@code nbt} field {@link NBTLoadable} used to fill on
 * {@link ItemStackLoadable} and {@link FluidStackLoadable}: a stack's extra data is no longer a free form compound tag
 * but a {@link DataComponentPatch}, so it is read and written exactly as vanilla's own {@code components} field is
 * ({@link DataComponentPatch#CODEC} on a datapack, {@link DataComponentPatch#STREAM_CODEC} on the network).
 * @apiNote  Reading or writing a component whose value references a registry, notably {@code minecraft:enchantments},
 *           needs the registries. On the network they come from the buffer; on a datapack they must come from the ops,
 *           either because the caller passed a {@link net.minecraft.resources.RegistryOps} or because it supplied
 *           {@link slimeknights.mantle.data.loadable.field.ContextKey#REGISTRY_ACCESS} in the context. The write side
 *           takes no context, so it has only the ops.
 */
public enum DataComponentsLoadable implements Loadable<DataComponentPatch> {
  INSTANCE;

  @Override
  public DataComponentPatch convert(JsonElement element, String key, TypedMap context) {
    return convert(JsonOps.INSTANCE, element, key, context);
  }

  @Override
  public <O> DataComponentPatch convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
    return DataComponentPatch.CODEC.parse(OpsHelper.withRegistries(ops, context), input).getOrThrow(ErrorFactory.JSON_SYNTAX_ERROR::create);
  }

  @Override
  public JsonElement serialize(DataComponentPatch object) {
    return serialize(JsonOps.INSTANCE, object);
  }

  @Override
  public <O> O serialize(DynamicOps<O> ops, DataComponentPatch object) {
    return DataComponentPatch.CODEC.encodeStart(ops, object).getOrThrow(ErrorFactory.RUNTIME::create);
  }

  @Override
  public DataComponentPatch decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return DataComponentPatch.STREAM_CODEC.decode(buffer);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, DataComponentPatch value) {
    DataComponentPatch.STREAM_CODEC.encode(buffer, value);
  }
}
