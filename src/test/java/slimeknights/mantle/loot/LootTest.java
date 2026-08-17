package slimeknights.mantle.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.loot.LootTableInjection.LootPoolInjection;
import slimeknights.mantle.loot.condition.BlockTagLootCondition;
import slimeknights.mantle.loot.condition.HasLootContextSetCondition;
import slimeknights.mantle.loot.entry.TagPreferenceLootEntry;
import slimeknights.mantle.test.LoadableTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the loot subsystem's converged-registry ports: the two conditions and the one pool entry that became a
 * {@link com.mojang.serialization.MapCodec}, and {@link LootTableInjection}'s rebuild around {@code LootPool#entries}
 * being a {@code List} rather than an array.
 */
class LootTest extends LoadableTest {
  /** Parses a JSON string into a {@link JsonElement}, the format every fixture below is written in */
  private static JsonElement parse(String json) {
    return JsonParser.parseString(json);
  }

  @Test
  void blockTagCondition_roundTrips() {
    BlockTagLootCondition condition = new BlockTagLootCondition(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("mantle", "some_blocks")));
    assertCodecRoundTrip(BlockTagLootCondition.CODEC.codec(), condition);
    assertThat(write(BlockTagLootCondition.CODEC.codec(), JsonOps.INSTANCE, condition))
      .isEqualTo(parse("{\"tag\": \"mantle:some_blocks\"}"));
  }

  @Test
  void hasContextSetCondition_roundTrips() {
    HasLootContextSetCondition condition = new HasLootContextSetCondition(LootContextParamSets.CHEST);
    assertCodecRoundTrip(HasLootContextSetCondition.CODEC.codec(), condition);
  }

  @Test
  void tagPreferenceEntry_roundTripsAsJson() {
    // TagPreferenceLootEntry has no equals (a plain LootPoolSingletonContainer, like every vanilla sibling), so this
    // pins the JSON shape directly rather than round tripping an instance, the same way RecipeSerializerTest does
    // for objects with no equality of their own. Tested through the bare entry codec, so no dispatch "type" key -
    // that is LootPoolEntries.CODEC's job - and weight/quality are absent at their defaults, as optionalFieldOf
    // does not write a default back out.
    JsonElement json = parse("{\"tag\": \"minecraft:planks\"}");
    TagPreferenceLootEntry entry = (TagPreferenceLootEntry) success(TagPreferenceLootEntry.CODEC.codec().parse(JsonOps.INSTANCE, json));
    assertThat(write(TagPreferenceLootEntry.CODEC.codec(), JsonOps.INSTANCE, entry)).isEqualTo(json);
  }

  /** Builds a one-pool, one-entry loot table for {@link #injection_appendsIntoExistingPool} */
  private static LootTable oneEntryTable(String poolName, Builder<?> entry) {
    return LootTable.lootTable()
                     .withPool(LootPool.lootPool().name(poolName).setRolls(ConstantValue.exactly(1)).add(entry))
                     .setParamSet(LootContextParamSets.CHEST)
                     .build();
  }

  @Test
  void injection_appendsIntoExistingPool() {
    LootTable table = oneEntryTable("main", EmptyLootItem.emptyItem());
    LootPoolEntryContainer injected = EmptyLootItem.emptyItem().build();
    new LootPoolInjection("main", List.of(injected)).inject(table);

    LootPool pool = table.getPool("main");
    assertThat(pool).isNotNull();
    assertThat(pool.entries).as("the original entry survives, and the injected one is appended").hasSize(2);
    assertThat(pool.entries.get(1)).isSameAs(injected);
  }

  @Test
  void injection_missingPoolDoesNotThrow() {
    LootTable table = oneEntryTable("main", EmptyLootItem.emptyItem());
    // "other" does not exist on this table; the injection should log and return rather than NPE
    new LootPoolInjection("other", List.of(EmptyLootItem.emptyItem().build())).inject(table);
    assertThat(table.getPool("main").entries).hasSize(1);
  }

  @Test
  void addEntryLootModifier_roundTripsAsJson() {
    JsonElement json = parse("""
      {
        "conditions": [],
        "post_conditions": [],
        "entry": {"type": "minecraft:empty"},
        "functions": []
      }
      """);
    AddEntryLootModifier modifier = success(AddEntryLootModifier.CODEC.codec().parse(JsonOps.INSTANCE, json));
    assertThat(write(AddEntryLootModifier.CODEC.codec(), JsonOps.INSTANCE, modifier)).isEqualTo(json);
  }

  @Test
  void replaceItemLootModifier_roundTripsAsJson() {
    // ItemOutput's codec writes the compact bare-name form when it can, same as the tag preference recipe helper
    JsonElement json = parse("""
      {
        "conditions": [],
        "original": {"item": "minecraft:cobblestone"},
        "replacement": "minecraft:stone",
        "functions": []
      }
      """);
    ReplaceItemLootModifier modifier = success(ReplaceItemLootModifier.CODEC.codec().parse(JsonOps.INSTANCE, json));
    assertThat(write(ReplaceItemLootModifier.CODEC.codec(), JsonOps.INSTANCE, modifier)).isEqualTo(json);
  }
}
