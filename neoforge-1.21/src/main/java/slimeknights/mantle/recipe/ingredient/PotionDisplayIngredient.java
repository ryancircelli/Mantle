package slimeknights.mantle.recipe.ingredient;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.MantleIngredients;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

/** Ingredient that shows all potion variants on the displayed item list */
public class PotionDisplayIngredient extends ItemIngredient {
  public static final RecordLoadable<PotionDisplayIngredient> LOADABLE = RecordLoadable.create(ITEMS_FIELD, TAG_FIELD, PotionDisplayIngredient::new);

  protected PotionDisplayIngredient(List<Item> items, @Nullable TagKey<Item> tag) {
    super(items, tag);
  }

  /** Creates a ingredient matching a list of items */
  public static PotionDisplayIngredient of(List<ItemLike> items) {
    return new PotionDisplayIngredient(toItem(items), null);
  }

  /** Creates a ingredient matching a list of items */
  public static PotionDisplayIngredient of(ItemLike... items) {
    return of(List.of(items));
  }

  /** Creates a ingredient matching a tag */
  public static PotionDisplayIngredient of(TagKey<Item> tag) {
    return new PotionDisplayIngredient(List.of(), tag);
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public Stream<ItemStack> getItems() {
    // show every potion on every matched stack, as the ingredient itself does not care which potion it is
    return matchingItems().flatMap(item -> BuiltInRegistries.POTION.holders().map(potion -> PotionContents.createItemStack(item, potion)));
  }

  @Override
  public IngredientType<?> getType() {
    return MantleIngredients.POTION_DISPLAY.get();
  }
}
