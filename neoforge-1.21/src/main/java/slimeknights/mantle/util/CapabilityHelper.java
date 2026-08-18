package slimeknights.mantle.util;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Shortcuts for asking a stack or block entity for an item or fluid handler right now.
 * <p>
 * NeoForge's capability system resolves capabilities synchronously - there is no {@code LazyOptional} to thread
 * through the call site anymore, so every method here is a direct query against the block capability or item
 * capability constant. Anything that holds onto a queried capability across ticks - a block entity caching a
 * neighbor's handler to avoid re-querying every tick, for instance - should use
 * {@link net.neoforged.neoforge.capabilities.BlockCapabilityCache} instead of calling these repeatedly, as that
 * cache is what now carries the invalidation listener this class' methods have no equivalent for.
 */
public final class CapabilityHelper {
  private CapabilityHelper() {}

  /**
   * Gets the item handler on the given stack, if any.
   * @param stack  Stack to query
   * @return  Item handler, or null if the stack has none
   */
  @Nullable
  public static IItemHandler itemHandler(ItemStack stack) {
    return Capabilities.ItemHandler.ITEM.getCapability(stack, null);
  }

  /**
   * Gets the item handler on the given block entity, if any.
   * @param blockEntity  Block entity to query
   * @param side         Side to fetch the handler from, or null for the side-agnostic handler
   * @return  Item handler, or null if the block entity has none on that side
   */
  @Nullable
  public static IItemHandler itemHandler(BlockEntity blockEntity, @Nullable Direction side) {
    Level level = blockEntity.getLevel();
    if (level == null) {
      return null;
    }
    return Capabilities.ItemHandler.BLOCK.getCapability(level, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, side);
  }

  /**
   * Gets the fluid handler on the given stack, if any.
   * @param stack  Stack to query
   * @return  Fluid handler, or null if the stack has none
   */
  @Nullable
  public static IFluidHandlerItem fluidHandler(ItemStack stack) {
    return Capabilities.FluidHandler.ITEM.getCapability(stack, null);
  }

  /**
   * Gets the fluid handler on the given block entity, if any.
   * @param blockEntity  Block entity to query
   * @param side         Side to fetch the handler from, or null for the side-agnostic handler
   * @return  Fluid handler, or null if the block entity has none on that side
   */
  @Nullable
  public static IFluidHandler fluidHandler(BlockEntity blockEntity, @Nullable Direction side) {
    Level level = blockEntity.getLevel();
    if (level == null) {
      return null;
    }
    return Capabilities.FluidHandler.BLOCK.getCapability(level, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, side);
  }
}
