package slimeknights.mantle.recipe.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

/**
 * Builds an {@link IngredientType} from a loadable, replacing 1.20's {@code LoadableIngredientSerializer}.
 * <p>
 * Forge's {@code IIngredientSerializer} was three methods over gson and a buffer; NeoForge's type is a
 * {@link com.mojang.serialization.MapCodec} and a {@link net.minecraft.network.codec.StreamCodec}, both of which a
 * {@link RecordLoadable} already is.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoadableIngredientType {
  /**
   * Creates an ingredient type reading and writing through the given loadable.
   * @param loadable  Loadable for the ingredient
   * @param <T>  Ingredient class
   * @return  Ingredient type, to be registered to {@link net.neoforged.neoforge.registries.NeoForgeRegistries#INGREDIENT_TYPES}
   */
  public static <T extends ICustomIngredient> IngredientType<T> of(RecordLoadable<T> loadable) {
    return new IngredientType<>(loadable.mapCodec(), loadable);
  }
}
