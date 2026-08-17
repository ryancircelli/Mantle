package slimeknights.mantle.recipe.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Common logic to create a recipe builder class.
 * <p>
 * 1.21 deleted {@code FinishedRecipe}: a builder constructs the real {@link Recipe} and hands it to a
 * {@link RecipeOutput} along with its advancement, and the recipe serializes itself through its own serializer's codec.
 * The builder therefore no longer needs to be told which loadable to write with.
 * @param <T>  Builder class
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class AbstractRecipeBuilder<T extends AbstractRecipeBuilder<T>> {
  /**
   * Criteria for this recipe's advancement.
   * @apiNote  {@link Advancement.Builder}'s own criteria map is an {@code ImmutableMap.Builder}, which has no
   *           overwrite and throws on a duplicate key, so the builder keeps its own map and only assembles the
   *           advancement at save.
   */
  protected final Map<String,Criterion<?>> criteria = new LinkedHashMap<>();
  /** Group for this recipe */
  @Nonnull
  protected String group = "";

  /**
   * Adds a criteria to the recipe
   * @param name      Criteria name
   * @param criterion Criteria instance
   * @return  Builder
   */
  @SuppressWarnings("unchecked")
  public T unlockedBy(String name, Criterion<?> criterion) {
    this.criteria.put(name, criterion);
    return (T)this;
  }

  /**
   * Sets the group for this recipe
   * @param group  Recipe group
   * @return  Builder
   */
  @SuppressWarnings("unchecked")
  public T group(String group) {
    this.group = group;
    return (T)this;
  }

  /**
   * Sets the group for this recipe
   * @param group  Recipe resource location group
   * @return  Builder
   */
  public T group(ResourceLocation group) {
    // if minecraft, no namepsace. Groups are technically not namespaced so this is for consistency with vanilla
    if ("minecraft".equals(group.getNamespace())) {
      return group(group.getPath());
    }
    return group(group.toString());
  }

  /**
   * Base logic for advancement building
   * @param output  Recipe output, which supplies the advancement parented to the recipe root
   * @param id      Recipe ID
   * @param folder  Group folder for saving recipes. Vanilla typically uses item groups, but for mods might as well base on the recipe
   * @return Advancement instance
   */
  private AdvancementHolder buildAdvancementInternal(RecipeOutput output, ResourceLocation id, String folder) {
    Advancement.Builder builder = output.advancement()
                                        .rewards(AdvancementRewards.Builder.recipe(id))
                                        .requirements(AdvancementRequirements.Strategy.OR);
    // has_the_recipe goes in last so it replaces a criterion of the same name instead of erroring, as it did in 1.20
    criteria.forEach((name, criterion) -> {
      if (!"has_the_recipe".equals(name)) {
        builder.addCriterion(name, criterion);
      }
    });
    builder.addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id));
    return builder.build(id.withPrefix("recipes/" + folder + "/"));
  }

  /**
   * Builds and validates the advancement, intended to be called in {@link #save(RecipeOutput, ResourceLocation)}
   * @param output  Recipe output
   * @param id      Recipe ID
   * @param folder  Group folder for saving recipes. Vanilla typically uses item groups, but for mods might as well base on the recipe
   * @return Advancement instance
   */
  protected AdvancementHolder buildAdvancement(RecipeOutput output, ResourceLocation id, String folder) {
    if (this.criteria.isEmpty()) {
      throw new IllegalStateException("No way of obtaining recipe " + id);
    }
    return buildAdvancementInternal(output, id, folder);
  }

  /**
   * Builds an optional advancement, intended to be called in {@link #save(RecipeOutput, ResourceLocation)}
   * @param output  Recipe output
   * @param id      Recipe ID
   * @param folder  Group folder for saving recipes. Vanilla typically uses item groups, but for mods might as well base on the recipe
   * @return Advancement instance, or null if the advancement was not defined
   */
  @SuppressWarnings("SameParameterValue")  // API
  @Nullable
  protected AdvancementHolder buildOptionalAdvancement(RecipeOutput output, ResourceLocation id, String folder) {
    if (this.criteria.isEmpty()) {
      return null;
    }
    return buildAdvancementInternal(output, id, folder);
  }

  /**
   * Writes the recipe with an optional advancement, which is what nearly every subclass wants at the end of
   * {@link #save(RecipeOutput, ResourceLocation)}.
   * @param output  Recipe output
   * @param id      Recipe ID
   * @param recipe  Recipe to write, which serializes itself through its own serializer
   * @param folder  Group folder for the advancement
   */
  protected void save(RecipeOutput output, ResourceLocation id, Recipe<?> recipe, String folder) {
    output.accept(id, recipe, buildOptionalAdvancement(output, id, folder));
  }

  /**
   * Builds the recipe with a default recipe ID, typically based on the output
   * @param output  Recipe output
   */
  public abstract void save(RecipeOutput output);

  /**
   * Builds the recipe
   * @param output  Recipe output
   * @param id      Recipe ID
   */
  public abstract void save(RecipeOutput output, ResourceLocation id);
}
