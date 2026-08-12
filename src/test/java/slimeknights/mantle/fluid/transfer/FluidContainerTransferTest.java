package slimeknights.mantle.fluid.transfer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.test.BaseMcTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the fluid transfer JSON system: the format a 1.21 pack writes, what a 1.20 pack is told when its two NBT
 * types are gone, and what the component copying transfers actually move between an item stack and a fluid stack.
 */
class FluidContainerTransferTest extends BaseMcTest {
  private static final int BOTTLE = 250;

  @BeforeAll
  static void registerLoaders() {
    // the same call Mantle makes from RegisterEvent, so the corpus below goes through the real dispatch table
    FluidContainerTransferManager.registerDefaults();
  }

  /** Parses a transfer through the manager's own gson, as a datapack file would be read */
  private static IFluidContainerTransfer parse(String json) {
    return FluidContainerTransferManager.GSON.fromJson(json, IFluidContainerTransfer.class);
  }

  /** Serializes a transfer through the manager's own gson, as datagen would write it */
  private static JsonObject serialize(IFluidContainerTransfer transfer) {
    return FluidContainerTransferManager.GSON.toJsonTree(transfer, IFluidContainerTransfer.class).getAsJsonObject();
  }

  /** Asserts a transfer survives a serialize then parse round trip with the same JSON on both sides */
  private static void assertRoundTrip(IFluidContainerTransfer transfer, String expectedType) {
    JsonObject first = serialize(transfer);
    assertEquals(expectedType, first.get("type").getAsString());
    assertEquals(first, serialize(parse(first.toString())));
  }


  /* The format decision: 1.20's two NBT types are gone, and the error says what replaced them */

  @Test
  void emptyNbt_isRejectedNamingItsReplacement() {
    // the shape Mantle's own 1.20 datagen wrote for honey, minus the conditions
    JsonSyntaxException e = assertThrows(JsonSyntaxException.class, () -> parse("""
      {
        "type": "mantle:empty_nbt",
        "input": {"item": "minecraft:honey_bottle"},
        "result": "minecraft:glass_bottle",
        "fluid": {"fluid": "minecraft:water", "amount": 250}
      }"""));
    assertTrue(e.getMessage().contains(EmptyFluidWithComponentsTransfer.ID.toString()),
               "error should name the replacement type, was: " + e.getMessage());
  }

  @Test
  void fillNbt_isRejectedNamingItsReplacement() {
    JsonSyntaxException e = assertThrows(JsonSyntaxException.class, () -> parse("""
      {
        "type": "mantle:fill_nbt",
        "input": {"item": "minecraft:glass_bottle"},
        "result": "minecraft:potion",
        "fluid": {"fluid": "minecraft:water", "amount": 250}
      }"""));
    assertTrue(e.getMessage().contains(FillFluidWithComponentsTransfer.ID.toString()),
               "error should name the replacement type, was: " + e.getMessage());
  }

  @Test
  void removedTypes_areTheTwoThisVersionDropped() {
    assertEquals(Mantle.getResource("empty_nbt"), RemovedTransferTypes.EMPTY_NBT);
    assertEquals(Mantle.getResource("fill_nbt"), RemovedTransferTypes.FILL_NBT);
  }

  @Test
  void anUnknownType_stillFailsWithoutNamingAReplacement() {
    JsonSyntaxException e = assertThrows(JsonSyntaxException.class, () -> parse("""
      {"type": "mantle:not_a_transfer"}"""));
    assertTrue(e.getMessage().contains("mantle:not_a_transfer"), e.getMessage());
  }


  /* Round trips of the format a 1.21 pack writes */

  @Test
  void emptyItem_roundTrips() {
    assertRoundTrip(new EmptyFluidContainerTransfer(
      Ingredient.of(Items.HONEY_BOTTLE), ItemOutput.fromItem(Items.GLASS_BOTTLE), FluidOutput.fromFluid(Fluids.WATER, BOTTLE)),
      "mantle:empty_item");
  }

  @Test
  void fillItem_roundTrips() {
    assertRoundTrip(new FillFluidContainerTransfer(
      Ingredient.of(Items.GLASS_BOTTLE), ItemOutput.fromItem(Items.HONEY_BOTTLE), FluidIngredient.of(Fluids.WATER, BOTTLE)),
      "mantle:fill_item");
  }

