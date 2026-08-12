package slimeknights.mantle.test;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;

import javax.annotation.Nullable;

/**
 * Base class for any test needing the vanilla registries, notably anything touching a registry backed loadable.
 * Bootstrapping is only done once per JVM as {@link Bootstrap#bootStrap()} guards itself.
 */
public class BaseMcTest {
  /**
   * Registries available to a test, covering every static registry the bootstrap populates.
   * Datapack registries (enchantments and friends, which moved out of {@link BuiltInRegistries} in 1.21) are not here,
   * as they only exist once a world is loaded.
   */
  protected static RegistryAccess registryAccess() {
    return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
  }

  /* Custom data helpers
   * 1.21 removed a stack's free form tag in favor of data components, and the component a mod's own entries live in is
   * DataComponents.CUSTOM_DATA. These three stand in for ItemStack#setTag, #getTag and #hasTag, which is what the
   * item data tests used to reach for. Note CustomData is copy on write for everyone outside its package, so the two
   * accessors returning the live compound use the deprecated getUnsafe; a test which only reads it, or which writes
   * through it deliberately to build a fixture, is exactly the caller that is safe for.
   */

  /** Stand-in for {@code ItemStack#setTag}. An empty tag removes the component, as {@link CustomData#set} does. */
  protected static ItemStack setCustomData(ItemStack stack, CompoundTag tag) {
    CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    return stack;
  }

  /** Stand-in for {@code ItemStack#getTag}, giving the live compound so a test can compare or mutate it */
  @SuppressWarnings("deprecation")
  @Nullable
  protected static CompoundTag getCustomData(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    return data == null ? null : data.getUnsafe();
  }

  /** Stand-in for {@code ItemStack#getOrCreateTag}, giving the live compound and creating it if the stack has none */
  @SuppressWarnings("deprecation")
  protected static CompoundTag getOrCreateCustomData(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data == null) {
      data = CustomData.of(new CompoundTag());
      stack.set(DataComponents.CUSTOM_DATA, data);
    }
    return data.getUnsafe();
  }

  /** Stand-in for {@code ItemStack#hasTag} */
  protected static boolean hasCustomData(ItemStack stack) {
    return stack.has(DataComponents.CUSTOM_DATA);
  }

  @BeforeAll
  static void setUpRegistries() {
    // FML's junit setup has already set the real version by the time a test runs, and setVersion throws on a second
    // different value, so ask for detection rather than supplying one. This is why there is no TestWorldVersion here:
    // it existed to give a bare JUnit run something to bootstrap against, and neoForge.unitTest removes the need.
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }
}
