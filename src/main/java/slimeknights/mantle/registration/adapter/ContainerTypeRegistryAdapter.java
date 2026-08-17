package slimeknights.mantle.registration.adapter;

import net.minecraft.core.Registry;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.RegisterEvent.RegisterHelper;

@SuppressWarnings("unused")
public class ContainerTypeRegistryAdapter extends RegistryAdapter<MenuType<?>> {
  /** @inheritDoc */
  public ContainerTypeRegistryAdapter(Registry<MenuType<?>> registry, RegisterHelper<MenuType<?>> helper, String modId) {
    super(registry, helper, modId);
  }

  /**
   * Registers a container type
   * @param name     Container name
   * @param factory  Container factory
   * @param <C>      Container type
   * @return  Registry object containing the container type
   */
  public <C extends AbstractContainerMenu> MenuType<C> registerType(IContainerFactory<C> factory, String name) {
    return register(IMenuTypeExtension.create(factory), name);
  }
}
