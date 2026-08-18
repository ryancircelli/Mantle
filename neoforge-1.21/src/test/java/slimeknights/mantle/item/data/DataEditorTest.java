package slimeknights.mantle.item.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.common.NBTLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.test.BaseMcTest;
import slimeknights.mantle.util.typed.TypedMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests the write side of the item data API */
class DataEditorTest extends BaseMcTest {
  private static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath("mantle_test", "editor_" + path);
  }

  /** Loadable whose empty string writes nothing at all, the case where a value has no entry to store */
  private static final Loadable<String> OPTIONAL_STRING = new Loadable<>() {
    @Override
    public String convert(JsonElement element, String key, TypedMap context) {
      return convert(JsonOps.INSTANCE, element, key, context);
    }

    @Override
    public <O> String convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
      return OpsHelper.isEmpty(ops, input) ? "" : OpsHelper.getString(ops, input, key);
    }

    @Override
    public JsonElement serialize(String object) {
      return serialize(JsonOps.INSTANCE, object);
    }

    @Override
    public <O> O serialize(DynamicOps<O> ops, String object) {
      return object.isEmpty() ? ops.empty() : ops.createString(object);
    }

    @Override
    public String decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return buffer.readUtf();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, String value) {
      buffer.writeUtf(value);
    }
  };

  private static final DataKey<String> NAME = DataKey.of(id("name"), StringLoadable.DEFAULT);
  private static final DataKey<Integer> COUNT = DataKey.of(id("count"), IntLoadable.ANY_FULL);
  private static final DataKey<CompoundTag> NBT = DataKey.of(id("nbt"), NBTLoadable.DISALLOW_STRING);
  private static final DataKey<String> SHORT_NAME = DataKey.of(id("short_name"), StringLoadable.DEFAULT.validate((value, error) -> {
    if (value.length() > 4) {
      throw error.create("Name may be at most 4 characters");
    }
    return value;
  }));
  private static final DataKey<String> OPTIONAL = DataKey.of(id("optional"), OPTIONAL_STRING);

  /** Name another mod wrote into the same tag, which nothing here may disturb */
  private static final String FOREIGN_NAME = "other_mod:their_data";

  /** Builds a stack whose tag already holds another mod's data */
  private static ItemStack foreignStack() {
    ItemStack stack = new ItemStack(Items.STONE);
    CompoundTag foreign = new CompoundTag();
    foreign.putInt("their_value", 42);
    CompoundTag tag = getOrCreateCustomData(stack);
    tag.put(FOREIGN_NAME, foreign);
    tag.putString("Legacy", "an entry with no owner at all");
    return stack;
  }


  /* Nothing happens until apply */

  @Test
  void setIsNotVisibleUntilApply() {
    ItemStack stack = new ItemStack(Items.STONE);
    DataEditor editor = DataEditor.edit(stack);
    editor.set(NAME, "hello");
    assertThat(hasCustomData(stack)).as("the stack must not even gain a tag before apply").isFalse();
    assertThat(DataView.of(stack).get(NAME)).isNull();

    assertThat(editor.apply(stack)).isSameAs(stack);
    assertThat(DataView.of(stack).get(NAME)).isEqualTo("hello");
  }

  @Test
  void setOnAnExistingTagIsNotVisibleUntilApply() {
    ItemStack stack = foreignStack();
    CompoundTag before = getCustomData(stack).copy();
    DataEditor editor = DataEditor.edit(stack).set(NAME, "hello").set(COUNT, 3);
    assertThat(getCustomData(stack)).isEqualTo(before);

    editor.apply(stack);
    assertThat(getCustomData(stack)).isNotEqualTo(before);
    assertThat(DataView.of(stack).get(NAME)).isEqualTo("hello");
  }

  @Test
  void removeIsNotVisibleUntilApply() {
    ItemStack stack = new ItemStack(Items.STONE);
    getOrCreateCustomData(stack).putString(NAME.getName(), "hello");
    DataEditor editor = DataEditor.edit(stack).remove(NAME);
    assertThat(DataView.of(stack).get(NAME)).isEqualTo("hello");

    editor.apply(stack);
    assertThat(DataView.of(stack).has(NAME)).isFalse();
  }


  /* Reading an editor */

  @Test
  void readsItsOwnWrites() {
    DataEditor editor = DataEditor.edit();
    assertThat(editor.get(NAME)).isNull();
    editor.set(NAME, "hello");
    assertThat(editor.get(NAME)).isEqualTo("hello");
    assertThat(editor.has(NAME)).isTrue();
    assertThat(editor.getOrDefault(NAME, "fallback")).isEqualTo("hello");
    assertThat(editor.getStrict(NAME)).isEqualTo("hello");
  }

  @Test
  void lastSetWins() {
    DataEditor editor = DataEditor.edit().set(COUNT, 1).set(COUNT, 2).set(COUNT, 3);
    assertThat(editor.get(COUNT)).isEqualTo(3);
    ItemStack stack = editor.apply(new ItemStack(Items.STONE));
    assertThat(DataView.of(stack).get(COUNT)).isEqualTo(3);
  }

  @Test
  void removeThenSetKeepsTheValue() {
    ItemStack stack = new ItemStack(Items.STONE);
    getOrCreateCustomData(stack).putString(NAME.getName(), "old");
    DataEditor editor = DataEditor.edit(stack).remove(NAME).set(NAME, "new");
    assertThat(editor.get(NAME)).isEqualTo("new");
    editor.apply(stack);
    assertThat(DataView.of(stack).get(NAME)).isEqualTo("new");
  }

  @Test
  void setThenRemoveDropsTheValue() {
    ItemStack stack = new ItemStack(Items.STONE);
    getOrCreateCustomData(stack).putString(NAME.getName(), "old");
    DataEditor editor = DataEditor.edit(stack).set(NAME, "new").remove(NAME);
    assertThat(editor.get(NAME)).isNull();
    assertThat(editor.has(NAME)).isFalse();
    editor.apply(stack);
    assertThat(DataView.of(stack).has(NAME)).isFalse();
  }

  @Test
  void removeHidesTheStacksValue() {
    ItemStack stack = new ItemStack(Items.STONE);
    getOrCreateCustomData(stack).putString(NAME.getName(), "hello");
    DataEditor editor = DataEditor.edit(stack).remove(NAME);
    assertThat(editor.get(NAME)).isNull();
    assertThat(editor.getStrict(NAME)).isNull();
    assertThat(editor.has(NAME)).isFalse();
    assertThat(editor.getOrDefault(NAME, "fallback")).isEqualTo("fallback");
  }

  @Test
  void editStackReadsThroughToTheStack() {
    ItemStack stack = new ItemStack(Items.STONE);
    getOrCreateCustomData(stack).putString(NAME.getName(), "hello");
    DataEditor editor = DataEditor.edit(stack).set(COUNT, 1);
    assertThat(editor.get(NAME)).isEqualTo("hello");
    assertThat(editor.has(NAME)).isTrue();
  }

  @Test
  void editWithNoStackReadsNothing() {
    ItemStack stack = new ItemStack(Items.STONE);
    getOrCreateCustomData(stack).putString(NAME.getName(), "hello");
    DataEditor editor = DataEditor.edit();
    assertThat(editor.get(NAME)).isNull();
    assertThat(editor.has(NAME)).isFalse();
    // and applying it leaves the value it never read alone
    editor.set(COUNT, 1).apply(stack);
    assertThat(DataView.of(stack).get(NAME)).isEqualTo("hello");
  }

  @Test
  void readModifyWriteOfOneKey() {
    ItemStack stack = new ItemStack(Items.STONE);
    DataEditor editor = DataEditor.edit(stack);
    editor.set(COUNT, editor.getOrDefault(COUNT, 0) + 1).apply(stack);
    assertThat(DataView.of(stack).get(COUNT)).isEqualTo(1);

    DataEditor again = DataEditor.edit(stack);
    again.set(COUNT, again.getOrDefault(COUNT, 0) + 1).apply(stack);
    assertThat(DataView.of(stack).get(COUNT)).isEqualTo(2);
  }


  /* Leaving everything else alone */

  @Test
  void applyKeepsUnrelatedEntries() {
    ItemStack stack = foreignStack();
    DataEditor.edit(stack).set(NAME, "hello").set(COUNT, 3).apply(stack);

    CompoundTag tag = getCustomData(stack);
    assertThat(tag.getCompound(FOREIGN_NAME).getInt("their_value")).isEqualTo(42);
    assertThat(tag.getString("Legacy")).isEqualTo("an entry with no owner at all");
    assertThat(tag.getAllKeys()).containsExactlyInAnyOrder(FOREIGN_NAME, "Legacy", NAME.getName(), COUNT.getName());
  }

  @Test
  void removeKeepsUnrelatedEntries() {
    ItemStack stack = foreignStack();
    DataEditor.edit(stack).set(NAME, "hello").apply(stack);
    DataEditor.edit(stack).remove(NAME).apply(stack);

    CompoundTag tag = getCustomData(stack);
    assertThat(tag.getCompound(FOREIGN_NAME).getInt("their_value")).isEqualTo(42);
    assertThat(tag.getAllKeys()).containsExactlyInAnyOrder(FOREIGN_NAME, "Legacy");
  }

  @Test
  void removingAKeyItDoesNotHaveChangesNothing() {
    ItemStack stack = foreignStack();
    CompoundTag before = getCustomData(stack).copy();
    DataEditor.edit(stack).remove(NAME).apply(stack);
    assertThat(getCustomData(stack)).isEqualTo(before);
  }


  /* Tag lifecycle */

  @Test
  void applyWithNoChangesLeavesTheStackAlone() {
    ItemStack stack = new ItemStack(Items.STONE);
    DataEditor.edit(stack).apply(stack);
    assertThat(hasCustomData(stack)).isFalse();
  }

  @Test
  void applyWithOnlyRemovalsCreatesNoTag() {
    ItemStack stack = new ItemStack(Items.STONE);
    DataEditor.edit(stack).remove(NAME).remove(COUNT).apply(stack);
    assertThat(hasCustomData(stack)).as("an empty tag would stop the stack from stacking with a plain one").isFalse();
  }

  @Test
  void applyDroppingTheLastValueDropsTheTag() {
    ItemStack stack = new ItemStack(Items.STONE);
    DataEditor.edit(stack).set(NAME, "hello").apply(stack);
    assertThat(hasCustomData(stack)).isTrue();
    DataEditor.edit(stack).remove(NAME).apply(stack);
    assertThat(hasCustomData(stack)).isFalse();
  }

  @Test
  void applyToAnEmptyStackDoesNothing() {
    // the empty stack is shared by everything holding nothing, writing to it would give the data to all of them
    ItemStack stack = ItemStack.EMPTY;
    assertThat(DataEditor.edit().set(NAME, "hello").apply(stack)).isSameAs(stack);
    assertThat(hasCustomData(stack)).isFalse();
  }


  /* Values are the editor's own */

  @Test
  void applyingTwiceDoesNotShareValues() {
    CompoundTag value = new CompoundTag();
    value.putInt("value", 1);
    DataEditor editor = DataEditor.edit().set(NBT, value);
    ItemStack first = editor.apply(new ItemStack(Items.STONE));
    ItemStack second = editor.apply(new ItemStack(Items.STONE));

    // changing what one stack stores must not reach the other
    getCustomData(first).getCompound(NBT.getName()).putInt("value", 99);
    assertThat(DataView.of(second).get(NBT).getInt("value")).isEqualTo(1);
  }

  @Test
  void setCopiesTheValueItIsGiven() {
    // NBTLoadable writes a tag by handing back the value it was given, which would leave the editor holding the
    // caller's own object and let a later change to it reach the stack
    CompoundTag value = new CompoundTag();
    value.putInt("value", 1);
    DataEditor editor = DataEditor.edit().set(NBT, value);
    value.putInt("value", 99);

    assertThat(editor.get(NBT).getInt("value")).isEqualTo(1);
    ItemStack stack = editor.apply(new ItemStack(Items.STONE));
    assertThat(DataView.of(stack).get(NBT).getInt("value")).isEqualTo(1);
  }

  @Test
  void readingAnEditorBuildsAFreshValue() {
    CompoundTag value = new CompoundTag();
    value.putInt("value", 1);
    DataEditor editor = DataEditor.edit().set(NBT, value);
    CompoundTag read = editor.get(NBT);
    assertThat(read).isEqualTo(value).isNotSameAs(value);
    read.putInt("value", 99);
    assertThat(editor.get(NBT).getInt("value")).isEqualTo(1);
  }


  /* Failures */

  @Test
  void setReportsAValueItCannotWrite() {
    ItemStack stack = new ItemStack(Items.STONE);
    DataEditor editor = DataEditor.edit(stack);
    assertThatThrownBy(() -> editor.set(SHORT_NAME, "far too long"))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("at most 4 characters");
    // and the failed set left nothing behind, on the editor or the stack
    assertThat(editor.has(SHORT_NAME)).isFalse();
    assertThat(editor.apply(stack)).isSameAs(stack);
    assertThat(hasCustomData(stack)).isFalse();
  }

  @Test
  void setOfAValueWritingNothingRemovesTheEntry() {
    // a loadable which writes the empty value of the format has no entry to store, so there is nothing to keep
    ItemStack stack = new ItemStack(Items.STONE);
    DataEditor.edit(stack).set(OPTIONAL, "hello").apply(stack);
    assertThat(DataView.of(stack).get(OPTIONAL)).isEqualTo("hello");

    DataEditor editor = DataEditor.edit(stack).set(OPTIONAL, "");
    assertThat(editor.has(OPTIONAL)).isFalse();
    editor.apply(stack);
    assertThat(DataView.of(stack).has(OPTIONAL)).isFalse();
    assertThat(hasCustomData(stack)).isFalse();
  }
}
