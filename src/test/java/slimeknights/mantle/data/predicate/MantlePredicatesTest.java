package slimeknights.mantle.data.predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
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
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the datapack names of Mantle's own predicates.
 * <p>
 * A predicate registry is plain static state, so a missing {@link MantlePredicates#registerDefaults()} line is not a
 * compile error and not a startup error: it is a datapack that stops parsing, months later, in a downstream mod. This
 * test is the guard for that. Each name below is the name 1.20 registered, so a failure here is a format break for
 * every pack in the wild, not a test that needs updating.
 * <p>
 * Nothing here calls {@link MantlePredicates#registerDefaults()}: {@code neoForge.unitTest} loads Mantle as a real
 * mod, so its constructor and {@code RegisterEvent} handler have already run by the time a test does. Calling it
 * again throws {@code Duplicate registration}, which is itself the proof that the game lifecycle reaches it — so this
 * class asserts against the state the running mod left behind rather than a hand-built one.
 */
class MantlePredicatesTest extends BaseMcTest {
  /** Asserts the registry knows the loader under {@code mantle:<name>}, in both directions */
  private static <T> void assertRegistered(GenericLoaderRegistry<IJsonPredicate<T>> registry, String name, RecordLoadable<? extends IJsonPredicate<T>> loader) {
    ResourceLocation id = Mantle.getResource(name);
    assertThat(registry.getName(loader)).as("%s must be registered as %s", loader.getClass().getSimpleName(), id).isEqualTo(id);
  }

  /** Asserts a singleton predicate registered under {@code mantle:<name>} serializes to that name and reads back as itself */
  private static <T> void assertSingleton(GenericLoaderRegistry<IJsonPredicate<T>> registry, String name, IJsonPredicate<T> predicate) {
    assertRegistered(registry, name, predicate.getLoader());
    // every predicate registry is compact, so a fieldless predicate is a bare string in JSON rather than {"type": ...}
    JsonElement json = registry.serialize(predicate);
    assertThat(json).as("a fieldless predicate serializes to its bare name").isEqualTo(new JsonPrimitive("mantle:" + name));
    assertThat(registry.convert(json, "test")).as("and parses back to the same singleton").isSameAs(predicate);
  }

  @Test
  void blockPredicates() {
    assertSingleton(BlockPredicate.LOADER, "requires_tool", BlockPredicate.REQUIRES_TOOL);
    assertSingleton(BlockPredicate.LOADER, "blocks_motion", BlockPredicate.BLOCKS_MOTION);
    assertSingleton(BlockPredicate.LOADER, "can_be_replaced", BlockPredicate.CAN_BE_REPLACED);
    assertRegistered(BlockPredicate.LOADER, "block_properties", BlockPropertiesPredicate.LOADER);
  }

  @Test
  void itemPredicates() {
    assertSingleton(ItemPredicate.LOADER, "has_container", ItemPredicate.HAS_CONTAINER);
    assertSingleton(ItemPredicate.LOADER, "may_have_transfer", ItemPredicate.MAY_HAVE_TRANSFER);
  }

  @Test
  void fluidPredicates() {
    assertRegistered(FluidPredicate.LOADER, "fluid_type", FluidTypePredicate.LOADER);
    assertSingleton(FluidPredicate.LOADER, "is_source", FluidPredicate.SOURCE);
    assertSingleton(FluidPredicate.LOADER, "has_bucket", FluidPredicate.HAS_BUCKET);
    assertSingleton(FluidPredicate.LOADER, "lighter_than_air", FluidPredicate.LIGHTER_THAN_AIR);
  }

  @Test
  void entityPredicates() {
    assertSingleton(LivingEntityPredicate.LOADER, "fire_immune", LivingEntityPredicate.FIRE_IMMUNE);
    assertSingleton(LivingEntityPredicate.LOADER, "can_freeze", LivingEntityPredicate.CAN_FREEZE);
    assertSingleton(LivingEntityPredicate.LOADER, "water_sensitive", LivingEntityPredicate.WATER_SENSITIVE);
    assertSingleton(LivingEntityPredicate.LOADER, "on_fire", LivingEntityPredicate.ON_FIRE);
    assertSingleton(LivingEntityPredicate.LOADER, "is_freezing", LivingEntityPredicate.IS_FREEZING);
    assertSingleton(LivingEntityPredicate.LOADER, "is_in_powdered_snow", LivingEntityPredicate.IS_IN_POWDERED_SNOW);
    assertSingleton(LivingEntityPredicate.LOADER, "on_ground", LivingEntityPredicate.ON_GROUND);
    assertSingleton(LivingEntityPredicate.LOADER, "crouching", LivingEntityPredicate.CROUCHING);
    assertSingleton(LivingEntityPredicate.LOADER, "sprinting", LivingEntityPredicate.SPRINTING);
    assertSingleton(LivingEntityPredicate.LOADER, "blocking", LivingEntityPredicate.BLOCKING);
    assertSingleton(LivingEntityPredicate.LOADER, "elytra_flying", LivingEntityPredicate.ELYTRA_FLYING);
    assertSingleton(LivingEntityPredicate.LOADER, "eyes_in_water", LivingEntityPredicate.EYES_IN_WATER);
    assertSingleton(LivingEntityPredicate.LOADER, "feet_in_water", LivingEntityPredicate.FEET_IN_WATER);
    assertSingleton(LivingEntityPredicate.LOADER, "underwater", LivingEntityPredicate.UNDERWATER);
    assertSingleton(LivingEntityPredicate.LOADER, "raining_at", LivingEntityPredicate.RAINING);
    assertRegistered(LivingEntityPredicate.LOADER, "has_effect", HasMobEffectPredicate.LOADER);
    assertRegistered(LivingEntityPredicate.LOADER, "block_at_entity", BlockAtEntityPredicate.LOADER);
    assertRegistered(LivingEntityPredicate.LOADER, "mob_type", MobTypePredicate.LOADER);
    assertRegistered(LivingEntityPredicate.LOADER, "has_enchantment", HasEnchantmentEntityPredicate.LOADER);
  }

  @Test
  void damagePredicates() {
    assertSingleton(DamageSourcePredicate.LOADER, "has_entity", DamageSourcePredicate.HAS_ENTITY);
    assertSingleton(DamageSourcePredicate.LOADER, "is_indirect", DamageSourcePredicate.IS_INDIRECT);
    assertSingleton(DamageSourcePredicate.LOADER, "can_protect", DamageSourcePredicate.CAN_PROTECT);
    assertRegistered(DamageSourcePredicate.LOADER, "damage_type", DamageTypePredicate.LOADER);
    assertRegistered(DamageSourcePredicate.LOADER, "message", SourceMessagePredicate.LOADER);
    assertRegistered(DamageSourcePredicate.LOADER, "attacker", SourceAttackerPredicate.LOADER);
  }

  /**
   * The reverse lookup {@link #assertRegistered} uses proves a loader is in the registry, not that a datapack naming
   * it parses. This is one end-to-end pass over a field bearing type to close that gap.
   */
  @Test
  void namedTypeParsesFromJson() {
    JsonObject json = new JsonObject();
    json.addProperty("type", "mantle:message");
    json.addProperty("message", "inFire");
    IJsonPredicate<DamageSource> parsed = DamageSourcePredicate.LOADER.convert(json, "test");
    assertThat(parsed).isEqualTo(new SourceMessagePredicate("inFire"));
    assertThat(DamageSourcePredicate.LOADER.serialize(parsed)).as("and writes the same object back").isEqualTo(json);
  }
}
