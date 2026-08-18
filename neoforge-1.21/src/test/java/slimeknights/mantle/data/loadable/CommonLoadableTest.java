package slimeknights.mantle.data.loadable;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.common.BlockStateLoadable;
import slimeknights.mantle.data.loadable.common.CodecLoadable;
import slimeknights.mantle.data.loadable.common.ItemStackLoadable;
import slimeknights.mantle.data.loadable.common.NBTLoadable;
import slimeknights.mantle.data.loadable.common.Vector3fLoadable;
import slimeknights.mantle.test.LoadableTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Round trip tests for the loadables wrapping common Minecraft types */
class CommonLoadableTest extends LoadableTest {
  /** Builds a tag with a value of each numeric type, which only survives a format that has them all */
  private static CompoundTag mixedTag() {
    CompoundTag tag = new CompoundTag();
    tag.putByte("byte", (byte)1);
    tag.putShort("short", (short)2);
    tag.putInt("int", 3);
    tag.putLong("long", 4);
    tag.putFloat("float", 5.5f);
    tag.putDouble("double", 6.5);
    tag.putString("string", "text");
    tag.putIntArray("ints", new int[] { 1, 2, 3 });
    return tag;
  }


  /* NBT */

  /** Builds a tag whose types all survive a trip through gson */
  private static CompoundTag simpleTag() {
    CompoundTag tag = new CompoundTag();
    tag.putString("string", "text");
    tag.putBoolean("flag", true);
    return tag;
  }

  @Test
  void nbtLoadable_roundTrips() {
    assertRoundTrip(NBTLoadable.DISALLOW_STRING, simpleTag());
    assertRoundTrip(NBTLoadable.ALLOW_STRING, simpleTag());
  }

  @Test
  void nbtLoadable_keepsEveryNumericTypeThroughNbtOps() {
    // gson has no notion of the NBT numeric types, so only the ops path can promise this
    assertNbtRoundTrip(NBTLoadable.DISALLOW_STRING, mixedTag());
    assertNetworkRoundTrip(NBTLoadable.DISALLOW_STRING, mixedTag());
  }

  @Test
  void nbtLoadable_isIdentityThroughNbtOps() {
    // reading NBT out of NBT should not touch the tag at all, which is also what keeps the numeric types
    CompoundTag tag = mixedTag();
    assertThat(toNbt(NBTLoadable.DISALLOW_STRING, tag)).isSameAs(tag);
    assertThat(NBTLoadable.DISALLOW_STRING.convert(NbtOps.INSTANCE, tag, KEY)).isSameAs(tag);
  }

  @Test
  void nbtLoadable_allowStringParsesATagString() {
    CompoundTag expected = new CompoundTag();
    expected.putInt("Damage", 5);
    assertThat(NBTLoadable.ALLOW_STRING.convert(new JsonPrimitive("{Damage:5}"), KEY)).isEqualTo(expected);
    assertThat(NBTLoadable.ALLOW_STRING.convert(NbtOps.INSTANCE, StringTag.valueOf("{Damage:5}"), KEY)).isEqualTo(expected);
  }


  /* Item stacks */

  @Test
  void itemStack_roundTrips() {
    assertJsonRoundTripOfStack(ItemStackLoadable.OPTIONAL_STACK, new ItemStack(Items.DIAMOND, 3));
    assertJsonRoundTripOfStack(ItemStackLoadable.OPTIONAL_STACK, new ItemStack(Items.DIAMOND));
    assertJsonRoundTripOfStack(ItemStackLoadable.OPTIONAL_STACK_NBT, new ItemStack(Items.DIAMOND, 2));
  }

  @Test
  void itemStackWithNbt_roundTrips() {
    ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
    setCustomData(stack, simpleTag());
    assertJsonRoundTripOfStack(ItemStackLoadable.OPTIONAL_STACK_NBT, stack);
  }

  @Test
  void itemStackWithNbt_keepsNumericTypesThroughNbtOps() {
    ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
    setCustomData(stack, mixedTag());
    ItemStack result = ItemStackLoadable.OPTIONAL_STACK_NBT.convert(
      NbtOps.INSTANCE, ItemStackLoadable.OPTIONAL_STACK_NBT.serialize(NbtOps.INSTANCE, stack), KEY);
    // compare against the stack rather than the tag we built, so the assertion follows whatever the component stored
    assertThat(getCustomData(result)).isEqualTo(getCustomData(stack));
  }

