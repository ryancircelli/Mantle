package slimeknights.mantle.plugin.jei;

import com.google.common.collect.Streams;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipe;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * JEI crafting extension to properly show, animate, and focus {@link ShapedRetexturedRecipe} instances
 * @apiNote  JEI 19's {@link ICraftingCategoryExtension} is registered once per recipe class rather than once per recipe
 *           instance - {@code addCategoryExtension(Class, Function<R, extension>)} became
 *           {@code addExtension(Class, ICraftingCategoryExtension<R>)}, and every method now takes the
 *           {@link RecipeHolder} it applies to as a parameter instead of reading fields a constructor captured. This
 *           class is therefore a stateless singleton now; everything the 1.20 constructor precomputed once per recipe
 *           (the display outputs, the texture-matching input slots) is recomputed per call in {@link #setRecipe} instead.
 */
public enum RetexturableRecipeExtension implements ICraftingCategoryExtension<ShapedRetexturedRecipe> {
  INSTANCE;

  /** Checks if two ingredients match based on their display items */
  private static boolean ingredientsMatch(Ingredient left, Ingredient right) {
    ItemStack[] leftStacks = left.getItems();
    ItemStack[] rightStacks = right.getItems();
    if (leftStacks.length != rightStacks.length) {
      return false;
    }
    for (int i = 0; i < leftStacks.length; i++) {
      if (!ItemStack.isSameItemSameComponents(leftStacks[i], rightStacks[i])) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int getWidth(RecipeHolder<ShapedRetexturedRecipe> recipeHolder) {
    return recipeHolder.value().getWidth();
  }

  @Override
  public int getHeight(RecipeHolder<ShapedRetexturedRecipe> recipeHolder) {
    return recipeHolder.value().getHeight();
  }

  @Override
  public void setRecipe(RecipeHolder<ShapedRetexturedRecipe> recipeHolder, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
    ShapedRetexturedRecipe recipe = recipeHolder.value();
    RegistryAccess access = Objects.requireNonNull(SafeClientAccess.getRegistryAccess());

    // set the output to display all variants from the texture ingredient
    Ingredient texture = recipe.getTexture();
    // fetch all stacks from the ingredient, note any variants that are not blocks will get a blank look
    List<ItemStack> displayOutputs = Arrays.stream(texture.getItems())
                                           .map(stack -> recipe.getResultItem(stack.getItem(), access))
                                           .toList();
    // empty display means the tag found nothing, so just use the original output
    if (displayOutputs.isEmpty()) {
      displayOutputs = List.of(recipe.getResultItem(access));
    }

    // find out which inputs match the texture, we will need to use those for the focus link
    List<Ingredient> inputs = recipe.getIngredients();
    int[] textureSlots = IntStream.range(0, inputs.size()).filter(i -> ingredientsMatch(texture, inputs.get(i))).toArray();

    // we need the blank version for the sake of recipe lookup due to the subtype interpreter making it not the same
    builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(recipe.getResultItem(access));

    // add the itemstacks to the grid
    List<List<ItemStack>> inputStacks = inputs.stream().map(ingredient -> List.of(ingredient.getItems())).toList();
    int width = recipe.getWidth();
    int height = recipe.getHeight();
    List<IRecipeSlotBuilder> slots = craftingGridHelper.createAndSetInputs(builder, inputStacks, width, height);
    IRecipeSlotBuilder output = craftingGridHelper.createAndSetOutputs(builder, displayOutputs);
    if (slots.size() != 9) {
      Mantle.logger.error("Failed to create focus link for {} as the layout {} is not 3x3", recipeHolder.id(), builder.getClass().getName());
    } else {
      // link the output to all inputs that match the texture
      builder.createFocusLink(Streams.concat(Stream.<IIngredientAcceptor<?>>of(output), Arrays.stream(textureSlots).mapToObj(i -> slots.get(MantleJEIConstants.getCraftingIndex(i, width, height)))).toArray(IIngredientAcceptor<?>[]::new));
    }
  }
}
