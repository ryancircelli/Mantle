package slimeknights.mantle.data.loadable;

import com.google.gson.JsonSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.test.LoadableTest;

import javax.annotation.Nullable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Round trip tests for record loadables, covering every arity plus the field behaviors */
class RecordLoadableTest extends LoadableTest {
  /** Creates a field reading index {@code index} of a list, letting us build a record of any arity */
  private static RecordField<Integer,List<Integer>> field(int index) {
    return IntLoadable.ANY_FULL.<List<Integer>>requiredField("f" + index, list -> list.get(index));
  }


  /* Arities */

  @Test
  void arity1_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      a -> List.of(a));
    assertRoundTrip(loadable, List.of(1));
  }

  @Test
  void arity2_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      (a, b) -> List.of(a, b));
    assertRoundTrip(loadable, List.of(1, 2));
  }

  @Test
  void arity3_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      (a, b, c) -> List.of(a, b, c));
    assertRoundTrip(loadable, List.of(1, 2, 3));
  }

  @Test
  void arity4_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      field(3),
      (a, b, c, d) -> List.of(a, b, c, d));
    assertRoundTrip(loadable, List.of(1, 2, 3, 4));
  }

  @Test
  void arity5_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      field(3),
      field(4),
      (a, b, c, d, e) -> List.of(a, b, c, d, e));
    assertRoundTrip(loadable, List.of(1, 2, 3, 4, 5));
  }

  @Test
  void arity6_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      field(3),
      field(4),
      field(5),
      (a, b, c, d, e, f) -> List.of(a, b, c, d, e, f));
    assertRoundTrip(loadable, List.of(1, 2, 3, 4, 5, 6));
  }

  @Test
  void arity7_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      field(3),
      field(4),
      field(5),
      field(6),
      (a, b, c, d, e, f, g) -> List.of(a, b, c, d, e, f, g));
    assertRoundTrip(loadable, List.of(1, 2, 3, 4, 5, 6, 7));
  }

  @Test
  void arity8_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      field(3),
      field(4),
      field(5),
      field(6),
      field(7),
      (a, b, c, d, e, f, g, h) -> List.of(a, b, c, d, e, f, g, h));
    assertRoundTrip(loadable, List.of(1, 2, 3, 4, 5, 6, 7, 8));
  }

  @Test
  void arity9_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      field(3),
      field(4),
      field(5),
      field(6),
      field(7),
      field(8),
      (a, b, c, d, e, f, g, h, i) -> List.of(a, b, c, d, e, f, g, h, i));
    assertRoundTrip(loadable, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
  }

  @Test
  void arity10_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      field(3),
      field(4),
      field(5),
      field(6),
      field(7),
      field(8),
      field(9),
      (a, b, c, d, e, f, g, h, i, j) -> List.of(a, b, c, d, e, f, g, h, i, j));
    assertRoundTrip(loadable, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
  }

  @Test
  void arity11_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      field(3),
      field(4),
      field(5),
      field(6),
      field(7),
      field(8),
      field(9),
      field(10),
      (a, b, c, d, e, f, g, h, i, j, k) -> List.of(a, b, c, d, e, f, g, h, i, j, k));
    assertRoundTrip(loadable, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
  }

  @Test
  void arity12_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      field(3),
      field(4),
      field(5),
      field(6),
      field(7),
      field(8),
      field(9),
      field(10),
      field(11),
      (a, b, c, d, e, f, g, h, i, j, k, l) -> List.of(a, b, c, d, e, f, g, h, i, j, k, l));
    assertRoundTrip(loadable, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
  }

  @Test
  void arity13_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      field(3),
      field(4),
      field(5),
      field(6),
      field(7),
      field(8),
      field(9),
      field(10),
      field(11),
      field(12),
      (a, b, c, d, e, f, g, h, i, j, k, l, m) -> List.of(a, b, c, d, e, f, g, h, i, j, k, l, m));
    assertRoundTrip(loadable, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13));
  }

  @Test
  void arity14_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      field(3),
      field(4),
      field(5),
      field(6),
      field(7),
      field(8),
      field(9),
      field(10),
      field(11),
      field(12),
      field(13),
      (a, b, c, d, e, f, g, h, i, j, k, l, m, n) -> List.of(a, b, c, d, e, f, g, h, i, j, k, l, m, n));
    assertRoundTrip(loadable, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14));
  }

  @Test
  void arity15_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      field(3),
      field(4),
      field(5),
      field(6),
      field(7),
      field(8),
      field(9),
      field(10),
      field(11),
      field(12),
      field(13),
      field(14),
      (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) -> List.of(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o));
    assertRoundTrip(loadable, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
  }

  @Test
  void arity16_roundTrips() {
    RecordLoadable<List<Integer>> loadable = RecordLoadable.create(
      field(0),
      field(1),
      field(2),
      field(3),
      field(4),
      field(5),
      field(6),
      field(7),
      field(8),
      field(9),
      field(10),
      field(11),
      field(12),
      field(13),
      field(14),
      field(15),
      (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) -> List.of(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p));
    assertRoundTrip(loadable, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16));
  }


  /* Field behaviors */

  /** Record with an assortment of field types */
  private record Fields(int required, int defaulted, @Nullable String nullable, boolean flag) {}

  private static final LoadableField<Integer,Fields> REQUIRED = IntLoadable.ANY_FULL.requiredField("required", Fields::required);
  private static final LoadableField<Integer,Fields> DEFAULTED = IntLoadable.ANY_FULL.defaultField("defaulted", 7, Fields::defaulted);
  private static final LoadableField<String,Fields> NULLABLE = StringLoadable.DEFAULT.nullableField("nullable", Fields::nullable);
  private static final LoadableField<Boolean,Fields> FLAG = BooleanLoadable.INSTANCE.defaultField("flag", false, Fields::flag);
  private static final RecordLoadable<Fields> FIELDS = RecordLoadable.create(REQUIRED, DEFAULTED, NULLABLE, FLAG, Fields::new);

  @Test
  void fields_roundTripWithAllPresent() {
    assertRoundTrip(FIELDS, new Fields(1, 2, "text", true));
  }

  @Test
  void fields_roundTripWithOptionalsAbsent() {
    assertRoundTrip(FIELDS, new Fields(1, 7, null, false));
  }

  @Test
  void defaultingField_omitsTheDefault() {
    CompoundTag tag = (CompoundTag)toNbt(FIELDS, new Fields(1, 7, null, false));
    assertThat(tag.contains("defaulted")).as("a defaulted value must not be written").isFalse();
    assertThat(tag.contains("nullable")).as("a null value must not be written").isFalse();
    // booleans opt into always serializing as they read better in a datapack
    assertThat(tag.contains("flag")).as("booleans always serialize").isTrue();
  }

  @Test
  void defaultingField_writesNonDefault() {
    CompoundTag tag = (CompoundTag)toNbt(FIELDS, new Fields(1, 8, "text", true));
    assertThat(tag.get("defaulted")).isEqualTo(IntTag.valueOf(8));
    assertThat(tag.get("nullable")).isEqualTo(StringTag.valueOf("text"));
  }

  @Test
  void defaultingField_treatsEmptyAsAbsent() {
    // an explicit empty value, a JSON null or an end tag, is treated as missing
    CompoundTag tag = new CompoundTag();
    tag.put("required", IntTag.valueOf(1));
    tag.put("defaulted", NbtOps.INSTANCE.empty());
    assertThat(FIELDS.convert(NbtOps.INSTANCE, tag, KEY)).isEqualTo(new Fields(1, 7, null, false));
  }

  @Test
  void requiredField_rejectsMissing() {
    assertThatThrownBy(() -> FIELDS.convert(NbtOps.INSTANCE, new CompoundTag(), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("required");
  }

  @Test
  void record_rejectsNonObject() {
    assertThatThrownBy(() -> FIELDS.convert(NbtOps.INSTANCE, StringTag.valueOf("nope"), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining(KEY);
  }

  @Test
  void record_writesNativeNumericTags() {
    // a record built through gson would land its ints in byte tags
    CompoundTag tag = (CompoundTag)toNbt(FIELDS, new Fields(1, 2, "text", true));
    assertThat(tag.get("required")).isEqualTo(IntTag.valueOf(1));
    assertThat(tag.get("defaulted")).isEqualTo(IntTag.valueOf(2));
    assertThat(tag.get("nullable")).isEqualTo(StringTag.valueOf("text"));
    assertThat(tag.getBoolean("flag")).isTrue();
  }


  /* Nesting */

  /** Record nesting another record both as a field and directly */
  private record Nested(Fields nested, Fields direct, List<Integer> list) {}

  private static final RecordLoadable<Nested> NESTED = RecordLoadable.create(
    FIELDS.requiredField("nested", Nested::nested),
    FIELDS.directField(Nested::direct),
    IntLoadable.ANY_FULL.list().requiredField("list", Nested::list),
    Nested::new);

  @Test
  void nestedRecord_roundTrips() {
    assertRoundTrip(NESTED, new Nested(new Fields(1, 2, "a", true), new Fields(3, 4, "b", false), List.of(5, 6)));
  }

  @Test
  void nestedRecord_keepsNumericTypes() {
    CompoundTag tag = (CompoundTag)toNbt(NESTED, new Nested(new Fields(1, 2, "a", true), new Fields(3, 4, "b", false), List.of(5, 6)));
    assertThat(tag.getCompound("nested").get("required")).isEqualTo(IntTag.valueOf(1));
    assertThat(tag.get("required")).as("a direct field writes into the parent").isEqualTo(IntTag.valueOf(3));
    assertThat(tag.get("list")).isEqualTo(NbtOps.INSTANCE.createIntList(List.of(5, 6).stream().mapToInt(Integer::intValue)));
  }


  /* Singleton */

  @Test
  void singletonLoader_roundTrips() {
    Object instance = new Object();
    RecordLoadable<Object> loadable = new SingletonLoader<>(instance);
    assertThat(loadable.convert(NbtOps.INSTANCE, new CompoundTag(), KEY)).isSameAs(instance);
    assertThat(toNbt(loadable, instance)).isEqualTo(new CompoundTag());
  }
}
