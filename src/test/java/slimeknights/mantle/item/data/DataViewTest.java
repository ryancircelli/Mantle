package slimeknights.mantle.item.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.common.NBTLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.test.BaseMcTest;
import slimeknights.mantle.util.typed.TypedMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests the read side of the item data API */
class DataViewTest extends BaseMcTest {
  private static ResourceLocation id(String path) {
    return new ResourceLocation("mantle_test", "view_" + path);
  }

  /** Loadable recording how often it was asked to read, used to show a view parses only what it is asked for */
  private static class CountingLoadable<T> implements Loadable<T> {
    private final Loadable<T> base;
    int reads = 0;

    CountingLoadable(Loadable<T> base) {
      this.base = base;
    }

    @Override
    public T convert(JsonElement element, String key, TypedMap context) {
      return convert(JsonOps.INSTANCE, element, key, context);
    }

    @Override
    public <O> T convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
      reads++;
      return base.convert(ops, input, key, context);
    }

    @Override
    public JsonElement serialize(T object) {
      return base.serialize(object);
    }

    @Override
    public <O> O serialize(DynamicOps<O> ops, T object) {
      return base.serialize(ops, object);
    }

    @Override
    public T decode(FriendlyByteBuf buffer, TypedMap context) {
      return base.decode(buffer, context);
    }

