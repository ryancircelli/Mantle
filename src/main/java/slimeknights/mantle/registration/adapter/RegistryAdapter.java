package slimeknights.mantle.registration.adapter;

import lombok.RequiredArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegisterEvent.RegisterHelper;

import java.util.Objects;

/**
 * A convenience wrapper for vanilla/NeoForge registries, to be used in combination with the {@link RegisterEvent} event.
 * Simply put it allows you to register things by passing (thing, name) instead of having to set the name inline.
 * There also is a convenience variant for items and itemblocks, see {@link ItemRegistryAdapter}.
 * @apiNote Forge's {@code IForgeRegistry} combined read (key lookups) and write (registration) into a single type.
 *          NeoForge splits them: {@link Registry} for reads, and {@link RegisterHelper} - handed out per registry
 *          inside {@link RegisterEvent#register(net.minecraft.resources.ResourceKey, java.util.function.Consumer)} -
 *          for writes. This adapter now wraps both. It also drops the Forge-only single argument constructor that
 *          auto-detected the mod ID from {@code ModLoadingContext}: NeoForge has no equivalent static "currently
 *          loading mod" context, so callers must now always pass their mod ID explicitly.
 */
@RequiredArgsConstructor
public class RegistryAdapter<T> {
  private final Registry<T> registry;
  private final RegisterHelper<T> helper;
  private final String modId;

  /**
   * Construct a resource location that belongs to the given namespace. Usually your mod.
   * @param name  Name for location
   */
  public ResourceLocation getResource(String name) {
    return ResourceLocation.fromNamespaceAndPath(modId, name);
  }

  /**
   * Construct a resource location string that belongs to the given namespace. Usually your mod.
   * @param name  Name for location
   */
  public String resourceName(String name) {
    return modId + ":" + name;
  }

  /**
   * General purpose registration method. Just pass the name you want your thing registered as.
   * @param entry  Entry to register
   * @param name   Registry name
   * @return Registry entry
   */
  public <I extends T> I register(I entry, String name) {
    return this.register(entry, this.getResource(name));
  }

  /**
   * Registers an entry using the name from another entry
   * @param entry  Entry to register
   * @param name   Entry name to copy
   * @param <I>    Value type
   * @return  Registered entry
   */
  public <I extends T> I register(I entry, T name) {
    return this.register(entry, Objects.requireNonNull(registry.getKey(name)));
  }

  /**
   * General purpose backup registration method. In case you want to set a very specific resource location.
   * You should probably use the special purpose methods instead of this.
   * <p>
   * Note: changes the things registry name. Do not call this with already registered objects!
   * @param entry     Entry to register
   * @param location  Registry name
   * @return Registry entry
   */
  public <I extends T> I register(I entry, ResourceLocation location) {
    helper.register(location, entry);
    return entry;
  }
}
