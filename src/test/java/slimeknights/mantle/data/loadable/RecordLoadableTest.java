package slimeknights.mantle.data.loadable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.test.LoadableTest;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;
import java.util.ArrayList;
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


  /* Declaration order */

  /** Name written bare, but read from either a bare value or the object a later field may have wrapped it in */
  private static final Loadable<String> NAME = new Loadable<>() {
    @Override
    public String convert(JsonElement element, String key, TypedMap context) {
      if (element.isJsonObject()) {
        return GsonHelper.getAsString(element.getAsJsonObject(), "name");
      }
      return GsonHelper.convertToString(element, key);
    }

    @Override
    public JsonElement serialize(String object) {
      return new JsonPrimitive(object);
    }

    @Override
    public String decode(FriendlyByteBuf buffer, TypedMap context) {
      return buffer.readUtf();
    }

    @Override
    public void encode(FriendlyByteBuf buffer, String object) {
      buffer.writeUtf(object);
    }
  };

  /** Record whose scales complete the entries written by its names, the shape of a field in the wild */
  private record Scaled(List<String> names, List<Float> scales) {}

  /**
   * Field writing no key of its own: it finds the list an earlier field wrote and wraps each entry in an object
   * carrying the scale. Implements only the gson pair, as a field written before the ops methods existed would.
   */
  private record ScaleField(String listKey) implements RecordField<List<Float>,Scaled> {
    @Override
    public List<Float> get(JsonObject json, TypedMap context) {
      JsonArray list = GsonHelper.getAsJsonArray(json, listKey);
      List<Float> scales = new ArrayList<>(list.size());
      for (JsonElement element : list) {
        scales.add(element.isJsonObject() ? GsonHelper.getAsFloat(element.getAsJsonObject(), "scale") : 1f);
      }
      return scales;
    }

    @Override
    public void serialize(Scaled parent, JsonObject json) {
      // expect the list to be serialized before us
      JsonArray list = GsonHelper.getAsJsonArray(json, listKey);
      // nothing to say if every scale is the default, leaving the compact form the names wrote
      if (parent.scales().stream().allMatch(scale -> scale == 1f)) {
        return;
      }
      for (int i = 0; i < list.size(); i++) {
        JsonObject entry = new JsonObject();
        entry.add("name", list.get(i));
        entry.addProperty("scale", parent.scales().get(i));
        list.set(i, entry);
      }
    }

    @Override
    public List<Float> decode(FriendlyByteBuf buffer, TypedMap context) {
      int size = buffer.readVarInt();
      List<Float> scales = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        scales.add(buffer.readFloat());
      }
      return scales;
    }

    @Override
    public void encode(FriendlyByteBuf buffer, Scaled parent) {
      buffer.writeVarInt(parent.scales().size());
      for (float scale : parent.scales()) {
        buffer.writeFloat(scale);
      }
    }
  }

  private static final RecordLoadable<Scaled> SCALED = RecordLoadable.create(
    NAME.list().requiredField("names", Scaled::names),
    new ScaleField("names"),
    Scaled::new);

  @Test
  void laterField_seesTheFieldsBeforeIt() {
    assertRoundTrip(SCALED, new Scaled(List.of("a", "b"), List.of(2f, 3f)));
  }

  @Test
  void laterField_seesTheFieldsBeforeItWhenItWritesNothing() {
    assertRoundTrip(SCALED, new Scaled(List.of("a", "b"), List.of(1f, 1f)));
  }

  @Test
  void laterField_editsTheFieldsBeforeItThroughOps() {
    // the names field wrote a list of strings, the scales field replaced it with a list of objects
    CompoundTag tag = (CompoundTag)toNbt(SCALED, new Scaled(List.of("a", "b"), List.of(2f, 3f)));
    ListTag names = tag.getList("names", Tag.TAG_COMPOUND);
    assertThat(names).hasSize(2);
    assertThat(names.getCompound(0).getString("name")).isEqualTo("a");
    assertThat(names.getCompound(0).getFloat("scale")).isEqualTo(2f);
    assertThat(names.getCompound(1).getString("name")).isEqualTo("b");
    assertThat(names.getCompound(1).getFloat("scale")).isEqualTo(3f);
  }

  @Test
  void laterField_leavesUneditedFieldsAlone() {
    // the scales wrote nothing, so the names must survive in the native form their own loadable chose
    CompoundTag tag = (CompoundTag)toNbt(SCALED, new Scaled(List.of("a", "b"), List.of(1f, 1f)));
    assertThat(tag.getList("names", Tag.TAG_STRING))
      .as("a field an editor left alone keeps its own form")
      .containsExactly(StringTag.valueOf("a"), StringTag.valueOf("b"));
  }

  /** Record loadable written against gson alone, standing in for a loader in the wild used as a direct field */
  private record Suffix(String suffix) {
    static final RecordLoadable<Suffix> LOADABLE = new RecordLoadable<>() {
      @Override
      public Suffix deserialize(JsonObject json, TypedMap context) {
        return new Suffix(GsonHelper.getAsString(json, "suffix"));
      }

      @Override
      public void serialize(Suffix object, JsonObject json) {
        // append ourselves to the name an earlier field wrote, rather than writing a name of our own
        json.addProperty("name", GsonHelper.getAsString(json, "name") + object.suffix());
        json.addProperty("suffix", object.suffix());
      }

      @Override
      public Suffix decode(FriendlyByteBuf buffer, TypedMap context) {
        return new Suffix(buffer.readUtf());
      }

      @Override
      public void encode(FriendlyByteBuf buffer, Suffix object) {
        buffer.writeUtf(object.suffix());
      }
    };
  }

  private record Suffixed(String name, Suffix suffix) {}

  private static final RecordLoadable<Suffixed> SUFFIXED = RecordLoadable.create(
    StringLoadable.DEFAULT.requiredField("name", Suffixed::name),
    Suffix.LOADABLE.directField(Suffixed::suffix),
    Suffixed::new);

  @Test
  void directField_seesTheFieldsBeforeIt() {
    // the suffix is appended to the name each time, so this is not a round trip; check both paths agree on the edit
    Suffixed value = new Suffixed("base", new Suffix("!"));
    assertThat(SUFFIXED.serialize(value).getAsJsonObject().get("name").getAsString()).isEqualTo("base!");
    assertThat(((CompoundTag)toNbt(SUFFIXED, value)).getString("name")).isEqualTo("base!");
  }

  @Test
  void record_writesFieldsInDeclarationOrder() {
    JsonObject json = FIELDS.serialize(new Fields(1, 2, "text", true)).getAsJsonObject();
    assertThat(json.keySet()).containsExactly("required", "defaulted", "nullable", "flag");
    JsonElement ops = FIELDS.serialize(JsonOps.INSTANCE, new Fields(1, 2, "text", true));
    assertThat(ops.getAsJsonObject().keySet())
      .as("the ops path must write the fields in the same order as the gson path")
      .containsExactly("required", "defaulted", "nullable", "flag");
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
