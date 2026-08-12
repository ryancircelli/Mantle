package slimeknights.mantle.data.loadable.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

/**
 * Loadables for NeoForge's {@link SizedIngredient}, which replaced Mantle's own class of that name in 1.21.
 * <p>
 * Both forms read and write exactly what {@link SizedIngredient#FLAT_CODEC} and {@link SizedIngredient#NESTED_CODEC}
 * do, so a Mantle recipe's sized ingredient is the same JSON every other 1.21 mod writes. They exist because the
 * loadable API composes in ways a codec does not: {@code list()}, {@code defaultField()} and friends are what a
 * {@link RecordLoadable} based recipe is built out of.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SizedIngredientLoadable {
  /** Count field, always written so a reader never has to know the default */
  private static final IntLoadable COUNT = IntLoadable.FROM_ONE;

  /**
   * Form writing the ingredient into the parent object alongside the count, as
   * {@code {"item": "minecraft:apple", "count": 3}}.
   * <p>
   * An ingredient which cannot be written as an object, notably the array form, falls back to the nested key.
   */
  public static final RecordLoadable<SizedIngredient> FLAT = RecordLoadable.create(
    IngredientLoadable.DISALLOW_EMPTY.tryDirectField("ingredient", SizedIngredient::ingredient, "count"),
    COUNT.defaultField("count", 1, true, SizedIngredient::count),
    SizedIngredient::new);

  /**
   * Form writing the ingredient under its own key, as
   * {@code {"ingredient": {"item": "minecraft:apple"}, "count": 3}}.
   */
  public static final RecordLoadable<SizedIngredient> NESTED = RecordLoadable.create(
    IngredientLoadable.DISALLOW_EMPTY.requiredField("ingredient", SizedIngredient::ingredient),
    COUNT.defaultField("count", 1, true, SizedIngredient::count),
    SizedIngredient::new);
}
