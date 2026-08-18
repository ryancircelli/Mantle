package slimeknights.mantle.test;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import slimeknights.mantle.data.loadable.Loadable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for loadable tests, providing the round trip assertions.
 * A loadable round trips when writing a value then reading it back gives an equal value, which must hold for every
 * format the loadable supports: gson, any {@link com.mojang.serialization.DynamicOps} and the network.
 */
public abstract class LoadableTest extends BaseMcTest {
  /** Key used in error messages for the tested value */
  protected static final String KEY = "test";

  /** Asserts the value survives the gson methods */
  public static <T> void assertJsonRoundTrip(Loadable<T> loadable, T value) {
    JsonElement json = loadable.serialize(value);
    assertThat(loadable.convert(json, KEY)).as("gson round trip of %s", json).isEqualTo(value);
  }

  /** Asserts the value survives the ops methods with {@link JsonOps}, and that they agree with the gson methods */
  public static <T> void assertJsonOpsRoundTrip(Loadable<T> loadable, T value) {
    JsonElement json = loadable.serialize(JsonOps.INSTANCE, value);
    assertThat(json).as("json ops must write the same as the gson method").isEqualTo(loadable.serialize(value));
    assertThat(loadable.convert(JsonOps.INSTANCE, json, KEY)).as("json ops round trip of %s", json).isEqualTo(value);
  }

  /** Asserts the value survives the ops methods with {@link NbtOps} */
  public static <T> void assertNbtRoundTrip(Loadable<T> loadable, T value) {
    Tag tag = loadable.serialize(NbtOps.INSTANCE, value);
    assertThat(loadable.convert(NbtOps.INSTANCE, tag, KEY)).as("nbt round trip of %s", tag).isEqualTo(value);
  }

  /** {@return an empty buffer with the test registries attached}, as every loadable writes to a registry buffer */
  public static RegistryFriendlyByteBuf buffer() {
    return new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(), ConnectionType.NEOFORGE);
  }

  /** Asserts the value survives the network methods */
  public static <T> void assertNetworkRoundTrip(Loadable<T> loadable, T value) {
    RegistryFriendlyByteBuf buffer = buffer();
    loadable.encode(buffer, value);
    assertThat(loadable.decode(buffer)).as("network round trip").isEqualTo(value);
    assertThat(buffer.readableBytes()).as("network round trip must consume the whole buffer").isZero();
  }

  /** Asserts the value survives every format the loadable supports */
  public static <T> void assertRoundTrip(Loadable<T> loadable, T value) {
    assertJsonRoundTrip(loadable, value);
    assertJsonOpsRoundTrip(loadable, value);
    assertNbtRoundTrip(loadable, value);
    assertNetworkRoundTrip(loadable, value);
  }

  /** Writes the value through {@link NbtOps}, for asserting the resulting tag type */
  public static <T> Tag toNbt(Loadable<T> loadable, T value) {
    return loadable.serialize(NbtOps.INSTANCE, value);
  }


  /* Codec views */

  /** Unwraps a result, failing the test with the error message instead of an empty optional */
  public static <T> T success(DataResult<T> result) {
    assertThat(result.error().map(DataResult.Error::message)).as("expected a successful result").isEmpty();
    return result.result().orElseThrow();
  }

  /** {@return the error message of a result}, failing the test if it succeeded */
  public static String error(DataResult<?> result) {
    assertThat(result.result()).as("expected an error result, got %s", result.result().orElse(null)).isEmpty();
    return result.error().orElseThrow().message();
  }

  /** Writes the value through the codec with the given ops, for asserting the written form */
  public static <O,T> O write(Codec<T> codec, DynamicOps<O> ops, T value) {
    return success(codec.encodeStart(ops, value));
  }

  /** Asserts the value survives the codec with the given ops */
  public static <O,T> void assertCodecRoundTrip(Codec<T> codec, DynamicOps<O> ops, T value) {
    O written = write(codec, ops, value);
    Pair<T,O> read = success(codec.decode(ops, written));
    assertThat(read.getFirst()).as("codec round trip of %s", written).isEqualTo(value);
  }

  /** Asserts the value survives the codec with both {@link JsonOps} and {@link NbtOps} */
  public static <T> void assertCodecRoundTrip(Codec<T> codec, T value) {
    assertCodecRoundTrip(codec, JsonOps.INSTANCE, value);
    assertCodecRoundTrip(codec, NbtOps.INSTANCE, value);
  }
}
