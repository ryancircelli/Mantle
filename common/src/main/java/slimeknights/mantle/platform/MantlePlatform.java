package slimeknights.mantle.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.network.PacketRegistry;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * The set of operations shared code cannot express against Minecraft alone, implemented once per target.
 * <p>
 * Every method here exists because the same operation is spelled differently on the two targets and there is no
 * expression that compiles on both. That is the only admission criterion: an operation that compiles unchanged on both
 * targets stays in shared code and never appears on this interface.
 */
public interface MantlePlatform {
  /* Identifiers */

  /**
   * Builds an identifier from its two halves.
   * <p>
   * Shared code cannot do this itself: on 1.20.1 the constructor is public and the factory does not exist, on 1.21.1
   * the constructor is private and the factory does. Every identifier a shared class builds comes through here.
   * @param namespace  Namespace, typically a mod ID
   * @param path       Path within the namespace
   * @return  Identifier
   */
  ResourceLocation id(String namespace, String path);

  /**
   * Parses an identifier from its combined string form, throwing if it is malformed.
   * @param id  Combined {@code namespace:path} form
   * @return  Identifier
   */
  ResourceLocation parseId(String id);


  /* Buffers */

  /**
   * Writes an item stack to a buffer, accepting an empty stack.
   * <p>
   * Shared code cannot do this itself: 1.20.1 writes the stack's NBT through {@code FriendlyByteBuf#writeItem} while
   * 1.21.1 writes its data components through a stream codec that needs the connection's registries, which are only
   * reachable from the registry-carrying buffer subtype that does not exist on 1.20.1.
   * @param buffer  Buffer to write to; must be the buffer the transport handed the packet
   * @param stack   Stack to write
   */
  void writeItem(FriendlyByteBuf buffer, ItemStack stack);

  /**
   * Reads an item stack written by {@link #writeItem(FriendlyByteBuf, ItemStack)}.
   * @param buffer  Buffer to read from
   * @return  Stack read
   */
  ItemStack readItem(FriendlyByteBuf buffer);


  /* Networking */

  /**
   * Creates the transport backing a channel. The transport is what actually talks to the loader's networking, which
   * has no common shape: 1.20.1 builds a channel eagerly and registers each message as it arrives, 1.21.1 collects
   * registrations and hands the loader a set of payloads at a fixed point in mod loading.
   * @param channel   Channel identifier
   * @param version   Protocol version both sides must agree on
   * @param registry  Registry the channel's packets are recorded in
   * @return  Transport for the channel
   */
  PacketTransport createTransport(ResourceLocation channel, String version, PacketRegistry registry);


  /* Client */

  /**
   * Swings the given entity's arm without resetting its attack cooldown.
   * <p>
   * Shared code cannot do this itself: the cooldown tracker is attached to the entity through the loader's
   * capability system on 1.20.1 and its data attachment system on 1.21.1, which are different types with different
   * lifetimes.
   * @param entity         Entity to swing
   * @param hand           Hand to swing
   * @param updateTracker  Whether to reset the offhand cooldown
   */
  void swingHand(LivingEntity entity, InteractionHand hand, boolean updateTracker);


  /* Lookup */

  /**
   * The implementation supplied by the running target.
   * <p>
   * Resolved once at class initialization. A missing or duplicated implementation is a packaging error, so it fails
   * immediately rather than at the first call.
   */
  MantlePlatform INSTANCE = load();

  private static MantlePlatform load() {
    Iterator<MantlePlatform> found = ServiceLoader.load(MantlePlatform.class).iterator();
    if (!found.hasNext()) {
      throw new IllegalStateException("No " + MantlePlatform.class.getName() + " implementation on the classpath; the target module is missing or its service file was not packaged");
    }
    MantlePlatform platform = found.next();
    if (found.hasNext()) {
      throw new IllegalStateException("Multiple " + MantlePlatform.class.getName() + " implementations on the classpath: " + platform.getClass().getName() + " and " + found.next().getClass().getName());
    }
    return platform;
  }
}
