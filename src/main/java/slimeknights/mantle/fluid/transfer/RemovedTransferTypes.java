package slimeknights.mantle.fluid.transfer;

import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.Mantle;

/**
 * Fluid transfer type IDs that 1.21 removed, kept registered so a datapack written for 1.20 fails at its {@code type}
 * key with a message naming the replacement, rather than at whichever field the replacement happens to read first.
 * <p>
 * There is deliberately no translation shim. A 1.20 {@code mantle/fluid_transfer} file cannot load on 1.21 whatever
 * these two ids do: its {@code input} is a 1.20 ingredient, a tag-preference {@code fluid} or {@code result} carries
 * an {@code nbt} field that is now {@code components}, and a file level {@code conditions} array is now
 * {@code neoforge:conditions}. Aliasing only the type key would move the failure later and make it harder to read.
 */
public class RemovedTransferTypes {
  private RemovedTransferTypes() {}

  /** 1.20's {@code mantle:empty_nbt}, replaced by {@link EmptyFluidWithComponentsTransfer} */
  public static final ResourceLocation EMPTY_NBT = Mantle.getResource("empty_nbt");
  /** 1.20's {@code mantle:fill_nbt}, replaced by {@link FillFluidWithComponentsTransfer} */
  public static final ResourceLocation FILL_NBT = Mantle.getResource("fill_nbt");

  /** Registers every removed type with {@link FluidContainerTransferManager#TRANSFER_LOADERS} */
  public static void register() {
    register(EMPTY_NBT, EmptyFluidWithComponentsTransfer.ID);
    register(FILL_NBT, FillFluidWithComponentsTransfer.ID);
  }

  /** Registers a single removed type */
  private static void register(ResourceLocation removed, ResourceLocation replacement) {
    FluidContainerTransferManager.TRANSFER_LOADERS.registerDeserializer(removed, (element, type, context) -> {
      throw new JsonSyntaxException(
        "Fluid transfer type " + removed + " was removed in 1.21 as item and fluid stacks no longer have NBT. Use " + replacement + ", which copies data components instead.");
    });
  }
}
