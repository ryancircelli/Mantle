package slimeknights.mantle.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.MantleIngredients;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Simple ingredient checking for an item with a specific potion.
 * @apiNote  {@code PotionUtils} is gone; a stack's potion is the {@link DataComponents#POTION_CONTENTS} component and a
 *           potion is a {@link Holder}. There is no longer an empty potion, so the field is required rather than
 *           defaulting to {@code Potions.EMPTY} as it did in 1.20.
 */
public class PotionIngredient extends ItemIngredient {
  public static final RecordLoadable<PotionIngredient> LOADABLE = RecordLoadable.create(
    ITEMS_FIELD, TAG_FIELD,
    Loadables.POTION.flatXmap(BuiltInRegistries.POTION::wrapAsHolder, Holder::value).requiredField("potion", i -> i.potion),
    PotionIngredient::new);

  private final Holder<Potion> potion;
  protected PotionIngredient(List<Item> items, @Nullable TagKey<Item> itemTag, Holder<Potion> potion) {
    super(items, itemTag);
    this.potion = potion;
  }

  /** Creates a potion ingredient matching a list of items */
  public static PotionIngredient of(Holder<Potion> potion, List<ItemLike> items) {
    return new PotionIngredient(toItem(items), null, potion);
  }

  /** Creates a potion ingredient matching a list of items */
  public static PotionIngredient of(Holder<Potion> potion, ItemLike... items) {
    return of(potion, Arrays.asList(items));
  }

  /** Creates a potion ingredient matching a tag */
  public static PotionIngredient of(Holder<Potion> potion, TagKey<Item> tag) {
    return new PotionIngredient(List.of(), tag, potion);
  }

  @Override
  public boolean test(ItemStack stack) {
    // stack must match, any item must match, and potion must match
    return super.test(stack) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(potion);
  }

  @Override
  public Stream<ItemStack> getItems() {
    return matchingItems().map(item -> PotionContents.createItemStack(item, potion));
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return MantleIngredients.POTION.get();
  }

  @Override
  public boolean equals(Object other) {
    return super.equals(other) && potion.equals(((PotionIngredient)other).potion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), potion);
  }
}
