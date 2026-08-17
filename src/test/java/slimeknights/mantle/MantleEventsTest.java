package slimeknights.mantle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.item.data.DataEditor;
import slimeknights.mantle.item.data.DataView;
import slimeknights.mantle.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the soulbound slot marker to the exact tag layout it used before {@link MantleEvents#SOULBOUND} existed, since
 * {@link MantleEvents#SOULBOUND_SLOT} is documented as usable by dependent mods writing the raw tag directly.
 * <p>
 * The compound those entries live in is {@code minecraft:custom_data} rather than the stack's own tag, so the
 * assertions read it through {@link BaseMcTest}'s custom data helpers. The layout inside that compound - the
 * bare key and its int value - is what these tests exist to hold still, and it is unchanged.
 */
class MantleEventsTest extends BaseMcTest {
  @Test
  void soulbound_isStoredUnderTheBareLegacyName() {
    assertThat(MantleEvents.SOULBOUND.getName()).isEqualTo(MantleEvents.SOULBOUND_SLOT);
  }

  @Test
  void soulbound_readsATagWrittenTheOldWay() {
    ItemStack stack = new ItemStack(Items.STONE);
    CompoundTag tag = new CompoundTag();
    tag.putInt(MantleEvents.SOULBOUND_SLOT, 7);
    setCustomData(stack, tag);

    assertThat(DataView.of(stack).get(MantleEvents.SOULBOUND)).isEqualTo(7);
  }

  @Test
  void soulbound_writesExactlyTheLegacyTagLayout() {
    ItemStack stack = new ItemStack(Items.STONE);
    DataEditor.edit().set(MantleEvents.SOULBOUND, 7).apply(stack);

    CompoundTag tag = getCustomData(stack);
    assertThat(tag).isNotNull();
    assertThat(tag.getAllKeys()).containsExactly(MantleEvents.SOULBOUND_SLOT);
    assertThat(tag.get(MantleEvents.SOULBOUND_SLOT)).isEqualTo(IntTag.valueOf(7));
  }

  @Test
  void soulbound_nonNumericEntryReadsAsAbsent() {
    // the original guard was tag.contains(key, TAG_ANY_NUMERIC), so a non-numeric value was always treated as unset
    ItemStack stack = new ItemStack(Items.STONE);
    CompoundTag tag = new CompoundTag();
    tag.putString(MantleEvents.SOULBOUND_SLOT, "not a number");
    setCustomData(stack, tag);

    assertThat(DataView.of(stack).get(MantleEvents.SOULBOUND)).isNull();
  }

  @Test
  void soulbound_removeDropsAnOtherwiseEmptyTag() {
    ItemStack stack = new ItemStack(Items.STONE);
    DataEditor.edit().set(MantleEvents.SOULBOUND, 3).apply(stack);
    assertThat(hasCustomData(stack)).isTrue();

    DataEditor.edit(stack).remove(MantleEvents.SOULBOUND).apply(stack);
    assertThat(hasCustomData(stack)).isFalse();
  }

  @Test
  void soulbound_removeKeepsUnrelatedEntries() {
    ItemStack stack = new ItemStack(Items.STONE);
    getOrCreateCustomData(stack).putString("other_mod:their_key", "value");
    DataEditor.edit(stack).set(MantleEvents.SOULBOUND, 3).apply(stack);

    DataEditor.edit(stack).remove(MantleEvents.SOULBOUND).apply(stack);
    assertThat(getCustomData(stack).getAllKeys()).containsExactly("other_mod:their_key");
  }
}
