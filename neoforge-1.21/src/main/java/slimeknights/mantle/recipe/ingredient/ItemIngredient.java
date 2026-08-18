package slimeknights.mantle.recipe.ingredient;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.util.RegistryHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Abstract ingredient that matches a list of items or a tag, mirroring the vanilla syntax.
 * @apiNote  1.20 built this out of {@link net.minecraft.world.item.crafting.Ingredient.Value} instances and had to sync
 *           the tag to the client as a resolved item list, because Forge's ingredient network format could not carry a
 *           tag. 1.21 syncs a non-simple custom ingredient through its own codec, and tags are on the client anyway, so
 *           both the tag and the item list go over the wire as themselves.
 */
public abstract class ItemIngredient implements ICustomIngredient {
  /** Field for the list of items */
  protected static final LoadableField<List<Item>,ItemIngredient> ITEMS_FIELD = itemList().defaultField("item", List.of(), i -> i.items);
  /** Field for the item tag */
  protected static final LoadableField<TagKey<Item>,ItemIngredient> TAG_FIELD = Loadables.ITEM_TAG.nullableField("tag", i -> i.tag);

  /** List loadable, extracted as the field initializer order matters for a static in an abstract class */
  private static Loadable<List<Item>> itemList() {
    return Loadables.ITEM.list(ArrayLoadable.COMPACT_OR_EMPTY);
  }

  protected final List<Item> items;
  @Nullable
  protected final TagKey<Item> tag;

  protected ItemIngredient(List<Item> items, @Nullable TagKey<Item> tag) {
    this.items = items;
    this.tag = tag;
  }

  /** Maps the list to a list of items */
  protected static List<Item> toItem(List<ItemLike> items) {
    return items.stream().map(ItemLike::asItem).toList();
  }

  @Override
  public boolean test(ItemStack stack) {
    // for tag checks it's way easier to just check directly
    // also ensures we never match empty just because our lists are empty
    return items.contains(stack.getItem()) || tag != null && stack.is(tag);
  }

  /** Gets every item this ingredient matches, expanding the tag */
  protected Stream<Item> matchingItems() {
    if (tag == null) {
      return items.stream();
    }
    return Stream.concat(items.stream(), RegistryHelper.getTagValueStream(BuiltInRegistries.ITEM, tag));
  }

  @Override
  public Stream<ItemStack> getItems() {
    return matchingItems().map(ItemStack::new);
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other != null && getClass() == other.getClass()
           && items.equals(((ItemIngredient)other).items)
           && Objects.equals(tag, ((ItemIngredient)other).tag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass(), items, tag);
  }
}
