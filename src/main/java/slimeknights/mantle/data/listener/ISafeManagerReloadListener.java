package slimeknights.mantle.data.listener;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.fml.ModLoader;

/**
 * Same as {@link ResourceManagerReloadListener}, but only runs if the mod loader state is valid, used as client
 * resource listeners can cause a misleading crash report if something else throws.
 * @implNote  {@code ModLoader#isLoadingStateValid} became {@link ModLoader#hasErrors()} in NeoForge 1.21, which is the
 *            same question asked the other way round; the loading state enum it also checked is gone.
 */
public interface ISafeManagerReloadListener extends ResourceManagerReloadListener {
  @Override
  default void onResourceManagerReload(ResourceManager resourceManager) {
    if (!ModLoader.hasErrors()) {
      onReloadSafe(resourceManager);
    }
  }

  /**
   * Safely handle a resource manager reload. Only runs if the mod loading state is valid
   * @param resourceManager  Resource manager
   */
  void onReloadSafe(ResourceManager resourceManager);
}
