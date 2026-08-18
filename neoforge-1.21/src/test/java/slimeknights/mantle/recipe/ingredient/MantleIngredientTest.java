package slimeknights.mantle.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.common.SizedIngredientLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.data.ItemNameIngredient;
import slimeknights.mantle.test.LoadableTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the ingredients which became {@link net.neoforged.neoforge.common.crafting.ICustomIngredient}s, plus the
 * loadable view of NeoForge's {@link SizedIngredient} which replaced Mantle's class of that name.
 * <p>
 * Each is driven through its own loadable rather than through {@code Ingredient.CODEC}, as that codec dispatches on the
 * registry entry a unit test cannot supply; {@link slimeknights.mantle.recipe.MantleIngredients} is what wires them up.
 */
class MantleIngredientTest extends LoadableTest {
  /** Asserts the loadable writes the given JSON and reads it back to the same JSON */
  private static <T> void assertJsonForm(RecordLoadable<T> loadable, T value, String json) {
    JsonElement expected = JsonParser.parseString(json);
    assertThat(loadable.serialize(value)).as("written form").isEqualTo(expected);
    assertThat(loadable.serialize(loadable.convert(expected, "test"))).as("round trip").isEqualTo(expected);
  }


  /* Sized ingredient */

  @Test
  void sizedIngredient_flatFormWritesTheIngredientBesideItsCount() {
    // NeoForge's own flat spelling, which is the reason Mantle's amount_needed field is gone
    assertJsonForm(SizedIngredientLoadable.FLAT, new SizedIngredient(Ingredient.of(Items.APPLE), 3),
                   "{\"item\":\"minecraft:apple\",\"count\":3}");
  }

  @Test
  void sizedIngredient_flatFormAlwaysWritesTheCount() {
    assertJsonForm(SizedIngredientLoadable.FLAT, new SizedIngredient(Ingredient.of(ItemTags.PLANKS), 1),
                   "{\"tag\":\"minecraft:planks\",\"count\":1}");
  }

  @Test
  void sizedIngredient_nestedFormWritesTheIngredientUnderItsOwnKey() {
    assertJsonForm(SizedIngredientLoadable.NESTED, new SizedIngredient(Ingredient.of(Items.APPLE), 2),
                   "{\"ingredient\":{\"item\":\"minecraft:apple\"},\"count\":2}");
  }

  @Test
  void sizedIngredient_survivesTheNetwork() {
    SizedIngredient value = new SizedIngredient(Ingredient.of(Items.APPLE), 4);
    RegistryFriendlyByteBuf buffer = buffer();
    SizedIngredientLoadable.FLAT.encode(buffer, value);
    SizedIngredient read = SizedIngredientLoadable.FLAT.decode(buffer);
    assertThat(buffer.readableBytes()).isZero();
    assertThat(read.count()).isEqualTo(4);
    assertThat(read.test(new ItemStack(Items.APPLE, 4))).isTrue();
    assertThat(read.test(new ItemStack(Items.APPLE, 3))).isFalse();
  }


  /* Fluid container */

  @Test
  void fluidContainer_writesItsFluidUnderAKeyOfItsOwn() {
    assertJsonForm(FluidContainerIngredient.LOADABLE,
                   FluidContainerIngredient.fromIngredient(FluidIngredient.of(Fluids.WATER, 1000), Ingredient.of(Items.WATER_BUCKET)),
                   "{\"fluid\":{\"fluid\":\"minecraft:water\",\"amount\":1000},\"display\":{\"item\":\"minecraft:water_bucket\"}}");
  }

  @Test
  void fluidContainer_omitsAnAbsentDisplayAndThenShowsNothing() {
    FluidContainerIngredient ingredient = FluidContainerIngredient.fromIngredient(FluidIngredient.of(Fluids.WATER, 1000));
    assertJsonForm(FluidContainerIngredient.LOADABLE, ingredient, "{\"fluid\":{\"fluid\":\"minecraft:water\",\"amount\":1000}}");
    assertThat(ingredient.getItems()).isEmpty();
    // never simple: the whole point is that it inspects the stack's fluid handler
    assertThat(ingredient.isSimple()).isFalse();
  }

  @Test
  void fluidContainer_displayStacksComeFromTheDisplayIngredient() {
    FluidContainerIngredient ingredient = FluidContainerIngredient.fromIngredient(FluidIngredient.of(Fluids.WATER, 1000), Ingredient.of(Items.WATER_BUCKET));
    assertThat(ingredient.getItems().map(ItemStack::getItem)).containsExactly(Items.WATER_BUCKET);
  }


