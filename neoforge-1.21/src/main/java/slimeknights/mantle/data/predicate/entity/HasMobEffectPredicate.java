package slimeknights.mantle.data.predicate.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;

/**
 * Predicate that checks if an entity has the given mob effect.
 * @apiNote  The effect is a {@link Holder} as that is what {@link LivingEntity#hasEffect(Holder)} takes in 1.21. Unlike
 *           enchantments, mob effects are still a static registry, so the holder comes from {@link BuiltInRegistries}
 *           and {@link Loadables#MOB_EFFECT} is unchanged.
 */
public record HasMobEffectPredicate(Holder<MobEffect> effect) implements LivingEntityPredicate {
  /** Reads the effect ID exactly as {@link Loadables#MOB_EFFECT} does, then wraps it as a holder */
  private static final Loadable<Holder<MobEffect>> EFFECT_HOLDER = Loadables.MOB_EFFECT.flatXmap(BuiltInRegistries.MOB_EFFECT::wrapAsHolder, Holder::value);
  public static final RecordLoadable<HasMobEffectPredicate> LOADER = RecordLoadable.create(EFFECT_HOLDER.requiredField("effect", HasMobEffectPredicate::effect), HasMobEffectPredicate::new);

  @Override
  public boolean matches(LivingEntity living) {
    return living.hasEffect(effect);
  }

  @Override
  public RecordLoadable<? extends IJsonPredicate<LivingEntity>> getLoader() {
    return LOADER;
  }
}
