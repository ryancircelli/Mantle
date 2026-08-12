package slimeknights.mantle.data.loadable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * This interface implements Mojang's {@link StreamCodec} for the sake of ensuring all {@link Loadable} are usable
 * anywhere a stream codec is asked for, notably {@link net.minecraft.world.item.crafting.RecipeSerializer#streamCodec()}
 * and {@link net.neoforged.neoforge.common.crafting.IngredientType}.
 * @apiNote  The two abstract methods here are exactly {@link StreamCodec}'s, with the context taking overload of
 *           {@link #decode(RegistryFriendlyByteBuf, TypedMap)} being the one a loadable implements; the no-context
 *           {@link #decode(RegistryFriendlyByteBuf)} is what satisfies the interface.
 * @apiNote  The buffer is a {@link RegistryFriendlyByteBuf} for every loadable rather than only for the ones needing
 *           registry access. Loadables compose freely, so any loadable may end up as the element of a collection or the
 *           field of a record; if the interface asked only for a {@link net.minecraft.network.FriendlyByteBuf} then
 *           every registry backed loadable (item stacks, fluid stacks, ingredients and data components all require one
 *           in 1.21) would have to cast, which compiles and round trips in a test but throws the first time a packet is
 *           actually sent. Requiring the wider buffer everywhere makes that a compile error instead. NeoForge hands play
 *           payloads a {@link RegistryFriendlyByteBuf} already, so this costs a caller nothing in practice.
 */
public interface Streamable<T> extends StreamCodec<RegistryFriendlyByteBuf,T> {
  /**
   * Decodes this loadable from the network
   * @param buffer  Buffer instance
   * @param context Additional parsing context, used notably by recipe serializers to store the ID and serializer.
   * @return  Parsed object
   * @throws io.netty.handler.codec.DecoderException  If unable to decode
   */
  T decode(RegistryFriendlyByteBuf buffer, TypedMap context);

  /** Same as {@link #decode(RegistryFriendlyByteBuf, TypedMap)} but passes {@link TypedMap#EMPTY} for context. */
  @NonExtendable
  @Override
  default T decode(RegistryFriendlyByteBuf buffer) {
    return decode(buffer, TypedMap.EMPTY);
  }

  /**
   * Writes this object to the packet buffer
   * @param buffer  Buffer instance
   * @param value  Object to write
   * @throws io.netty.handler.codec.EncoderException  If unable to encode a value to network
   */
  @Override
  void encode(RegistryFriendlyByteBuf buffer, T value);

  /**
   * Views this loadable as a {@link StreamCodec} decoding with the given parsing context.
   * <p>
   * A loadable is already a stream codec, but a stream codec has nowhere to put a {@link TypedMap}, so a loadable
   * reading a context field needs the context fixed when the codec is built. This is the network counterpart of
   * {@link slimeknights.mantle.data.loadable.record.RecordLoadable#mapCodec(TypedMap)} and exists for the same caller.
   * @param context  Context handed to {@link #decode(RegistryFriendlyByteBuf, TypedMap)} on every decode
   * @return  Stream codec backed by this loadable
   */
  @NonExtendable
  default StreamCodec<RegistryFriendlyByteBuf,T> streamCodec(TypedMap context) {
    return StreamCodec.of(this::encode, buffer -> decode(buffer, context));
  }
}