    @Override
    public void encode(FriendlyByteBuf buffer, T value) {
      base.encode(buffer, value);
    }
  }

  private static final CountingLoadable<Integer> COUNTED = new CountingLoadable<>(IntLoadable.ANY_FULL);
  private static final DataKey<String> NAME = DataKey.of(id("name"), StringLoadable.DEFAULT);
  private static final DataKey<Integer> COUNT = DataKey.of(id("count"), COUNTED);
  private static final DataKey<CompoundTag> NBT = DataKey.of(id("nbt"), NBTLoadable.DISALLOW_STRING);

  @BeforeEach
  void resetCounts() {
    COUNTED.reads = 0;
  }

  /** Builds a stack whose tag holds a value for both simple keys */
  private static ItemStack filledStack() {
    ItemStack stack = new ItemStack(Items.STONE);
    CompoundTag tag = stack.getOrCreateTag();
    tag.putString(NAME.getName(), "hello");
    tag.putInt(COUNT.getName(), 7);
    return stack;
  }


  /* Absent data */

  @Test
  void stackWithNoTag_readsAsAbsent() {
    DataView view = DataView.of(new ItemStack(Items.STONE));
    assertThat(view.get(NAME)).isNull();
    assertThat(view.getStrict(NAME)).isNull();
    assertThat(view.has(NAME)).isFalse();
    assertThat(view.getOrDefault(NAME, "fallback")).isEqualTo("fallback");
  }

  @Test
  void tagWithoutTheKey_readsAsAbsent() {
    ItemStack stack = new ItemStack(Items.STONE);
    stack.getOrCreateTag().putString("other_mod:something", "value");
    DataView view = DataView.of(stack);
    assertThat(view.get(NAME)).isNull();
    assertThat(view.has(NAME)).isFalse();
  }

  @Test
  void empty_readsEveryKeyAsAbsent() {
    assertThat(DataView.EMPTY.get(NAME)).isNull();
    assertThat(DataView.EMPTY.getStrict(NAME)).isNull();
    assertThat(DataView.EMPTY.has(NAME)).isFalse();
    assertThat(DataView.EMPTY.getOrDefault(COUNT, 3)).isEqualTo(3);
  }

  @Test
  void ofNullTag_isTheEmptyView() {
    assertThat(DataView.of((CompoundTag)null)).isSameAs(DataView.EMPTY);
  }


  /* Present data */

  @Test
  void readsAValueFromTheTag() {
    DataView view = DataView.of(filledStack());
    assertThat(view.get(NAME)).isEqualTo("hello");
    assertThat(view.get(COUNT)).isEqualTo(7);
    assertThat(view.has(NAME)).isTrue();
    assertThat(view.getOrDefault(NAME, "fallback")).isEqualTo("hello");
  }

  @Test
  void tagView_readsTheSameValues() {
    DataView view = DataView.of(filledStack().getTag());
    assertThat(view.get(NAME)).isEqualTo("hello");
    assertThat(view.get(COUNT)).isEqualTo(7);
  }

  @Test
  void viewFollowsTheStack() {
    // a view is a window on the stack rather than a snapshot, so it sees a tag the stack did not have when it was made
    ItemStack stack = new ItemStack(Items.STONE);
    DataView view = DataView.of(stack);
    assertThat(view.get(NAME)).isNull();
    stack.getOrCreateTag().putString(NAME.getName(), "later");
    assertThat(view.get(NAME)).isEqualTo("later");
  }


  /* Reading only what was asked for */

  @Test
  void creatingAViewReadsNothing() {
    ItemStack stack = filledStack();
    DataView.of(stack);
    DataView.of(stack.getTag());
    assertThat(COUNTED.reads).isZero();
  }

  @Test
  void readingOneKeyDoesNotParseAnother() {
    DataView view = DataView.of(filledStack());
    assertThat(view.get(NAME)).isEqualTo("hello");
    assertThat(view.has(COUNT)).isTrue();
    assertThat(COUNTED.reads).as("only the key asked for is parsed").isZero();
    assertThat(view.get(COUNT)).isEqualTo(7);
    assertThat(COUNTED.reads).isEqualTo(1);
  }


  /* Malformed data */

  @Test
  void malformedValue_readsAsAbsent() {
    ItemStack stack = new ItemStack(Items.STONE);
    stack.getOrCreateTag().putString(COUNT.getName(), "not a number");
    DataView view = DataView.of(stack);
    assertThat(view.get(COUNT)).isNull();
    assertThat(view.getOrDefault(COUNT, 3)).isEqualTo(3);
  }

  @Test
  void malformedValue_isStillPresent() {
    // has reports the entry, not whether it can be read, which is what tells a broken value from a missing one
    ItemStack stack = new ItemStack(Items.STONE);
    stack.getOrCreateTag().putString(COUNT.getName(), "not a number");
    DataView view = DataView.of(stack);
    assertThat(view.has(COUNT)).isTrue();
    assertThat(view.get(COUNT)).isNull();
  }

  @Test
  void malformedValue_reportedByGetStrict() {
    ItemStack stack = new ItemStack(Items.STONE);
    stack.getOrCreateTag().putString(COUNT.getName(), "not a number");
    assertThatThrownBy(() -> DataView.of(stack).getStrict(COUNT))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining(COUNT.getName());
  }

  @Test
  void hostileTag_neverThrows() {
    // the shapes a stack can arrive from a client, a command or an old save with, none of which may crash a tooltip
    ItemStack stack = new ItemStack(Items.STONE);
    CompoundTag tag = stack.getOrCreateTag();
    tag.put(NAME.getName(), new ListTag());
    tag.put(COUNT.getName(), new CompoundTag());
    tag.putInt(NBT.getName(), 5);
    DataView view = DataView.of(stack);
    assertThat(view.get(NAME)).isNull();
    assertThat(view.get(COUNT)).isNull();
    assertThat(view.get(NBT)).isNull();
    // and it keeps giving the same answer rather than failing differently the second time
    assertThat(view.get(COUNT)).isNull();
  }


  /* Value semantics */

  @Test
  void get_buildsAFreshValueEachTime() {
    ItemStack stack = new ItemStack(Items.STONE);
    CompoundTag stored = new CompoundTag();
    stored.putInt("value", 1);
    stack.getOrCreateTag().put(NBT.getName(), stored);

    DataView view = DataView.of(stack);
    CompoundTag first = view.get(NBT);
    CompoundTag second = view.get(NBT);
    assertThat(first).isEqualTo(second).isNotSameAs(second);
  }

  @Test
  void get_doesNotHandOutTheStacksOwnTag() {
    // NBTLoadable reads a tag by handing back the value it was given, which would make the returned compound the
    // stack's live entry. the view copies it so a value from this API never writes through to a stack
    ItemStack stack = new ItemStack(Items.STONE);
    CompoundTag stored = new CompoundTag();
    stored.putInt("value", 1);
    stack.getOrCreateTag().put(NBT.getName(), stored);

    CompoundTag read = DataView.of(stack).get(NBT);
    assertThat(read).isNotSameAs(stored);
  }

  @Test
  void changingAValueDoesNotChangeTheStack() {
    ItemStack stack = new ItemStack(Items.STONE);
    CompoundTag stored = new CompoundTag();
    stored.putInt("value", 1);
    stack.getOrCreateTag().put(NBT.getName(), stored);

    DataView view = DataView.of(stack);
    CompoundTag read = view.get(NBT);
    read.putInt("value", 99);
    read.putString("added", "text");

    CompoundTag reread = view.get(NBT);
    assertThat(reread.getInt("value")).isEqualTo(1);
    assertThat(reread.contains("added")).isFalse();
  }
}
