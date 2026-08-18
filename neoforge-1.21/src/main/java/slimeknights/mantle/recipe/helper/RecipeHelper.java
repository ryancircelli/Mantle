package slimeknights.mantle.recipe.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.IMultiRecipe;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helpers used in creation of recipes.
 * @apiNote  1.21 wraps every recipe in a {@link RecipeHolder} carrying its ID, and the manager deals only in holders.
 *           These helpers keep returning the recipes themselves as they did in 1.20, since that is what a UI or a JEI
 *           plugin wants; only the sorting had to move onto the holder, as {@code Recipe#getId} no longer exists.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecipeHelper {

  /* Recipe manager utils */

  /**
   * Gets a recipe of a specific class type by name from the manager
   * @param manager  Recipe manager
   * @param name     Recipe name
   * @param clazz    Output class
   * @param <C>      Return type
   * @return  Optional of the recipe, or empty if the recipe is missing
   */
  public static <C extends Recipe<?>> Optional<C> getRecipe(RecipeManager manager, ResourceLocation name, Class<C> clazz) {
    return manager.byKey(name).map(RecipeHolder::value).filter(clazz::isInstance).map(clazz::cast);
  }

  /**
   * Gets a list of all recipes from the manager, safely casting to the specified type. Multi Recipes are kept as a single recipe instance
   * @param manager  Recipe manager
   * @param type     Recipe type
   * @param clazz    Preferred recipe class type
   * @param <I>  Recipe input type
   * @param <T>  Recipe class
   * @param <C>  Return type
   * @return  List of recipes from the manager
   */
  public static <I extends RecipeInput, T extends Recipe<I>, C extends T> List<C> getRecipes(RecipeManager manager, RecipeType<T> type, Class<C> clazz) {
    return manager.byType(type).stream()
                  .map(RecipeHolder::value)
                  .filter(clazz::isInstance)
                  .map(clazz::cast)
                  .collect(Collectors.toList());
  }

  /**
   * Gets a list of recipes for display in a UI list, such as UI buttons. Will be sorted to keep the order the same on both sides, and filtered based on the given predicate and class
   * @param manager  Recipe manager
   * @param type     Recipe type
   * @param clazz    Preferred recipe class type
   * @param filter   Filter for which recipes to add to the list
   * @param <I>  Recipe input type
   * @param <T>  Recipe class
   * @param <C>  Return type
   * @return  Recipe list
   */
  public static <I extends RecipeInput, T extends Recipe<I>, C extends T> List<C> getUIRecipes(RecipeManager manager, RecipeType<T> type, Class<C> clazz, Predicate<? super C> filter) {
    return manager.byType(type).stream()
                  .sorted(Comparator.comparing(RecipeHolder::id))
                  .map(RecipeHolder::value)
                  .filter(clazz::isInstance)
                  .map(clazz::cast)
                  .filter(filter)
                  .collect(Collectors.toList());
  }

  /**
   * Gets a list of all recipes from the manager, expanding multi recipes. Intended for use in recipe display such as JEI
   * @param <C>  Return type
   * @param access   Registry access instance
   * @param recipes  Stream of recipe holders
   * @param clazz    Preferred recipe class type
   * @return  List of flattened recipes from the manager
   */
  public static <C> List<C> getJEIRecipes(RegistryAccess access, Stream<? extends RecipeHolder<?>> recipes, Class<C> clazz) {
    return recipes
        .sorted((r1, r2) -> {
          // if one is multi, and the other not, the multi recipe is larger
          boolean m1 = r1.value() instanceof IMultiRecipe<?>;
          boolean m2 = r2.value() instanceof IMultiRecipe<?>;
          if (m1 && !m2) return 1;
          if (!m1 && m2) return -1;
          // fall back to recipe ID
          return r1.id().compareTo(r2.id());
        })
        .flatMap((holder) -> {
          // if its a multi recipe, extract child recipes and stream those
          if (holder.value() instanceof IMultiRecipe<?> multi) {
            // most multiregistries iterate some external registry to list their contents
            // sometimes people do dumb things and register broken objects, so best to avoid breaking the rest of the JEI plugin
            try {
              return multi.getRecipes(access).stream();
            } catch (Exception e) {
              Mantle.logger.error("Failed to fetch JEI recipes for multi recipe {} ({})", holder.id(), holder.value(), e);
              return Stream.empty();
            }
          }
          return Stream.of(holder.value());
        })
        .filter(clazz::isInstance)
        .map(clazz::cast)
        .collect(Collectors.toList());
  }

  /**
   * Gets a list of all recipes from the manager, expanding multi recipes. Intended for use in recipe display such as JEI
   * @param <C>  Return type
   * @param access   Registry access instance
   * @param manager  Recipe manager
   * @param type     Recipe type
   * @param clazz    Preferred recipe class type
   * @return  List of flattened recipes from the manager
   */
  public static <I extends RecipeInput, T extends Recipe<I>, C> List<C> getJEIRecipes(RegistryAccess access, RecipeManager manager, RecipeType<T> type, Class<C> clazz) {
    return getJEIRecipes(access, manager.byType(type).stream(), clazz);
  }
}
