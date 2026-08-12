package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;

/** Implementation of a loadable using a codec, reading and writing the codec's own format in every direction. */
public record CodecLoadable<T>(DynamicOps<Tag> ops, Codec<T> codec) implements Loadable<T> {
  /**
   * Key wrapping the codec's value on the network.
   * {@link FriendlyByteBuf} can only transfer a compound tag, but a codec is free to encode any tag, so the value is
   * nested under a single key instead of being sent as the packet's tag directly.
   */
  private static final String NETWORK_KEY = "value";

  public CodecLoadable(Codec<T> codec) {
    this(NbtOps.INSTANCE, codec);
  }

  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    return convert(JsonOps.INSTANCE, element, key, context);
  }

  @Override
  public <O> T convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
    return codec.parse(ops, input).getOrThrow(false, ErrorFactory.JSON_SYNTAX_ERROR);
  }

  @Override
  public JsonElement serialize(T object) {
    return serialize(JsonOps.INSTANCE, object);
  }

  @Override
  public <O> O serialize(DynamicOps<O> ops, T object) {
    return codec.encodeStart(ops, object).getOrThrow(false, ErrorFactory.RUNTIME);
  }

  @Override
  public T decode(FriendlyByteBuf buffer, TypedMap context) {
    @Nullable CompoundTag wrapper = buffer.readAnySizeNbt();
    @Nullable Tag value = wrapper != null ? wrapper.get(NETWORK_KEY) : null;
    // a missing key means the codec wrote the empty value, which a compound cannot store
    return codec.parse(ops, value != null ? value : ops.empty()).getOrThrow(false, ErrorFactory.DECODER_EXCEPTION);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, T object) {
    Tag value = codec.encodeStart(ops, object).getOrThrow(false, ErrorFactory.ENCODER_EXCEPTION);
    CompoundTag wrapper = new CompoundTag();
    // an end tag is not a valid compound value, so it is sent as a missing key
    if (value.getId() != Tag.TAG_END) {
      wrapper.put(NETWORK_KEY, value);
    }
    buffer.writeNbt(wrapper);
  }
}
