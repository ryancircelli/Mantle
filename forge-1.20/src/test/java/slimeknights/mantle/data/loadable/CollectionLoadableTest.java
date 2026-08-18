package slimeknights.mantle.data.loadable;

import com.google.gson.JsonSyntaxException;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.LongLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.test.LoadableTest;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Round trip and format fidelity tests for lists, sets, maps and arrays */
class CollectionLoadableTest extends LoadableTest {
  private static final Loadable<List<String>> STRING_LIST = StringLoadable.DEFAULT.list();
  private static final Loadable<List<Integer>> INT_LIST = IntLoadable.ANY_FULL.list();
  private static final Loadable<Set<String>> STRING_SET = StringLoadable.DEFAULT.set();
  private static final Loadable<Map<String,Integer>> STRING_INT_MAP = StringLoadable.DEFAULT.mapWithValues(IntLoadable.ANY_FULL);
  private static final ArrayLoadable<int[]> INT_ARRAY = IntLoadable.ANY_FULL.array(0);
  private static final ArrayLoadable<byte[]> BYTE_ARRAY = IntLoadable.ANY_BYTE.byteArray(0);
  private static final ArrayLoadable<long[]> LONG_ARRAY = LongLoadable.ANY.array(0);
  private static final ArrayLoadable<String[]> OBJECT_ARRAY = StringLoadable.DEFAULT.array(String[]::new, false, 0);


  /* Round trips */

  @Test
  void list_roundTrips() {
    assertRoundTrip(STRING_LIST, List.of("a"));
    assertRoundTrip(STRING_LIST, List.of("a", "b", "c"));
    assertRoundTrip(INT_LIST, List.of(1, 2, 3));
  }

  @Test
  void set_roundTrips() {
    assertRoundTrip(STRING_SET, Set.of("only"));
    assertRoundTrip(STRING_SET, Set.of("a", "b", "c"));
  }

  @Test
  void map_roundTrips() {
    assertRoundTrip(STRING_INT_MAP, Map.of("a", 1));
    assertRoundTrip(STRING_INT_MAP, Map.of("a", 1, "b", 2, "c", 3));
  }

  @Test
  void arrays_roundTrip() {
    assertRoundTrip(INT_ARRAY, new int[0]);
    assertRoundTrip(INT_ARRAY, new int[] { 1, 2, 3 });
    assertRoundTrip(BYTE_ARRAY, new byte[] { 1, -2, 3 });
    assertRoundTrip(LONG_ARRAY, new long[] { 1L, Long.MAX_VALUE });
    assertRoundTrip(OBJECT_ARRAY, new String[] { "a", "b" });
  }

  @Test
  void compactList_roundTripsBothShapes() {
    // a min size below zero means a single element may be written without the surrounding list
    Loadable<List<String>> compact = StringLoadable.DEFAULT.list(ArrayLoadable.COMPACT);
    assertRoundTrip(compact, List.of("only"));
    assertRoundTrip(compact, List.of("a", "b"));
    assertThat(toNbt(compact, List.of("only"))).isEqualTo(StringTag.valueOf("only"));
    assertThat(toNbt(compact, List.of("a", "b"))).isInstanceOf(ListTag.class);
  }

  @Test
  void nestedList_roundTrips() {
    Loadable<List<List<String>>> nested = StringLoadable.DEFAULT.list(0).list(0);
    assertRoundTrip(nested, List.of(List.of("a", "b"), List.of(), List.of("c")));
  }


  /* NBT fidelity, these fail if the loadables fall back to converting through gson */

  @Test
  void intList_writesIntArrayTag() {
    // through gson these small numbers land in a byte array, the native path keeps them ints
    assertThat(toNbt(INT_LIST, List.of(1, 2, 3))).isEqualTo(new IntArrayTag(new int[] { 1, 2, 3 }));
    assertThat(toNbt(INT_ARRAY, new int[] { 1, 2, 3 })).isEqualTo(new IntArrayTag(new int[] { 1, 2, 3 }));
  }

  @Test
  void byteArray_writesIntArrayTagAndReadsByteArrayTag() {
    // the element form comes from the base loadable, which for a byte array is an int loadable, so we write ints
    assertThat(toNbt(BYTE_ARRAY, new byte[] { 1, 2, 3 })).isEqualTo(new IntArrayTag(new int[] { 1, 2, 3 }));
    // a byte array tag written by anything else still reads back
    assertThat(BYTE_ARRAY.convert(NbtOps.INSTANCE, new ByteArrayTag(new byte[] { 1, 2, 3 }), KEY)).containsExactly((byte)1, (byte)2, (byte)3);
  }

  @Test
  void longArray_writesLongArrayTag() {
    assertThat(toNbt(LONG_ARRAY, new long[] { 1, 2, 3 })).isEqualTo(new LongArrayTag(new long[] { 1, 2, 3 }));
  }

  @Test
  void booleanList_writesByteArrayTag() {
    assertThat(toNbt(BooleanLoadable.INSTANCE.list(), List.of(true, false, true)))
      .isEqualTo(new ByteArrayTag(new byte[] { 1, 0, 1 }));
  }

  @Test
  void stringList_writesListTag() {
    ListTag expected = new ListTag();
    expected.add(StringTag.valueOf("a"));
    expected.add(StringTag.valueOf("b"));
    assertThat(toNbt(STRING_LIST, List.of("a", "b"))).isEqualTo(expected);
  }

  @Test
  void intList_readsNativeArrayTags() {
    assertThat(INT_LIST.convert(NbtOps.INSTANCE, new IntArrayTag(new int[] { 1, 2 }), KEY)).containsExactly(1, 2);
    assertThat(INT_LIST.convert(NbtOps.INSTANCE, new ByteArrayTag(new byte[] { 1, 2 }), KEY)).containsExactly(1, 2);
    assertThat(INT_LIST.convert(NbtOps.INSTANCE, new LongArrayTag(new long[] { 1, 2 }), KEY)).containsExactly(1, 2);
  }

  @Test
  void map_writesCompoundTag() {
    assertThat(toNbt(STRING_INT_MAP, Map.of("a", 1)).toString()).isEqualTo("{a:1}");
  }


  /* Errors */

  @Test
  void list_rejectsNonList() {
    assertThatThrownBy(() -> STRING_LIST.convert(NbtOps.INSTANCE, StringTag.valueOf("nope"), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining(KEY);
  }

  @Test
  void list_rejectsBelowMinSize() {
    assertThatThrownBy(() -> STRING_LIST.convert(NbtOps.INSTANCE, new ListTag(), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining(KEY);
  }

  @Test
  void map_rejectsBelowMinSize() {
    assertThatThrownBy(() -> STRING_INT_MAP.convert(NbtOps.INSTANCE, NbtOps.INSTANCE.emptyMap(), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining(KEY);
  }
}
