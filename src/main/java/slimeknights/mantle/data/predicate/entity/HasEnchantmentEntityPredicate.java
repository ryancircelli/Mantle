package slimeknights.mantle.data.predicate.entity;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

/**
 * Predicate that checks if the given entity has the given enchantment on any of their equipment
 * @apiNote  The enchantment is a {@link Holder} as 1.21 moved enchantments to a datapack registry; reading one needs
 *           the registries, see {@link slimeknights.mantle.data.loadable.common.DynamicRegistryLoadable}.
 */
public record HasEnchantmentEntityPredicate(Holder<Enchantment> enchantment) implements LivingEntityPredicate {
  public static final RecordLoadable<HasEnchantmentEntityPredicate> LOADER = RecordLoadable.create(Loadables.ENCHANTMENT.requiredField("enchantment", HasEnchantmentEntityPredicate::enchantment), HasEnchantmentEntityPredicate::new);

  @Override
  public boolean matches(LivingEntity entity) {
    return EnchantmentHelper.getEnchantmentLevel(enchantment, entity) > 0;
  }

  @Override
  public RecordLoadable<HasEnchantmentEntityPredicate> getLoader() {
    return LOADER;
  }
}
