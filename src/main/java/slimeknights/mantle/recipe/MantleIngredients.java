package slimeknights.mantle.recipe;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.data.ItemNameIngredient;
import slimeknights.mantle.recipe.helper.LoadableIngredientType;
import slimeknights.mantle.recipe.ingredient.FluidContainerIngredient;
import slimeknights.mantle.recipe.ingredient.PotionDisplayIngredient;
import slimeknights.mantle.recipe.ingredient.PotionIngredient;

/**
 * Registration for Mantle's custom ingredient types.
 * @apiNote  Forge kept ingredient serializers in the recipe serializer registry event and had the ingredient hand back
 *           its own serializer; NeoForge gives them a registry of their own, so the ingredient names a type it does not
 *           own and this class is where those types live.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MantleIngredients {
  private static final DeferredRegister<IngredientType<?>> INGREDIENTS = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, Mantle.modId);

  /** Registers this to the bus */
  public static void init(IEventBus bus) {
    INGREDIENTS.register(bus);
  }

  /** Ingredient matching an item containing a given fluid */
  public static final DeferredHolder<IngredientType<?>,IngredientType<FluidContainerIngredient>> FLUID_CONTAINER =
    INGREDIENTS.register("fluid_container", () -> LoadableIngredientType.of(FluidContainerIngredient.LOADABLE));
  /** Ingredient matching an item by registry name, for a mod which may not be present */
  public static final DeferredHolder<IngredientType<?>,IngredientType<ItemNameIngredient>> ITEM_NAME =
    INGREDIENTS.register("item_name", () -> LoadableIngredientType.of(ItemNameIngredient.LOADABLE));
  /** Ingredient matching an item with a specific potion */
  public static final DeferredHolder<IngredientType<?>,IngredientType<PotionIngredient>> POTION =
    INGREDIENTS.register("potion", () -> LoadableIngredientType.of(PotionIngredient.LOADABLE));
  /** Ingredient matching an item ignoring its potion, but displaying every potion variant */
  public static final DeferredHolder<IngredientType<?>,IngredientType<PotionDisplayIngredient>> POTION_DISPLAY =
    INGREDIENTS.register("potion_display", () -> LoadableIngredientType.of(PotionDisplayIngredient.LOADABLE));
}
