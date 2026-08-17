package slimeknights.mantle.plugin.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.screen.MultiModuleScreen;
import slimeknights.mantle.inventory.MultiModuleContainerMenu;
import slimeknights.mantle.plugin.jei.entity.EntityIngredientHelper;
import slimeknights.mantle.plugin.jei.entity.EntityIngredientRenderer;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipe;

import java.util.Collections;
import java.util.List;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
  @Override
  public ResourceLocation getPluginUid() {
    return Mantle.getResource("jei");
  }

  /**
   * @apiNote  The 4-arg {@code register} (no {@link com.mojang.serialization.Codec}) is deprecated for removal in JEI 19 in
   *           favor of a codec-carrying overload; {@link slimeknights.mantle.recipe.ingredient.EntityIngredient.EntityInput}
   *           only ever exists as a JEI display ingredient (never round-tripped through JSON), so there is no codec worth
   *           writing for it yet. Left suppressed rather than manufacturing one.
   */
  @SuppressWarnings("removal")
  @Override
  public void registerIngredients(IModIngredientRegistration registration) {
    registration.register(MantleJEIConstants.ENTITY_TYPE, Collections.emptyList(), new EntityIngredientHelper(), new EntityIngredientRenderer(16));
  }

  @Override
  public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registry) {
    registry.getCraftingCategory().addExtension(ShapedRetexturedRecipe.class, RetexturableRecipeExtension.INSTANCE);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    registration.addGuiContainerHandler(MultiModuleScreen.class, new MultiModuleContainerHandler());
  }

  private static class MultiModuleContainerHandler<C extends MultiModuleContainerMenu<?>> implements IGuiContainerHandler<MultiModuleScreen<C>> {
    @Override
    public List<Rect2i> getGuiExtraAreas(MultiModuleScreen<C> guiContainer) {
      return guiContainer.getModuleAreas();
    }
  }
}
