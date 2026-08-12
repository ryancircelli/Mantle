package slimeknights.mantle.recipe.crafting;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builder for a shaped recipe with fallbacks.
 * @apiNote  1.20 wrapped the {@code FinishedRecipe} the parent builder produced. 1.21 has no such object, so the wrap
 *           happens one level out: the parent saves into a {@link RecipeOutput} which swaps the recipe on its way past.
 */
@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "fallback")
public class ShapedFallbackRecipeBuilder {
  private final ShapedRecipeBuilder base;
  private final List<ResourceLocation> alternatives = new ArrayList<>();

  /**
   * Adds a single alternative to this recipe. Any matching alternative causes this recipe to fail
   * @param location  Alternative
   * @return  Builder instance
   */
  public ShapedFallbackRecipeBuilder addAlternative(ResourceLocation location) {
    this.alternatives.add(location);
    return this;
  }

  /**
   * Adds a list of alternatives to this recipe. Any matching alternative causes this recipe to fail
   * @param locations  Alternative list
   * @return  Builder instance
   */
  public ShapedFallbackRecipeBuilder addAlternatives(Collection<ResourceLocation> locations) {
    this.alternatives.addAll(locations);
    return this;
  }

  /**
   * Builds the recipe using the output as the name
   * @param output  Recipe output
   */
  public void build(RecipeOutput output) {
    base.save(wrap(output));
  }

  /**
   * Builds the recipe using the given ID
   * @param output  Recipe output
   * @param id      Recipe ID
   */
  public void build(RecipeOutput output, ResourceLocation id) {
    base.save(wrap(output), id);
  }

  /** Wraps the output so the shaped recipe the parent builds becomes a fallback recipe */
  private RecipeOutput wrap(RecipeOutput output) {
    List<ResourceLocation> alternatives = List.copyOf(this.alternatives);
    return new RecipeOutput() {
      @Override
      public Advancement.Builder advancement() {
        return output.advancement();
      }

      @Override
      public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
        output.accept(id, new ShapedFallbackRecipe((ShapedRecipe)recipe, alternatives), advancement, conditions);
      }
    };
  }
}