  @Test
  void emptyComponents_roundTrips() {
    assertRoundTrip(new EmptyFluidWithComponentsTransfer(
      Ingredient.of(Items.POTION), ItemOutput.fromItem(Items.GLASS_BOTTLE), FluidOutput.fromFluid(Fluids.WATER, BOTTLE)),
      "mantle:empty_components");
  }

  @Test
  void fillComponents_roundTrips() {
    assertRoundTrip(new FillFluidWithComponentsTransfer(
      Ingredient.of(Items.GLASS_BOTTLE), ItemOutput.fromItem(Items.POTION), FluidIngredient.of(Fluids.WATER, BOTTLE)),
      "mantle:fill_components");
  }

  @Test
  void emptyPotion_roundTrips() {
    assertRoundTrip(new EmptyPotionTransfer(
      Ingredient.of(Items.POTION), ItemOutput.fromItem(Items.GLASS_BOTTLE), BOTTLE),
      "mantle:empty_potion");
  }

  @Test
  void tagOutput_writesComponentsRatherThanNbt() {
    // FluidOutput's tag form carried an "nbt" compound in 1.20; the field is a component patch named "components" now
    JsonObject fluid = serialize(new EmptyFluidContainerTransfer(
      Ingredient.of(Items.HONEY_BOTTLE), ItemOutput.fromItem(Items.GLASS_BOTTLE), FluidOutput.fromTag(FluidTags.WATER, BOTTLE)))
      .getAsJsonObject("fluid");
    assertEquals(FluidTags.WATER.location().toString(), fluid.get("tag").getAsString());
    assertFalse(fluid.has("nbt"), "tag output should no longer write an nbt field");
  }


  /* What the component copying transfers actually move */

  @Test
  void emptyComponents_movesTheItemsComponentsOntoTheFluid() {
    ItemStack potion = setCustomData(new ItemStack(Items.POTION), tagWith("flavour", "grape"));
    FluidTank tank = new FluidTank(1000);

    TransferResult result = new EmptyFluidWithComponentsTransfer(
      Ingredient.of(Items.POTION), ItemOutput.fromItem(Items.GLASS_BOTTLE), FluidOutput.fromFluid(Fluids.WATER, BOTTLE))
      .transfer(potion, FluidStack.EMPTY, tank, TransferDirection.AUTO);

    assertNotNull(result);
    assertEquals(Fluids.WATER, result.fluid().getFluid());
    assertEquals(BOTTLE, result.fluid().getAmount());
    assertEquals(potion.get(DataComponents.CUSTOM_DATA), result.fluid().get(DataComponents.CUSTOM_DATA));
    // and it reached the tank, not just the reported result
    assertEquals(potion.get(DataComponents.CUSTOM_DATA), tank.getFluid().get(DataComponents.CUSTOM_DATA));
  }

  @Test
  void emptyItem_leavesTheFluidBare() {
    ItemStack bottle = setCustomData(new ItemStack(Items.HONEY_BOTTLE), tagWith("flavour", "grape"));

    TransferResult result = new EmptyFluidContainerTransfer(
      Ingredient.of(Items.HONEY_BOTTLE), ItemOutput.fromItem(Items.GLASS_BOTTLE), FluidOutput.fromFluid(Fluids.WATER, BOTTLE))
      .transfer(bottle, FluidStack.EMPTY, new FluidTank(1000), TransferDirection.AUTO);

    assertNotNull(result);
    assertNull(result.fluid().get(DataComponents.CUSTOM_DATA));
  }

  @Test
  void fillComponents_movesTheFluidsComponentsOntoTheItem() {
    FluidStack water = flavouredWater();
    FluidTank tank = new FluidTank(1000);
    tank.fill(water.copy(), FluidAction.EXECUTE);

    TransferResult result = new FillFluidWithComponentsTransfer(
      Ingredient.of(Items.GLASS_BOTTLE), ItemOutput.fromItem(Items.POTION), FluidIngredient.of(Fluids.WATER, BOTTLE))
      .transfer(new ItemStack(Items.GLASS_BOTTLE), water, tank, TransferDirection.AUTO);

    assertNotNull(result);
    assertEquals(Items.POTION, result.stack().getItem());
    assertEquals(water.get(DataComponents.CUSTOM_DATA), result.stack().get(DataComponents.CUSTOM_DATA));
  }

