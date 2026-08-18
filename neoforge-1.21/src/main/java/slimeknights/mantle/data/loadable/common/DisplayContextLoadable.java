package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonSyntaxException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemDisplayContext;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.mapping.EnumMapLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Map;

/**
 * Special loadable for display contexts, as {@link ItemDisplayContext} has neither the naming nor the network form of a
 * plain enum.
 * @apiNote  This was a {@link slimeknights.mantle.data.loadable.primitive.ResourceLocationLoadable} in 1.20, where
 *           display contexts lived in a Forge registry keyed by resource location. In 1.21 they are an extensible enum
 *           instead, named by {@link ItemDisplayContext#getSerializedName()} (values such as {@code thirdperson_lefthand},
 *           which is not a resource location) and numbered by {@link ItemDisplayContext#getId()}. This loadable
 *           therefore reads and writes exactly what {@link ItemDisplayContext#CODEC} does, and travels as the single
 *           byte the vanilla network format uses.
 */
public enum DisplayContextLoadable implements StringLoadable<ItemDisplayContext> {
  INSTANCE;

  @Override
  public ItemDisplayContext parseString(String value, String key, TypedMap context) {
    // values() grows when another mod extends the enum, but extension happens during mod loading, long before anything
    // parses a model, so a linear scan of the current values is always up to date here
    for (ItemDisplayContext displayContext : ItemDisplayContext.values()) {
      if (displayContext.getSerializedName().equals(value)) {
        return displayContext;
      }
    }
    throw new JsonSyntaxException("Unable to parse " + key + " as there is no ItemDisplayContext named " + value);
  }

  @Override
  public String getString(ItemDisplayContext object) {
    return object.getSerializedName();
  }

  @Override
  public ItemDisplayContext decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return ItemDisplayContext.BY_ID.apply(buffer.readByte());
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, ItemDisplayContext value) {
    buffer.writeByte(value.getId());
  }

  @Override
  public <V> Loadable<Map<ItemDisplayContext,V>> mapWithValues(Loadable<V> valueLoadable, int minSize) {
    return new EnumMapLoadable<>(ItemDisplayContext.class, this, valueLoadable, minSize);
  }
}