  /* Item ingredients */

  @Test
  void itemName_writesASingleNameCompactly() {
    assertJsonForm(ItemNameIngredient.LOADABLE, ItemNameIngredient.from(ResourceLocation.fromNamespaceAndPath("othermod", "widget")),
                   "{\"item\":\"othermod:widget\"}");
  }

  @Test
  void itemName_matchesNothingWhenTheItemIsAbsent() {
    ItemNameIngredient ingredient = ItemNameIngredient.from(ResourceLocation.fromNamespaceAndPath("othermod", "widget"));
    assertThat(ingredient.getItems()).isEmpty();
    assertThat(ingredient.test(new ItemStack(Items.APPLE))).isFalse();
  }

  @Test
  void itemName_matchesAPresentItem() {
    ItemNameIngredient ingredient = ItemNameIngredient.from(ResourceLocation.withDefaultNamespace("apple"));
    assertThat(ingredient.getItems().map(ItemStack::getItem)).containsExactly(Items.APPLE);
    assertThat(ingredient.test(new ItemStack(Items.APPLE))).isTrue();
    assertThat(ingredient.test(new ItemStack(Items.STICK))).isFalse();
  }

  @Test
  void itemName_componentsMakeTheMatchStrict() {
    DataComponentPatch patch = DataComponentPatch.builder().set(DataComponents.DAMAGE, 5).build();
    ItemNameIngredient ingredient = ItemNameIngredient.from(ResourceLocation.withDefaultNamespace("iron_pickaxe"), patch);
    assertThat(ingredient.isSimple()).isFalse();
    assertThat(ingredient.test(new ItemStack(Items.IRON_PICKAXE))).isFalse();
    ItemStack damaged = new ItemStack(Items.IRON_PICKAXE);
    damaged.applyComponents(patch);
    assertThat(ingredient.test(damaged)).isTrue();
  }

  @Test
  void potion_matchesOnlyTheNamedPotion() {
    PotionIngredient ingredient = PotionIngredient.of(Potions.WATER, Items.POTION);
    assertThat(ingredient.isSimple()).isFalse();
    ItemStack water = ingredient.getItems().toList().get(0);
    assertThat(ingredient.test(water)).isTrue();
    assertThat(ingredient.test(new ItemStack(Items.POTION))).isFalse();
    assertThat(ingredient.test(new ItemStack(Items.APPLE))).isFalse();
  }

  @Test
  void potion_writesItsItemsAndPotion() {
    assertJsonForm(PotionIngredient.LOADABLE, PotionIngredient.of(Potions.WATER, Items.POTION),
                   "{\"item\":\"minecraft:potion\",\"potion\":\"minecraft:water\"}");
  }

  @Test
  void potionDisplay_showsEveryPotionButIgnoresThemWhenMatching() {
    PotionDisplayIngredient ingredient = PotionDisplayIngredient.of(Items.POTION);
    assertThat(ingredient.isSimple()).isTrue();
    // matching is item only, so a plain potion stack matches
    assertThat(ingredient.test(new ItemStack(Items.POTION))).isTrue();
    assertThat(ingredient.test(new ItemStack(Items.APPLE))).isFalse();
    // display is one stack per potion, which is the reason the class exists
    assertThat(ingredient.getItems().count()).isGreaterThan(1);
  }

  @Test
  void itemIngredient_carriesItsTagRatherThanResolvingItForTheNetwork() {
    // 1.20 wrote the tag out as the items it matched, as Forge's ingredient network format could not carry a tag;
    // a non simple 1.21 ingredient syncs through this same loadable, so the tag itself crosses
    assertJsonForm(PotionDisplayIngredient.LOADABLE, PotionDisplayIngredient.of(ItemTags.PLANKS), "{\"tag\":\"minecraft:planks\"}");
    RegistryFriendlyByteBuf buffer = buffer();
    PotionDisplayIngredient.LOADABLE.encode(buffer, PotionDisplayIngredient.of(ItemTags.PLANKS));
    assertThat(PotionDisplayIngredient.LOADABLE.decode(buffer).getItems()).isNotNull();
    assertThat(buffer.readableBytes()).isZero();
  }

  @Test
  void itemIngredient_anEmptyIngredientWritesAnEmptyObject() {
    assertJsonForm(PotionDisplayIngredient.LOADABLE, PotionDisplayIngredient.of(List.of()), "{}");
  }
}
