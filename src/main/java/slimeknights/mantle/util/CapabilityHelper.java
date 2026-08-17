package slimeknights.mantle.util;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Shortcuts for asking a stack or block entity for an item or fluid handler right now, without threading a
 * {@link net.minecraftforge.common.util.LazyOptional} through the call site for a value that is used immediately.
 * <p>
 * These methods are for point in time queries only: call, get an answer, move on. They resolve the
 * {@code LazyOptional} on the spot and throw it away, so nothing here can notice the capability being invalidated
 * later. Anything that holds onto a queried capability across ticks - a block entity caching a neighbor's handler
 * to avoid re-querying every tick, for instance - needs that invalidation signal to know when to drop its
 * reference, and must keep querying {@link net.minecraftforge.common.capabilities.ICapabilityProvider#getCapability}
 * directly and registering a listener via {@code LazyOptional#addListener}. Do not "simplify" a cache onto this
 * class; it will hold a stale handler after the neighbor unloads or its capability provider swaps.
 * <p>
 * Every method here is the same one line - resolve the provider's {@code LazyOptional} for a fixed capability via
 * {@link LogicHelper#orElseNull(net.minecraftforge.common.util.LazyOptional)} - so the null-or-value contract itself
 * is covered by {@code LogicHelperTest} rather than repeated per overload here.
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
    return LogicHelper.orElseNull(stack.getCapability(ForgeCapabilities.ITEM_HANDLER));
  }

  /**
   * Gets the item handler on the given block entity, if any.
   * @param blockEntity  Block entity to query
   * @param side         Side to fetch the handler from, or null for the side-agnostic handler
   * @return  Item handler, or null if the block entity has none on that side
   */
  @Nullable
  public static IItemHandler itemHandler(BlockEntity blockEntity, @Nullable Direction side) {
    return LogicHelper.orElseNull(blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side));
  }

  /**
   * Gets the fluid handler on the given stack, if any.
   * @param stack  Stack to query
   * @return  Fluid handler, or null if the stack has none
   */
  @Nullable
  public static IFluidHandlerItem fluidHandler(ItemStack stack) {
    return LogicHelper.orElseNull(stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM));
  }

  /**
   * Gets the fluid handler on the given block entity, if any.
   * @param blockEntity  Block entity to query
   * @param side         Side to fetch the handler from, or null for the side-agnostic handler
   * @return  Fluid handler, or null if the block entity has none on that side
   */
  @Nullable
  public static IFluidHandler fluidHandler(BlockEntity blockEntity, @Nullable Direction side) {
    return LogicHelper.orElseNull(blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, side));
  }
}
