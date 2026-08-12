package slimeknights.mantle.fluid;

import net.neoforged.neoforge.fluids.FluidType;

/**
 * Fluid type whose color and textures are determined by the model.
 * <p>
 * 1.20 had this class implement {@code initializeClient} to hand back a {@code ClientTextureFluidType}, which forced a
 * common class to name a client one. 1.21 deprecated that method in favour of
 * {@link net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent}, so the client half lives
 * entirely on the client side now: see
 * {@link slimeknights.mantle.fluid.texture.ClientTextureFluidType#registerExtensions}.
 */
public class TextureFluidType extends FluidType {
  public TextureFluidType(Properties properties) {
    super(properties);
  }
}
