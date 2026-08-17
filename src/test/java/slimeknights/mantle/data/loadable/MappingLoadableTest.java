package slimeknights.mantle.data.loadable;

import com.google.gson.JsonSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.mapping.EitherLoadable;
import slimeknights.mantle.data.loadable.mapping.SimpleRecordLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.test.LoadableTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Round trip tests for the mapping loadables, which choose between forms based on the shape of the value */
class MappingLoadableTest extends LoadableTest {
  /* Mapped */

  @Test
  void xmap_roundTrips() {
    Loadable<String> loadable = IntLoadable.ANY_FULL.flatXmap(String::valueOf, Integer::parseInt);
    assertRoundTrip(loadable, "42");
    assertThat(toNbt(loadable, "42")).as("the mapped loadable keeps the base format").isEqualTo(IntTag.valueOf(42));
  }

  @Test
  void validate_rejectsBadValue() {
    Loadable<Integer> loadable = IntLoadable.ANY_FULL.validate((value, error) -> {
      if (value % 2 != 0) {
        throw error.create("must be even");
      }
      return value;
    });
    assertRoundTrip(loadable, 4);
    assertThatThrownBy(() -> loadable.convert(NbtOps.INSTANCE, IntTag.valueOf(3), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("must be even");
  }


  /* Compact */

  /** Record with a single number, compacting to just that number */
  private record Compact(int value, String text) {}

  private static final RecordLoadable<Compact> COMPACT = RecordLoadable
    .create(
      IntLoadable.ANY_FULL.requiredField("value", Compact::value),
      StringLoadable.DEFAULT.defaultField("text", "", Compact::text),
      Compact::new)
    .compact(IntLoadable.ANY_FULL.flatXmap(value -> new Compact(value, ""), Compact::value), compact -> compact.text().isEmpty());

  @Test
  void compact_roundTripsBothShapes() {
    assertRoundTrip(COMPACT, new Compact(1, ""));
    assertRoundTrip(COMPACT, new Compact(1, "text"));
  }

  @Test
  void compact_writesTheCompactForm() {
    assertThat(toNbt(COMPACT, new Compact(5, ""))).isEqualTo(IntTag.valueOf(5));
    assertThat(toNbt(COMPACT, new Compact(5, "text"))).isInstanceOf(CompoundTag.class);
  }

  @Test
  void compact_readsBothShapes() {
    assertThat(COMPACT.convert(NbtOps.INSTANCE, IntTag.valueOf(5), KEY)).isEqualTo(new Compact(5, ""));
    CompoundTag tag = new CompoundTag();
    tag.put("value", IntTag.valueOf(5));
    tag.put("text", StringTag.valueOf("text"));
    assertThat(COMPACT.convert(NbtOps.INSTANCE, tag, KEY)).isEqualTo(new Compact(5, "text"));
  }


  /* Single key record */

  private static final RecordLoadable<Integer> SIMPLE = new SimpleRecordLoadable<>(IntLoadable.ANY_FULL, "value", null, false);
  private static final RecordLoadable<Integer> SIMPLE_DEFAULT = new SimpleRecordLoadable<>(IntLoadable.ANY_FULL, "value", 3, false);

  @Test
  void simpleRecord_roundTrips() {
    assertRoundTrip(SIMPLE, 5);
    assertThat(toNbt(SIMPLE, 5)).isInstanceOf(CompoundTag.class);
  }

  @Test
  void simpleRecord_readsThePrimitiveForm() {
    assertThat(SIMPLE.convert(NbtOps.INSTANCE, IntTag.valueOf(5), KEY)).isEqualTo(5);
  }

  @Test
  void simpleRecord_usesTheDefaultWhenAbsent() {
    assertThat(SIMPLE_DEFAULT.convert(NbtOps.INSTANCE, new CompoundTag(), KEY)).isEqualTo(3);
    assertThatThrownBy(() -> SIMPLE.convert(NbtOps.INSTANCE, new CompoundTag(), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("value");
  }


  /* Either */

  /** Shared type for the either options */
  private interface Choice extends IAmLoadable.Record {}

  private record ByNumber(int value) implements Choice {
    static final RecordLoadable<ByNumber> LOADABLE = RecordLoadable.create(
      IntLoadable.ANY_FULL.requiredField("number", ByNumber::value), ByNumber::new);

    @Override
    public RecordLoadable<ByNumber> loadable() {
      return LOADABLE;
    }
  }

  private record ByText(String value) implements Choice {
    static final RecordLoadable<ByText> LOADABLE = RecordLoadable.create(
      StringLoadable.DEFAULT.requiredField("text", ByText::value), ByText::new);

    @Override
    public RecordLoadable<ByText> loadable() {
      return LOADABLE;
    }
  }

  private static final RecordLoadable<Choice> EITHER = EitherLoadable.<Choice>record()
    .key("number", ByNumber.LOADABLE)
    .key("text", ByText.LOADABLE)
    .build();

  @Test
  void either_roundTripsBothBranches() {
    assertRoundTrip(EITHER, new ByNumber(5));
    assertRoundTrip(EITHER, new ByText("value"));
  }

  @Test
  void either_keepsNumericTypes() {
    CompoundTag tag = (CompoundTag)toNbt(EITHER, new ByNumber(5));
    assertThat(tag.get("number")).isEqualTo(IntTag.valueOf(5));
  }

  @Test
  void either_rejectsUnknownShape() {
    assertThatThrownBy(() -> EITHER.convert(NbtOps.INSTANCE, new CompoundTag(), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("number");
  }
}
