package slimeknights.mantle.fluid.transfer;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.ItemOutput;

/**
 * Fluid transfer info that empties a fluid from an item, copying the item's data components onto the fluid.
 * <p>
 * This is the 1.21 form of {@code mantle:empty_nbt}, which copied the item stack's whole NBT compound onto the fluid
 * stack. Neither stack has NBT any more, so what moves across is the item's
 * {@link ItemStack#getComponentsPatch() component patch} - everything about the item that differs from its defaults,
 * which is what the old tag held.
 * @see RemovedTransferTypes  for what a datapack written against the old type id sees
 */
public class EmptyFluidWithComponentsTransfer extends EmptyFluidContainerTransfer {
  public static final ResourceLocation ID = Mantle.getResource("empty_components");

  public EmptyFluidWithComponentsTransfer(Ingredient input, ItemOutput filled, FluidOutput fluid) {
    super(input, filled, fluid);
  }

  @Override
  protected FluidStack getFluid(ItemStack stack) {
    // built from the fluid and amount rather than a copy of the output, so the item's components win outright;
    // the 1.20 form did the same by passing the item's tag straight into the fluid stack constructor
    FluidStack contained = new FluidStack(fluid.get().getFluid(), fluid.getAmount());
    contained.applyComponents(stack.getComponentsPatch());
    return contained;
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject json = super.serialize(context);
    json.addProperty("type", ID.toString());
    return json;
  }

  /** Unique loader instance */
  public static final JsonDeserializer<EmptyFluidContainerTransfer> DESERIALIZER = new Deserializer<>(EmptyFluidWithComponentsTransfer::new);
}
