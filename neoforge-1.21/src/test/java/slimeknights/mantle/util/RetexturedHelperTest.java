package slimeknights.mantle.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.item.data.DataView;
import slimeknights.mantle.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the "texture" entry to the exact tag layout it used before {@link RetexturedHelper#TEXTURE} existed, so a
 * world saved by an older Mantle still loads and a stack this class writes still loads on an older Mantle.
 */
class RetexturedHelperTest extends BaseMcTest {
  @Test
  void texture_isStoredUnderTheBareLegacyName() {
    // renaming this would strand the texture on every retextured block item in every existing world
    assertThat(RetexturedHelper.TEXTURE.getName()).isEqualTo("texture");
  }

  @Test
  void getTexture_readsATagWrittenTheOldWay() {
    ItemStack stack = new ItemStack(Items.STONE);
    CompoundTag tag = new CompoundTag();
    tag.putString("texture", "minecraft:dirt");
    setCustomData(stack, tag);

    assertThat(RetexturedHelper.getTexture(stack)).isEqualTo(Blocks.DIRT);
    assertThat(RetexturedHelper.getTextureName(stack)).isEqualTo("minecraft:dirt");
    assertThat(DataView.of(stack).get(RetexturedHelper.TEXTURE)).isEqualTo(Blocks.DIRT);
  }

  @Test
  void getTexture_missingTagReadsAsAir() {
    ItemStack stack = new ItemStack(Items.STONE);
    assertThat(RetexturedHelper.getTexture(stack)).isEqualTo(Blocks.AIR);
    assertThat(RetexturedHelper.getTextureName(stack)).isEmpty();
  }

  @Test
  void getTexture_malformedEntryReadsAsAir() {
    // an unregistered or non-string value never crashed the old raw reader, and must not crash this one either
    ItemStack stack = new ItemStack(Items.STONE);
    CompoundTag tag = new CompoundTag();
    tag.putString("texture", "not a valid: id");
    setCustomData(stack, tag);
    assertThat(RetexturedHelper.getTexture(stack)).isEqualTo(Blocks.AIR);
  }

  @Test
  void setTexture_writesExactlyTheLegacyTagLayout() {
    ItemStack stack = new ItemStack(Items.STONE);
    RetexturedHelper.setTexture(stack, Blocks.DIRT);

    CompoundTag tag = getCustomData(stack);
    assertThat(tag).isNotNull();
    assertThat(tag.getAllKeys()).containsExactly("texture");
    assertThat(tag.get("texture")).isEqualTo(StringTag.valueOf("minecraft:dirt"));
  }

  @Test
  void setTexture_stringOverloadMatchesBlockOverload() {
    ItemStack stack = new ItemStack(Items.STONE);
    RetexturedHelper.setTexture(stack, "minecraft:dirt");

    CompoundTag tag = getCustomData(stack);
    assertThat(tag).isNotNull();
    assertThat(tag.getAllKeys()).containsExactly("texture");
    assertThat(tag.get("texture")).isEqualTo(StringTag.valueOf("minecraft:dirt"));
  }

  @Test
  void setTexture_airRemovesTheEntryAndDropsTheTag() {
    ItemStack stack = new ItemStack(Items.STONE);
    RetexturedHelper.setTexture(stack, Blocks.DIRT);
    assertThat(hasCustomData(stack)).isTrue();

    RetexturedHelper.setTexture(stack, Blocks.AIR);
    assertThat(hasCustomData(stack)).isFalse();
  }

  @Test
  void setTexture_emptyStringRemovesTheEntryAndDropsTheTag() {
    ItemStack stack = new ItemStack(Items.STONE);
    RetexturedHelper.setTexture(stack, "minecraft:dirt");
    assertThat(hasCustomData(stack)).isTrue();

    RetexturedHelper.setTexture(stack, "");
    assertThat(hasCustomData(stack)).isFalse();
  }

  @Test
  void setTexture_onATaglessStackWithNoTextureDoesNothing() {
    ItemStack stack = new ItemStack(Items.STONE);
    RetexturedHelper.setTexture(stack, Blocks.AIR);
    assertThat(hasCustomData(stack)).isFalse();
  }

  @Test
  void setTexture_keepsUnrelatedEntries() {
    ItemStack stack = new ItemStack(Items.STONE);
    CompoundTag tag = getOrCreateCustomData(stack);
    tag.putString("other_mod:their_key", "value");

    RetexturedHelper.setTexture(stack, Blocks.DIRT);
    assertThat(getCustomData(stack).getAllKeys()).containsExactlyInAnyOrder("texture", "other_mod:their_key");

    RetexturedHelper.setTexture(stack, Blocks.AIR);
    assertThat(getCustomData(stack).getAllKeys()).containsExactly("other_mod:their_key");
  }

  @Test
  void getTextureName_compoundTagOverloadMatchesTheOldRawRead() {
    CompoundTag tag = new CompoundTag();
    tag.putString("texture", "minecraft:dirt");
    assertThat(RetexturedHelper.getTextureName(tag)).isEqualTo("minecraft:dirt");
    assertThat(RetexturedHelper.getTextureName((CompoundTag)null)).isEmpty();
  }
}
