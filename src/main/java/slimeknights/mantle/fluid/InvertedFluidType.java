package slimeknights.mantle.fluid;

import net.neoforged.neoforge.fluids.FluidType;

/**
 * Fluid type adding an extra flipped texture for the in world block.
 * Its client extensions are registered by
 * {@link slimeknights.mantle.fluid.texture.ClientTextureFluidType#registerExtensions}, see {@link TextureFluidType}.
 */
public class InvertedFluidType extends FluidType {
  public InvertedFluidType(Properties properties) {
    super(properties);
  }
}
