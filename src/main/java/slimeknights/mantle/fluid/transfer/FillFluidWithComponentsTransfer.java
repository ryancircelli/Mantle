package slimeknights.mantle.fluid.transfer;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

/**
 * Fluid transfer info that fills a fluid into an item, copying the fluid's data components onto the item.
 * <p>
 * This is the 1.21 form of {@code mantle:fill_nbt}, which replaced the result stack's whole NBT compound with the
 * fluid's. It applies the fluid's {@link FluidStack#getComponentsPatch() component patch} over the result instead of
 * replacing, so a result that carries components of its own - a custom name, say - keeps everything the fluid does not
 * itself set. With a plain result the two are the same thing.
 * @see RemovedTransferTypes  for what a datapack written against the old type id sees
 */
public class FillFluidWithComponentsTransfer extends FillFluidContainerTransfer {
  public static final ResourceLocation ID = Mantle.getResource("fill_components");

  public FillFluidWithComponentsTransfer(Ingredient input, ItemOutput filled, FluidIngredient fluid) {
    super(input, filled, fluid);
  }

  @Override
  protected ItemStack getFilled(FluidStack drained) {
    ItemStack filled = super.getFilled(drained);
    filled.applyComponents(drained.getComponentsPatch());
    return filled;
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject json = super.serialize(context);
    json.addProperty("type", ID.toString());
    return json;
  }

  /** Unique loader instance */
  public static final JsonDeserializer<FillFluidWithComponentsTransfer> DESERIALIZER = new Deserializer<>(FillFluidWithComponentsTransfer::new);
}
