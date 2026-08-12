package slimeknights.mantle.registration;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.block.entity.MantleHangingSignBlockEntity;
import slimeknights.mantle.block.entity.MantleSignBlockEntity;
import slimeknights.mantle.registration.deferred.BlockEntityTypeDeferredRegister;

/**
 * Various objects registered under Mantle
 * <p>
 * 1.20 exposed these as {@code @ObjectHolder} fields injected after the block entity type registry closed; NeoForge
 * has no {@code @ObjectHolder} equivalent, so they are a small {@link BlockEntityTypeDeferredRegister} of their own,
 * called from {@link Mantle}'s constructor via {@link #init(IEventBus)}.
 */
public class MantleRegistrations {
  private MantleRegistrations() {}

  private static final BlockEntityTypeDeferredRegister BLOCK_ENTITIES = new BlockEntityTypeDeferredRegister(Mantle.modId);

  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MantleSignBlockEntity>> SIGN =
    BLOCK_ENTITIES.register("sign", MantleSignBlockEntity::new, builder -> builder.addAll(MantleSignBlockEntity.buildSignBlocks()));

  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MantleHangingSignBlockEntity>> HANGING_SIGN =
    BLOCK_ENTITIES.register("hanging_sign", MantleHangingSignBlockEntity::new, builder -> builder.addAll(MantleHangingSignBlockEntity.buildSignBlocks()));

  /** Registers this class' deferred registers to the given mod event bus. Called once from {@link Mantle}'s constructor. */
  public static void init(IEventBus modBus) {
    BLOCK_ENTITIES.register(modBus);
  }
}
