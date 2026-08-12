package slimeknights.mantle.recipe.ingredient;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.MantleIngredients;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.mantle.util.CapabilityHelper;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Ingredient that matches a container of fluid.
 * <p>
 * The 1.20 class extended Forge's {@code AbstractIngredient} and carried its own {@code IIngredientSerializer}; both
 * are gone. It is an {@link ICustomIngredient} now, reached through {@link #toVanilla()} wherever an
 * {@link Ingredient} is wanted.
 */
@SuppressWarnings("unused")  // API
public class FluidContainerIngredient implements ICustomIngredient {
  /**
   * Loadable for this ingredient.
   * @apiNote  The fluid ingredient sits under its own key rather than being flattened into the parent as it was in
   *           1.20. The flat form needed a hand written "is this a nested object or a fluid name" branch to be readable
   *           at all, and it now shares the object with the {@code type} key every custom ingredient carries.
   */
  public static final RecordLoadable<FluidContainerIngredient> LOADABLE = RecordLoadable.create(
    FluidIngredient.LOADABLE.requiredField("fluid", i -> i.fluidIngredient),
    IngredientLoadable.DISALLOW_EMPTY.nullableField("display", i -> i.display),
    FluidContainerIngredient::new);

  /** Ingredient to use for matching */
  private final FluidIngredient fluidIngredient;
  /** Internal ingredient to display the ingredient in recipe viewers */
  @Nullable
  private final Ingredient display;

  protected FluidContainerIngredient(FluidIngredient fluidIngredient, @Nullable Ingredient display) {
    this.fluidIngredient = fluidIngredient;
    this.display = display;
  }

  /** Creates an instance from a fluid ingredient with a display container */
  public static FluidContainerIngredient fromIngredient(FluidIngredient ingredient, Ingredient display) {
    return new FluidContainerIngredient(ingredient, display);
  }

  /** Creates an instance from a fluid ingredient with no display, not recommended */
  public static FluidContainerIngredient fromIngredient(FluidIngredient ingredient) {
    return new FluidContainerIngredient(ingredient, null);
  }

  /** Creates an instance from a fluid ingredient with a display container */
  public static FluidContainerIngredient fromFluid(FluidObject<?> fluid) {
    return fromIngredient(fluid.ingredient(FluidType.BUCKET_VOLUME), Ingredient.of(fluid));
  }

  @Override
  public boolean test(ItemStack stack) {
    // first, must have a fluid capability
    if (stack.isEmpty()) {
      return false;
    }
    IFluidHandlerItem handler = CapabilityHelper.fluidHandler(stack);
    if (handler == null || handler.getTanks() != 1) {
      return false;
    }
    // second, must contain enough fluid
    FluidStack contained = handler.getFluidInTank(0);
    if (contained.isEmpty() || fluidIngredient.getAmount(contained.getFluid()) != contained.getAmount() || !fluidIngredient.test(contained.getFluid())) {
      return false;
    }
    // so far so good, from this point on we are forced to make copies as we need to try draining, so copy and fetch the copy's cap
    ItemStack copy = stack.copyWithCount(1);
    IFluidHandlerItem copyHandler = CapabilityHelper.fluidHandler(copy);
    if (copyHandler == null) {
      return false;
    }
    // alright, we know it has the fluid, the question is just whether draining the fluid will give us the desired result
    Fluid fluid = copyHandler.getFluidInTank(0).getFluid();
    int amount = fluidIngredient.getAmount(fluid);
    FluidStack drained = copyHandler.drain(amount, FluidAction.EXECUTE);
    // we need an exact match, and we need the resulting container item to be the same as the item stack's container item
    return drained.getFluid() == fluid && drained.getAmount() == amount && ItemStack.matches(stack.getCraftingRemainingItem(), copyHandler.getContainer());
  }

  @Override
  public Stream<ItemStack> getItems() {
    // no container? unfortunately hard to display this recipe so show nothing
    if (display == null) {
      return Stream.of();
    }
    return Stream.of(display.getItems());
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return MantleIngredients.FLUID_CONTAINER.get();
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof FluidContainerIngredient ingredient
           && fluidIngredient.equals(ingredient.fluidIngredient)
           && Objects.equals(display, ingredient.display);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fluidIngredient, display);
  }
}