  @Test
  void itemStack_keepsCountAsAnIntTag() {
    CompoundTag tag = (CompoundTag)toNbt(ItemStackLoadable.OPTIONAL_STACK, new ItemStack(Items.DIAMOND, 3));
    assertThat(tag.get("count")).isEqualTo(IntTag.valueOf(3));
    assertThat(tag.get("item")).isEqualTo(StringTag.valueOf("minecraft:diamond"));
  }

  /** Item stacks have no useful equals, so compare the serialized forms instead */
  private static void assertJsonRoundTripOfStack(Loadable<ItemStack> loadable, ItemStack stack) {
    ItemStack fromJson = loadable.convert(loadable.serialize(stack), KEY);
    assertThat(ItemStack.isSameItemSameComponents(fromJson, stack)).as("gson round trip").isTrue();
    assertThat(fromJson.getCount()).as("gson round trip count").isEqualTo(stack.getCount());
    ItemStack fromNbt = loadable.convert(NbtOps.INSTANCE, loadable.serialize(NbtOps.INSTANCE, stack), KEY);
    assertThat(ItemStack.isSameItemSameComponents(fromNbt, stack)).as("nbt round trip").isTrue();
    assertThat(fromNbt.getCount()).as("nbt round trip count").isEqualTo(stack.getCount());
  }


  /* Block states */

  @Test
  void blockState_roundTrips() {
    // the network form is a block state ID, which needs a populated state registry we do not have in a bare bootstrap
    BlockState slab = Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
    for (BlockState state : new BlockState[] { Blocks.STONE.defaultBlockState(), slab }) {
      for (BlockStateLoadable loadable : BlockStateLoadable.values()) {
        assertJsonRoundTrip(loadable, state);
        assertJsonOpsRoundTrip(loadable, state);
        assertNbtRoundTrip(loadable, state);
      }
    }
  }

  @Test
  void blockState_writesTheCompactFormForADefaultState() {
    assertThat(toNbt(BlockStateLoadable.DIFFERENCE, Blocks.STONE.defaultBlockState())).isEqualTo(StringTag.valueOf("minecraft:stone"));
    assertThat(toNbt(BlockStateLoadable.ALL, Blocks.STONE.defaultBlockState())).isInstanceOf(CompoundTag.class);
  }


  /* Vectors */

  @Test
  void vector3f_roundTrips() {
    assertRoundTrip(Vector3fLoadable.INSTANCE, new Vector3f(1.5f, -2.5f, 0));
  }

  @Test
  void vector3f_writesFloatTags() {
    ListTag tag = (ListTag)toNbt(Vector3fLoadable.INSTANCE, new Vector3f(1.5f, 2.5f, 3.5f));
    assertThat(tag).containsExactly(FloatTag.valueOf(1.5f), FloatTag.valueOf(2.5f), FloatTag.valueOf(3.5f));
  }


  /* Codecs */

  @Test
  void codecLoadable_readsTheFormatDirectly() {
    Loadable<Integer> loadable = new CodecLoadable<>(Codec.INT.fieldOf("value").codec());
    assertRoundTrip(loadable, 5);
    // a codec loadable used to force everything through JSON, so this pins that it no longer does
    CompoundTag expected = new CompoundTag();
    expected.putInt("value", 5);
    assertThat(toNbt(loadable, 5)).isEqualTo(expected);
  }

  @Test
  void codecLoadable_sendsANonCompoundValueOverTheNetwork() {
    // the buffer can only carry a compound tag, so a codec writing anything else used to throw a ClassCastException
    assertRoundTrip(new CodecLoadable<>(Codec.INT), 5);
    assertRoundTrip(new CodecLoadable<>(Codec.STRING), "text");
    assertRoundTrip(new CodecLoadable<>(Codec.INT.listOf()), List.of(1, 2, 3));
  }

  @Test
  void codecLoadable_writesTheCodecsOwnTags() {
    assertThat(toNbt(new CodecLoadable<>(Codec.INT), 5)).isEqualTo(IntTag.valueOf(5));
    assertThat(toNbt(new CodecLoadable<>(Codec.STRING), "text")).isEqualTo(StringTag.valueOf("text"));
  }
}
