package slimeknights.mantle.data.loadable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * This interface partially implements Mojang's {@link net.minecraft.network.codec.StreamCodec} for the sake of ensuring
 * all {@link Loadable} are automatically compatible with stream codecs.
 * @apiNote  The buffer is a {@link RegistryFriendlyByteBuf} for every loadable rather than only for the ones needing
 *           registry access. Loadables compose freely, so any loadable may end up as the element of a collection or the
 *           field of a record; if the interface asked only for a {@link net.minecraft.network.FriendlyByteBuf} then
 *           every registry backed loadable (item stacks, fluid stacks, ingredients and data components all require one
 *           in 1.21) would have to cast, which compiles and round trips in a test but throws the first time a packet is
 *           actually sent. Requiring the wider buffer everywhere makes that a compile error instead. NeoForge hands play
 *           payloads a {@link RegistryFriendlyByteBuf} already, so this costs a caller nothing in practice.
 */
public interface Streamable<T> {
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
  default T decode(RegistryFriendlyByteBuf buffer) {
    return decode(buffer, TypedMap.EMPTY);
  }

  /**
   * Writes this object to the packet buffer
   * @param buffer  Buffer instance
   * @param value  Object to write
   * @throws io.netty.handler.codec.EncoderException  If unable to encode a value to network
   */
  void encode(RegistryFriendlyByteBuf buffer, T value);
}
