package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.test.LoadableTest;
import slimeknights.mantle.util.typed.TypedMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that a loadable written entirely against gson, as most loadables outside Mantle are, keeps working through
 * the ops methods. This is what makes the ops methods safe to add: the gson methods stay the abstract pair, so an
 * implementation providing only those still answers every format, and the two families can never fall through to
 * each other.
 */
class LoadableCompatibilityTest extends LoadableTest {
  /** A loadable implementing only the gson methods, as an implementation outside Mantle would */
  private record GsonOnly(String prefix) implements Loadable<String> {
    @Override
    public String convert(JsonElement element, String key, TypedMap context) {
      return prefix + GsonHelper.convertToString(element, key);
    }

    @Override
    public JsonElement serialize(String object) {
      return new JsonPrimitive(object.substring(prefix.length()));
    }

    @Override
    public String decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return prefix + buffer.readUtf();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, String value) {
      buffer.writeUtf(value.substring(prefix.length()));
    }
  }

  /** A record loadable implementing only the gson methods */
  private record GsonOnlyRecord(String unused) implements RecordLoadable<String> {
    @Override
    public String deserialize(JsonObject json, TypedMap context) {
      return GsonHelper.getAsString(json, "value");
    }

    @Override
    public void serialize(String object, JsonObject json) {
      json.addProperty("value", object);
    }

    @Override
    public String decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return buffer.readUtf();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, String value) {
      buffer.writeUtf(value);
    }
  }

  private static final Loadable<String> GSON_ONLY = new GsonOnly("prefix:");
  private static final RecordLoadable<String> GSON_ONLY_RECORD = new GsonOnlyRecord("");

  @Test
  void gsonOnlyLoadable_worksThroughEveryFormat() {
    assertRoundTrip(GSON_ONLY, "prefix:value");
  }

  @Test
  void gsonOnlyLoadable_bridgesToTheGsonMethods() {
    assertThat(toNbt(GSON_ONLY, "prefix:value")).isEqualTo(StringTag.valueOf("value"));
    assertThat(GSON_ONLY.convert(NbtOps.INSTANCE, StringTag.valueOf("value"), KEY)).isEqualTo("prefix:value");
  }

  @Test
  void gsonOnlyRecord_worksThroughEveryFormat() {
    assertRoundTrip(GSON_ONLY_RECORD, "value");
  }

  @Test
  void gsonOnlyRecord_bridgesToTheGsonMethods() {
    CompoundTag expected = new CompoundTag();
    expected.putString("value", "text");
    assertThat(toNbt(GSON_ONLY_RECORD, "text")).isEqualTo(expected);
    assertThat(GSON_ONLY_RECORD.convert(NbtOps.INSTANCE, expected, KEY)).isEqualTo("text");
  }

  @Test
  void gsonOnlyLoadable_composesWithNativeLoadables() {
    // the interesting case: a native collection of a bridged element, and a native record with a bridged field
    assertRoundTrip(GSON_ONLY.list(), java.util.List.of("prefix:a", "prefix:b"));
    RecordLoadable<String> record = RecordLoadable.create(GSON_ONLY.requiredField("field", value -> value), value -> value);
    assertRoundTrip(record, "prefix:value");
  }
}
