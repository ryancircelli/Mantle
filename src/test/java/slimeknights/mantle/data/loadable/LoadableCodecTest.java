package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.LongLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.test.LoadableTest;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the codec views of a loadable: {@link Loadable#codec()} and {@link RecordLoadable#mapCodec()}.
 * The loadables themselves are covered by the other tests in this package, so the fixtures here are the smallest ones
 * exercising each shape a codec consumer cares about.
 */
class LoadableCodecTest extends LoadableTest {
  /* Fixtures, mirroring those used by the loadable round trip tests */

  private record Fields(int required, int defaulted, @Nullable String nullable, List<Integer> list) {}

  private static final LoadableField<Integer,Fields> REQUIRED = IntLoadable.ANY_FULL.requiredField("required", Fields::required);
  private static final LoadableField<Integer,Fields> DEFAULTED = IntLoadable.ANY_FULL.defaultField("defaulted", 7, Fields::defaulted);
  private static final LoadableField<String,Fields> NULLABLE = StringLoadable.DEFAULT.nullableField("nullable", Fields::nullable);
  private static final LoadableField<List<Integer>,Fields> LIST = IntLoadable.ANY_FULL.list().requiredField("list", Fields::list);
  private static final RecordLoadable<Fields> FIELDS = RecordLoadable.create(REQUIRED, DEFAULTED, NULLABLE, LIST, Fields::new);

  private record Nested(Fields nested, Fields direct, String name) {}

  private static final RecordLoadable<Nested> NESTED = RecordLoadable.create(
    FIELDS.requiredField("nested", Nested::nested),
    FIELDS.directField(Nested::direct),
    StringLoadable.DEFAULT.requiredField("name", Nested::name),
    Nested::new);

  private static final Fields FIELDS_VALUE = new Fields(1, 2, "text", List.of(3, 4));
  private static final Nested NESTED_VALUE = new Nested(FIELDS_VALUE, new Fields(5, 7, null, List.of(6)), "outer");


  /* Element level codec */

  @Test
  void codec_roundTripsPrimitives() {
    assertCodecRoundTrip(IntLoadable.ANY_FULL.codec(), 5);
    assertCodecRoundTrip(LongLoadable.ANY.codec(), 5_000_000_000L);
    assertCodecRoundTrip(StringLoadable.DEFAULT.codec(), "text");
    assertCodecRoundTrip(Loadables.RESOURCE_LOCATION.codec(), new ResourceLocation("mantle", "test"));
  }

  @Test
  void codec_roundTripsCollections() {
    assertCodecRoundTrip(IntLoadable.ANY_FULL.list().codec(), List.of(1, 2, 3));
    assertCodecRoundTrip(StringLoadable.DEFAULT.set().codec(), java.util.Set.of("a", "b"));
  }

  @Test
  void codec_roundTripsRecords() {
    assertCodecRoundTrip(FIELDS.codec(), FIELDS_VALUE);
    assertCodecRoundTrip(FIELDS.codec(), new Fields(1, 7, null, List.of(2)));
    assertCodecRoundTrip(NESTED.codec(), NESTED_VALUE);
  }

  @Test
  void codec_roundTripsASingletonRecord() {
    Object instance = new Object();
    assertCodecRoundTrip(new SingletonLoader<>(instance).codec(), instance);
  }

  @Test
  void codec_keepsTheFormatsOwnTypes() {
    // the whole point of the exercise: no JSON in the middle, so the numbers land in their native tags
    CompoundTag tag = (CompoundTag)write(FIELDS.codec(), NbtOps.INSTANCE, FIELDS_VALUE);
    assertThat(tag.get("required")).isEqualTo(IntTag.valueOf(1));
    assertThat(tag.get("nullable")).isEqualTo(StringTag.valueOf("text"));
    assertThat(tag.get("list")).isEqualTo(NbtOps.INSTANCE.createIntList(java.util.stream.IntStream.of(3, 4)));
    assertThat(write(LongLoadable.ANY.codec(), NbtOps.INSTANCE, 5L)).isEqualTo(LongTag.valueOf(5));
  }

  @Test
  void codec_writesTheSameFormAsTheLoadable() {
    JsonElement json = write(FIELDS.codec(), JsonOps.INSTANCE, FIELDS_VALUE);
    assertThat(json).isEqualTo(FIELDS.serialize(FIELDS_VALUE));
  }

  @Test
  void codec_consumesTheWholeInput() {
    Tag tag = write(FIELDS.codec(), NbtOps.INSTANCE, FIELDS_VALUE);
    Pair<Fields,Tag> decoded = success(FIELDS.codec().decode(NbtOps.INSTANCE, tag));
    assertThat(decoded.getSecond()).as("nothing is left over").isEqualTo(NbtOps.INSTANCE.empty());
  }

  @Test
  void codec_rejectsANonEmptyPrefix() {
    // a loadable writes a whole value, so there is nothing sensible to append it to
    DataResult<Tag> result = FIELDS.codec().encode(FIELDS_VALUE, NbtOps.INSTANCE, StringTag.valueOf("prefix"));
    assertThat(error(result)).contains("prefix");
  }


  /* Error paths */

  @Test
  void codec_reportsAMissingFieldAsAnError() {
    // the value read is fine, the field inside it is not
    CompoundTag tag = new CompoundTag();
    tag.put("list", new ListTag());
    assertThat(error(FIELDS.codec().decode(NbtOps.INSTANCE, tag))).contains("required");
  }

  @Test
  void codec_reportsAWrongTypeAsAnError() {
    assertThat(error(FIELDS.codec().decode(NbtOps.INSTANCE, StringTag.valueOf("nope")))).contains("codec");
    assertThat(error(IntLoadable.ANY_FULL.codec().decode(NbtOps.INSTANCE, StringTag.valueOf("nope")))).contains("codec");
  }

  @Test
  void codec_reportsAnOutOfRangeValueAsAnError() {
    assertThat(error(IntLoadable.ANY_BYTE.codec().decode(NbtOps.INSTANCE, IntTag.valueOf(5000)))).contains("between");
  }

  /** Loadable throwing something other than a JsonParseException, as a dozen loadables in the framework do */
  private record Unhappy() implements Loadable<String> {
    @Override
    public String convert(JsonElement element, String key, TypedMap context) {
      throw new IllegalStateException("cannot read " + key);
    }

    @Override
    public JsonElement serialize(String object) {
      throw new IllegalStateException("cannot write " + object);
    }

    @Override
    public String decode(FriendlyByteBuf buffer, TypedMap context) {
      throw new IllegalStateException("cannot decode");
    }

    @Override
    public void encode(FriendlyByteBuf buffer, String object) {
      throw new IllegalStateException("cannot encode");
    }
  }

  @Test
  void codec_catchesEveryRuntimeExceptionNotJustJsonOnes() {
    // this used to escape the codec as a raw exception, as only JsonParseException was caught
    Codec<String> codec = new Unhappy().codec();
    assertThat(error(codec.decode(NbtOps.INSTANCE, StringTag.valueOf("x")))).contains("cannot read");
    assertThat(error(codec.encodeStart(NbtOps.INSTANCE, "x"))).contains("cannot write");
  }

  @Test
  void codec_catchesAnErrorFromWithinACollection() {
    // the string loadable throws a plain RuntimeException when writing an overlong string
    Codec<List<String>> codec = StringLoadable.maxLength(4).list().codec();
    assertThat(error(codec.encodeStart(NbtOps.INSTANCE, List.of("ok", "far too long")))).contains("4");
  }

  @Test
  void codec_reportsAnErrorWithoutAMessage() {
    // an NPE has no message, so the type name stands in rather than a null error
    Loadable<String> loadable = StringLoadable.DEFAULT.flatXmap(s -> {
      throw new NullPointerException();
    }, s -> s);
    assertThat(error(loadable.codec().decode(JsonOps.INSTANCE, new JsonPrimitive("x")))).contains("NullPointerException");
  }


  /* Map codec */

  @Test
  void mapCodec_roundTrips() {
    assertCodecRoundTrip(FIELDS.mapCodec().codec(), FIELDS_VALUE);
    assertCodecRoundTrip(NESTED.mapCodec().codec(), NESTED_VALUE);
  }

  @Test
  void mapCodec_writesTheSameFormAsTheLoadable() {
    assertThat(write(FIELDS.mapCodec().codec(), JsonOps.INSTANCE, FIELDS_VALUE)).isEqualTo(FIELDS.serialize(FIELDS_VALUE));
    assertThat(write(FIELDS.mapCodec().codec(), NbtOps.INSTANCE, FIELDS_VALUE)).isEqualTo(toNbt(FIELDS, FIELDS_VALUE));
  }

  @Test
  void mapCodec_reportsErrorsInsteadOfThrowing() {
    assertThat(error(FIELDS.mapCodec().codec().decode(NbtOps.INSTANCE, new CompoundTag()))).contains("required");
    assertThat(error(NESTED.mapCodec().codec().encodeStart(NbtOps.INSTANCE, new Nested(FIELDS_VALUE, FIELDS_VALUE, null))))
      .isNotEmpty();
  }

  @Test
  void mapCodec_composesInsideARecordCodecBuilder() {
    // the shape a recipe serializer or a loot modifier wants: the loadable as one field of a larger codec
    record Wrapper(String name, Fields fields, int count) {}
    Codec<Wrapper> codec = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.fieldOf("name").forGetter(Wrapper::name),
      FIELDS.mapCodec().forGetter(Wrapper::fields),
      Codec.INT.fieldOf("count").forGetter(Wrapper::count)
    ).apply(instance, Wrapper::new));

    Wrapper value = new Wrapper("outer", FIELDS_VALUE, 3);
    assertCodecRoundTrip(codec, value);

    // the loadable's fields land in the parent object, as a map codec has no key of its own
    CompoundTag tag = (CompoundTag)write(codec, NbtOps.INSTANCE, value);
    assertThat(tag.get("name")).isEqualTo(StringTag.valueOf("outer"));
    assertThat(tag.get("required")).isEqualTo(IntTag.valueOf(1));
    assertThat(tag.get("count")).isEqualTo(IntTag.valueOf(3));
  }

  @Test
  void mapCodec_composesInsideADispatch() {
    // the shape a registry of implementations wants
    Codec<Fields> fieldsCodec = FIELDS.mapCodec().codec();
    Codec<Fields> dispatched = Codec.STRING.<Fields>partialDispatch(
      "type",
      fields -> DataResult.success("fields"),
      type -> {
        if ("fields".equals(type)) {
          return DataResult.success(fieldsCodec);
        }
        return DataResult.error(() -> "Unknown type " + type);
      });

    assertCodecRoundTrip(dispatched, FIELDS_VALUE);
    CompoundTag tag = (CompoundTag)write(dispatched, NbtOps.INSTANCE, FIELDS_VALUE);
    assertThat(tag.get("type")).isEqualTo(StringTag.valueOf("fields"));
    assertThat(tag.get("required")).isEqualTo(IntTag.valueOf(1));

    CompoundTag unknown = new CompoundTag();
    unknown.putString("type", "missing");
    assertThat(error(dispatched.decode(NbtOps.INSTANCE, unknown))).contains("missing");
  }

  @Test
  void mapCodec_worksAsAMapValue() {
    Codec<Fields> codec = FIELDS.mapCodec().codec();
    Codec<Map<String,Fields>> map = Codec.unboundedMap(Codec.STRING, codec);
    assertCodecRoundTrip(map, Map.of("a", FIELDS_VALUE));
  }

  @Test
  void mapCodec_listsNoKeysByDefault() {
    // a loadable cannot enumerate its keys, see LoadableMapCodec#keys
    assertThat(FIELDS.mapCodec().keys(NbtOps.INSTANCE)).isEmpty();
    assertThat(FIELDS.mapCodec("required", "defaulted").keys(JsonOps.INSTANCE))
      .containsExactly(new JsonPrimitive("required"), new JsonPrimitive("defaulted"));
  }

  @Test
  void mapCodec_refusesCompressedMapsRatherThanCorruptThem() {
    MapCodec<Fields> mapCodec = FIELDS.mapCodec();
    assertThat(error(mapCodec.codec().encodeStart(JsonOps.COMPRESSED, FIELDS_VALUE))).contains("compressed");
    assertThat(error(mapCodec.codec().decode(JsonOps.COMPRESSED, new JsonArray()))).contains("compressed");
    // an uncompressed ops is unaffected
    assertCodecRoundTrip(mapCodec.codec(), JsonOps.INSTANCE, FIELDS_VALUE);
  }

  @Test
  void mapCodec_worksWithCompressedMapsOnceTheKeysAreDeclared() {
    MapCodec<Fields> mapCodec = FIELDS.mapCodec("required", "defaulted", "nullable", "list");
    assertCodecRoundTrip(mapCodec.codec(), JsonOps.COMPRESSED, FIELDS_VALUE);
  }
}