  @Test
  void fillItem_leavesTheItemBare() {
    FluidStack water = flavouredWater();
    FluidTank tank = new FluidTank(1000);
    tank.fill(water.copy(), FluidAction.EXECUTE);

    TransferResult result = new FillFluidContainerTransfer(
      Ingredient.of(Items.GLASS_BOTTLE), ItemOutput.fromItem(Items.POTION), FluidIngredient.of(Fluids.WATER, BOTTLE))
      .transfer(new ItemStack(Items.GLASS_BOTTLE), water, tank, TransferDirection.AUTO);

    assertNotNull(result);
    assertNull(result.stack().get(DataComponents.CUSTOM_DATA));
  }

  @Test
  void fillComponents_keepsResultComponentsTheFluidDoesNotSet() {
    // 1.20 replaced the result's whole tag with the fluid's; applying a patch means a result keeps its own data
    ItemStack named = new ItemStack(Items.POTION);
    named.set(DataComponents.ITEM_NAME, Component.literal("Grape Juice"));

    FluidStack water = flavouredWater();
    FluidTank tank = new FluidTank(1000);
    tank.fill(water.copy(), FluidAction.EXECUTE);

    TransferResult result = new FillFluidWithComponentsTransfer(
      Ingredient.of(Items.GLASS_BOTTLE), ItemOutput.fromStack(named), FluidIngredient.of(Fluids.WATER, BOTTLE))
      .transfer(new ItemStack(Items.GLASS_BOTTLE), water, tank, TransferDirection.AUTO);

    assertNotNull(result);
    assertEquals(named.get(DataComponents.ITEM_NAME), result.stack().get(DataComponents.ITEM_NAME));
    assertEquals(water.get(DataComponents.CUSTOM_DATA), result.stack().get(DataComponents.CUSTOM_DATA));
  }


  /* Conditions moved to NeoForge's namespaced key */

  @Test
  void conditionsUnderTheNeoforgeKey_areApplied() {
    load(Map.of(
      Mantle.getResource("kept"), transferJson(Items.HONEY_BOTTLE, "\"neoforge:conditions\": [{\"type\": \"neoforge:true\"}],"),
      Mantle.getResource("dropped"), transferJson(Items.MILK_BUCKET, "\"neoforge:conditions\": [{\"type\": \"neoforge:false\"}],")));

    assertTrue(FluidContainerTransferManager.INSTANCE.mayHaveTransfer(Items.HONEY_BOTTLE), "a passing condition should load its file");
    assertFalse(FluidContainerTransferManager.INSTANCE.mayHaveTransfer(Items.MILK_BUCKET), "a failing condition should drop its file");
  }

  @Test
  void theBareConditionsKey_isNoLongerRead() {
    // a 1.20 file's "conditions" array is not the condition key any more, so it no longer suppresses anything
    load(Map.of(Mantle.getResource("legacy"), transferJson(Items.MILK_BUCKET, "\"conditions\": [{\"type\": \"neoforge:false\"}],")));
    assertTrue(FluidContainerTransferManager.INSTANCE.mayHaveTransfer(Items.MILK_BUCKET));
  }


  /* Helpers */

  /** Runs the manager's reload over the given files */
  private static void load(Map<ResourceLocation,String> files) {
    Map<ResourceLocation,JsonElement> parsed = new HashMap<>();
    files.forEach((id, json) -> parsed.put(id, JsonParser.parseString(json)));
    FluidContainerTransferManager.INSTANCE.apply(parsed, null, null);
  }

  /** A minimal empty transfer for the given input item, with the given extra keys spliced in */
  private static String transferJson(net.minecraft.world.level.ItemLike input, String extraKeys) {
    return """
      {
        "type": "mantle:empty_item",
        %s
        "input": {"item": "%s"},
        "result": "minecraft:glass_bottle",
        "fluid": {"fluid": "minecraft:water", "amount": 250}
      }""".formatted(extraKeys, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(input.asItem()));
  }

  /** Water carrying a custom data component, standing in for the potion data a real transfer moves */
  private static FluidStack flavouredWater() {
    FluidStack water = new FluidStack(Fluids.WATER, BOTTLE);
    water.set(DataComponents.CUSTOM_DATA, CustomData.of(tagWith("flavour", "grape")));
    return water;
  }

  /** Builds a compound tag with a single string entry */
  private static CompoundTag tagWith(String key, String value) {
    CompoundTag tag = new CompoundTag();
    tag.putString(key, value);
    return tag;
  }
}
