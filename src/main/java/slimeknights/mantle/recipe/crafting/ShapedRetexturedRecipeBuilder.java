package slimeknights.mantle.recipe.crafting;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;

/**
 * Builder for {@link ShapedRetexturedRecipe}.
 * @apiNote  The texture is a symbol from the recipe's key. 1.20's {@code setSource(Ingredient)} and
 *           {@code setSource(TagKey)} are gone with the inline ingredient form they wrote, which had already been
 *           deprecated in favour of the key.
 */
@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "fromShaped")
public class ShapedRetexturedRecipeBuilder {
  private final ShapedRecipeBuilder parent;
  private char textureKey = '\0';
  private boolean matchAll = false;

  /** Sets the texture source to a key from the recipe's key map. Is not validated as that is too much work. */
  public ShapedRetexturedRecipeBuilder setSource(char textureKey) {
    this.textureKey = textureKey;
    return this;
  }

  /**
   * Sets the match first property on the recipe.
   * If set, the recipe uses the first ingredient match for the texture. If unset, all items that match the ingredient must be the same or no texture is applied
   * @return Builder instance
   */
  public ShapedRetexturedRecipeBuilder setMatchAll() {
    this.matchAll = true;
    return this;
  }

  /**
   * Builds the recipe with the default name using the given output
   * @param output  Recipe output
   */
  public void build(RecipeOutput output) {
    this.validate();
    parent.save(wrap(output));
  }

  /**
   * Builds the recipe using the given output
   * @param output    Recipe output
   * @param location  Recipe location
   */
  public void build(RecipeOutput output, ResourceLocation location) {
    this.validate();
    parent.save(wrap(output), location);
  }

  /**
   * Ensures this recipe can be built
   * @throws IllegalStateException If the recipe cannot be built
   */
  private void validate() {
    if (textureKey == '\0') {
      throw new IllegalStateException("No texture defined for texture recipe");
    }
  }

  /** Wraps the output so the shaped recipe the parent builds becomes a retextured recipe */
  private RecipeOutput wrap(RecipeOutput output) {
    return new RecipeOutput() {
      @Override
      public Advancement.Builder advancement() {
        return output.advancement();
      }

      @Override
      public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
        // the texture ingredient is left empty: the built recipe is only ever serialized, and the serialized form
        // names the key symbol, which is resolved against the key map when the recipe is read back
        output.accept(id, new ShapedRetexturedRecipe((ShapedRecipe)recipe, textureKey, Ingredient.EMPTY, matchAll), advancement, conditions);
      }
    };
  }
}
