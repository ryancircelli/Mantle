package slimeknights.mantle.platform.neoforge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.network.PacketRegistry;
import slimeknights.mantle.platform.MantlePlatform;
import slimeknights.mantle.platform.PacketTransport;
import slimeknights.mantle.util.OffhandCooldownTracker;

/** {@link MantlePlatform} for NeoForge 1.21.1. */
public class NeoForgePlatform implements MantlePlatform {
  @Override
  public ResourceLocation id(String namespace, String path) {
    return ResourceLocation.fromNamespaceAndPath(namespace, path);
  }

  @Override
  public ResourceLocation parseId(String id) {
    return ResourceLocation.parse(id);
  }

  /**
   * {@inheritDoc}
   * <p>
   * The cast is the price of the shared signature. An item stack's wire form needs the connection's registries, which
   * only the play buffer subtype carries, and that subtype has no 1.20.1 spelling so it cannot appear in the shared
   * signature. Every play buffer is one, so the cast holds for every packet on a Mantle channel - but it is a runtime
   * check standing where the compiler used to stand, which is a real loss.
   */
  @Override
  public void writeItem(FriendlyByteBuf buffer, ItemStack stack) {
    // the stack may be empty, and the empty aware codec is the one that matches 1.20.1's writeItem
    ItemStack.OPTIONAL_STREAM_CODEC.encode(registryBuffer(buffer), stack);
  }

  @Override
  public ItemStack readItem(FriendlyByteBuf buffer) {
    return ItemStack.OPTIONAL_STREAM_CODEC.decode(registryBuffer(buffer));
  }

  private static RegistryFriendlyByteBuf registryBuffer(FriendlyByteBuf buffer) {
    if (buffer instanceof RegistryFriendlyByteBuf registry) {
      return registry;
    }
    throw new IllegalStateException("Buffer " + buffer.getClass().getName() + " does not carry the connection's registries; an item stack can only be written to a play buffer");
  }

  @Override
  public PacketTransport createTransport(ResourceLocation channel, String version, PacketRegistry registry) {
    return new NeoForgePacketTransport(channel, version);
  }

  @Override
  public void swingHand(LivingEntity entity, InteractionHand hand, boolean updateTracker) {
    OffhandCooldownTracker.swingHand(entity, hand, updateTracker);
  }
}
