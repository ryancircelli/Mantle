package slimeknights.mantle.recipe.data;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.common.DataComponentsLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.MantleIngredients;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Ingredient matching an item by registry name, for a recipe naming an item from a mod which may not be present.
 * <p>
 * 1.20 wrote itself as plain vanilla ingredient JSON and threw from every runtime method, since a datagen provider
 * could hand gson any name it liked. 1.21 serializes ingredients through {@link net.minecraft.world.item.crafting.Ingredient#CODEC},
 * which resolves the item as it writes, so faking the vanilla form is no longer possible. It is a real ingredient
 * instead, resolving the name when the recipe is used and matching nothing if the item is absent, which is what the
 * mod-loaded condition guarding such a recipe already assumed.
 * <p>
 * The {@code components} field replaces 1.20's separate {@code NBTNameIngredient}, which built on Forge's
 * {@code StrictNBTIngredient}. A stack matches only if its component patch equals the one given, so an empty patch
 * means "this item with no data".
 */
public class ItemNameIngredient implements ICustomIngredient {
  public static final RecordLoadable<ItemNameIngredient> LOADABLE = RecordLoadable.create(
    Loadables.RESOURCE_LOCATION.list(ArrayLoadable.COMPACT).requiredField("item", i -> i.names),
    DataComponentsLoadable.INSTANCE.defaultField("components", DataComponentPatch.EMPTY, i -> i.components),
    ItemNameIngredient::new);

  private final List<ResourceLocation> names;
  private final DataComponentPatch components;

  protected ItemNameIngredient(List<ResourceLocation> names, DataComponentPatch components) {
    this.names = names;
    this.components = components;
  }

  /** Creates a new ingredient from a list of names */
  public static ItemNameIngredient from(List<ResourceLocation> names) {
    return new ItemNameIngredient(names, DataComponentPatch.EMPTY);
  }

  /** Creates a new ingredient from a list of names */
  public static ItemNameIngredient from(ResourceLocation... names) {
    return from(Arrays.asList(names));
  }

  /** Creates a new ingredient from a name and the components the stack must carry */
  public static ItemNameIngredient from(ResourceLocation name, DataComponentPatch components) {
    return new ItemNameIngredient(List.of(name), components);
  }

  /** Resolves the names against the item registry, dropping any which are not present */
  private Stream<Item> items() {
    return names.stream().flatMap(name -> BuiltInRegistries.ITEM.getOptional(name).stream());
  }

  @Override
  public boolean test(ItemStack stack) {
    return names.contains(BuiltInRegistries.ITEM.getKey(stack.getItem())) && components.equals(stack.getComponentsPatch());
  }

  @Override
  public Stream<ItemStack> getItems() {
    return items().map(item -> {
      ItemStack stack = new ItemStack(item);
      stack.applyComponents(components);
      return stack;
    });
  }

  @Override
  public boolean isSimple() {
    return components.isEmpty();
  }

  @Override
  public IngredientType<?> getType() {
    return MantleIngredients.ITEM_NAME.get();
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof ItemNameIngredient ingredient && names.equals(ingredient.names) && components.equals(ingredient.components);
  }

  @Override
  public int hashCode() {
    return Objects.hash(names, components);
  }
}
