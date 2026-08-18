package slimeknights.mantle.recipe.condition;

import com.mojang.serialization.MapCodec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import slimeknights.mantle.Mantle;

/**
 * Registration for Mantle's data pack conditions.
 * <p>
 * A 1.21 condition is a {@link MapCodec} in {@code neoforge:condition_codecs}, where 1.20 had a Forge
 * {@code IConditionSerializer} keyed by an ID the condition returned itself. Two of Mantle's conditions are also
 * {@link net.minecraft.world.level.storage.loot.predicates.LootItemCondition}s, whose type registry is a static vanilla
 * one, so those are registered from {@link #registerLootConditions(RegisterEvent)} rather than deferred.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MantleConditions {
  private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Mantle.modId);

  /** Matches if the passed tag is empty, for any registry */
  public static final ResourceLocation TAG_EMPTY = Mantle.getResource("tag_empty");
  /** Matches if the passed tag has any entries, for any registry */
  public static final ResourceLocation TAG_FILLED = Mantle.getResource("tag_filled");
  /** Matches if the intersection of several tags has any entries */
  public static final ResourceLocation TAG_COMBINATION_FILLED = Mantle.getResource("tag_combination_filled");

  static {
    CONDITIONS.register(TAG_EMPTY.getPath(), () -> TagEmptyCondition.CODEC);
    CONDITIONS.register(TAG_FILLED.getPath(), () -> TagFilledCondition.CODEC);
    CONDITIONS.register(TAG_COMBINATION_FILLED.getPath(), () -> TagCombinationCondition.CODEC);
  }

  /** Registers this to the bus */
  public static void init(IEventBus bus) {
    CONDITIONS.register(bus);
  }

  /**
   * Registers the loot condition types for the two conditions which are also loot conditions.
   * @apiNote  These two lines lived in {@code MantleLoot} in 1.20. They moved here because the conditions own their own
   *           {@link net.minecraft.world.level.storage.loot.predicates.LootItemConditionType} in 1.21, which breaks the
   *           cycle that had {@code recipe.condition} waiting on {@code loot} to be able to name its type.
   */
  public static void registerLootConditions(RegisterEvent event) {
    ResourceKey<?> key = event.getRegistryKey();
    if (key == Registries.LOOT_CONDITION_TYPE) {
      Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, TAG_EMPTY, TagEmptyCondition.LOOT_TYPE);
      Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, TAG_FILLED, TagFilledCondition.LOOT_TYPE);
    }
  }
}
