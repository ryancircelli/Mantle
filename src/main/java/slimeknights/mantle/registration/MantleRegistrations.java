package slimeknights.mantle.registration;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.block.entity.MantleHangingSignBlockEntity;
import slimeknights.mantle.block.entity.MantleSignBlockEntity;
import slimeknights.mantle.command.argument.ResourceOrTagKeyArgument;
import slimeknights.mantle.registration.deferred.ArgumentTypeDeferredRegister;
import slimeknights.mantle.registration.deferred.BlockEntityTypeDeferredRegister;

/**
 * Various objects registered under Mantle, registered to the mod bus from {@link Mantle}'s constructor via
 * {@link #init(IEventBus)}.
 */
public class MantleRegistrations {
  private MantleRegistrations() {}

  private static final BlockEntityTypeDeferredRegister BLOCK_ENTITIES = new BlockEntityTypeDeferredRegister(Mantle.modId);
  private static final ArgumentTypeDeferredRegister ARGUMENT_TYPES = new ArgumentTypeDeferredRegister(Mantle.modId);

  /**
   * Serializer for {@link ResourceOrTagKeyArgument}, which {@code /mantle tag} builds its entry argument from.
   * <p>
   * A command argument type is only usable if its info is in the {@code COMMAND_ARGUMENT_TYPE} registry: that is how
   * the argument survives the command tree the server syncs to a client on login. {@link ArgumentTypeDeferredRegister}
   * does both steps (registry entry, then {@code ArgumentTypeInfos.registerByClass}).
   * <p>
   * The {@code Object} witness on the info's type parameter is arbitrary and is not the registry the argument ends up
   * reading: an argument's registry travels in its {@code Template} and is chosen per command node. The info is
   * looked up by the argument's erased class, so one instance serves every {@code ResourceOrTagKeyArgument<T>}.
   */
  public static final DeferredHolder<ArgumentTypeInfo<?,?>,ResourceOrTagKeyArgument.Info<Object>> RESOURCE_OR_TAG_KEY_ARGUMENT =
    ARGUMENT_TYPES.register("resource_or_tag_key", ResourceOrTagKeyArgument.class, ResourceOrTagKeyArgument.Info<Object>::new);

  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MantleSignBlockEntity>> SIGN =
    BLOCK_ENTITIES.register("sign", MantleSignBlockEntity::new, builder -> builder.addAll(MantleSignBlockEntity.buildSignBlocks()));

  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MantleHangingSignBlockEntity>> HANGING_SIGN =
    BLOCK_ENTITIES.register("hanging_sign", MantleHangingSignBlockEntity::new, builder -> builder.addAll(MantleHangingSignBlockEntity.buildSignBlocks()));

  /** Registers this class' deferred registers to the given mod event bus. Called once from {@link Mantle}'s constructor. */
  public static void init(IEventBus modBus) {
    BLOCK_ENTITIES.register(modBus);
    ARGUMENT_TYPES.register(modBus);
  }
}
