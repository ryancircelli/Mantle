package slimeknights.mantle.data.loadable;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.CharacterLoadable;
import slimeknights.mantle.data.loadable.primitive.DoubleLoadable;
import slimeknights.mantle.data.loadable.primitive.EnumLoadable;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.LongLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.test.LoadableTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Round trip and format fidelity tests for the primitive loadables */
class PrimitiveLoadableTest extends LoadableTest {
  /** Enum used to test the enum loadable */
  private enum TestEnum { FIRST, SECOND, THIRD }


  /* Round trips */

  @Test
  void intLoadable_roundTrips() {
    for (int value : new int[] { 0, 1, -1, 127, 128, Short.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE }) {
      assertRoundTrip(IntLoadable.ANY_FULL, value);
    }
  }

  @Test
  void longLoadable_roundTrips() {
    for (long value : new long[] { 0, 1, -1, Integer.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE }) {
      assertRoundTrip(LongLoadable.ANY, value);
    }
  }

  @Test
  void floatLoadable_roundTrips() {
    for (float value : new float[] { 0, 1, -1.5f, 0.1f, Float.MAX_VALUE }) {
      assertRoundTrip(FloatLoadable.ANY, value);
    }
  }

  @Test
  void doubleLoadable_roundTrips() {
    for (double value : new double[] { 0, 1, -1.5, 0.1, Double.MAX_VALUE }) {
      assertRoundTrip(DoubleLoadable.ANY, value);
    }
  }

  @Test
  void booleanLoadable_roundTrips() {
    assertRoundTrip(BooleanLoadable.INSTANCE, true);
    assertRoundTrip(BooleanLoadable.INSTANCE, false);
  }

  @Test
  void stringLoadable_roundTrips() {
    assertRoundTrip(StringLoadable.DEFAULT, "");
    assertRoundTrip(StringLoadable.DEFAULT, "hello world");
    assertRoundTrip(StringLoadable.DEFAULT, "{\"quoted\": true}");
  }

  @Test
  void characterLoadable_roundTrips() {
    assertRoundTrip(CharacterLoadable.INSTANCE, 'a');
    assertRoundTrip(CharacterLoadable.INSTANCE, '\'');
  }

  @Test
  void enumLoadable_roundTrips() {
    Loadable<TestEnum> loadable = new EnumLoadable<>(TestEnum.class);
    for (TestEnum value : TestEnum.values()) {
      assertRoundTrip(loadable, value);
    }
  }

  @Test
  void resourceLocationLoadable_roundTrips() {
    assertRoundTrip(Loadables.RESOURCE_LOCATION, ResourceLocation.fromNamespaceAndPath("mantle", "test/value"));
  }

  @Test
  void registryLoadable_roundTrips() {
    assertRoundTrip(Loadables.ITEM, Items.DIAMOND_PICKAXE);
  }

  @Test
  void intAsString_roundTrips() {
    assertRoundTrip(IntLoadable.ANY_FULL.asString(16), 0xBEEF);
    assertRoundTrip(IntLoadable.ANY_FULL.asString(10), -12);
  }

  @Test
  void longAsString_roundTrips() {
    assertRoundTrip(LongLoadable.ANY.asString(16), 0xDEADBEEFL);
  }


  /* NBT fidelity, these fail if the loadables fall back to converting through gson */

  @Test
  void intLoadable_writesIntTag() {
    // a small int converted through gson lands in a byte tag, so this pins the native path
    assertThat(toNbt(IntLoadable.ANY_FULL, 5)).isEqualTo(IntTag.valueOf(5));
    assertThat(toNbt(IntLoadable.ANY_FULL, 5)).isInstanceOf(IntTag.class);
  }

  @Test
  void longLoadable_writesLongTag() {
    assertThat(toNbt(LongLoadable.ANY, 5L)).isEqualTo(LongTag.valueOf(5));
    assertThat(toNbt(LongLoadable.ANY, 5L)).isInstanceOf(LongTag.class);
  }

  @Test
  void floatLoadable_writesFloatTag() {
    // gson has no float, so a converted float lands in a double tag
    assertThat(toNbt(FloatLoadable.ANY, 1.5f)).isEqualTo(FloatTag.valueOf(1.5f));
    assertThat(toNbt(FloatLoadable.ANY, 1.5f)).isInstanceOf(FloatTag.class);
  }

  @Test
  void doubleLoadable_writesDoubleTag() {
    assertThat(toNbt(DoubleLoadable.ANY, 1.5)).isEqualTo(DoubleTag.valueOf(1.5));
  }

  @Test
  void booleanLoadable_writesByteTag() {
    assertThat(toNbt(BooleanLoadable.INSTANCE, true)).isEqualTo(ByteTag.valueOf(true));
    assertThat(toNbt(BooleanLoadable.INSTANCE, false)).isEqualTo(ByteTag.valueOf(false));
  }

  @Test
  void booleanLoadable_readsByteTag() {
    assertThat(BooleanLoadable.INSTANCE.convert(NbtOps.INSTANCE, ByteTag.valueOf(true), KEY)).isTrue();
    assertThat(BooleanLoadable.INSTANCE.convert(NbtOps.INSTANCE, ByteTag.valueOf(false), KEY)).isFalse();
  }

  @Test
  void stringLoadable_writesStringTag() {
    assertThat(toNbt(StringLoadable.DEFAULT, "value")).isEqualTo(StringTag.valueOf("value"));
  }

  @Test
  void intLoadable_readsAnyNumericTag() {
    // an int field written by another mod may legitimately be any numeric tag
    assertThat(IntLoadable.ANY_FULL.convert(NbtOps.INSTANCE, ByteTag.valueOf((byte)5), KEY)).isEqualTo(5);
    assertThat(IntLoadable.ANY_FULL.convert(NbtOps.INSTANCE, IntTag.valueOf(5), KEY)).isEqualTo(5);
    assertThat(IntLoadable.ANY_FULL.convert(NbtOps.INSTANCE, LongTag.valueOf(5), KEY)).isEqualTo(5);
  }


  /* Errors */

  @Test
  void intLoadable_rejectsNonNumber() {
    assertThatThrownBy(() -> IntLoadable.ANY_FULL.convert(NbtOps.INSTANCE, StringTag.valueOf("nope"), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining(KEY);
    assertThatThrownBy(() -> IntLoadable.ANY_FULL.convert(new JsonPrimitive("nope"), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining(KEY);
  }

  @Test
  void intLoadable_rejectsOutOfRange() {
    assertThatThrownBy(() -> IntLoadable.FROM_ZERO.convert(NbtOps.INSTANCE, IntTag.valueOf(-1), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining(KEY);
    assertThatThrownBy(() -> IntLoadable.FROM_ZERO.convert(JsonOps.INSTANCE, new JsonPrimitive(-1), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining(KEY);
  }

  @Test
  void enumLoadable_rejectsUnknownValue() {
    Loadable<TestEnum> loadable = new EnumLoadable<>(TestEnum.class);
    assertThatThrownBy(() -> loadable.convert(NbtOps.INSTANCE, StringTag.valueOf("fourth"), KEY))
      .isInstanceOf(JsonSyntaxException.class);
  }
}
