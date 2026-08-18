package slimeknights.mantle.data.predicate;

import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.predicate.block.BlockPredicate;
import slimeknights.mantle.data.predicate.block.BlockPropertiesPredicate;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.mantle.data.predicate.damage.DamageTypePredicate;
import slimeknights.mantle.data.predicate.damage.SourceAttackerPredicate;
import slimeknights.mantle.data.predicate.damage.SourceMessagePredicate;
import slimeknights.mantle.data.predicate.entity.BlockAtEntityPredicate;
import slimeknights.mantle.data.predicate.entity.HasEnchantmentEntityPredicate;
import slimeknights.mantle.data.predicate.entity.HasMobEffectPredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.mantle.data.predicate.entity.MobTypePredicate;
import slimeknights.mantle.data.predicate.fluid.FluidPredicate;
import slimeknights.mantle.data.predicate.fluid.FluidTypePredicate;
import slimeknights.mantle.data.predicate.item.ItemPredicate;

/**
 * Registers the names Mantle's own predicates answer to in datapacks.
 * <p>
 * Each {@code *Predicate} interface builds its registry as a static field and each singleton predicate knows its own
 * loader, but neither step gives a predicate a <em>name</em>. Until {@link #registerDefaults()} runs, a
 * {@code {"type": "mantle:requires_tool"}} in a datapack is an unknown type. The registries are plain static state
 * rather than game registries, so this is idempotent-unsafe by nature and is called exactly once, from
 * {@link Mantle}'s {@code RegisterEvent} handler alongside the fluid transfer loaders.
 * <p>
 * Every id below is the id 1.20 registered, in the same order, so no datapack sees a rename. The five
 * {@code mantle:mob_type} sub-names 1.20 registered into a {@code MobTypePredicate.MOB_TYPES} registry are gone:
 * 1.20.5 deleted {@code MobType} and the predicate reads an entity type tag now, which needs no registry of its own.
 * See {@link MobTypePredicate} for what that costs a datapack.
 */
public class MantlePredicates {
  private MantlePredicates() {}

  /** Registers every predicate type Mantle ships under its datapack name. Call once. */
  public static void registerDefaults() {
    // block predicates
    BlockPredicate.LOADER.register(Mantle.getResource("requires_tool"), BlockPredicate.REQUIRES_TOOL.getLoader());
    BlockPredicate.LOADER.register(Mantle.getResource("blocks_motion"), BlockPredicate.BLOCKS_MOTION.getLoader());
    BlockPredicate.LOADER.register(Mantle.getResource("can_be_replaced"), BlockPredicate.CAN_BE_REPLACED.getLoader());
    BlockPredicate.LOADER.register(Mantle.getResource("block_properties"), BlockPropertiesPredicate.LOADER);

    // item predicates
    ItemPredicate.LOADER.register(Mantle.getResource("has_container"), ItemPredicate.HAS_CONTAINER.getLoader());
    ItemPredicate.LOADER.register(Mantle.getResource("may_have_transfer"), ItemPredicate.MAY_HAVE_TRANSFER.getLoader());

    // fluid predicates
    FluidPredicate.LOADER.register(Mantle.getResource("fluid_type"), FluidTypePredicate.LOADER);
    FluidPredicate.LOADER.register(Mantle.getResource("is_source"), FluidPredicate.SOURCE.getLoader());
    FluidPredicate.LOADER.register(Mantle.getResource("has_bucket"), FluidPredicate.HAS_BUCKET.getLoader());
    FluidPredicate.LOADER.register(Mantle.getResource("lighter_than_air"), FluidPredicate.LIGHTER_THAN_AIR.getLoader());

    // entity predicates
    // simple
    LivingEntityPredicate.LOADER.register(Mantle.getResource("fire_immune"), LivingEntityPredicate.FIRE_IMMUNE.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("can_freeze"), LivingEntityPredicate.CAN_FREEZE.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("water_sensitive"), LivingEntityPredicate.WATER_SENSITIVE.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("on_fire"), LivingEntityPredicate.ON_FIRE.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("is_freezing"), LivingEntityPredicate.IS_FREEZING.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("is_in_powdered_snow"), LivingEntityPredicate.IS_IN_POWDERED_SNOW.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("on_ground"), LivingEntityPredicate.ON_GROUND.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("crouching"), LivingEntityPredicate.CROUCHING.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("sprinting"), LivingEntityPredicate.SPRINTING.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("blocking"), LivingEntityPredicate.BLOCKING.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("elytra_flying"), LivingEntityPredicate.ELYTRA_FLYING.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("has_effect"), HasMobEffectPredicate.LOADER);
    LivingEntityPredicate.LOADER.register(Mantle.getResource("block_at_entity"), BlockAtEntityPredicate.LOADER);
    LivingEntityPredicate.LOADER.register(Mantle.getResource("eyes_in_water"), LivingEntityPredicate.EYES_IN_WATER.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("feet_in_water"), LivingEntityPredicate.FEET_IN_WATER.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("underwater"), LivingEntityPredicate.UNDERWATER.getLoader());
    LivingEntityPredicate.LOADER.register(Mantle.getResource("raining_at"), LivingEntityPredicate.RAINING.getLoader());
    // property
    LivingEntityPredicate.LOADER.register(Mantle.getResource("mob_type"), MobTypePredicate.LOADER);
    LivingEntityPredicate.LOADER.register(Mantle.getResource("has_enchantment"), HasEnchantmentEntityPredicate.LOADER);

    // damage predicates
    // simple
    DamageSourcePredicate.LOADER.register(Mantle.getResource("has_entity"), DamageSourcePredicate.HAS_ENTITY.getLoader());
    DamageSourcePredicate.LOADER.register(Mantle.getResource("is_indirect"), DamageSourcePredicate.IS_INDIRECT.getLoader());
    DamageSourcePredicate.LOADER.register(Mantle.getResource("can_protect"), DamageSourcePredicate.CAN_PROTECT.getLoader());
    // fields
    DamageSourcePredicate.LOADER.register(Mantle.getResource("damage_type"), DamageTypePredicate.LOADER);
    DamageSourcePredicate.LOADER.register(Mantle.getResource("message"), SourceMessagePredicate.LOADER);
    DamageSourcePredicate.LOADER.register(Mantle.getResource("attacker"), SourceAttackerPredicate.LOADER);
  }
}
