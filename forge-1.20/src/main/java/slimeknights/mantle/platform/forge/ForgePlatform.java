package slimeknights.mantle.platform.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.network.PacketRegistry;
import slimeknights.mantle.platform.MantlePlatform;
import slimeknights.mantle.platform.PacketTransport;
import slimeknights.mantle.util.OffhandCooldownTracker;

/** {@link MantlePlatform} for Forge 1.20.1. */
public class ForgePlatform implements MantlePlatform {
  @Override
  public ResourceLocation id(String namespace, String path) {
    return new ResourceLocation(namespace, path);
  }

  @Override
  public ResourceLocation parseId(String id) {
    return new ResourceLocation(id);
  }

  @Override
  public void writeItem(FriendlyByteBuf buffer, ItemStack stack) {
    buffer.writeItem(stack);
  }

  @Override
  public ItemStack readItem(FriendlyByteBuf buffer) {
    return buffer.readItem();
  }

  @Override
  public PacketTransport createTransport(ResourceLocation channel, String version, PacketRegistry registry) {
    return new ForgePacketTransport(channel, version);
  }

  @Override
  public void swingHand(LivingEntity entity, InteractionHand hand, boolean updateTracker) {
    OffhandCooldownTracker.swingHand(entity, hand, updateTracker);
  }
}
