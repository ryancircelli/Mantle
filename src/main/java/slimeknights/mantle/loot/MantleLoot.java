package slimeknights.mantle.loot;

import com.google.gson.JsonDeserializer;
import com.mojang.serialization.MapCodec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.loot.condition.BlockTagLootCondition;
import slimeknights.mantle.loot.condition.ContainsItemModifierLootCondition;
import slimeknights.mantle.loot.condition.EmptyModifierLootCondition;
import slimeknights.mantle.loot.condition.HasLootContextSetCondition;
import slimeknights.mantle.loot.condition.ILootModifierCondition;
import slimeknights.mantle.loot.condition.InvertedModifierLootCondition;
import slimeknights.mantle.loot.entry.TagPreferenceLootEntry;
import slimeknights.mantle.loot.function.RetexturedLootFunction;
import slimeknights.mantle.loot.function.SetFluidLootFunction;

import static slimeknights.mantle.loot.condition.ILootModifierCondition.MODIFIER_CONDITIONS;

/**
 * Registration for Mantle's vanilla loot registries: conditions, functions, pool entries, and global loot modifiers.
 * <p>
 * {@link LootItemConditionType}, {@link LootItemFunctionType} and {@link LootPoolEntryType} are each a record wrapping
 * one {@link MapCodec}, as is a global loot modifier's {@code codec()}, so all four go through a
 * {@link DeferredRegister}.
 * <p>
 * The two conditions that are also data pack conditions ({@code tag_empty}/{@code tag_filled}) own their
 * {@code LOOT_TYPE} constant themselves (see {@link slimeknights.mantle.recipe.condition.TagEmptyCondition}) and
 * register it from {@link slimeknights.mantle.recipe.condition.MantleConditions#registerLootConditions(RegisterEvent)},
 * so this class does not re-register them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MantleLoot {
  private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Mantle.modId);

  static {
    GLOBAL_LOOT_MODIFIERS.register("add_entry", () -> AddEntryLootModifier.CODEC);
    GLOBAL_LOOT_MODIFIERS.register("replace_item", () -> ReplaceItemLootModifier.CODEC);
  }

  /** Condition to match a block tag and property predicate */
  public static LootItemConditionType BLOCK_TAG_CONDITION;
  /** Condition for global loot modifiers that ensures a context set is present. Useful to check if we are in a specific context like entity. */
  public static LootItemConditionType HAS_CONTEXT_SET;
  /** Function to add block entity texture to a dropped item */
  public static LootItemFunctionType<RetexturedLootFunction> RETEXTURED_FUNCTION;
  /** Function to add a fluid to an item fluid capability */
  public static LootItemFunctionType<SetFluidLootFunction> SET_FLUID_FUNCTION;
  /** Entry to pull a value from a tag preference */
  public static LootPoolEntryType TAG_PREFERENCE;

  /** Registers this class' deferred registers to the given mod event bus */
  public static void init(IEventBus modBus) {
    GLOBAL_LOOT_MODIFIERS.register(modBus);
    // loot modifier conditions, Mantle's own gson-based mini registry for GLM post_conditions, unrelated to the vanilla registries below
    MODIFIER_CONDITIONS.registerDeserializer(InvertedModifierLootCondition.ID, (JsonDeserializer<? extends ILootModifierCondition>)InvertedModifierLootCondition::deserialize);
    MODIFIER_CONDITIONS.registerDeserializer(EmptyModifierLootCondition.ID, EmptyModifierLootCondition.INSTANCE);
    MODIFIER_CONDITIONS.registerDeserializer(ContainsItemModifierLootCondition.ID, (JsonDeserializer<? extends ILootModifierCondition>)ContainsItemModifierLootCondition::deserialize);
  }

  /**
   * Called during registry registration to register Mantle's vanilla loot condition/function/entry types
   */
  public static void register(RegisterEvent event) {
    ResourceKey<?> key = event.getRegistryKey();
    if (key == Registries.LOOT_FUNCTION_TYPE) {
      RETEXTURED_FUNCTION = registerFunction("fill_retextured_block", RetexturedLootFunction.CODEC);
      SET_FLUID_FUNCTION = registerFunction("set_fluid", SetFluidLootFunction.CODEC);

    } else if (key == Registries.LOOT_CONDITION_TYPE) {
      BLOCK_TAG_CONDITION = Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, Mantle.getResource("block_tag"), new LootItemConditionType(BlockTagLootCondition.CODEC));
      HAS_CONTEXT_SET = Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, Mantle.getResource("has_context_set"), new LootItemConditionType(HasLootContextSetCondition.CODEC));

    } else if (key == Registries.LOOT_POOL_ENTRY_TYPE) {
      TAG_PREFERENCE = Registry.register(BuiltInRegistries.LOOT_POOL_ENTRY_TYPE, Mantle.getResource("tag_preference"), new LootPoolEntryType(TagPreferenceLootEntry.CODEC));
    }
  }

  /**
   * Registers a loot function
   * @param name   Loot function name
   * @param codec  Loot function codec
   * @return  Registered loot function type
   */
  private static <T extends LootItemFunction> LootItemFunctionType<T> registerFunction(String name, MapCodec<T> codec) {
    return Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Mantle.getResource(name), new LootItemFunctionType<>(codec));
  }
}
